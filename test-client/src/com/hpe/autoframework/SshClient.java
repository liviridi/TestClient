package com.hpe.autoframework;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.TestException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

/**
 * SshClient implements client of SSH protocol.
 * It simulates vt100 terminal.
 *
 */
public class SshClient {
	
	/**
	 * Internal prompt
	 */
	private static final String INTERNAL_PROMPT = "[TESTFRAME]>";
	
	/**
	 * Line feed
	 */
	private static final String LINE_FEED = "\n"; 

	/**
	 * Default terminal width
	 */
	private static final int DEFAULT_WIDTH = 197;
	
	/**
	 * Default terminal height
	 */
	private static final int DEFAULT_HEIGHT = 45;

	/**
	 * SSH connection
	 */
	private Connection conn_= null;
	
	/**
	 * SSH session
	 */
	private Session sess_ = null;
	
	/**
	 * Data receiving input stream
	 */
	private InputStream outs_;
	
	/**
	 * Log output stream 
	 */
	private OutputStreamWriter inwrt_;
	
	/**
	 * Log output enable flag
	 */
	private boolean logenabled_;
	
	/**
	 * Response data receiving thread
	 */
	private IoThread outtrd_;
	
	/**
	 * Exit code extraction regular expression
	 */
	private Pattern exitCodePattern_ = Pattern.compile("\\[\\[(\\d+)\\]\\]\n$");
	
	/**
	 * Command output list
	 */
	private List<String> result_ = new ArrayList<String>();
	
	/**
	 * IOException of response data receiving thread
	 */
	private IOException exp_;
	
	/**
	 * Prompt to wait for
	 */
	private volatile String prompt_;
	
	/**
	 * SSH session character set
	 */
	private Charset charset_ = StandardCharsets.UTF_8;
	
	/**
	 * Text wait time out in second
	 */
	private long waitTime_ = 0L;
	
	/**
	 * Terminal width
	 */
	private int width_ = DEFAULT_WIDTH;

	/**
	 * Terminal height
	 */
	private int height_ = DEFAULT_HEIGHT;

	/**
	 * Response data receiving thread
	 * It receives data continuously until disconnection.
	 *
	 */
	private class IoThread extends Thread {
		/**
		 * Receiving buffer size
		 */
		private static final int BUF_SIZE = 1024 * 16; 

		/**
		 * Input stream
		 */
		private InputStream ins_;
		
		/**
		 * Received data queue
		 */
		private Queue<String> que_ = new ConcurrentLinkedQueue<String>();
		
		/**
		 * Prompt receiving lock
		 */
		private Lock lock_ = new ReentrantLock();
		
		/**
		 * Prompt receiving condition variable
		 */
		private Condition hasPromptCond_ = lock_.newCondition();
		
		/**
		 * Prompt received flag
		 */
		private boolean hasPrompt_ = false;
		
		/**
		 * Text to wait for
		 */
		private String[] waitText_;
		
		/**
		 * Time out flag
		 */
		private boolean timeout_;
		
		/**
		 * Constructor
		 * @param ins input stream
		 */
		public IoThread(InputStream ins) {
			ins_ = ins;
		}
		
		/**
		 * Check whether any time out happens
		 * @return true: has time out   false:no time out
		 */
		public boolean checkTimeout() {
			boolean tmout = timeout_;
			timeout_ = false;
			return tmout;
		}
		
		/**
		 * Set expected text to wait for, it will returns if anyone matches
		 * @param text expected text to wait for
		 */
		public void setWaitText(String[] text) {
			lock_.lock();
			waitText_ = text;
			lock_.unlock();
		}
		
