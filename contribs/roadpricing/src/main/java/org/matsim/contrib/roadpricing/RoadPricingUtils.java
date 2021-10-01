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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.core.config.ConfigUtils;

/**
 * Utility to create different road pricing schemes.
 *
 * @author jwjoubert
 */
public class RoadPricingUtils {

	public static RoadPricingConfigGroup createConfigGroup() {
		return new RoadPricingConfigGroup();
	}

	//	public static RoadPricingModule createModule(RoadPricingScheme scheme) {
//		return new RoadPricingModule(scheme);
//	}


	/**
	 * @deprecated -- please inline.  Reason: move syntax closer ot how it is in {@link ConfigUtils}.
	 */
	public static RoadPricingSchemeImpl createAndRegisterMutableScheme( Scenario scenario ) {
		return addOrGetMutableRoadPricingScheme( scenario );
	}
	public static RoadPricingSchemeImpl addOrGetMutableRoadPricingScheme( Scenario scenario ) {
		return (RoadPricingSchemeImpl) addOrGetRoadPricingScheme( scenario );
	}

	/**
	 * @deprecated -- please inline.  Reason: make syntax more consistent.
	 */
	public static RoadPricingScheme getScheme(Scenario sc){
		return getRoadPricingScheme( sc );
	}

	/**
	 * This method will fail if it is not there.
	 */
	public static RoadPricingScheme getRoadPricingScheme( Scenario sc ) {
		if ( sc.getScenarioElement( RoadPricingScheme.ELEMENT_NAME ) == null ) {
			throw new RuntimeException( "\n\ncannot retreive RoadPricingScheme from scenario; typical ways to resolve" +
								    "that are to call RoadPricingUtils.loadRoadPricingScheme... " +
								    " or RoadPricingUtils.addOrGetRoadPricingScheme... " +
								    "or RoadPricingUtils.addRoadPricingScheme... early enough.");

		}
		return (RoadPricingScheme) sc.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
	}

	public static RoadPricingScheme addOrGetRoadPricingScheme( Scenario sc ) {
		Object o = sc.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		if (o == null) {
			sc.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, new RoadPricingSchemeImpl());
		}
		return (RoadPricingScheme) sc.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
	}
	
	public static void addRoadPricingScheme( Scenario scenario, RoadPricingScheme roadPricingScheme ) {
		scenario.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, roadPricingScheme);
	}

	public static void setType(RoadPricingSchemeImpl scheme, String type) {
		scheme.setType(type);
	}

	public static void setName(RoadPricingSchemeImpl scheme, String name){
		scheme.setName(name);
	}

	public static void setDescription(RoadPricingSchemeImpl scheme, String description){
		scheme.setDescription(description);
	}

	public static Cost createAndAddGeneralCost(RoadPricingSchemeImpl scheme, final double startTime, final double endTime, final double amount){
		return scheme.createAndAddCost(startTime, endTime, amount);
	}

	public static void addLink(RoadPricingSchemeImpl scheme, Id<Link> linkId){
		scheme.addLink(linkId);
	}

	public static void addLinkSpecificCost(RoadPricingSchemeImpl scheme, Id<Link> linkId, double startTime, double endTime, double amount){
		scheme.addLinkCost(linkId, startTime, endTime, amount);
	}

	/**
	 * @deprecated -- please inline.  Reason: Make syntax more consistent across contribs.
	 */
	public static RoadPricingSchemeImpl loadRoadPricingScheme( Scenario sc ){
		return loadRoadPricingSchemeAccordingToRoadPricingConfig( sc );
	}
	public static RoadPricingSchemeImpl loadRoadPricingSchemeAccordingToRoadPricingConfig( Scenario sc ){
		RoadPricingSchemeImpl scheme = (RoadPricingSchemeImpl) addOrGetRoadPricingScheme( sc );
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule( sc.getConfig(), RoadPricingConfigGroup.class );
		;
		new RoadPricingReaderXMLv1( scheme ).readFile( rpConfig.getTollLinksFile() );
		return scheme;
	}
}
