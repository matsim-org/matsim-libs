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

import playground.mmoyo.ptRouterAdapted.AdaptedLauncher;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlansMerger;

/**
 *  -reads a base plan 
 *  -routes it with a given number of parameter combinations
 *  -creates it over-estimated version (agent add stay-home plan and the agent is cloned) 
 *  -calibrates it if wanted
 */
public class OverDemandPlan_router {
	private ScenarioImpl scenario;
	private Config config;
	private MyTransitRouterConfig myTransitRouterConfig;
	private PopulationWriter popwriter;
	
	public OverDemandPlan_router(final ScenarioImpl scenario){
		this.scenario = scenario;
		this.config = this.scenario.getConfig();
		
		myTransitRouterConfig = new MyTransitRouterConfig(this.scenario.getConfig().planCalcScore(),
				this.scenario.getConfig().plansCalcRoute() );
	}
	
	/**
	 * returns the config file of the over-demand plan 
	 */
	public String run(String[] valuesArray){
		String splitter= "_";
		String[] routedPlanArray = new String[valuesArray.length];

		for (int i=0; i<valuesArray.length; i++){
			//read parameter combination
			String strComb =  valuesArray[i];
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
		Population mergedPop= new PlansMerger().plansAggregator(routedPlanArray);     
		
		//clone plans and add stay home plans
		Population clonedPop = new OverDemandPlanCreator(mergedPop).run(3, 2);
		mergedPop= null;
		
		//write plan in config-output directory
		String overEstimDemPlansFile = scenario.getConfig().controler().getOutputDirectory() + "overEstimatedDemandPlans.xml";		
		System.out.println("writing output cloned plan file..." +  overEstimDemPlansFile);
		popwriter = new PopulationWriter(clonedPop, scenario.getNetwork());
		popwriter.write(overEstimDemPlansFile);
		FileCompressor fileCompressor = new FileCompressor();
		fileCompressor.run(overEstimDemPlansFile);
		overEstimDemPlansFile += ".gz";
		
		//create the new config file
		String oldOutdir= this.config.controler().getOutputDirectory();
		String newOuputdirPath = this.config.controler().getOutputDirectory() + "output/";
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
		
		if (args.length>0){
			configFile = args[0];	
			valuesArray = new String[args.length-1];
			for (int i=0; i<args.length;i++){
				valuesArray[i]=args[i+1];
			}
		
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			valuesArray = new String[3];
			valuesArray[0] = "10_0.0_1020";
			valuesArray[1] = "10_0.6_1020";
			valuesArray[2] = "10_0.0_00";
		}
		
		ScenarioImpl scn = new DataLoader().loadScenarioWithTrSchedule(configFile);
		new OverDemandPlan_router(scn).run(valuesArray);
	}

}
