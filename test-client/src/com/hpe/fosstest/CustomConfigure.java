/**
 * Copyright (C) 2015 Hewlett-Packard Development Company, L.P.  All Rights Reserved.
 */
package com.hpe.fosstest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Properties;

import org.testng.TestException;

/**
 * 
 * Class implements configuration function.
 *
 */
public class CustomConfigure {
	
	/**
	 * Configuration info
	 */
	protected Properties config_;

	/**
	 * Load configuration
	 */
	public Properties loadConfigure(String confPath) {
		config_ = new Properties();
		try(BufferedReader brd = Files.newBufferedReader(FileSystems.getDefault().getPath(confPath), StandardCharsets.UTF_8)) {
			config_.load(brd);
		} catch (IOException exp) {
			throw new TestException("Configure loading failed", exp);
		} finally {
		}
		return config_;
	}
	
	/**
	 * Get configuration value
	 * @param name configuration key name
	 * @return value
	 */
	public String getResource(String name) {
		return config_.getProperty(name);
	}

}
