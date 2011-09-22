/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.ActivityToZoneMappingWriter;
import playground.mrieser.svi.data.CalculateActivityToZoneMapping;
import playground.mrieser.svi.data.DynamicODMatrix;
import playground.mrieser.svi.data.DynusTDynamicODDemandWriter;
import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.Zones;
import playground.mrieser.svi.replanning.DynamicODDemandCollector;

public class SviController {

	private final static Logger log = Logger.getLogger(SviController.class);
	
	private final Scenario scenario;
	private final ActivityToZoneMapping actToZoneMapping;
	private final ZoneIdToIndexMapping zoneIndexMapping;
	private final Zones zones = new Zones();
	private final String dynustDir;
	private final String modelDir;
	private final String outputDir;
	private String iterationDir = null;
	private int iteration = -1;

	public SviController(final String networkFile, final String populationFile, final String zonesShapeFile, final String zoneIdToIndexMappingFile, final String dynustDir, final String modelDir, final String outputDir) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.actToZoneMapping = new ActivityToZoneMapping();
		this.zoneIndexMapping = new ZoneIdToIndexMapping();

		log.info("Reading network...");
		new MatsimNetworkReader(this.scenario).readFile(networkFile);
		log.info("Reading population...");
		new MatsimPopulationReader(this.scenario).readFile(populationFile);
		log.info("Reading zones...");
		new ShapeZonesReader(this.zones).readShapefile(zonesShapeFile);
		log.info("Reading zone mapping...");
		new ZoneIdToIndexMappingReader(this.zoneIndexMapping).readFile(zoneIdToIndexMappingFile);
		
		this.dynustDir = dynustDir;
		this.modelDir = modelDir;
		this.outputDir = outputDir;
		
		File outDir = new File(outputDir);
		if (!outDir.exists()) {
			log.info("Creating output directory " + outputDir);
			outDir.mkdirs();
		}
	}

	public void run() {
		log.info("Analyzing zones for Population...");
		new CalculateActivityToZoneMapping(this.actToZoneMapping, this.zones).run(this.scenario.getPopulation());
		new ActivityToZoneMappingWriter(this.actToZoneMapping).writeFile("actToZoneMapping.txt");
		
		log.info("Starting Iterations...");
		for (this.iteration = 0; this.iteration < 6; this.iteration++) {
			this.iterationDir = this.outputDir + "/it." + this.iteration;
			new File(this.iterationDir).mkdir();
			selectPlans();
			createOdMatrix();
			runMobsim();
			calcScores();
		}
	}
	
	public void selectPlans() {
		
	}
	
	public void createOdMatrix() {
		DynamicODMatrix odm = new DynamicODMatrix(10*60, 24*60*60);
		DynamicODDemandCollector collector = new DynamicODDemandCollector(odm, this.actToZoneMapping);
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			collector.run(plan);
		}
		
		DynusTDynamicODDemandWriter writer = new DynusTDynamicODDemandWriter(odm, zoneIndexMapping);
		writer.writeTo(this.iterationDir + "/demand.dat");
	}
	
	public void runMobsim() {
		DynusTExe exe = new DynusTExe(this.dynustDir, this.modelDir, this.iterationDir);
		exe.runDynusT();
		System.out.println("dynust is not called yet");
	}
	
	public void calcScores() {
		
	}

	private static void printUsage() {
		System.out.println("SviController inNetwork inPopulation");
		System.out.println("");
	}

	public static void main(String[] args) {
		
		args = new String[] {"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/network.xml", 
				"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/population.xml", 
				"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/Test_Zonen.shp",
				"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/L41_Kreuzlingen_Konstanz_Nachfrage/l41 ZoneNo_TAZ_mapping.csv", 
				"/Volumes/Data/virtualbox/exchange/kreuzlingen/3.0x86 Beta",
//				"C:\\Program Files\\FHWA\\DynusT (Dynamic Urban Systems in Transportation) x86\\3.0x86 Beta",
				"/Volumes/Data/virtualbox/exchange/kreuzlingen/test",
//				"F:\\kreuzlingen\\test",
				"/Volumes/Data/virtualbox/exchange/kreuzlingen/testRun"
//				"F:\\kreuzlingen\\testRun"
		};
		
		
		if (args.length != 7) {
			printUsage();
			return;
		}
		SviController c = new SviController(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
		c.run();
	}
}
