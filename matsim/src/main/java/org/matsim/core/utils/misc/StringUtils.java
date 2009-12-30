/* *********************************************************************** *
 * project: org.matsim.*
 * StringUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.utils.misc;

/**
 * A helper class with utility functions for <code>String</code>s.
 *
 * @author mrieser
 */
public class StringUtils {

	/**
	 * Splits a string at the specified delimiter into multiple substrings. This
	 * method is similar to <code>String.split()</code>, but does only take a
	 * single char as a delimiter and not a regular expression, making this
	 * method a lot faster than <code>String.split()</code> if no regular
	 * expressions are required.
	 *
	 * The <code>limit</code> parameter controls how many parts will be returned
	 * at most. If the limit is zero, no limit is used and the string is splitted
	 * as many times as possible. Negative values for <code>limit</code> are not
	 * supported. If there are more delimiters than <code>limit</code>, only the
	 * first <em>limit-1</em> ones will be used for splitting the string, the
	 * additional delimiters are contained in the last part returned. Trailing
	 * empty strings are not included in the resulting array.
	 *
	 * @param str The string to be splitted.
	 * @param delimiter A single character where <code>str</code> should be splitted.
	 * @param limit The maximum number of parts returned.
	 * @return Returns an array of strings, each being a substring of
	 * 		<code>str</code>, that were connected by <code>delimiter</code> in the
	 * 		original string <code>str</code>.
	 *
	 * @author mrieser
	 */
	public static String[] explode(final String str, final char delimiter, final int limit) {
		int count = 0;
		int len = str.length();
		int maxPos = 0; // the position of the last non-delimiter char in the string
		int countAtMaxPos = 0;
		int upperLimit = limit-1;
		// count how often the delimiter occurs in the string
		for (int pos = 0; pos < len; pos++) {
			if ((str.charAt(pos) == delimiter) && (limit == 0 || count < upperLimit)) {
				count++;
			} else {
				maxPos = pos;
				countAtMaxPos = count;
			}
		}

		if (count == 0) {
			String[] parts = new String[1];
			parts[0] = str;
			return parts;
		}

		// create a big enough array to hold the result
		String[] parts = new String[countAtMaxPos + 1];
		int startPos = 0;
		count = 0;
		// loop once through the string and always take the substrings between the delimiters
		for (int pos = 0; pos < maxPos; pos++) {
			if ((str.charAt(pos) == delimiter) && (limit == 0 || count < upperLimit)) {
				parts[count] = str.substring(startPos, pos);
				startPos = pos + 1;
				count++;
			}
		}
		/* Add the final part ended by the string, not by a delimiter, to our list.
		 * There is always a final part because of the way maxPos and countAtMaxPos
		 * are derived: maxPos points to a non-delimiter. This means there is always
		 * at least one character between the last delimiter and maxPos, and that's
		 * the part we are still missing... so add it! */
		parts[count] = str.substring(startPos, maxPos+1);

		return parts;
	}

	/**
	 * Splits a string at the specified delimiter into multiple substrings. This
	 * method is similar to <code>String.split()</code>, but does only take a
	 * single char as a delimiter and not a regular expression, making this
	 * method a lot faster than <code>String.split()</code> if no regular
	 * expressions are required.
	 *
	 * @param str The string to be splitted.
	 * @param delimiter A single character where <code>str</code> should be splitted.
	 * @return Returns an array of strings, each being a substring of
	 * 		<code>str</code>, that were connected by <code>delimiter</code> in the
	 * 		original string <code>str</code>.
	 * @author mrieser
	 */
	public static String[] explode(final String str, final char delimiter) {
		return explode(str, delimiter, 0);
	}
}
