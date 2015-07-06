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
package org.matsim.contrib.matsim4urbansim.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public class Matsim4UrbansimConfigGroup extends ReflectiveConfigGroup {
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(Matsim4UrbansimConfigGroup.class);

	public static final String GROUP_NAME = "matsim4Urbansim";

	public Matsim4UrbansimConfigGroup() {
		super(GROUP_NAME);
	}
	
	private boolean usingRoadPricing = false ;
	private static final String USING_ROAD_PRICING = "usingRoadPricing" ;
	@StringGetter(USING_ROAD_PRICING)
	public boolean isUsingRoadPricing() {
		return usingRoadPricing;
	}
	@StringSetter(USING_ROAD_PRICING)
	public void setUsingRoadPricing(boolean usingRoadPricing) {
		this.usingRoadPricing = usingRoadPricing;
	}

}
