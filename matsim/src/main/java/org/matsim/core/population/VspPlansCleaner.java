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

import com.google.inject.Inject;

/**
 * @author nagel
 * <p>
 * Use with caution! (jb, Oct 2018)
 */
/* deliberately package */ class VspPlansCleaner implements BeforeMobsimListener {

	@Inject
	private PlansConfigGroup plansConfigGroup;
	@Inject
	private Population population;

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
						act.setMaximumDurationUndefined() ;
						
					} else if ( actDurInterp == PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration) {

						// set duration to undefined if there is an activity end time:
						if (act.getEndTime().isDefined()) {
							act.setMaximumDurationUndefined();
						}

					} else {
						throw new IllegalStateException("should not happen") ;
					}
					
					if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
						act.setStartTimeUndefined() ;
					}
					
				} else if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
//						leg.setDepartureTimeUndefined() ;
						//this information is not unneccesary, but may be used, e.g., by DRTRoutes and others.
						if ( leg.getRoute()!=null ) {
							leg.setTravelTimeUndefined();
						}
						
					}
				}
			}
									
		}
	}

}
