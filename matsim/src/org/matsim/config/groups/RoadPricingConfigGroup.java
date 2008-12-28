/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingConfigGroup.java
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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.matsim.config.Module;

public class RoadPricingConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "roadpricing";

	private static final String TOLL_LINKS_FILE = "tollLinksFile";

	private String tollLinksFile = null;

	public RoadPricingConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (TOLL_LINKS_FILE.equals(key)) {
			return getTollLinksFile();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (TOLL_LINKS_FILE.equals(key)) {
			setTollLinksFile(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		if (this.tollLinksFile != null) {
			map.put(TOLL_LINKS_FILE, getValue(TOLL_LINKS_FILE));
		}
		return map;
	}

	/* direct access */

	public String getTollLinksFile() {
		return this.tollLinksFile;
	}
	public void setTollLinksFile(final String tollLinksFile) {
		this.tollLinksFile = tollLinksFile;
	}
}
