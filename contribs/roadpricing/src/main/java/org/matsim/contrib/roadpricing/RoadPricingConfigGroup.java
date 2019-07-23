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

package org.matsim.contrib.roadpricing;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class RoadPricingConfigGroup extends ReflectiveConfigGroup {
	// has to be public

	/* Reason for removing "usingRoadPricing" switch: We found it hard to 
	 * interpret. Should a script "set" this switch, or rather "interpret" 
	 * it, or ignore it? For the Gauteng toll simulation, it had to be set 
	 * "false" in order to make everything work correctly. It is now gone; 
	 * if you want to simulate a non-toll base case, recommendation is to 
	 * use an empty toll file. In that way, you can be confident that you 
	 * do not get two different execution paths which may cause differences 
	 * by themselves. kai, in consultation with michael z. and johan j, sep'14
	 */

	private static final Logger LOG = Logger.getLogger(RoadPricingConfigGroup.class);

	public static final String GROUP_NAME = "roadpricing";

	private static final String TOLL_LINKS_FILE = "tollLinksFile";
	private static final String TOLL_ENFORCEMENT_PROBABILITY = "enforcementEfficiency";

	private String tollLinksFile = null;
	private double enforcementProbability = 1.0;

	/**
	 * Create using {@link RoadPricingUtils#createConfigGroup()}.
	 */
	public RoadPricingConfigGroup() {
		super(GROUP_NAME);

		getComments().put(GROUP_NAME, "The probability that the toll is actually paid by the vehicle/person. Must be in the range [0.0, 1.0], and defaults to 1.0");
	}

    @Override
    public Map<String, String> getComments() {
		return super.getComments();
    }

    @StringGetter(TOLL_LINKS_FILE)
    public String getTollLinksFile() {
		return this.tollLinksFile;
	}

    @StringSetter(TOLL_LINKS_FILE)
	public void setTollLinksFile(final String tollLinksFile) {
		this.tollLinksFile = tollLinksFile;
	}

	@StringGetter(TOLL_ENFORCEMENT_PROBABILITY)
	public double getEnforcementProbability() {
		return enforcementProbability;
	}

	@StringSetter(TOLL_ENFORCEMENT_PROBABILITY)
	public void setEnforcementProbability(double enforcementProbability) {
		if(enforcementProbability > 1.0){
			throw new IllegalArgumentException("The enforcement efficiency must be in the range [0,1]");
		}
		if(enforcementProbability != 1.0){
			LOG.warn("The probability of collecting toll is less than 1. Make sure this is what you want!");
		}
		this.enforcementProbability = enforcementProbability;
	}


}
