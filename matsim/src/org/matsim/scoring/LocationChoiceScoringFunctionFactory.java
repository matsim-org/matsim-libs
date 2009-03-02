/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceScoringFunctionFactory.java
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

package org.matsim.scoring;

import java.util.TreeMap;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


/**
 * A factory to create {@link LocationChoiceScoringFunction}s.
 *
 * @author anhorni
 */
public class LocationChoiceScoringFunctionFactory implements ScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	private final CharyparNagelScoringParameters params;
	
	public LocationChoiceScoringFunctionFactory(final CharyparNagelScoringConfigGroup config, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.params = new CharyparNagelScoringParameters(config);
		this.facilityPenalties = facilityPenalties;
	}
	
	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new LocationChoiceScoringFunction(plan, params, facilityPenalties);
	}

}
