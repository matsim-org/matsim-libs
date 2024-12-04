
/* *********************************************************************** *
 * project: org.matsim.*
 * XmlUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.utils.io;

/**
 * @author mrieser / Simunto GmbH
 */
public final class XmlUtils {

	private XmlUtils() {
		// static helper class
	}

	/**
	 * Encodes the given string in such a way that it no longer contains
	 * characters that have a special meaning in xml.
	 *
	 * @see <a href="http://www.w3.org/International/questions/qa-escapes#use">http://www.w3.org/International/questions/qa-escapes#use</a>
	 * @param attributeValue
	 * @return String with some characters replaced by their xml-encoding.
	 */
	public static String encodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		int len = attributeValue.length();
		boolean encode = false;
		for (int pos = 0; pos < len; pos++) {
			char ch = attributeValue.charAt(pos);
			if (ch == '<') {
				encode = true;
				break;
			} else if (ch == '>') {
				encode = true;
				break;
			} else if (ch == '\"') {
				encode = true;
				break;
			} else if (ch == '&') {
				encode = true;
				break;
			}
		}
		if (encode) {
			StringBuilder bf = new StringBuilder(attributeValue.length() + 30);
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					bf.append("&lt;");
				} else if (ch == '>') {
					bf.append("&gt;");
				} else if (ch == '\"') {
					bf.append("&quot;");
				} else if (ch == '&') {
					bf.append("&amp;");
				} else {
					bf.append(ch);
				}
			}

			return bf.toString();
		}
		return attributeValue;
	}

	/**
	 * Write encoded attribute value to the given StringBuilder.
	 * This is an optimized version of {@link #encodeAttributeValue(String)}, which does not create any intermediate objects.
	 */
	public static StringBuilder writeEncodedAttributeValue(StringBuilder out, String attributeValue) {

		if (attributeValue == null) {
			// By convention, null values are written as "null" in the xml output.
			out.append("null");
			return out;
		}

		int len = attributeValue.length();

		for (int pos = 0; pos < len; pos++) {
			char ch = attributeValue.charAt(pos);
			switch (ch) {
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '\"' -> out.append("&quot;");
				case '&' -> out.append("&amp;");
				default -> out.append(ch);
			};
		}

		return out;
	}

	/**
	 * Helper function to write an attribute key-value pair to the given StringBuilder.
	 * Note, do not use this for primitive types as these don't need to be encoded and the {@link StringBuilder} has specialized methods fot these.
	 */
	public static StringBuilder writeEncodedAttributeKeyValue(StringBuilder out, String key, String value) {
		out.append(key).append("=\"");
		writeEncodedAttributeValue(out, value);
		out.append("\" ");
		return out;
	}

	public static String encodeContent(final String content) {
		if (content.contains("&") || content.contains("<") || content.contains(">")) {
			return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return content;
	}

}
