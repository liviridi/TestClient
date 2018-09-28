package com.hpe.autoframework;

import java.util.regex.Pattern;

/**
 * 
 * Log information includes log file name, finger-print, file size, etc.
 *
 */
public class LogInfo {
	
	/**
	 * Log file size
	 */
	protected long size_;

	/**
	 * Finger-print offset
	 */
	protected long fingerPrintOffset_;
	
	/**
	 * Log file name
	 */
	protected String filename_;
	
	/**
	 * Finger-print
	 */
	protected byte[] fingerPrint_;
	
	/**
	 * Rotated log file name pattern in regular expression
	 */
	protected Pattern filenamePattern_;

	/**
	 * Down-loaded local log file name
	 */
	protected String localFilename_;
	
	/**
	 * Constructor with default rotated log file name pattern (logfilename.*)
	 * @param filename log file name
	 */
	public LogInfo(String filename) {
		filename_ = filename;
		filenamePattern_ = Pattern.compile("^" + filename.replace(".", "\\.") + ".*$");
	}
	
	/**
	 * Constructor with specified rotated log file name pattern
	 * @param filename log file name
	 * @param pattern rotated log file name pattern in regular expression
	 */
	public LogInfo(String filename, String pattern) {
		filename_ = filename;
		filenamePattern_ = Pattern.compile("^" + pattern + "$");
	}

	/**
	 * Get log file size
	 * @return log file size
	 */
	public long getSize() {
		return size_;
	}

	/**
	 * Set log file size
	 * @param size log file size
	 */
	public void setSize_(long size) {
		size_ = size;
	}

	/**
	 * Get log file name
	 * @return log file name
	 */
	public String getFilename() {
		return filename_;
	}

	/**
	 * Set log file name
	 * @param filename log file name
	 */
	public void setFilename(String filename) {
		filename_ = filename;
	}

	/**
	 * Get finger-print of the log file
	 * Finger-print is the last several kilobytes of the log file.
	 * @return finger-print finger-print
	 */
	public byte[] getFingerPrint() {
		return fingerPrint_;
	}

	/**
	 * Set finger-print of the log file
	 * @param fingerPrint finger-print
	 */
	public void setFingerPrint(byte[] fingerPrint) {
		fingerPrint_ = fingerPrint;
	}
	
	/**
	 * Get the directory path of log file
	 * @return directory path of log file
	 */
	public String getDirname() {
		int index = filename_.lastIndexOf('/');
		if (index != -1)
			return filename_.substring(0, index);
		return null;
	}
	
	/**
	 * Get rotated log file name pattern in regular expression
	 * @return rotated log file name pattern
	 */
	public Pattern getFilenamePattern() {
		return filenamePattern_;
	}

	/**
	 * Get finger-print offset of log file
	 * @return finger-print offset
	 */
	public long getFingerPrintOffset() {
		return fingerPrintOffset_;
	}

	/**
	 * Set finger-print offset of log file
	 * @param fingerPrintOffset finger-print offset
	 */
	public void setFingerPrintOffset(long fingerPrintOffset) {
		fingerPrintOffset_ = fingerPrintOffset;
	}

	/**
	 * Get down-loaded local log file name
	 * @return down-loaded local log file name
	 */
	public String getLocalFilename() {
		return localFilename_;
	}

	/**
	 * Set down-loaded local log file name
	 * @param localFilename down-loaded local log file name
	 */
	public void setLocalFilename(String localFilename) {
		localFilename_ = localFilename;
	}
}
