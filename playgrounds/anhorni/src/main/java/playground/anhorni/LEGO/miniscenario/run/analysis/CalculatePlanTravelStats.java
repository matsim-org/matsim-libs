/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.LEGO.miniscenario.run.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.analysis.Bins;


public class CalculatePlanTravelStats implements IterationEndsListener {
	
	private ConfigReader configReader; 
	private double maxNetworkDistance;
	private Bins bins;
	private String bestOrSelected = "selected";
	private String type = null;


	public CalculatePlanTravelStats(ConfigReader configReader, String bestOrSelected, String type) {	
		this.maxNetworkDistance = configReader.getSideLengt();
		this.configReader = configReader;
		this.bestOrSelected = bestOrSelected;
		this.type = type;
		this.bins = new Bins(this.configReader.getSpacing() * 2, maxNetworkDistance, type + "_distance");
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {	
		this.bins.clear();
		
		for (Person p : event.getControler().getPopulation().getPersons().values()) {
			
			// if person is not in the analysis population
			if (Integer.parseInt(p.getId().toString()) > configReader.getAnalysisPopulationOffset()) continue;
			
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
					if (((Activity) pe).getType().startsWith(type)) {
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
