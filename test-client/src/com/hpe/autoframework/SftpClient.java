package com.hpe.autoframework;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.testng.TestException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * SFTP Client implements client of SFTP protocol.
 *
 */
public class SftpClient {
	
	/**
	 * Max retry times when the log file is not found
	 */
	private static final int LOG_POLLING_RETRY = 3;

	/**
	 * File download buffer size
	 */
	private static final int BUF_SIZE = 1024 * 4;
	
	/**
	 * Log file finger-print size
	 */
	private static final int FINGER_PRINT_SIZE = 1024; 
	
	/**
	 * JSch object
	 */
	private JSch ftps_ = new JSch();
	
	/**
	 * SSH session
	 */
	private Session session_;
	
	/**
	 * SFTP channel
	 */
	private ChannelSftp channel_;
	
	/**
	 * Remote SFTP server host name
	 */
	private String hostname_;
	
	/**
	 * Flag specifies whether down-loaded local file has host name prefix or not
	 */
	private boolean localFileWithHostNamePrefix_ = false;
	
	/**
	 * Host name prefix to add to local file name
	 */
	private String hostNamePrefix_;
	
	/**
	 * Constructor
	 */
	public SftpClient() {
		
	}
	
	/**
	 * Set the flag specifies whether down-loaded local file has host name prefix or not
	 * @param localFileWithHostNamePrefix flag specifies whether down-loaded local file has host name prefix or not
	 */
	public void setLocalFileWithHostNamePrefix(boolean localFileWithHostNamePrefix) {
		localFileWithHostNamePrefix_ = localFileWithHostNamePrefix;
	}
	
	/**
	 * Set the host name prefix to add to local file name
	 * @param hostNamePrefix host name prefix to add to local file name
	 */
	public void setHostNamePrefix(String hostNamePrefix) {
		hostNamePrefix_ = hostNamePrefix;
	}
	
	/**
	 * Connect to remote SFTP server
	 * @param hostname remote host name
	 * @param port remote host port
	 * @param username login user name
	 * @param password login password
	 */
	public void connect(String hostname, int port, String username, String password) {
		hostname_ = hostname;
		
		try {
			session_ = ftps_.getSession(username, hostname_, port);
			session_.setPassword(password);
			session_.setConfig("StrictHostKeyChecking", "no");
			session_.connect();
			channel_ = (ChannelSftp)session_.openChannel("sftp");
			channel_.connect();
		} catch (JSchException exp) {
			close();
			throw new TestException("Sftp connection(" + hostname + "," + port + "," + username + "," + password + ") failed", exp);
		}
	}
	
	/**
	 * Download specified file from remote host
	 * @param filename file to download
	 */
	public void getFile(String filename) {
		byte[] buf = new byte[BUF_SIZE];
		int byteread;
		OutputStream out = null;
		InputStream in = null;
		try {
			in = channel_.get(filename);
			out = Files.newOutputStream(getLocalFilePath(filename), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			byteread = in.read(buf, 0, buf.length);
			while (byteread >= 0) {
				if (byteread > 0)
					out.write(buf, 0, byteread);
				byteread = in.read(buf, 0, buf.length);
			}
		} catch (SftpException exp) {
			throw new TestException("Get file" + filename + " failed", exp);
		} catch (IOException exp) {
			throw new TestException("Get file" + filename + " failed", exp);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException exp) {
				// ignore
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException exp) {
				// ignore
			}
		}
	}

