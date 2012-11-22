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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

import playground.anhorni.surprice.preprocess.rwscenario.ConvertThurgau2Plans;

public class AdaptNextDay implements StartupListener {
	private Population populationPreviousDay;
	private final static Logger log = Logger.getLogger(AdaptNextDay.class);
	
	public AdaptNextDay(Population populationPreviousDay) {
		this.populationPreviousDay = populationPreviousDay;
	}

	@Override
	public void notifyStartup(StartupEvent event) {		
		// on monday or saturday return
		if (this.populationPreviousDay == null) return;
		
		Population population = event.getControler().getPopulation();
		int cntTime = 0;
		int cntRouteMode = 0;
		for (Person p : this.populationPreviousDay.getPersons().values()) {
			Plan plan = population.getPersons().get(p.getId()).getSelectedPlan();
			int [] res = this.comparePlans((PlanImpl)p.getSelectedPlan(), (PlanImpl)plan, cntTime, cntRouteMode);
			cntTime += res[0];
			cntRouteMode += res[1];
		}
		log.info("number of acts with identical end time and leg departure times: " + cntTime);
		log.info("number of legs with identical routes and modes:" + cntRouteMode);
	}
	
	private int[] comparePlans(PlanImpl planPreviousDay, PlanImpl plan, int cntTime, int cntRouteMode) {
		int planElementIndex = 0;
		ActivityImpl previousAct = null;
		ActivityImpl previousActPreviousDay = null;
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				ActivityImpl actPreviousDay = null;
				planElementIndex += 2;
				
				if (planPreviousDay.getPlanElements().size() >= planElementIndex) {
					actPreviousDay = (ActivityImpl) planPreviousDay.getPlanElements().get(planElementIndex);
					cntTime++;
					// identical times
					if (act.getType().equals(actPreviousDay.getType())) {
						act.setEndTime(actPreviousDay.getEndTime());
						act.setStartTime(
								Math.max(0.0, act.getEndTime() - plan.getPerson().getDesires().getActivityDuration(act.getType())));
						
						if (planElementIndex < plan.getPlanElements().size()) {
							LegImpl leg = (LegImpl) plan.getNextLeg(act);
							leg.setDepartureTime(act.getEndTime());
						}
						
						// identical route and mode
						if (act.getFacilityId().equals(actPreviousDay.getFacilityId()) && 
								previousAct != null && previousActPreviousDay != null &&
								previousAct.getType().equals(previousActPreviousDay.getType()) &&
								previousAct.getFacilityId().equals(previousActPreviousDay.getFacilityId())) {
							
							LegImpl leg = (LegImpl) plan.getPreviousLeg(act);
							leg.setMode(planPreviousDay.getPreviousLeg(actPreviousDay).getMode());
							leg.setRoute(planPreviousDay.getPreviousLeg(actPreviousDay).getRoute());
							cntRouteMode++;
						}
					}
				}	
				previousAct = act;
				previousActPreviousDay = actPreviousDay;
			}
		}
		int [] res = {cntTime, cntRouteMode};
		return res;
	}	
}

