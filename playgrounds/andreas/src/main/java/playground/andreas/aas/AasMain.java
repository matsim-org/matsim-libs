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

package playground.andreas.aas;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

/**
 * 
 * @author aneumann
 *
 */
public class AasMain {
	
	private final static Logger log = Logger.getLogger(AasMain.class);
	
	private final Config config;
	private final String baseFolder;
	private final String iterationOutputDir;
	private final String eventsFile;

	private MutableScenario scenario;
	private Collection<SimpleFeature> shapeFile;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String baseFolder = "F:/anaTest/";
		final String iteration = "1000";
		final String CONFIGFILE = "run75_config.xml";
		final String AASRUNNERCONFIGFILE = null;

//		final String COOPLOGGERFILE = "f:/p_runs/txl/" + runId + "/" + runId + ".pCoopLogger.txt";
//		final String ALLLINESSHAPEOUTFILE = "f:/p_runs/txl/" + runId + "/it." + iteration + "/" + runId + "." + iteration + ".transitSchedule.shp";
//		final String PARAINBUSINESSSHAPEOUTFILE = "f:/p_runs/txl/" + runId + "/it." + iteration + "/" + runId + "." + iteration + ".transitSchedule_para_in_business.shp";
//		final int removeAllParatransitLinesYoungerThanIteration = 2729;
		
		AasMain aM = new AasMain(baseFolder, CONFIGFILE, iteration);
		String shapeFile = aM.getConfig().findParam(PConfigGroup.GROUP_NAME, "serviceAreaFile");
		aM.readFiles(aM.getBaseFolder() + shapeFile.substring(2));
		
		AasRunner aR = new AasRunner(aM.getScenario(), aM.getBaseFolder(), aM.getIterationOutputDir(), aM.getEventsFile(), aM.getShapeFile());
		aR.init(AASRUNNERCONFIGFILE);
		aR.preProcess();
		aR.run();
		aR.postProcess();
		aR.writeResults();

	}

	
	public AasMain(String baseFolder, String configFile, String iteration) {
		Gbl.startMeasurement();
		log.info("init...");
		
		this.baseFolder = baseFolder;
		this.config = ConfigUtils.loadConfig(this.baseFolder + configFile);
		this.iterationOutputDir = this.baseFolder + this.config.controler().getOutputDirectory().substring(2) + "/ITERS/it." + iteration + "/";
		this.eventsFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".events.xml.gz";
		
		String networkFile = this.baseFolder + this.config.network().getInputFile().substring(2);
		log.info("Setting network to " + networkFile);
		this.config.network().setInputFile(networkFile);
		
		String popFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".plans.xml.gz";
		log.info("Setting population to " + popFile);
		this.config.plans().setInputFile(popFile);
		
		String transitScheduleFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".transitSchedule.xml.gz";
		log.info("Setting transit schedule to " + transitScheduleFile);
		this.config.transit().setTransitScheduleFile(transitScheduleFile);
		
		String vehiclesFile = this.iterationOutputDir + this.config.controler().getRunId() + "." + iteration + ".vehicles.xml.gz";
		log.info("Setting vehicles to " + vehiclesFile);
		this.config.transit().setVehiclesFile(vehiclesFile);
		
		log.info("init... done.");
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}


	private void readFiles(String shapeFile) {
		log.info("Reading scenario...");
		this.scenario = (MutableScenario) ScenarioUtils.loadScenario(this.config);
		log.info("Reading scenario... done");

		log.info("Reading shapeFile " + shapeFile);
		this.shapeFile = new ShapeFileReader().readFileAndInitialize(shapeFile);
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}


	private Config getConfig() {
		return config;
	}


	private String getBaseFolder() {
		return baseFolder;
	}


	private String getIterationOutputDir() {
		return iterationOutputDir;
	}


	private String getEventsFile() {
		return this.eventsFile;
	}


	private MutableScenario getScenario() {
		return this.scenario;
	}
	
	private Collection<SimpleFeature> getShapeFile() {
		return this.shapeFile;
	}
}
