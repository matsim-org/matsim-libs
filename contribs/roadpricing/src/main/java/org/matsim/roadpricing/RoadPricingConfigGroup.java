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

package org.matsim.roadpricing;

import org.matsim.core.config.Module;

import java.util.Map;
import java.util.TreeMap;

public class RoadPricingConfigGroup extends Module {

    private static final String USE_ROADPRICING = "useRoadpricing";

	public static final String GROUP_NAME = "roadpricing";

	private static final String TOLL_LINKS_FILE = "tollLinksFile";

    private boolean useRoadpricing = false;
	private String tollLinksFile = null;

	public RoadPricingConfigGroup() {
		super(GROUP_NAME);
	}


    @Override
    public Map<String, String> getComments() {
        Map<String,String> map = super.getComments();
        map.put(USE_ROADPRICING, "Set this parameter to true if roadpricing should be used, false if not.");
        return map;
    }

	@Override
	public String getValue(final String key) {
		if (TOLL_LINKS_FILE.equals(key)) {
			return getTollLinksFile();
		} else if (USE_ROADPRICING.equalsIgnoreCase(key)){
            return Boolean.toString(this.isUseRoadpricing());
        }
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
        if (USE_ROADPRICING.equalsIgnoreCase(key)){
            this.useRoadpricing = Boolean.parseBoolean(value.trim());
        } else if (TOLL_LINKS_FILE.equals(key)) {
			setTollLinksFile(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
        map.put(USE_ROADPRICING, getValue(USE_ROADPRICING));
        if (this.tollLinksFile != null) {
			map.put(TOLL_LINKS_FILE, getValue(TOLL_LINKS_FILE));
		}
		return map;
	}

    public boolean isUseRoadpricing() {
        return this.useRoadpricing;
    }

    public void setUseRoadpricing(final boolean useRoadpricing) {
        this.useRoadpricing = useRoadpricing;
    }

    public String getTollLinksFile() {
		return this.tollLinksFile;
	}
	public void setTollLinksFile(final String tollLinksFile) {
		this.tollLinksFile = tollLinksFile;
	}
}
