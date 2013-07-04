/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class SVDScoringfunctionFactory implements ScoringFunctionFactory {
	private final Map <Id, IndividualPreferences> svdValuesMap;
	private final Network net;
	private final TransitSchedule schedule;
	private final double scoreWeight;
	
	public SVDScoringfunctionFactory(final Map <Id, IndividualPreferences> svdValuesMap, final Network net, final TransitSchedule schedule, double scoreWeight ) {
		this.svdValuesMap = svdValuesMap;
		this.net = net; 
		this.schedule = schedule;
		this.scoreWeight = scoreWeight;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		final IndividualPreferences svdValues = svdValuesMap.get(plan.getPerson().getId());
		ScoringFunction svdScoringFunction = new IndividualPreferencesLegScoring(plan, svdValues, net, schedule, scoreWeight);
		return svdScoringFunction;
	}
}