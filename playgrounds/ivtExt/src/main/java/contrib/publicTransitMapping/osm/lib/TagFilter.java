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

package contrib.publicTransitMapping.osm.lib;

import org.matsim.core.utils.collections.MapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to easily find out if some OSM entity contains
 * the key-value pairs or tags (with all possible values) one is
 * interested in or not.
 * 
 * @author mrieser / Senozon AG
 *
 * Slightly adapted, exceptions are possible.
 */
public class TagFilter {

	public final static String MATCH_ALL = "*";
	private final Map<String, Set<String>> keyValuePairs = new HashMap<>();
	private final Map<String, Set<String>> keyValueExceptions = new HashMap<>();

	public TagFilter() {
	}

	/**
	 *
	 * @param key tag name
	 * @param value <code>null</code> if all values should be taken
	 */
	public void add(final String key, final String value) {
		if(value == null) {
			keyValuePairs.put(key, null);
		} else {
			Set<String> values = MapUtils.getSet(key, keyValuePairs);
			values.add(value);
		}
	}

	public void add(final String key) {
		add(key, null);
	}

	/**
	 * @param tags key,value pairs
	 * @return <code>true</code> if at least one of the given tags matches any one of the specified filter-tags.
	 */
	public boolean matches(final Map<String, String> tags) {
		// check for exceptions
		for (Map.Entry<String, Set<String>> e : keyValueExceptions.entrySet()) {
			if(tags.containsKey(e.getKey()) && e.getValue() == null) {
				return false;
			}
			String value = tags.get(e.getKey());
			if (value != null && (e.getValue().contains(value) || e.getValue().contains(MATCH_ALL))) {
				return false;
			}
		}

		// check for positive list
		for (Map.Entry<String, Set<String>> e : keyValuePairs.entrySet()) {
			if(tags.containsKey(e.getKey()) && e.getValue() == null) {
				return true;
			}
			String value = tags.get(e.getKey());
			if (value != null && (e.getValue().contains(value) || e.getValue().contains(MATCH_ALL))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds an exception to the filter, that is if this key and value appears,
	 * the filter will return false
	 */
	public void addException(final String key, final String value) {
		if(value == null) {
			keyValueExceptions.put(key, null);
		} else {
			Set<String> values = MapUtils.getSet(key, keyValueExceptions);
			values.add(value);
		}
	}

	public void addException(final String key) {
		addException(key, null);
	}
}
