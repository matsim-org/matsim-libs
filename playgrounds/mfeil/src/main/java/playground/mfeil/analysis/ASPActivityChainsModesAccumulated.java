/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansActivityChainsAccumulated.java
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

package playground.mfeil.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.knowledges.Knowledges;



/**
 * Simple class to analyze the selected plans of a plans (output) file. Extracts the
 * activity chains per modes and their number of occurrences. Does not differentiate the
 * order of acts/legs within the plan (since not required for estimation)
 *
 * @author mfeil
 */
public class ASPActivityChainsModesAccumulated extends ASPActivityChainsModes{

	private static final Logger log = Logger.getLogger(ASPActivityChainsModesAccumulated.class);

	public ASPActivityChainsModesAccumulated(final Population population, Knowledges knowledges, final String outputDir) {
		super (population, knowledges, outputDir);
	}

	public ASPActivityChainsModesAccumulated(final Population population) {
		super (population);
		this.outputDir = "/home/baug/mfeil/data/choiceSet/it0";
	//	this.outputDir = "./plans";
	}

	@Override
	public void run(){
		this.initAnalysis();
		this.analyze();
	}

	private void initAnalysis(){

		this.activityChains = new ArrayList<List<PlanElement>>();
		this.plans = new ArrayList<ArrayList<Plan>>();
		ActChainEqualityCheck ac = new ActChainEqualityCheck();
		for (Person person : this.population.getPersons().values()) {
			boolean alreadyIn = false;
			for (int i=0;i<this.activityChains.size();i++){
				if (ac.checkEqualActChainsModesAccumulated(person.getSelectedPlan().getPlanElements(), this.activityChains.get(i))){
					plans.get(i).add(person.getSelectedPlan());
					alreadyIn = true;
					break;
				}
			}
			if (!alreadyIn){
				this.activityChains.add(person.getSelectedPlan().getPlanElements());
				this.plans.add(new ArrayList<Plan>());
				this.plans.get(this.plans.size()-1).add(person.getSelectedPlan());
			}
		}
	}


	public static void main(final String [] args) {
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
//		final String populationFilename = "./plans/output_plans.xml.gz";
//		final String networkFilename = "./plans/network.xml";
//		final String facilitiesFilename = "./plans/facilities.xml.gz";

		final String outputDir = "/home/baug/mfeil/data/mz";
//		final String outputDir = "./plans";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ASPActivityChainsModesAccumulated sp = new ASPActivityChainsModesAccumulated(scenario.getPopulation(), scenario.getKnowledges(), outputDir);
		sp.run();

		log.info("Analysis of plan finished.");
	}

}

