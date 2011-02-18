/* *********************************************************************** *
 * project: org.matsim.*
 * VspPlansCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

/**
 * @author nagel
 *
 */
public class VspPlansCleaner implements BeforeMobsimListener {

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Population pop = event.getControler().getScenario().getPopulation();
		Config config = event.getControler().getScenario().getConfig() ;
		for ( Person person : pop.getPersons().values() ) {

			Plan plan = person.getSelectedPlan() ; 
			// do this only for the selected plan in the assumption that the other ones are clean
			
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe ;
					
					if ( config.vspExperimental().getActivityDurationInterpretation()
							.equals(VspExperimentalConfigGroup.MIN_OF_DURATION_AND_END_TIME) ) {
						
						// person stays at the activity either until its duration is over or until its end time, whatever comes first
						// do nothing
						
					} else if ( config.vspExperimental().getActivityDurationInterpretation()
							.equals(VspExperimentalConfigGroup.END_TIME_ONLY ) ) {
						
						// always set duration to undefined:
						((ActivityImpl)act).setMaximumDuration( Time.UNDEFINED_TIME ) ;
						
					} else if ( config.vspExperimental().getActivityDurationInterpretation()
							.equals(VspExperimentalConfigGroup.TRY_END_TIME_THEN_DURATION ) ) {
						
						// set duration to undefined if there is an activity end time:
						if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
							((ActivityImpl)act).setMaximumDuration( Time.UNDEFINED_TIME ) ;
						} ;
						
					} else {
						throw new IllegalStateException("should not happen") ;
					}

					
					
					
				}
			}
									
		}
	}

}
