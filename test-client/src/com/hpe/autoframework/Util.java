package com.hpe.autoframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * Utility class
 *
 */
public class Util {

	/**
	 * Sleep for the specified second
	 * @param sec sleeping second
	 */
	public static void sleep(int sec) {
		try {
			Thread.sleep(sec * 1000L);
		} catch (InterruptedException e) {
			// ignore
		}
	}
	
	/**
	 * Assert the collection contains the text
	 * @param list collection
	 * @param text text
	 */
	public static void assertContain(Collection<String> list, String text) {
		assert list.contains(text);
	}

	/**
	 * Assert the collection doesn't contain the text
	 * @param list collection
	 * @param text text
	 */
	public static void assertNotContain(Collection<String> list, String text) {
		assert !list.contains(text);
	}
	
	/**
	 * Assert the collection contains the regular expression
	 * @param list collection
	 * @param regex regular expression
	 */
	public static void assertContainByRegex(Collection<String> list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			if (ptn.matcher(line).matches())
				return;
		}
		assert false;
	}
	
	/**
	 * Assert the collection doesn't contain the regular expression
	 * @param list collection
	 * @param regex regular expression
	 */
	public static void assertNotContainByRegex(Collection<String> list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			if (ptn.matcher(line).matches())
				assert false;
		}
	}

	/**
	 * Get value from the line in format of 'keyvalue'
	 * @param list collection
	 * @param key key including the delimiter
	 * @return value
	 */
	public static String getPairValue(Collection<String> list, String key) {
		for (String line : list) {
			if (line.startsWith(key)) {
				return line.substring(key.length()).trim();
			}
		}		
		return null;
	}
	
	/**
	 * Get value by regular expression
	 * @param list collection
	 * @param regex regular expression
	 * @return value
	 */
	public static String getValueByRegex(Collection<String> list, String regex) {
		String[] values = getValuesByRegex(list, regex);
		assert values.length == 1;
		return values[0];
	}
	
	/**
	 * Get value by regular expression
	 * @param list collection
	 * @param regex regular expression
	 * @return value array
	 */
	public static String[] getValuesByRegex(Collection<String> list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			Matcher mt = ptn.matcher(line);
			if (mt.matches()) {
				int grpcount = mt.groupCount();
				String[] value = new String[grpcount];
				for (int i = 0; i < grpcount; i ++)
					value[i] = mt.group(i + 1);
				return value;
			}
		}
		return null;
	}

	/**
	 * Proceed if matches the regular expression
	 * @param list collection
	 * @param regex regular expression
	 * @param proc process to proceed
	 */
	public static void processIfMatchRegex(Collection<String> list, String regex, Proc proc) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			Matcher mt = ptn.matcher(line);
			if (mt.matches()) {
				if (mt.groupCount() == 0)
					proc.process(mt.group(0));
				else
					proc.process(mt.group(1));
				return;
			}
		}
	}
	
	/**
	 * Assert the list contains the text
	 * @param list list
	 * @param text text
	 */
	public static void assertContain(String[] list, String text) {
		for (String line : list) {
			if (line.equals(text))
				return;
		}
		assert false;
	}

	/**
	 * Assert the list doesn't contain the text
	 * @param list list
	 * @param text text
	 */
	public static void assertNotContain(String[] list, String text) {
		for (String line : list) {
			if (line.equals(text))
				assert false;
		}
	}
	
	/**
	 * Assert the list contains the regular expression
	 * @param list list
	 * @param regex regular expression
	 */
	public static void assertContainByRegex(String[] list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			if (ptn.matcher(line).matches())
				return;
		}
		assert false;
	}
	
	/**
	 * Assert the list doesn't contain the regular expression
	 * @param list list
	 * @param regex regular expression
	 */
	public static void assertNotContainByRegex(String[] list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			if (ptn.matcher(line).matches())
				assert false;
		}
	}

	/**
	 * Get value from the line in format of 'keyvalue'
	 * @param list list
	 * @param key key including the delimiter
	 * @return value
	 */
	public static String getPairValue(String[] list, String key) {
		for (String line : list) {
			if (line.startsWith(key)) {
				return line.substring(key.length()).trim();
			}
		}
		assert false;
		return null;
	}
	
	/**
	 * Get value by regular expression
	 * @param list list
	 * @param regex regular expression
	 * @return value
	 */
	public static String getValueByRegex(String[] list, String regex) {
		String[] values = getValuesByRegex(list, regex);
		assert values.length == 1;
		return values[0];
	}
	
	/**
	 * Get value by regular expression
	 * @param list list
	 * @param regex regular expression
	 * @return value array
	 */
	public static String[] getValuesByRegex(String[] list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			Matcher mt = ptn.matcher(line);
			if (mt.matches()) {
				int grpcount = mt.groupCount();
				String[] value = new String[grpcount];
				for (int i = 0; i < grpcount; i ++)
					value[i] = mt.group(i + 1);
				return value;
			}
		}
		assert false;
		return null;
	}
	
	public static List<String> getValuesByRegexfromMutiLine(String[] list, String regex, int index) {
		List<String> value = new ArrayList<String>();
		
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			Matcher mt = ptn.matcher(line);
			if (mt.matches()) {
				int grpcount = mt.groupCount();
				for (int i = 1; i <= grpcount; i++){
					if (i == index){ 
						value.add(mt.group(i));
					}
				}
			}
		}
		assert value.size() != 0;
		return value;
	}
	
	/**
	 * Proceed if matches the regular expression
	 * @param list list
	 * @param regex regular expression
	 * @param proc process to proceed
	 */
	public static void processIfMatchRegex(String[] list, String regex, Proc proc) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			Matcher mt = ptn.matcher(line);
			if (mt.matches()) {
				if (mt.groupCount() == 0)
					proc.process(mt.group(0));
				else
					proc.process(mt.group(1));
				return;
			}
		}
	}
	
	/**
	 * Assert the collection matches the regular expression
	 * @param list collection
	 * @param regex regular expression
	 */
	public static void assertByRegex(Collection<String> list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			assert ptn.matcher(line).matches();
		}
	}
	
	/**
	 * Assert the list contains the regular expression
	 * @param list list
	 * @param regex regular expression
	 */
	public static void assertByRegex(String[] list, String regex) {
		Pattern ptn = Pattern.compile(regex);
		for (String line : list) {
			assert ptn.matcher(line).matches();
		}
	}

	/**
	 * Assert the collection matches the regular expression cyclically
	 * @param list collection
	 * @param regexs regular expression array
	 */
	public static void assertByRegex(Collection<String> list, String[] regexs) {
		Pattern[] ptns = new Pattern[regexs.length];
		for (int i = 0; i < regexs.length; i ++) {
			ptns[i] = Pattern.compile(regexs[i]);
		}
		
		int i = 0;
		for (String line : list) {
			assert ptns[i % ptns.length].matcher(line).matches();
			i ++;
		}
	}
	
	/**
	 * Assert the collection matches the regular expression cyclically
	 * @param list list
	 * @param regexs regular expression array
	 */
	public static void assertByRegex(String[] list, String[] regexs) {
		Pattern[] ptns = new Pattern[regexs.length];
		for (int i = 0; i < regexs.length; i ++) {
			ptns[i] = Pattern.compile(regexs[i]);
		}
		
		int i = 0;
		for (String line : list) {
			assert ptns[i % ptns.length].matcher(line).matches();
			i ++;
		}
	}

	/**
	 * Assert the collection contains all of the regular expression
	 * @param list collection
	 * @param regexs array of regular expressions
	 */
	public static void assertContainByRegex(Collection<String> list, String[] regexs) {
		assertContainByRegex(list.toArray(new String[0]), regexs);
	}

	/**
	 * Assert the list contains all of the regular expressions
	 * @param list list
	 * @param regexs array of regular expressions
	 */
	public static void assertContainByRegex(String[] list, String[] regexs) {
		List<Pattern> ptns = new ArrayList<Pattern>();
		for (String regex : regexs) {
			ptns.add(Pattern.compile(regex));
		}
		
		for (String line : list) {
			Pattern ptn;
			for (int i = 0; i < ptns.size(); i ++) {
				ptn = ptns.get(i);
				if (ptn.matcher(line).matches()) {
					ptns.remove(i);
					i --;
					if (ptns.isEmpty())
						return;
				}
			}
		}
		assert false;
	}
}
