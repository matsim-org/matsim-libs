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

