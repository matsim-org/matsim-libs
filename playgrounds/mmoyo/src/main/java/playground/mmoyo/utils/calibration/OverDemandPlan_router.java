/* *********************************************************************** *
 * project: org.matsim.*
 * PlanCreator.java
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

package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mmoyo.cadyts_integration.Z_Launcher;
import playground.mmoyo.ptRouterAdapted.AdaptedLauncher;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlansMerger;

/**
 *  -reads a base plan 
 *  -routes it with a given number of parameter combinations
 *  -creates it over-estimated version (agent add stay-home plan and the agent is cloned) 
 *  -calibrates it if wanted(using cadyts as strategy in config file)
 */
public class OverDemandPlan_router {
	private ScenarioImpl scenario;
	private Config config;
	private PopulationWriter popwriter;
	
	public OverDemandPlan_router(final ScenarioImpl scenario){
		this.scenario = scenario;
		this.config = this.scenario.getConfig();
	}
	
	/**
	 * returns the config file of the over-demand plan 
	 */
	public String run(String[] paramValuesArray, int numHomePlans, int numClons){
		String splitter= "_";
		String[] routedPlanArray = new String[paramValuesArray.length];

		for (int i=0; i<paramValuesArray.length; i++){
			//read parameter combination
			String strComb =  paramValuesArray[i];
			String[] strComb2 = strComb.split(splitter);
			
			System.out.println("beta walk: " + strComb2[0] );
			System.out.println("beta distance: " + strComb2[1] );
			System.out.println("beta transfer: " + strComb2[2] );

			//route
			AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(this.scenario);
			adaptedLauncher.set_betaWalk(Double.parseDouble(strComb2[0]));
			adaptedLauncher.set_betaDistance(Double.parseDouble(strComb2[1]));
			adaptedLauncher.set_betaTransfer(Double.parseDouble(strComb2[2]));
			routedPlanArray[i] = adaptedLauncher.route();
			adaptedLauncher= null;
		}
		
		//merge plans
		PlansMerger plansMerger = new PlansMerger();
		plansMerger.setNetFilePath(this.scenario.getConfig().network().getInputFile());
		Population mergedPop= plansMerger.plansAggregator(routedPlanArray);
		
		//clone plans and add stay home plans
		Population clonedPop = new OverDemandPlanCreator(mergedPop).run(numHomePlans, numClons);
		mergedPop= null;
		
		//write plan in config-output directory
		String overEstimDemPlansFile = scenario.getConfig().controler().getOutputDirectory() + "overEstimatedDemandPlans.xml.gz";		
		System.out.println("writing output cloned plan file..." +  overEstimDemPlansFile);
		popwriter = new PopulationWriter(clonedPop, scenario.getNetwork());
		popwriter.write(overEstimDemPlansFile);
		
		//create the new config file
		String oldOutdir= this.config.controler().getOutputDirectory();
		String newOuputdirPath = this.config.controler().getOutputDirectory() + "outputCal/";
		this.config.setParam("plans", "inputPlansFile", overEstimDemPlansFile );
		this.config.setParam("controler", "outputDirectory", newOuputdirPath);

		ConfigWriter configWriter = new ConfigWriter(this.config);
		String configClonedFile = oldOutdir + "configOverEstimatedDemandPlans.xml";
		System.out.println(this.config.controler().getOutputDirectory());
		System.out.println(configClonedFile);
		configWriter.write(configClonedFile);
		
		oldOutdir = null;
		this.config = null;
		this.scenario= null;
		configWriter= null;
		newOuputdirPath = null;
		
		return configClonedFile; 
	}

	public static void main(String[] args) {
		String configFile= null;
		String[] valuesArray = null;
		int numHomePlans;
		int numClons;
		
		if (args.length>0){
			configFile = args[0];	  //config comb1 comb2 comb3 clones homPlans
			valuesArray = new String[3];
			valuesArray[0]=args[1];
			valuesArray[1]=args[2];
			valuesArray[2]=args[3];
			numClons = Integer.valueOf(args[4]);
			numHomePlans = Integer.valueOf(args[5]);
		}else{
			configFile = "../../berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			valuesArray = new String[3];
			valuesArray[0] = "6_0.0_1200";
			valuesArray[1] = "10_0.0_240";
			valuesArray[2] = "8_0.5_720";
		
			numClons = 0;
			numHomePlans = 1;
		}
		
		ScenarioImpl scn = new DataLoader().loadScenario(configFile);
		new OverDemandPlan_router(scn).run(valuesArray, numHomePlans,numClons);
	}

}
