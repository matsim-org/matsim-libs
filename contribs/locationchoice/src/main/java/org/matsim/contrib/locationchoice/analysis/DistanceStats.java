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

package org.matsim.contrib.locationchoice.analysis;

import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;


public class DistanceStats implements IterationEndsListener {	
	private Config config; 
	private double analysisBoundary;
	private Bins bins;
	private String bestOrSelected = "selected";
	private String type = null;
	private ActTypeConverter actTypeConverter;
	
	public DistanceStats(Config config, String bestOrSelected, String type, ActTypeConverter actTypeConverter) {	
		this.analysisBoundary = Double.parseDouble(config.locationchoice().getAnalysisBoundary()); 
		this.config = config;
		this.bestOrSelected = bestOrSelected;
		this.type = type;
		this.actTypeConverter = actTypeConverter;
		this.bins = new Bins(Double.parseDouble(config.locationchoice().getAnalysisBinSize()),
				analysisBoundary, type + "_distance");
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {	
		this.bins.clear();
		
		for (Person p : event.getControler().getPopulation().getPersons().values()) {
			
			// if person is not in the analysis population
			if (Integer.parseInt(p.getId().toString()) > Integer.parseInt(this.config.locationchoice().getIdExclusion())) continue;
					
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			
			if (bestOrSelected.equals("best")) {
				double bestPlanScore = -999.0;
				int bestIndex = 0;
				int cnt = 0;
				for (Plan planTmp : p.getPlans()) {
					if (planTmp.getScore() > bestPlanScore){
						bestPlanScore = planTmp.getScore();
						bestIndex = cnt;
					}
					cnt++;
				}
				plan = (PlanImpl) p.getPlans().get(bestIndex);	
				//log.info(plan.getScore() + "\t" + bestIndex + "\tperson " + p.getId());
			}		
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (this.actTypeConverter.convertType(((Activity) pe).getType()).equals(type)) {
						double distance = ((CoordImpl)((Activity) pe).getCoord()).calcDistance(
								plan.getPreviousActivity(plan.getPreviousLeg((Activity)pe)).getCoord());
						this.bins.addVal(distance, 1.0);
					}	
				}
			}
		}
		this.bins.plotBinnedDistribution(
				event.getControler().getControlerIO().getIterationPath(
						event.getIteration())+ "/" + event.getControler().getConfig().getParam("controler", "runId") + "." + 
						event.getIteration() + ".plan=" + this.bestOrSelected +"_" , "#", "m");
	}
}
