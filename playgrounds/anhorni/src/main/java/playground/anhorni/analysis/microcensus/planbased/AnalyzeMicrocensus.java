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
	
	private Bins distanceDistribution;
	private Bins distanceDistributionHomeBased;
	private Bins distanceDistributionHomeBasedRoundTrip;
	private Bins shoppingDistanceDistributionHomeBasedRoundTripGrocery;
	private Bins shoppingDistanceDistributionHomeBasedRoundTripNonGrocery;
	
	String type ="";
		
	private ScenarioImpl scenario = null;
	private String mode = null;
		
	public static void main(final String[] args) {
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.run(args[0], args[1], args[2]);	
		log.info("Analysis finished -----------------------------------------");
	}
	
	public void run(String mode, String plansFilePath, String type) {
		this.type = type;
		this.init(mode, plansFilePath);
		this.runAnalysis();
		this.write();		
	}
	
	private void init(String mode, String plansFilePath) {
		scenario = new ScenarioImpl();
		this.mode = mode;
		this.distanceDistribution = new Bins(500.0, 40000.0, type + "_trips_mc_" + this.mode);
		this.distanceDistributionHomeBased = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_" + this.mode);
		this.distanceDistributionHomeBasedRoundTrip = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_round-trip_" + this.mode);
		
		this.shoppingDistanceDistributionHomeBasedRoundTripGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_grocery_" + this.mode);
		this.shoppingDistanceDistributionHomeBasedRoundTripNonGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_nongrocery_" + this.mode);
		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);	
	}
	
	private void runAnalysis() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith(this.type)) {
						if (plan.getPreviousLeg(act).getMode().equals(this.mode)) {
							ActivityImpl previousAct = (ActivityImpl) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = ((CoordImpl)previousAct.getCoord()).calcDistance(act.getCoord());
							distanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						
							if (previousAct.getType().equals("h")) {
								distanceDistributionHomeBased.addVal(distance, p.getSelectedPlan().getScore());	
								
								//check the subsequent activities
								Activity actTmp = act;
								String nextType = plan.getNextActivity(plan.getNextLeg(actTmp)).getType();
								while (nextType.startsWith(this.type)) {
									actTmp = plan.getNextActivity(plan.getNextLeg(actTmp));
									nextType = actTmp.getType();
								}
								if (nextType.equals("h")) {
									this.distanceDistributionHomeBasedRoundTrip.addVal(distance, p.getSelectedPlan().getScore());
									
									if (act.getType().equals("sg")) {
										this.shoppingDistanceDistributionHomeBasedRoundTripGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
									else if (act.getType().startsWith("s") && !(act.getType().equals("sg"))){
										this.shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.addVal(distance, p.getSelectedPlan().getScore());
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
		this.distanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.distanceDistributionHomeBased.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.distanceDistributionHomeBasedRoundTrip.plotBinnedDistribution(this.outputFolder, "m", "m");
		if (type.startsWith("s")) {
			this.shoppingDistanceDistributionHomeBasedRoundTripGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
			this.shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
		}
	}
}
