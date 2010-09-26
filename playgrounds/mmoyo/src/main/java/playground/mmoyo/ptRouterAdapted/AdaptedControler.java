/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
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

package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.mmoyo.utils.TransScenarioLoader;

/**
 * @author manuel
 * 
 * runs transit simulation with adapter router
  */
public class AdaptedControler extends Controler {

	public AdaptedControler(final ScenarioImpl scenario) {
		super(scenario);
	}
	
	public AdaptedControler(final Config config) {
		super(config);
	}
	
	//loads the AdaptedStrategyManagerConfigLoader to get the MmoyoTimeAllocationMutatorReRoute strategy
	/*
	@Override
	protected StrategyManagerImpl loadStrategyManager() {
		StrategyManagerImpl manager = new StrategyManagerImpl();
		AdaptedStrategyManagerConfigLoader.load(this, manager);
		return manager;
	}
	*/
		
	
	/*
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		//Avoids creating the controler default routing algorithm
		return null;
	}
	*/

	public static void main(String[] args){
		String configFile; 
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/test/input/playground/mmoyo/EquilCalibration/equil_config.xml";
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			//configFile = "../playgrounds/mmoyo/test/input/playground/mmoyo/EquilCalibration/equil_config.xml";
		}

		if (!new File(configFile).exists()) {
			try {
				throw new FileNotFoundException(configFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFile);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);

		Controler controler = new AdaptedControler( config ) ;
		controler.setCreateGraphs(true);
		controler.setOverwriteFiles(true);
		controler.setWriteEventsInterval(5); 
		controler.run();
	}
}
