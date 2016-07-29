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
package org.matsim.core.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

/**
 * @author nagel
 *
 */
/* deliberately package */ class VspPlansCleaner implements BeforeMobsimListener {

	@Inject private PlansConfigGroup plansConfigGroup;
	@Inject private Population population;

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		PlansConfigGroup.ActivityDurationInterpretation actDurInterp = (plansConfigGroup.getActivityDurationInterpretation() ) ;
		for ( Person person : population.getPersons().values() ) {

			Plan plan = person.getSelectedPlan() ; 
			// do this only for the selected plan in the assumption that the other ones are clean
			
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe ;
					
					if ( actDurInterp == PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime ) {
						
						// person stays at the activity either until its duration is over or until its end time, whatever comes first
						// do nothing
						
					} else if ( actDurInterp == PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly ) {
						
						// always set duration to undefined:
						act.setMaximumDuration( Time.UNDEFINED_TIME ) ;
						
					} else if ( actDurInterp == PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ) {
						
						// set duration to undefined if there is an activity end time:
						if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
							act.setMaximumDuration(Time.UNDEFINED_TIME) ;
						}
						
					} else {
						throw new IllegalStateException("should not happen") ;
					}
					
					if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
						act.setStartTime(Time.UNDEFINED_TIME) ;
					}
					
				} else if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
						leg.setDepartureTime(Time.UNDEFINED_TIME) ;
						Leg r = (leg); // given by activity end time; everything else confuses
						r.setTravelTime( Time.UNDEFINED_TIME - r.getDepartureTime() );
						leg.setTravelTime( Time.UNDEFINED_TIME ); // added apr'2015
					}
				}
			}
									
		}
	}

}
