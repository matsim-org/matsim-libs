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

package playground.anhorni.surprice.warmstart;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;

public class AdaptNextDay implements StartupListener {
	private Population populationPreviousDay;
	private final static Logger log = Logger.getLogger(AdaptNextDay.class);
	
	public AdaptNextDay(Population populationPreviousDay) {
		this.populationPreviousDay = populationPreviousDay;
	}

	@Override
	public void notifyStartup(StartupEvent event) {		
		// on monday or saturday no warmstarting
		if (this.populationPreviousDay == null) return;
		log.info("Adapting plans of next day ... ");

        Population population = event.getServices().getScenario().getPopulation();
		int cntTime = 0;
		int cntRouteMode = 0;
		int cntActs = 0;
		for (Person p : this.populationPreviousDay.getPersons().values()) {			
			Plan plan = population.getPersons().get(p.getId()).getSelectedPlan();
			int [] res = this.adaptPlan((PlanImpl)p.getSelectedPlan(), (PlanImpl)plan);
			cntTime += res[0];
			cntRouteMode += res[1];
			cntActs += res[2];
		}
		log.info("number of persons : " + this.populationPreviousDay.getPersons().size());
		log.info("total number of acts : " + cntActs);
		log.info("number of acts with identical end time and leg departure times: " + cntTime);
		log.info("number of legs with identical routes and modes:" + cntRouteMode);
	}
	
	private int[] adaptPlan(PlanImpl planPreviousDay, PlanImpl plan) {
		int planElementIndex = 0;
			
		int cntTime = 0;
		int cntRouteMode = 0;
		int cntAct = 0;
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity)pe;
				Activity actPreviousDay = null;
				cntAct++;
				
				if (planPreviousDay.getPlanElements().size() >= planElementIndex) {
					actPreviousDay = (Activity) planPreviousDay.getPlanElements().get(planElementIndex);
					
					// identical times
					if (act.getType().equals(actPreviousDay.getType())) { // set act times, leg departure time automatically set
						if (actPreviousDay.getEndTime() > Time.UNDEFINED_TIME) {	// it is not last home act of plan
							act.setEndTime(actPreviousDay.getEndTime());
						} // else do not touch end time
						act.setStartTime(
								Math.max(0.0, act.getEndTime() - plan.getPerson().getDesires().getActivityDuration(act.getType())));
						
						cntTime++;
						
						if (planElementIndex > 0) { 
							Activity previousAct = plan.getPreviousActivity(plan.getPreviousLeg(act));
							
							Leg previousLegPreviousDay = planPreviousDay.getPreviousLeg(actPreviousDay);
							Activity previousActPreviousDay = planPreviousDay.getPreviousActivity(previousLegPreviousDay);;
							
							// identical route and mode
							if (act.getFacilityId().equals(actPreviousDay.getFacilityId()) && 
									previousAct.getFacilityId().equals(previousActPreviousDay.getFacilityId())) {
								
								Leg leg = plan.getPreviousLeg(act);
								leg.setMode(planPreviousDay.getPreviousLeg(actPreviousDay).getMode());
								leg.setRoute(planPreviousDay.getPreviousLeg(actPreviousDay).getRoute());
								cntRouteMode++;
							}
						}
					}
					else {
						break;
					}
				}
				planElementIndex += 2;
			}
		}
		int [] res = {cntTime, cntRouteMode, cntAct};
		return res;
	}	
}

