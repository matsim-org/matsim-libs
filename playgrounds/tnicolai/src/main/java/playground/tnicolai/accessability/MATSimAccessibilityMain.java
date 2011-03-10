/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimAccessibilityMain.java
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

/**
 * 
 */
package playground.tnicolai.accessability;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thomas
 *
 */
public class MATSimAccessibilityMain {
	
	private static String configPath = null;
	
	private static Config config = null;
	
	private static ScenarioImpl scenario = null;

	/**
	 * constructor
	 * @param args
	 */
	public MATSimAccessibilityMain(String[] args){
		
		configPath = "/Users/thomas/SustainCity/Data/Zurich/config.xml";
		init();
	}
	
	/**
	 * init matsim config + scenario
	 */
	private void init(){
		config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile( configPath );
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
	}
	
	private void runMATSim(){
		
		
		
		
	}
	
	/**
	 * starting point
	 * @param args
	 */
	public static void main(String[] args){
		MATSimAccessibilityMain ma = new MATSimAccessibilityMain(args);
		ma.runMATSim();
	}
}

