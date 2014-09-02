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

import java.util.Map;

import org.matsim.core.config.experimental.ReflectiveModule;

public class RoadPricingConfigGroup extends ReflectiveModule {
	// has to be public

	public static final String GROUP_NAME = "roadpricing";

	private static final String USE_ROADPRICING = "useRoadpricing";
	private static final String TOLL_LINKS_FILE = "tollLinksFile";

    private boolean usingRoadpricing = false;
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

    @StringGetter(USE_ROADPRICING)
    public boolean isUsingRoadpricing() {
        return this.usingRoadpricing;
    }
    @StringSetter(USE_ROADPRICING)
    public void setUseRoadpricing(final boolean useRoadpricing) {
        this.usingRoadpricing = useRoadpricing;
    }
    @StringGetter(TOLL_LINKS_FILE)
    public String getTollLinksFile() {
		return this.tollLinksFile;
	}
    @StringSetter(TOLL_LINKS_FILE)
	public void setTollLinksFile(final String tollLinksFile) {
		this.tollLinksFile = tollLinksFile;
	}
}
