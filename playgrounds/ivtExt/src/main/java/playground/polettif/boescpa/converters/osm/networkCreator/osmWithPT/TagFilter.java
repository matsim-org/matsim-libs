/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.converters.osm.networkCreator.osmWithPT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to easily find out if some OSM entity contains
 * the tags one's interested in or not.
 * 
 * @author mrieser / Senozon AG
 */
public class TagFilter {

	public final static String MATCH_ALL = "*";
	private final Map<String, Set<String>> filters = new HashMap<String, Set<String>>();

	public TagFilter() {
	}

	public void add(final String key, final String value) {
		Set<String> values = this.filters.get(key);
		if (values == null) {
			values = new HashSet<String>();
			this.filters.put(key, values);
		}
		values.add(value);
	}

	/**
	 * @param tags
	 * @return <code>true</code> if at least one of the given tags matches any one of the specified filter-tags.
	 */
	public boolean matches(final Map<String, String> tags) {
		for (Map.Entry<String, Set<String>> e : this.filters.entrySet()) {
			String value = tags.get(e.getKey());
			if (value != null && (e.getValue().contains(value) || e.getValue().contains(MATCH_ALL))) {
				return true;
			}
		}
		return false;
	}

}
