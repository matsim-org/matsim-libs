
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
		if (attributeValue.contains("&") || attributeValue.contains("\"") || attributeValue.contains("<") || attributeValue.contains(">")) {
			return attributeValue.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return attributeValue;
	}

	public static String encodeContent(final String content) {
		if (content.contains("&") || content.contains("<") || content.contains(">")) {
			return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return content;
	}

}
