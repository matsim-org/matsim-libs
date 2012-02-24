/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.anhorni.surprice.AgentMemory;

public class LaggedScoringFunction extends CharyparNagelOpenTimesScoringFunction {
	
	private AgentMemory memory;

	public LaggedScoringFunction(Plan plan, CharyparNagelScoringParameters params, ActivityFacilities facilities,
			AgentMemory memory) {
		super(plan, params, facilities);
		this.memory = memory;
	}
	
	
public void finish() {		
		super.finish();	
		for (PlanElement pe : super.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += 0.0; // TODO: include lag effects from memory
			}
		}
	}
	
}
