/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.matsim.api.core.v01.network.Link;

import java.util.List;
import java.util.Map;

/**
 * Copies selected OSM tag values onto link attributes, prefixed with a fixed
 * string. Used by {@link BicycleLinkPolicy} to forward raw OSM tags that the
 * {@link org.matsim.contrib.osm.networkReader.OsmBicycleReader} doesn't write
 * itself but that downstream consumers want to see (typically under the
 * {@code "osm:"} prefix).
 *
 * <p>Configured with a list of OSM keys and the target prefix; instances are
 * immutable and reusable across many links. Empty or missing values are
 * skipped, so an empty key list makes {@link #copy} a no-op.
 *
 * @author smetzler
 */
public final class TagCopy {
	private final List<String> keys;
	private final String prefix;

	public TagCopy(List<String> keys, String prefix) {
		this.keys = keys;
		this.prefix = prefix;
	}

	public void copy(Link link, Map<String, String> tags) {
		for (String k : keys) {
			String v = tags.get(k);
			if (v != null && !v.isBlank()) {
				link.getAttributes().putAttribute(prefix + k, v);
			}
		}
	}
}

