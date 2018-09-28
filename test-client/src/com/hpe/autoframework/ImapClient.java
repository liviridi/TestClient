package com.hpe.autoframework;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.imap.IMAPReply;
import org.testng.TestException;

/**
 * 
 * Class implements IMAP client.
 *
 */
public class ImapClient implements ProtocolCommandListener {
	
	/**
	 * IMAP client
	 */
	private ExIMAPClient imap_;
	
	/**
	 * Command output
	 */
	private String[] replys_;
	
	/**
	 * log write
	 */
	private BufferedWriter wrt_ = null;

	/**
	 * Constructor
	 */
	public ImapClient() {
		
	}
	
	/**
	 * Connect to remote IMAP server
	 * @param hostname host name of IMAP server
	 * @param port port
	 */
	public void connect(String hostname, int port) {
		imap_= new ExIMAPClient();
		try {
			imap_.connect(hostname, port);
		} catch (IOException exp) {
			imap_ = null;
			throw new TestException("Imap connection(" + hostname + "," + port + ") failed", exp);
		}
	}
	
	/**
	 * Disconnect from IMAP server
	 */
	public void close() {
		if (imap_ != null) {
			try {
				imap_.disconnect();
			} catch (IOException e) {
				// ignore
			}
			imap_ = null;
		}
	}
	
	/**
	 * Send IMAP command with data and expect OK reply
	 * @param command IMAP command
	 * @param data data
	 * @return reply code
	 */
	public int command(String command, String data) {
		return command(command, data, IMAPReply.OK);
	}
	
	/**
	 * Send IMAP command with data and expect specified reply code
	 * @param command IMAP command
	 * @param data data
	 * @param expectedReplycode specified reply code to expect
	 * @return reply code
	 */
	public int command(String command, String data, int expectedReplycode) {
		int replycode = sendCommand(command, data);
		assert replycode == expectedReplycode : command + " failed";
		return replycode;
	}
	
	/**
	 * Send IMAP command and expect OK reply
	 * @param command IMAP command
	 * @return reply code
	 */
	public int command(String command) {
		return command(command, IMAPReply.OK);
	}
	
	/**
	 * 
	 * Send IMAP command and expect specified reply code
	 * @param command IMAP command
	 * @param expectedReplycode specified reply code to expect
	 * @return reply code
	 */
	public int command(String command, int expectedReplycode) {
		int replycode = sendCommand(command, null);
		assert replycode == expectedReplycode : command + " failed";
		return replycode;
	}

	/**
	 * Send IMAP command
	 * @param command IMAP command
	 * @param data data, null if without data
	 * @return reply code
	 */
	public int sendCommand(String command, String data) {
		return sendCommand(command, data, 0);
	}

	/**
	 * Send IMAP command
	 * @param command IMAP command
	 * @param data data, null if without data
	 * @param lengthDelta the delta length to add to the length tag
	 * @return reply code
	 */
	public int sendCommand(String command, String data, int lengthDelta) {
		Path logfile = null;
		logfile = ExProgressFormatter.getEvidenceDirName().resolve(Configure.getConfig("ImapLogFilename"));
		try {
			wrt_ = Files.newBufferedWriter(logfile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException exp) {
			throw new TestException("Term log file " + logfile + " open failed", exp);
		}
		
		int replycode = IMAPReply.BAD;
		replys_ = null;
		try {
			imap_.addProtocolCommandListener(this);
			int index = command.indexOf(' ');
			if (data == null) {
				if (index == -1)
					replycode = imap_.sendCommand(command);
				else
					replycode = imap_.sendCommand(command.substring(0, index), command.substring(index + 1));
			} else {
				if (index == -1)
					replycode = imap_.sendCommand(command + " {" + (imap_.getDataLengh(data) + lengthDelta) + "}");
				else
					replycode = imap_.sendCommand(command.substring(0, index), command.substring(index + 1) + " {" + (imap_.getDataLengh(data) + lengthDelta) + "}");
				replys_ = imap_.getReplyStrings();
				if (IMAPReply.isContinuation(replycode)) {
					replycode = imap_.sendData(data);
				}
			}
			
			replys_ = imap_.getReplyStrings();
			imap_.removeProtocolCommandListener(this);
			
		} catch (IOException exp) {
			throw new TestException("Imap send command(" + command + ") failed", exp);
		} finally {
			try {
				if (wrt_ != null)
					wrt_.close();
			} catch (IOException e) {
				// ignore
			}
		}
		
		return replycode;
	}
	
	/**
	 * Get the reply of IMAP command
	 * @return reply of IMAP command
	 */
	public String[] getReplys() {
		return replys_;
	}

	/**
	 * Log the IMAP command sent
	 * @param evt command event
	 */
	@Override
	public void protocolCommandSent(ProtocolCommandEvent evt) {
		try {
			wrt_.write(evt.getMessage());
		} catch (IOException exp) {
			throw new TestException("Log Imap command failed", exp);
		}
	}

	/**
	 * Log the IMAP reply received
	 * @param evt command event
	 */
	@Override
	public void protocolReplyReceived(ProtocolCommandEvent evt) {
		try {
			wrt_.write(evt.getMessage());
		} catch (IOException exp) {
			throw new TestException("Log Imap command failed", exp);
		}
	}
	
	/**
	 * Set character set
	 * @param charset character set name
	 */
    public void setCharset(String charset) {
    	imap_.setCharset(charset);
    }
    
    /**
     * Get the byte length of data
     * @param data data
     * @return byte length of data
     */
	public int getDataLengh(String data) {
		return imap_.getDataLengh(data);		
	}

	/**
	 * Keep IMAP session alive to avoid timeout
	 * Call this method before/after each test case.
	 */
	public void keepAlive() {
		try {
			imap_.noop();
		} catch (IOException exp) {
			throw new TestException("Imap keepAlive by sending command NOOP failed", exp);
		}
	}
}
