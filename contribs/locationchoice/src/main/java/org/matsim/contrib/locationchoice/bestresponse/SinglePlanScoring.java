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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

/**This would probably work, but in the end it seems more plausible to use the acts and legs directly if they already exist as entities.
 * 
 * @author nagel
 *
 */
public class SinglePlanScoring {
	
	private void scorePlan( Plan plan, ScoringFunctionFactory scoringFunctionFactory ) {
		
		Config dummyConfig = ConfigUtils.createConfig() ;
		Scenario dummyScenario = ScenarioUtils.createScenario(dummyConfig) ;
	
		EventsToScore ev2score = EventsToScore.createWithScoreUpdating(dummyScenario, scoringFunctionFactory);
		
		int idx = -1 ;
		double prevActEndTime = Time.UNDEFINED_TIME ;
		double prevLegTravTime = Time.UNDEFINED_TIME ;
		for ( PlanElement pe : plan.getPlanElements() ) {
			idx ++ ;
			if ( pe instanceof Activity ) {
				Activity act = (Activity) pe ;
				if ( idx > 0 ) {
					double actStartTime = prevActEndTime + prevLegTravTime ;
					ev2score.handleEvent(  new ActivityStartEvent( actStartTime, plan.getPerson().getId(), null, null, act.getType() ) ) ; 
				}
				ev2score.handleEvent(  new ActivityEndEvent( act.getEndTime(), plan.getPerson().getId(), null, null, act.getType() ) ) ; 
			} else if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
			} 
		}	
	}
}
