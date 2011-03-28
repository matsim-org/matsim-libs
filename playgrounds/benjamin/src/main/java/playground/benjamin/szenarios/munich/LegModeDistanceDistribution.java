/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeDistanceDistribution.java
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
package playground.benjamin.szenarios.munich;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistribution {

	private static final Logger logger = Logger.getLogger(LegModeDistanceDistribution.class);

	// INPUT
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run15/";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "output_plans.xml.gz";

	private final Scenario scenario;
	private List<Integer> distanceClasses;

	public LegModeDistanceDistribution(){
		Config config = ConfigUtils.createConfig();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.distanceClasses = new ArrayList<Integer>();
	}

	public static void main(String[] args) {
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.run(args);
	}

	private void run(String[] args) {
		loadSceanrio();
		setDistanceClasses();
		calculateDistanceClasses2ModeShare();
	}

	private void calculateDistanceClasses2ModeShare() {
		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();
		for(Person person : population.getPersons().values()){
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
				}
			}
		}
	}

		private void setDistanceClasses() {
			for(int noOfClasses = 0; noOfClasses < 14; noOfClasses++){
				int distanceClass = 100 * (int) Math.pow(2, noOfClasses);
				this.distanceClasses.add(distanceClass);
			}
			System.out.println(this.distanceClasses);
		}

		private void loadSceanrio() {
			Config config = scenario.getConfig();
			config.network().setInputFile(netFile);
			config.plans().setInputFile(plansFile);
			ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
			scenarioLoader.loadScenario() ;

		}
	}
