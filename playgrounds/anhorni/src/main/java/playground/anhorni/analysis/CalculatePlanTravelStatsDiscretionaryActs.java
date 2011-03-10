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

package playground.anhorni.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.anhorni.analysis.Bins;


public class CalculatePlanTravelStatsDiscretionaryActs {
	private Bins shopBins;
	private Bins leisureBins;
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outPath;
	private int analysisPopulationOffset = 0;
	private final static Logger log = Logger.getLogger(CalculatePlanTravelStatsDiscretionaryActs.class);


	public CalculatePlanTravelStatsDiscretionaryActs(double spacing, double maxNetworkDistance, String outPath, int analysisPopulationOffset) {	
		this.shopBins = new Bins(spacing, maxNetworkDistance, "shop_distance");
		this.leisureBins = new Bins(spacing, maxNetworkDistance, "leisure_distance");
		this.outPath = outPath;
	}
	
	public static void main(String [] args) {
		CalculatePlanTravelStatsDiscretionaryActs calculator = new CalculatePlanTravelStatsDiscretionaryActs(
				Double.parseDouble(args[2]), Double.parseDouble(args[3]), args[4], Integer.parseInt(args[5]));
		calculator.init(args[0], args[1]);
		calculator.run();
		log.info("Analysis finished -----------------------------------------");
	}
	
	private void init(final String plansFilePath, final String networkFilePath) {
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}

	private void run() {			
		for (Person p : this.scenario.getPopulation().getPersons().values()) {	
			
			if (Integer.parseInt(p.getId().toString()) > this.analysisPopulationOffset) continue;
			
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
				
			// find best plan
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
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().startsWith("s") || ((Activity) pe).getType().startsWith("l")) {
						double distance = ((CoordImpl)((Activity) pe).getCoord()).calcDistance(
								plan.getPreviousActivity(plan.getPreviousLeg((Activity)pe)).getCoord());
						if (((Activity) pe).getType().startsWith("s")) {
							this.shopBins.addVal(distance, 1.0);
						}
						else if (((Activity) pe).getType().startsWith("l")) {
							this.leisureBins.addVal(distance, 1.0);
						}
					}
				}
			}
		}
		this.shopBins.plotBinnedDistribution(this.outPath + this.shopBins.interval + "_", "#", "m");
		this.leisureBins.plotBinnedDistribution(this.outPath + this.leisureBins.interval + "_", "#", "m");
	}
}
