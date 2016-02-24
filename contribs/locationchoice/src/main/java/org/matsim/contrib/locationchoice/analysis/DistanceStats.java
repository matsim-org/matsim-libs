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
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class DistanceStats implements IterationEndsListener {	

	private DestinationChoiceConfigGroup dccg;
	private double analysisBoundary;
	private Bins bins;
	private String bestOrSelected = "selected";
	private String type = null;
	private ActTypeConverter actTypeConverter;
	private String mode;
	
	public DistanceStats(Config config, String bestOrSelected, String type, ActTypeConverter actTypeConverter, String mode) {
		
		this.dccg = (DestinationChoiceConfigGroup) config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.analysisBoundary = this.dccg.getAnalysisBoundary(); 
		this.bestOrSelected = bestOrSelected;
		this.type = type;
		this.actTypeConverter = actTypeConverter;
		this.mode = mode;
		this.bins = new Bins(this.dccg.getAnalysisBinSize(), analysisBoundary, type + "_" + mode + "_distance");
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {	
		this.bins.clear();

        for (Person p : event.getServices().getScenario().getPopulation().getPersons().values()) {
			
			// continue if person is in the analysis population or if the id is not numeric
			if (this.dccg.getIdExclusion() == null || !this.isLong(p.getId().toString()) ||
					Long.parseLong(p.getId().toString()) > this.dccg.getIdExclusion()) continue;
					
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
					if (this.actTypeConverter.convertType(((Activity) pe).getType()).equals(this.actTypeConverter.convertType(type)) &&
							plan.getPreviousLeg((Activity)pe).getMode().equals(this.mode)) {
						double distance = CoordUtils.calcEuclideanDistance(((Activity) pe).getCoord(), plan.getPreviousActivity(plan.getPreviousLeg((Activity)pe)).getCoord()); 
						this.bins.addVal(distance, 1.0);
					}	
				}
			}
		}
		
		// Actually, path is not the full file name - inside the plotBinnedDistribution some other stuff is added. 
		String path = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "plan=" + this.bestOrSelected + "_");
		this.bins.plotBinnedDistribution(path, "#", "m");
	}
	
	private boolean isLong(String str) {
	    try {
	        Long.parseLong(str);
	        return true;
	    } catch (NumberFormatException nfe) {}
	    return false;
	}
}
