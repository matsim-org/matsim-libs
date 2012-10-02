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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.ActivityToZoneMappingWriter;
import playground.mrieser.svi.data.CalculateActivityToZoneMapping;
import playground.mrieser.svi.data.DynamicODMatrix;
import playground.mrieser.svi.data.DynusTDynamicODDemandWriter;
import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.Zones;
import playground.mrieser.svi.data.vehtrajectories.CalculateTravelTimeMatrixFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.DynamicTravelTimeMatrix;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;
import playground.mrieser.svi.replanning.DynamicODDemandCollector;

/**
 * @author mrieser / senozon
 */
@Deprecated
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

	public SviController(final String configFilename) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFilename));
		Config config = this.scenario.getConfig();

		String networkFile = config.network().getInputFile();
		String populationFile = config.plans().getInputFile();
		String zonesShapeFile = config.getParam("dynus-t", "zonesShapes");
		String zoneIdToIndexMappingFile = config.getParam("dynus-t", "zonesMapping");
		this.dynustDir = config.getParam("dynus-t", "dynusTDirectory");
		this.modelDir = config.getParam("dynus-t", "modelDirectory");
		this.outputDir = config.getParam("dynus-t", "outputDirectory");

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

		File outDir = new File(this.outputDir);
		if (!outDir.exists()) {
			log.info("Creating output directory " + this.outputDir);
			outDir.mkdirs();
		}
	}



	public void run() {
		log.info("Analyzing zones for Population...");
		new CalculateActivityToZoneMapping(this.actToZoneMapping, this.zones, "id").run(this.scenario.getPopulation());
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
		dumpPlans();
	}



	public void selectPlans() {
		ExpBetaPlanSelector planSelector = new ExpBetaPlanSelector(this.scenario.getConfig().planCalcScore());

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = ((PersonImpl) person).getRandomUnscoredPlan();
			if (plan == null) {
				plan = planSelector.selectPlan(person);
			}
			((PersonImpl) person).setSelectedPlan(plan);
		}
	}



	public void createOdMatrix() {
		DynamicODMatrix odm = new DynamicODMatrix(10*60, 24*60*60);
		DynamicODDemandCollector collector = new DynamicODDemandCollector(odm, this.actToZoneMapping);

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			collector.run(plan);
		}

		DynusTDynamicODDemandWriter writer = new DynusTDynamicODDemandWriter(odm, this.zoneIndexMapping);
		writer.writeTo(this.iterationDir + "/demand.dat");
	}



	public void runMobsim() {
		DynusTExe exe = new DynusTExe(this.dynustDir, this.modelDir, this.iterationDir);
		exe.runDynusT();
		System.out.println("dynust is not called yet");
	}



	public void calcScores() {
		String vehTrajFilename = this.dynustDir + "/VehTrajectory.dat";
		DynamicTravelTimeMatrix matrix = new DynamicTravelTimeMatrix(600, 30*3600.0); // 10min time bins, at most 30 hours
		new VehicleTrajectoriesReader(new CalculateTravelTimeMatrixFromVehTrajectories(matrix), this.zoneIndexMapping).readFile(vehTrajFilename);

		PlanScorer scorer = new PlanScorer(matrix, this.actToZoneMapping);
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			scorer.calculateScore(plan);
		}
	}



	public void dumpPlans() {
		String plansFilename = this.outputDir + "/output_plans.xml.gz";
		new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork()).writeV5(plansFilename);
	}



	private static void printUsage() {
		System.out.println("SviController configFile");
		System.out.println("");
	}



	public static void main(final String[] args) {
		if (args.length != 1) {
			printUsage();
			return;
		}

		SviController c = new SviController(args[0]);
		c.run();
	}
}