		/**
		 * Thread process
		 */
		public void run() {
			char[] buf = new char[BUF_SIZE];
			char[] tmpbuf = new char[BUF_SIZE];
			int dataSize = 0;
			try (InputStreamReader rdr = new InputStreamReader(ins_, charset_)) {
				int num = rdr.read(buf, dataSize, buf.length - dataSize);
				while (num >= 0)
				{
					if (num > 0) {
						if (logenabled_) {
							writeLog(buf, dataSize, num);
						}
						if (dataSize > 0) {
							num += dataSize;
							dataSize = 0;
						}
						dataSize = enque(buf, num, rdr.ready(), tmpbuf);
					}
					num = rdr.read(buf, dataSize, buf.length - dataSize);
				}
			} catch (IOException exp) {
				exp_ = exp;
				// unblock thread invoked getResult()
				lock_.lock();
				try {
					waitText_ = null;
					hasPrompt_ = true;
					hasPromptCond_.signal();						
				} finally {
					lock_.unlock();
				}
			}
		}
		
		/**
		 * Insert the data to received data queue,
		 * data is broken into lines to insert.
		 * Control characters(0x00 ~ 0x31) are removed.
		 * @param buf data buffer
		 * @param size data size
		 * @param hasmoredata whether any more data waits for receiving 
		 * @param tmpbuf internal buffer for data editing 
		 * @return data size remained in buffer
		 * @throws IOException throw when I/O error happens
		 */
		private int enque(char[] buf, int size, boolean hasmoredata, char[] tmpbuf)  throws IOException {
			char c;
			int offset = 0;
			int len = 0;
			for (int i = 0; i < size; i ++) {
				c = buf[i];
				if (c == '\n') {
					tmpbuf[len ++] = c;
					enque(new String(tmpbuf, 0, len));
					len = 0;
					offset = i + 1;
				} else if (c < '\0' || c >= ' ') {
					tmpbuf[len ++] = c;
				}
			}
			if (offset < size) {
				if (hasmoredata) {
					System.arraycopy(buf, offset, buf, 0, size - offset);
					return size - offset;
				} else {
					enque(new String(tmpbuf, 0, len));
				}
			}
			return 0;
		}
		
		/**
		 * Insert the data to received data queue
		 * @param line data to insert
		 * @throws IOException throw when I/O error happens
		 */
		private void enque(String line) throws IOException {
			lock_.lock();
			try {
				que_.add(line);
				if (waitText_ != null) {
					if (checkWaitText(line, waitText_) || isPromptLine(line)) {
						waitText_ = null;
						hasPrompt_ = true;
						hasPromptCond_.signal();						
					}
				} else {
					if (isPromptLine(line)) {
						hasPrompt_ = true;
						hasPromptCond_.signal();
					}
				}
			} finally {
				lock_.unlock();
			}
		}
		
		/**
		 * Check whether any wait text matches
		 * @param line line to check if it contains the text
		 * @param waittext wait text to check
		 * @return true: matches, false: unmatched
		 */
		private boolean checkWaitText(String line, String[] waittext) {
			for (String text: waittext) {
				if (line.contains(text))
					return true;
			}
			
			return false;
		}
		
		/**
		 * 
		 * Get the output text list of command
		 * @return output text list
		 * @return
		 */
		public List<String> getResult() {
			List<String> list = new ArrayList<String>();
			lock_.lock();
			long remain = waitTime_;
			try {
				while (!hasPrompt_) {
					try {
						if (waitTime_ == 0L || waitText_ == null) {
							hasPromptCond_.await();
						} else {
							remain = hasPromptCond_.awaitNanos(remain);
							if (remain <= 0L) {
								timeout_ = true;
								break;
							}
						}
					} catch (InterruptedException e) {
						// ignore
					}
				}
				String line = que_.poll();
				while (line != null) {
					list.add(line);
					line = que_.poll();
				}
				hasPrompt_ = false;
			} finally {
				lock_.unlock();
			}
			return list;
		}
		
		/**
		 * Clear prompt received flag
		 */
		public void clearPromptFlg() {
			lock_.lock();
			try {
				hasPrompt_ = false;
			} finally {
				lock_.unlock();
			}
		}
		
		/**
		 * Check whether I/O error happens
		 * @return true:I/O error happens  false:I/O error doesn't happen
		 */
		public boolean hasIOException() {
			return (exp_ != null);
		}
		
		/**
		 * Get IOException
		 * @return IOException
		 */
		public IOException getIOException() {
			return exp_;
		}
	}

	/**
	 * Constructor
	 */
	public SshClient() {
		logenabled_ = true;
	}