	/**
	 * Upload file to remote host
	 * @param localfilename local file to upload
	 * @param remotefilename remote file to be generated
	 */
	public void putFile(String localfilename, String remotefilename) {
		InputStream in = null;
		try {
			in = Files.newInputStream(FileSystems.getDefault().getPath(localfilename), StandardOpenOption.READ);
			channel_.put(in, remotefilename);
		} catch (SftpException exp) {
			throw new TestException("Put file " + localfilename + " as " + remotefilename + " failed", exp);
		} catch (IOException exp) {
			throw new TestException("Put file " + localfilename + " as " + remotefilename + " failed", exp);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException exp) {
				// ignore
			}
		}
	}

	/**
	 * Close SFTP connection to remote host
	 */
	public void close() {
		if (channel_ != null) {
			channel_.disconnect();;
			channel_ = null;
		}
		if (session_ != null) {
			session_.disconnect();;
			session_ = null;
		}
	}
	
	/**
	 * Create local file name by replacing all the separator '/' with '_'.
	 * And remove leading '_' if it exists.  
	 * @param filename file name path include directory on remote server
	 * @return local file name to create
	 */
	static private String createLocalFilename(String filename) {
		String localfilename = filename.replace('/', '_');
		if (localfilename.startsWith("_")) {
			localfilename = localfilename.substring(1);
		}
		
		return localfilename;
	}
	
	/**
	 * Get the information of the specified log file.
	 * The information includes the size and finger-print of the log file.
	 * @param filename log file name
	 * @param pattern rotated log file name matching pattern in regular expression
	 * @return information of the specified log file
	 */
	public LogInfo getLoginfo(String filename) {
		return getLoginfo(filename, null);
	}
	
	/**
	 * Get the information of the specified log file.
	 * The information includes the size and finger-print of the log file.
	 * @param filename log file name
	 * @param pattern rotated log file name matching pattern in regular expression
	 *        the default pattern is logfilename.*
	 * @return information of the specified log file
	 */
	public LogInfo getLoginfo(String filename, String pattern) {
		LogInfo loginfo;
		if (pattern == null)
			loginfo = new LogInfo(filename);
		else
			loginfo = new LogInfo(filename, pattern);
		// set local file name into LogInfo object
		loginfo.setLocalFilename(getLocalFile(loginfo.getFilename()));
		
		int retrytimes = 0;
		for (;;) {
			int byteread;
			InputStream in = null;
			try {
				// get file size
				SftpATTRS attrs = channel_.stat(loginfo.getFilename());
				loginfo.setSize_(attrs.getSize());
				int size;
				if (attrs.getSize() < FINGER_PRINT_SIZE)
					size = (int)attrs.getSize();
				else
					size = FINGER_PRINT_SIZE;
				byte[] buf = new byte[size];
				long fileoff = attrs.getSize() - size;
				loginfo.setFingerPrintOffset(fileoff);
	
					try {
						in = channel_.get(loginfo.getFilename(), null, fileoff);
					} catch (SftpException exp) {
						throw exp;
					}
					
				// read finger print
				int index = 0;
				byteread = in.read(buf, index, buf.length - index);
				while (byteread >= 0) {
					index += byteread;
					if (index >= buf.length) {
						break;
					}
					byteread = in.read(buf, index, buf.length - index);
				}
				loginfo.setFingerPrint(buf);
			} catch (SftpException exp) {
				// if log file not found, it might be rotated, retry
				if (exp instanceof SftpException) {
					if (exp.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
						// retry over max
						if (retrytimes < LOG_POLLING_RETRY) {
							retrytimes ++;
							Util.sleep(1);
							continue;
						}
					}
				}
				throw new TestException("Get file" + loginfo.getFilename() + " failed", exp);
			} catch (IOException exp) {
				throw new TestException("Get file" + loginfo.getFilename() + " failed", exp);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException exp) {
					// ignore
				}
			}
			break;
		}
		return loginfo;
	}
	
	/**
	 * Download the diff part of the log file between the invocation of getLoginfo() and getLogFile().
	 * @param loginfo information of the specified log file
	 */
	@SuppressWarnings("rawtypes")
	public void getLogFile(LogInfo loginfo) {
		if (!tryGetLogFile(loginfo, null)) {
			// get current log file
			getFile(loginfo.getFilename());
			
			// get rotated log file
			String dirname = loginfo.getDirname();
			if (dirname != null) {
				List<LsEntry> sortedlist = new ArrayList<LsEntry>();
				try {
					Vector dirs = channel_.ls(dirname);
					LsEntry dir;
					for (int i = 0; i < dirs.size(); i ++) {
						dir = (LsEntry)dirs.get(i);
						if (loginfo.getFilenamePattern().matcher(dirname + "/" + dir.getFilename()).matches() && !loginfo.getFilename().equals(dirname + "/" + dir.getFilename())) {
							addFilename(dir, sortedlist);
						}
					}
				} catch (SftpException exp) {
					throw new TestException("List directory" + dirname + " failed", exp);
				}
				
				for (LsEntry dir : sortedlist) {
					String filename = dirname + "/" + dir.getFilename();
					if (tryGetLogFile(loginfo, filename)) {
						break;
					} else {
						getFile(filename);
					}
				}
			}
		}
	}
	
	/**
	 * Add the file name entry to the sorted list in order of modified time
	 * @param dir file name entry to add
	 * @param sortedlist sorted list in order of modified time
	 */
	private void addFilename(LsEntry dir, List<LsEntry> sortedlist) {
		LsEntry dirEntry;
		int size = sortedlist.size();
		for (int i = 0; i < size; i ++) {
			dirEntry = sortedlist.get(i);
			if (dir.getAttrs().getMTime() >= dirEntry.getAttrs().getMTime()) {
				sortedlist.add(i, dir);
				return;
			}
		}
		sortedlist.add(dir);
	}
	
	/**
	 * Try to download the rotated log file.
	 * Check the size and finger-print to judge whether it's the rotated log file.
	 * @param loginfo information of the specified log file
	 * @param filename rotated log file name
	 * @return true:found the rotated log file  false:not found
	 */
	private boolean tryGetLogFile(LogInfo loginfo, String filename) {
		String filenm = filename == null ? loginfo.getFilename() : filename;
		
		int byteread;
		OutputStream out = null;
		InputStream in = null;
		byte[] buf = new byte[BUF_SIZE];
		try {
			// get file size
			SftpATTRS attrs = channel_.stat(filenm);
			if (attrs.getSize() < loginfo.getSize()) {
				// log file got smaller, it must be rotated
				return false;
			}
			
			int size = loginfo.getFingerPrint().length;
			
			in = channel_.get(filenm, null, loginfo.getFingerPrintOffset());

			// read finger print
			int index = 0;
			byteread = in.read(buf, index, size - index);
			while (byteread >= 0) {
				index += byteread;
				if (index >= size) {
					break;
				}
				byteread = in.read(buf, index, size - index);
			}
			if (index < size) {
				// failed in getting the whole finger print, file must be rotated
				return false;
			}
			if (!Arrays.equals(loginfo.getFingerPrint(), Arrays.copyOf(buf, loginfo.getFingerPrint().length))) {
				// finger print not match, file must be rotated
				return false;
			}
			
			// get the appended part of log file
			out = Files.newOutputStream(getLocalFilePath(filenm), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			byteread = in.read(buf, 0, buf.length);
			while (byteread >= 0) {
				if (byteread > 0)
					out.write(buf, 0, byteread);
				byteread = in.read(buf, 0, buf.length);
			}
		} catch (SftpException exp) {
			throw new TestException("Get file" + filenm + " failed", exp);
		} catch (IOException exp) {
			throw new TestException("Get file" + filenm + " failed", exp);
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException exp) {
				// ignore
			}
		}
		return true;
	}
	
	/**
	 * Get the local file name of the down-loaded log file
	 * @param filename down-loaded log file name
	 * @return local file name
	 */
	public String getLocalFile(String filename) {
		return getLocalFilePath(filename).toString();
	}
	
	/**
	 * Get the local file name of the down-loaded log file
	 * @param filename down-loaded log file name
	 * @return local file name path
	 */
	private Path getLocalFilePath(String filename) {
		if (localFileWithHostNamePrefix_) {
			
			if (hostNamePrefix_ == null)
				return ExProgressFormatter.getEvidenceDirName().resolve(hostname_ + "_" + createLocalFilename(filename));
			else
				return ExProgressFormatter.getEvidenceDirName().resolve(hostNamePrefix_ + "_" + createLocalFilename(filename));
			
		} else {
			return ExProgressFormatter.getEvidenceDirName().resolve(createLocalFilename(filename));
		}
	}
	
	/**
	 * Keep SFTP session alive to avoid timeout
	 * Call this method before/after each test case.
	 */
	public void keepAlive() {
		try {
			channel_.ls(".");
		} catch (SftpException exp) {
			throw new TestException("List current directory failed", exp);
		}
	}
}
