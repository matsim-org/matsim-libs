/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;

/**
 * Utility to create different road pricing schemes.
 * 
 * @author jwjoubert
 */
public class RoadPricingUtils {

	public static RoadPricingConfigGroup createConfigGroup(){
		return new RoadPricingConfigGroup();
	}

	public static RoadPricingModule createModule(){ return new RoadPricingModule(); }

	public static RoadPricingModule createModule(RoadPricingScheme scheme){ return new RoadPricingModule(scheme); }

	public static RoadPricingSchemeImpl createDefaultScheme(){ return new RoadPricingSchemeImpl(); }

	public static RoadPricingScheme getScheme(Scenario sc){
		Object o = sc.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		if(o == null){
			sc.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, new RoadPricingSchemeImpl());
		}
		return (RoadPricingScheme) sc.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
	}
	
}