	/**
	 * Constructor
	 * @param logenabled enabling terminal log output, default value is true
	 */
	public SshClient(boolean logenabled) {
		logenabled_ = logenabled;		
	}
	
	/**
	 * Set terminal width
	 * @param width terminal width in character
	 */
	public void setWidth(int width) {
		width_ = width;
	}

	/**
	 * Set terminal height
	 * @param height terminal height in character
	 */
	public void setHeight(int height) {
		height_ = height;
	}

	/**
	 * Set wait time for the expected response
	 * @param waittime wait time for the expected response in second
	 */
	public void setWaitTime(long waittime) {
		waitTime_ = TimeUnit.SECONDS.toNanos(waittime);
	}
	
	/**
	 * Clear wait time to wait for ever
	 */
	public void clearWaitTime() {
		waitTime_ = 0L;
	}
	
	/**
	 * Set character set for the SSH session
	 * @param charsetName character set for the SSH session, default UTF-8
	 */
	public void setCharset(String charsetName) {
		try {
			charset_ = Charset.forName(charsetName);
		} catch (IllegalCharsetNameException exp) {
			throw new TestException("Illegal Charset: " + charsetName, exp);
		} catch (UnsupportedCharsetException exp) {
			throw new TestException("Unsupported Charset: " + charsetName, exp);
		}
	}
	
	/**
	 * Connect to remote host
	 * @param hostname remote host name
	 * @param port remote host port
	 * @param username login user name
	 * @param password login password
	 */
	public void connect(String hostname, int port, String username, String password) {
		conn_ = null;
		sess_ = null;
		try {
			conn_ = new Connection(hostname, port);
			conn_.connect();
			boolean isAuthenticated = conn_.authenticateWithPassword(username, password);
			if (isAuthenticated == false)
				throw new TestException("Authentication failed.");
			sess_ = conn_.openSession();
			sess_.requestPTY("dumb", width_, height_, 0, 0, null);
			sess_.startShell();
			outs_ = sess_.getStdout();
			inwrt_ = new OutputStreamWriter(sess_.getStdin());
			outtrd_ = new IoThread(outs_);
			outtrd_.start();
			// set prompt
			Util.sleep(1);
			outtrd_.clearPromptFlg();
			inwrt_.write("PS1='" + INTERNAL_PROMPT + "'" + LINE_FEED);
			inwrt_.flush();
			result_ = outtrd_.getResult();
			checkIoThreadException();
		} catch(IOException exp) {
			try {
				if (outs_ != null) {
					outs_.close();
					outs_ = null;
				}
				if (inwrt_ != null) {
					inwrt_.close();
					inwrt_ = null;
				}
			} catch (IOException e) {
				// ignore
			}
			if (sess_ != null) {
				sess_.close();
				sess_ = null;
			}
			if (conn_ != null) {
				conn_.close();
				conn_ = null;
			}
			throw new TestException("Ssh connection(" + hostname + "," + port + "," + username + "," + password + ") failed", exp);
		}
	}
	
	/**
	 * Execute command on remote host with input data
	 * @param command command to execute
	 * @param input input data to the command
	 * @return exit code
	 */
	public int command(String command, String input){
		return command(command, 0, input);
	}
	
	/**
	 * Execute command on remote host
	 * @param command command to execute
	 * @return exit code
	 */
	public int command(String command){
		return command(command, 0);
	}
	
	/**
	 * 
	 * Execute command on remote host
	 * @param command command to execute
	 * @param expectExitCode expected exit code
	 * @return exit code
	 */
	public int command(String command, int expectExitCode){
		int exitcode = execCommand(command, null);
		assert exitcode == expectExitCode : "command(" + command + ") failed: exitcode=" + exitcode;
		return exitcode;
	}
	
	/**
	 * 
	 * Execute command on remote host without checking the exit code
	 * @param command command to execute
	 * @return exit code
	 */
	public int commandNoCheck(String command){
		int exitcode = execCommand(command, null);
		return exitcode;
	}
	
