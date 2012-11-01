/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.ikaddoura.aas;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author aneumann, ikaddoura
 *
 */
public class AasMain_ik {
	
	private final static Logger log = Logger.getLogger(AasMain_ik.class);
	
	private final Config config;
	private final String iterationOutputDir;
	private final String eventsFile;

	private ScenarioImpl scenario;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String iteration = "1";
		final String CONFIGFILE = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/test/test_config.xml";
		final String AASRUNNERCONFIGFILE = null;
		
		AasMain_ik aM = new AasMain_ik(CONFIGFILE, iteration);
		aM.readFiles(null);
		
		AasRunner_ik aR = new AasRunner_ik(aM.scenario, aM.iterationOutputDir, aM.eventsFile);
		aR.init(AASRUNNERCONFIGFILE);
		aR.preProcess();
		aR.run();
		aR.postProcess();
		aR.writeResults();

	}
	
	public AasMain_ik(String configFile, String iteration) {
		
		Gbl.startMeasurement();
	
		log.info("Setting files...");
		
		this.config = ConfigUtils.loadConfig(configFile);
		
		// assuming a runID to be set in the controler
		this.iterationOutputDir = this.config.controler().getOutputDirectory() + "/ITERS/it." + iteration + "/";
		this.eventsFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".events.xml.gz";
		
		String networkFile = this.config.network().getInputFile();
		log.info("Setting network to " + networkFile);
		this.config.network().setInputFile(networkFile);
		
		String popFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".plans.xml.gz";
		log.info("Setting population to " + popFile);
		this.config.plans().setInputFile(popFile);
		
		String transitScheduleFile = this.config.transit().getTransitScheduleFile();
		log.info("Setting transit schedule to " + transitScheduleFile);
		this.config.transit().setTransitScheduleFile(transitScheduleFile);
		
		String vehiclesFile = this.config.transit().getTransitScheduleFile();
		log.info("Setting vehicles to " + vehiclesFile);
		this.config.transit().setVehiclesFile(vehiclesFile);
		
		log.info("Setting files... done.");
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}


	private void readFiles(String shapeFile) {
		
		log.info("Reading scenario...");
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(this.config);
		log.info("Reading scenario... done.");
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}
}
