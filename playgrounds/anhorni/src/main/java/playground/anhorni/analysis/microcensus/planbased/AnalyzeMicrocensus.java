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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.filters.PersonIntersectAreaFilter;

import playground.anhorni.analysis.Bins;

/**
 * Based on Balmers conversion of the MC to MATSim plans
 *
 * @author anhorni
 */

public class AnalyzeMicrocensus {

	private final static Logger log = Logger.getLogger(AnalyzeMicrocensus.class);
	private String outputFolder="src/main/java/playground/anhorni/output/microcensus/";
	
	private Bins ch_distanceDistribution;
	private Bins ch_distanceDistributionHomeBased;
	private Bins ch_distanceDistributionHomeBasedRoundTrip;
	private Bins ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery;
	private Bins ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery;
	
	private Bins zh_distanceDistribution;
	
	String type ="";
		
	private ScenarioImpl scenarioCH;
	private ScenarioImpl scenarioZH;
	private String mode = null;
		
	public static void main(final String[] args) {
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.run(args[0], args[1], args[2], args[3]);	
		log.info("Analysis finished -----------------------------------------");
	}
	
	public void run(String mode,  String type, String plansFilePath, String networkfilePath) {
		this.type = type;
		this.init(mode, plansFilePath, networkfilePath);
		this.runAnalysisCH();	
		
		this.dilutedZH();
		this.runAnalysisZH();
	}
	
	private void init(String mode, String plansFilePath, String networkfilePath) {
		scenarioCH = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.mode = mode;
		this.ch_distanceDistribution = new Bins(500.0, 40000.0, type + "_trips_mc_" + this.mode);
		this.ch_distanceDistributionHomeBased = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_" + this.mode);
		this.ch_distanceDistributionHomeBasedRoundTrip = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_round-trip_" + this.mode);
		
		this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_grocery_" + this.mode);
		this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_nongrocery_" + this.mode);
		
		this.zh_distanceDistribution = new Bins(500.0, 40000.0, type + "_zh_trips_mc_" + this.mode);
		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenarioCH);
		populationReader.readFile(plansFilePath);
		
		scenarioZH = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenarioZH).readFile(networkfilePath);
	}
	
	private void dilutedZH() {
		double aoiRadius = 30000.0;
		final CoordImpl aoiCenter = new CoordImpl(683518.0,246836.0);
		
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(null, null, this.scenarioZH.getNetwork());
		filter.setAlternativeAOI(aoiCenter, aoiRadius);
		
		for (Person person : this.scenarioCH.getPopulation().getPersons().values()) {
			if (filter.judge(person)) {
				this.scenarioZH.getPopulation().addPerson(person);
			}
		}
	}
	
	private void runAnalysisZH() {
		for (Person p : this.scenarioZH.getPopulation().getPersons().values()) {
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith(this.type)) {
						if (plan.getPreviousLeg(act).getMode().equals(this.mode)) {
							ActivityImpl previousAct = (ActivityImpl) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = ((CoordImpl)previousAct.getCoord()).calcDistance(act.getCoord());
							zh_distanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						}
					}
				}
			}
		}	
		this.zh_distanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m");		
	}
	
	private void runAnalysisCH() {
		for (Person p : this.scenarioCH.getPopulation().getPersons().values()) {
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith(this.type)) {
						if (plan.getPreviousLeg(act).getMode().equals(this.mode)) {
							ActivityImpl previousAct = (ActivityImpl) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = ((CoordImpl)previousAct.getCoord()).calcDistance(act.getCoord());
							ch_distanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						
							if (previousAct.getType().equals("h")) {
								ch_distanceDistributionHomeBased.addVal(distance, p.getSelectedPlan().getScore());	
								
								//check the subsequent activities
								Activity actTmp = act;
								String nextType = plan.getNextActivity(plan.getNextLeg(actTmp)).getType();
								while (nextType.startsWith(this.type)) {
									actTmp = plan.getNextActivity(plan.getNextLeg(actTmp));
									nextType = actTmp.getType();
								}
								if (nextType.equals("h")) {
									this.ch_distanceDistributionHomeBasedRoundTrip.addVal(distance, p.getSelectedPlan().getScore());
									
									if (act.getType().equals("sg")) {
										this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
									else if (act.getType().startsWith("s") && !(act.getType().equals("sg"))){
										this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
								}
							}
						}
					}
				}
			}
		}	
		this.ch_distanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.ch_distanceDistributionHomeBased.plotBinnedDistribution(this.outputFolder, "m", "m");
		this.ch_distanceDistributionHomeBasedRoundTrip.plotBinnedDistribution(this.outputFolder, "m", "m");
		if (type.startsWith("s")) {
			this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
			this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.plotBinnedDistribution(this.outputFolder, "m", "m");
		}		
	}
}