	/**
	 * 
	 * Execute command on remote host
	 * @param command command to execute
	 * @param expectExitCode expected exit code
	 * @param input input data to the command
	 * @return exit code
	 */
	public int command(String command, int expectExitCode, String input){
		int exitcode = execCommand(command, input);
		assert exitcode == expectExitCode : "command(" + command + ") failed: exitcode=" + exitcode;
		return exitcode;
	}
	
	/**
	 * 
	 * Execute command on remote host
	 * @param command command to execute
	 * @param input input data to the command
	 * @return exit code
	 */
	public int execCommand(String command, String input){
		int exitcode = 0;
		result_ = null;
		try {
			outtrd_.clearPromptFlg();
			inwrt_.write(command + LINE_FEED);
			inwrt_.flush();
	
			if (input != null) {
				Util.sleep(1);
				inwrt_.write(input);
				inwrt_.flush();
			}
			
			result_ = outtrd_.getResult();
			checkIoThreadException();
			
			if (prompt_ == null) {
				outtrd_.clearPromptFlg();
				inwrt_.write("echo [[$?]]" + LINE_FEED);
				inwrt_.flush();
				
				List<String> retstr = outtrd_.getResult();
				checkIoThreadException();
				exitcode = getExitCode(retstr);
			} else {
				// back to default prompt 
				if (!checkPrompt())
					prompt_ = null;
			}
			
		} catch (IOException exp) {
			throw new TestException("Command " + command + " failed", exp);
		}
		
		return exitcode;
	}

	/**
	 * Check exit code of last command by send echo[[$?]]
	 */
	public void checkExitCode() {
		checkExitCode(0);
	}

	/**
	 * Check exit code of last command by send echo[[$?]]
	 * @param expectExitCode expected exit code
	 */
	public void checkExitCode(int expectExitCode) {
		int exitcode = 0;
		try {
			outtrd_.clearPromptFlg();
			inwrt_.write("echo [[$?]]" + LINE_FEED);
			inwrt_.flush();
	
			List<String> retstr = outtrd_.getResult();
			checkIoThreadException();
			
			exitcode = getExitCode(retstr);
			assert exitcode == expectExitCode;
			
		} catch (IOException exp) {
			throw new TestException("Command echo [[$?]] failed", exp);
		}
	}
	
	/**
	 * Send Ctrl-C(0x30) to remote
	 */
	public void sendCtrlC(){
		send(new String(new byte[] {0x03}), null);
	}
	
	/**
	 * Send one line of text to remote, and clear the wait text
	 * @param text text to send, a LF(0x10) is appended to the text
	 * @return true: received expected text or prompt  false: time out
	 */
	public boolean sendLine(String text){
		return send(text + LINE_FEED, null);
	}
	
	/**
	 * Send one line of text to remote, and wait for the specified text
	 * @param text text to send, a LF(0x10) is appended to the text
	 * @param waitText expected text to wait for
	 * @return true: received expected text or prompt  false: time out
	 */
	public boolean sendLine(String text, String waitText){
		return send(text + LINE_FEED, new String[] {waitText});
	}
	
	/**
	 * Send one line of text to remote, and wait for the specified text
	 * @param text text to send, a LF(0x10) is appended to the text
	 * @param waitText expected text to wait for, it will returns if anyone matches
	 * @return true: received expected text or prompt  false: time out
	 */
	public boolean sendLine(String text, String[] waitText){
		return send(text + LINE_FEED, waitText);
	}
	
	/**
	 * 
	 * Send text to remote, and wait for the specified text
	 * @param text text to send
	 * @param waitText expected text to wait for, it will returns if anyone matches
	 * @return true: received expected text or prompt  false: time out
	 */
	public boolean send(String text, String[] waitText){
		boolean timeout;
		outtrd_.setWaitText(waitText);
		
		try {
			outtrd_.clearPromptFlg();
			inwrt_.write(text);
			inwrt_.flush();
			
			result_ = outtrd_.getResult();
			checkIoThreadException();
			
		} catch (IOException exp) {
			throw new TestException("Send " + text + " failed", exp);
		} finally {
			outtrd_.setWaitText(null);
			timeout = outtrd_.checkTimeout();
		}
		return !timeout;
	}
	
