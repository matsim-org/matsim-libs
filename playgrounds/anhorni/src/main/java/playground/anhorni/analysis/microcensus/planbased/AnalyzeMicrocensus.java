/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis.microcensus.planbased;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.analysis.Bins;

/**
 * Based on Balmers conversion of the MC to MATSim plans
 *
 * @author anhorni
 */

public class AnalyzeMicrocensus {

	private final static Logger log = Logger.getLogger(AnalyzeMicrocensus.class);
	private String outputFolder="src/main/java/playground/anhorni/output/microcensus/";
	
	private Bins shoppingDistanceDistribution;
	private Bins shoppingDistanceDistributionHomeBased;
	private Bins shoppingDistanceDistributionRoundTrip;
	private Bins shoppingDistanceDistributionRoundTripGrocery;
	private Bins shoppingDistanceDistributionRoundTripNonGrocery;
	
	private ScenarioImpl scenario = new ScenarioImpl();
	private String mode = null;
		
	public static void main(final String[] args) {
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		String mode = args[0];
		analyzer.run(mode, args[1]);	
		log.info("Analysis finished -----------------------------------------");
	}
	
	public void run(String mode, String plansFilePath) {
		this.init(mode, plansFilePath);
		this.runAnalysis();
		this.write();		
	}
	
	private void init(String mode, String plansFilePath) {
		this.mode = mode;
		this.shoppingDistanceDistribution = new Bins(500.0, 40000.0, "shopping_trips_mc_" + this.mode);
		this.shoppingDistanceDistributionHomeBased = new Bins(500.0, 40000.0, "shopping_trips_mc_home-based_" + this.mode);
		this.shoppingDistanceDistributionRoundTrip = new Bins(500.0, 40000.0, "shopping_trips_mc_round-trip_" + this.mode);
		
		this.shoppingDistanceDistributionRoundTripGrocery = new Bins(500.0, 40000.0, "shopping_trips_mc_round-trip_grocery_" + this.mode);
		this.shoppingDistanceDistributionRoundTripNonGrocery = new Bins(500.0, 40000.0, "shopping_trips_mc_round-trip_nongrocery_" + this.mode);
		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);	
	}
	
	private void runAnalysis() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith("s")) {
						if (plan.getPreviousLeg(act).getMode().equals(this.mode)) {
							ActivityImpl previousAct = (ActivityImpl) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = ((CoordImpl)previousAct.getCoord()).calcDistance(act.getCoord());
							shoppingDistanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						
							if (previousAct.getType().equals("h")) {
								shoppingDistanceDistributionHomeBased.addVal(distance, p.getSelectedPlan().getScore());	
								
								//check the subsequent activities
								Activity actTmp = act;
								String nextType = plan.getNextActivity(plan.getNextLeg(actTmp)).getType();
								while (nextType.startsWith("s")) {
									actTmp = plan.getNextActivity(plan.getNextLeg(actTmp));
									nextType = actTmp.getType();
								}
								if (nextType.equals("h")) {
									this.shoppingDistanceDistributionRoundTrip.addVal(distance, p.getSelectedPlan().getScore());
									
									if (act.getType().equals("sg")) {
										this.shoppingDistanceDistributionRoundTripGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
									else {
										this.shoppingDistanceDistributionRoundTripNonGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
								}
							}
						}
					}
				}
			}
		}	
	}
			
	private void write() {
		this.shoppingDistanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.shoppingDistanceDistributionHomeBased.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.shoppingDistanceDistributionRoundTrip.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.shoppingDistanceDistributionRoundTripGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.shoppingDistanceDistributionRoundTripNonGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
	}
}
