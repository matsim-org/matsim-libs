/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jhackney.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.jhackney.SocNetConfigGroup;

public class EventSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
	private LinkedHashMap<Activity,ArrayList<Double>> actStats;
	private final SocNetConfigGroup snConfig;

	public EventSocScoringFactory(String factype,LinkedHashMap<Activity,ArrayList<Double>> actStats, SocNetConfigGroup snConfig) {
		this.factype=factype;
		this.actStats=actStats;
		this.snConfig = snConfig;
	}

	public EventSocScoringFunction createNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new EventSocScoringFunction(plan, factype, actStats, this.snConfig);
	}


}