	/**
	 * Send one line of text to remote, do not wait response
	 * @param text text to send, a LF(0x10) is appended to the text
	 */
	public void sendLineNoWait(String text){
		sendNoWait(text + LINE_FEED);
	}
	
	/**
	 * 
	 * Send text to remote, do not wait response
	 * @param text text to send
	 */
	public void sendNoWait(String text) {
		try {
			inwrt_.write(text);
			inwrt_.flush();
		} catch (IOException exp) {
			throw new TestException("Send " + text + " failed", exp);
		}
	}
	
	/**
	 * Open terminal log file to write
	 * @return BufferedWriter object of terminal log file
	 */
	private BufferedWriter getLogWriter() {
		Path logfile = ExProgressFormatter.getEvidenceDirName().resolve(Configure.getConfig("TermLogFilename"));
		try {
			return Files.newBufferedWriter(logfile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException exp) {
			throw new TestException("Term log file " + logfile + " open failed", exp);
		}
	}
	
	/**
	 * Get the exit code of command
	 * @param result output text of command
	 * @return exit code
	 */
	public int getExitCode(List<String> result) {
		for (String line : result) {
			Matcher m = exitCodePattern_.matcher(line);
			if (m.matches()) {
				try {
					return Integer.parseInt(m.group(1));
				} catch (NumberFormatException exp) {
					return -1;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Get the output text list of command
	 * @return output text list
	 */
	public List<String> result() {
		List<String> list = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		for (String line : result_) {
			if (line.endsWith("\n")) {
				sb.append(line.substring(0, line.length() - 1));
				list.add(sb.toString());
				sb = new StringBuffer();
			} else {
				sb.append(line);
			}
		}
		if (sb.length() > 0) {
			list.add(sb.toString());
		}
		return list;
	}
	
	/**
	 * Close SSH connection
	 */
	public void close() {
		if (sess_ != null) {
			sess_.close();
			sess_ = null;
		}
		try {
			if (outtrd_ != null && outtrd_.isAlive()) {
				outtrd_.join();
			}
		} catch (InterruptedException exp) {
			// ignore
		}
		if (conn_ != null) {
			conn_.close();
			conn_ = null;
		}
	}

	/**
	 * Set new prompt to wait for
	 * @param prompt new prompt to wait for
	 */
	public void setPrompt(String prompt) {
		prompt_ = prompt;
	}

	/**
	 * Check whether the line ends with prompt
	 * @param line line to check
	 * @return true: line ends with prompt   false:line doesn't end with prompt
	 */
	private boolean isPromptLine(String line) {
		if (prompt_ != null) {
			return (line.endsWith(prompt_) || line.endsWith(INTERNAL_PROMPT));
		}
		return line.endsWith(INTERNAL_PROMPT);
	}

	/**
	 * Check whether the last line of command output ends with prompt
	 * @return true: last line ends with prompt   false:last line doesn't end with prompt
	 */
	public boolean checkPrompt() {
		String lastline = result_.get(result_.size() - 1);
		if (prompt_ == null) {
			return lastline.endsWith(INTERNAL_PROMPT);
		} else {
			return lastline.endsWith(prompt_);
		}
	}
	
	/**
	 * Write log to terminal log file
	 * @param buf data to write
	 * @param offset offset of the data in buffer
	 * @param len length of the data in buffer
	 * @throws IOException throw when I/O error happens
	 */
	private synchronized void writeLog(char[] buf, int offset, int len) throws IOException {
		BufferedWriter wrt = null;

		try {
			wrt = getLogWriter();
			wrt.write(buf, offset, len);
		} finally { 
			try {
				if (wrt != null)
					wrt.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	/**
	 * Check whether any I/O error happens in the response data receiving thread.
	 * If there's any I/O error, throw TestException
	 */
	private void checkIoThreadException() {
		if (outtrd_ != null) {
			if (outtrd_.hasIOException()) {
				throw new TestException("Ssh shell failed", outtrd_.getIOException());
			}
		}
	}

	/**
	 * Keep SSH session alive to avoid timeout
	 * Call this method before/after each test case.
	 */
	public void keepAlive() {
		command("pwd");
	}
}
