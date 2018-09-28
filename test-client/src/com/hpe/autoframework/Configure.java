package com.hpe.autoframework;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 
 * Class implements configuration function.
 *
 */
public class Configure {
	/**
	 * COnfiguration property file
	 */
	private static final String CONFIG_FILENAME = "test.properties";
	
	/**
	 * Configuration info
	 */
	static protected Properties config_;

	/**
	 * Load configuration
	 * @throws Exception 
	 */
	static public void loadConfigure() throws Exception {
		config_ = new Properties();
		try(BufferedReader brd = Files.newBufferedReader(FileSystems.getDefault().getPath(CONFIG_FILENAME), StandardCharsets.UTF_8)) {
			config_.load(brd);
		} catch (IOException exp) {
			throw new Exception("Configure loading failed", exp);
		} finally {
		}
	}
	
	/**
	 * Get configuration value
	 * @param name configuration key name
	 * @return value
	 */
	static public String getConfig(String name) {
		return config_.getProperty(name);
	}

	/**
	 * Get configuration value as integer
	 * @param name configuration key name
	 * @return value as integer
	 */
	static public int getConfigAsInt(String name) {
		int ival = -1;
		String value = getConfig(name);
		if (value != null) {
			try {
				ival = Integer.parseInt(value);
			} catch (NumberFormatException exp) {
				// ignore
			}
		}
		return ival;
	}
	
	/**
	 * Get configuration value as boolean
	 * @param name configuration key name
	 * @return value as boolean
	 */
	static public boolean getConfigAsBoolean(String name) {
		boolean val = false;
		String value = getConfig(name);
		if (value != null) {
			val = Boolean.parseBoolean(value);
		}
		return val;
	}
}
