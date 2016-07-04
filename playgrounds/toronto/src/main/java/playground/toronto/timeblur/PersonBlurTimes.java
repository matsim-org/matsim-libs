/* *********************************************************************** *
 * project: org.matsim.*
 * PersonBlurTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.toronto.timeblur;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.utils.misc.Time;

public class PersonBlurTimes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final PlanAlgorithm pa;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonBlurTimes(Config config, int mutationRange) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		TimeAllocationMutator tam = new TimeAllocationMutator(config, null, mutationRange, true);
		tam.prepareReplanning(null);
		pa = tam.getPlanAlgoInstance();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		//Modified by pkucirek to account for the TTS day which begins at 04:00:00 and ends at 27:59:59
		//Basically, we first shift the plans backwards by 4 hrs, (so that the latest recorded time is
		//shifted to Time.MIDNIGHT), then we mutate the time, then we shift back.
		
		for (Plan p : person.getPlans()) {
			
			shiftPlan(p, (-1) * Time.parseTime("04:00:00"));
			this.run(p);
			shiftPlan(p, Time.parseTime("04:00:00"));
		}
	}

	private void shiftPlan(Plan plan, double timeShift){
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Activity){
				double start = ((Activity) pe).getStartTime();
				double end = ((Activity) pe).getEndTime();
				double dur = ((Activity) pe).getMaximumDuration();
				
				if (start != Time.UNDEFINED_TIME && start != 0){
					double newStart = start + timeShift;
					if (newStart < 0) 
						newStart = 0; //Shouldn't happen, but check to make sure.
					((Activity) pe).setStartTime(newStart);
				}
				if (end != Time.UNDEFINED_TIME){
					double newEnd = end + timeShift;
					((Activity) pe).setEndTime(newEnd);
				}
				if (dur != Time.UNDEFINED_TIME && dur != Double.POSITIVE_INFINITY){
					((Activity) pe).setMaximumDuration(((Activity) pe).getEndTime() - ((Activity) pe).getEndTime());
				}
			}else if(pe instanceof Leg){
				double dep = ((Leg) pe).getDepartureTime();
				Leg r = ((Leg) pe);
				double arr = r.getDepartureTime() + r.getTravelTime();
				((Leg) pe).setDepartureTime(dep + timeShift);
				Leg r1 = ((Leg) pe);
				r1.setTravelTime( arr + timeShift - r1.getDepartureTime() );
				
			}else{
				System.err.println("Cannot recognize plan element!");
				System.exit(-1);
			}
		}
	}
	
	public void run(Plan plan) {
		pa.run(plan);
	}
}
