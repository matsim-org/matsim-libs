/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.osm.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache for strings to make sure we don't have multiple
 * string objects with the same text in them, wasting memory.
 * Note that the cache itself keeps regular references to te strings,
 * so the memory is not freed if a String is not use anymore. Thus, the
 * cache should only be used in limited areas, e.g. during parsing of 
 * data, and be disposed afterwards. 
 * 
 * @author mrieser / Senozon AG
 */
/*package*/ class StringCache {
	private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(10000);
	/**
	 * Returns the cached version of the given String. If the strings was
	 * not yet in the cache, it is added and returned as well.
	 *
	 * @param string
	 * @return cached version of string
	 */
	public static String get(final String string) {
		String s = cache.putIfAbsent(string, string);
		if (s == null) {
			return string;
		}
		return s;
	}
}