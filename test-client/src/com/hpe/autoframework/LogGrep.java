package com.hpe.autoframework;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.testng.TestException;

/**
 * 
 * Class implements log grep.
 *
 */
public class LogGrep {
	
	/**
	 * Grep log
	 * @param loginfo log info
	 * @param regexPattern regular expression pattern
	 */
	static public void grep(LogInfo loginfo, RegexPattern regexPattern) {
		String filename = loginfo.getLocalFilename();
		
		String line;
		try (FileReader frd = new FileReader(filename); BufferedReader in = new BufferedReader(frd)) {
			line = in.readLine();
			while (line != null) {
				regexPattern.match(filename, line);
			
				line = in.readLine();
			}
		} catch (IOException exp) {
			throw new TestException("File " + filename + " grep failed", exp);
		}
	}
	
	/**
	 * Grep log
	 * @param loginfo log info
	 * @param regexPattern regular expression pattern
	 */
	static public void grep(LogInfo loginfo, RegexPattern regexPattern,int matchcount) {
		String filename = loginfo.getLocalFilename();
		int count = 0;
		String line;
		try (FileReader frd = new FileReader(filename); BufferedReader in = new BufferedReader(frd)) {
			line = in.readLine();
			while (line != null) {
				boolean matched =regexPattern.match(line);
				if(matched){
					count++;
				}
				
				line = in.readLine();
			}
			if(matchcount > 0){
				assert count >= matchcount : "File " + filename + " grep failed";
			}else {
				assert count > 0 : "File " + filename + " grep failed";
			}
			
		} catch (IOException exp) {
			throw new TestException("File " + filename + " grep failed", exp);
		}
	}
}
