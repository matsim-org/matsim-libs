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

package playground.anhorni.locationchoice.scoring;

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


/**
 * A factory to create {@link LocationChoiceScoringFunction}s.
 *
 * @author anhorni
 */
public class LocationChoiceScoringFunctionFactory implements ScoringFunctionFactory {

	TreeMap<Id, FacilityPenalty> facilityPenalties = null;
	
	public LocationChoiceScoringFunctionFactory(TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.facilityPenalties = facilityPenalties;
	}
	
	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new LocationChoiceScoringFunction(plan, facilityPenalties);
	}

}
