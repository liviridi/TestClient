package com.hpe.autoframework;

import java.util.regex.Pattern;

/**
 * 
 * Regular expression pattern
 *
 */
public class RegexPattern {
	
	/**
	 * Normal pattern
	 * False means anti pattern
	 */
	private boolean normalPattern_ = true;
	
	/**
	 * Pattern
	 */
	private Pattern pattern_;
	
	/**
	 * Next regular expression pattern in the pipeline
	 */
	private RegexPattern next_;
	
	
	/**
	 * Constructor
	 * @param regex regular expression
	 */
	public RegexPattern(String regex) {
		pattern_ = Pattern.compile(regex);
	}

	/**
	 * Constructor
	 * @param regex regular expression
	 * @param normalPattern true: normal pattern, false: anti pattern
	 */
	public RegexPattern(String regex, boolean normalPattern) {
		pattern_ = Pattern.compile(regex);
		normalPattern_= normalPattern;
	}

	/**
	 * Constructor
	 * @param regex regular expression
	 * @param next next regular expression in the pipeline
	 */
	public RegexPattern(String regex, RegexPattern next) {
		next_ = next;
		pattern_ = Pattern.compile(regex);
	}

	/**
	 * Constructor
	 * @param regex regular expression
	 * @param normalPattern true: normal pattern, false: anti pattern
	 * @param next next regular expression in the pipeline
	 */
	public RegexPattern(String regex, boolean normalPattern, RegexPattern next) {
		next_ = next;
		pattern_ = Pattern.compile(regex);
		normalPattern_= normalPattern;
	}
	
	/**
	 * Match with the specified file
	 * @param filename file to match
	 * @param line line
	 */
	void match(String filename, String line) {
		boolean matched = pattern_.matcher(line).matches();
		if (next_ == null) {
			if (normalPattern_) {
				assert matched : filename + " : " + line;
			} else {
				assert (!matched) : filename + " : " + line;
			}
		} else {
			if (normalPattern_) {
				if (matched)
					next_.match(filename, line);
			} else {
				if (!matched)
					next_.match(filename, line);
			}
		}
	}
	/**
	 * Match with the specified file
	 * @param filename file to match
	 * @param line line
	 */
	public boolean match(String line) {
		return pattern_.matcher(line).matches();
	}
}
