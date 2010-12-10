/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationFilter.java
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
package playground.tnicolai.accessability.utils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.SAXException;

/**
 * This class should help to create smaler samples of a plans file.
 * 
 * @author thomas
 *
 */
public class PopulationFilter {
	
	// logger
	private static final Logger log = Logger.getLogger(PopulationFilter.class);
	
	private static String configPath = null;
	private static String outputLocation = null;
	private static double fractionRate;
	
	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		
		log.info("Setting Arguments...");
		if(!setArguments(args)){
			log.error("Wrong arguments");
			return;
		}
		
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile( configPath );
		ScenarioImpl scenario = new ScenarioImpl(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
		
		System.gc();
		
		log.info("Start writing new population file");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), fractionRate).write(outputLocation);
		log.info("Finished...");
	}
	
	
	private static boolean setArguments(String args[]){
		try{
		configPath = args[0];
		outputLocation = args[1];
		fractionRate = Double.parseDouble(args[2]);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
