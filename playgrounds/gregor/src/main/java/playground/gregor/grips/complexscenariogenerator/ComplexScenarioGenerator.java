/* *********************************************************************** *
 * project: org.matsim.*
 * ComplexScenarioGenerator.java
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

package playground.gregor.grips.complexscenariogenerator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.population.PopulationWriter;

public class ComplexScenarioGenerator extends ScenarioGenerator {

	public ComplexScenarioGenerator(String config) {
		super(config);
	}

	
//	@Override
//	protected void generateAndSaveNetwork(Scenario sc) {
//
//		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
//		String gripsNetworkFile = gcm.getNetworkFileName();
//
//		// Step 1 raw network input
//		// for now grips network meta format is osm
//		// Hamburg example UTM32N. In future coordinate transformation should be performed beforehand
//		CoordinateTransformation ct =  new GeotoolsTransformation("WGS84", this.c.global().getCoordinateSystem());
//		OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct, true);
//		//				reader.setHighwayDefaults(1, "motorway",4, 5.0/3.6, 1.0, 10000,true);
//		//				reader.setHighwayDefaults(1, "motorway_link", 4,  5.0/3.6, 1.0, 10000,true);
//		//		reader.setHighwayDefaults(2, "trunk",         2,  30.0/3.6, 1., 10000);
//		//		reader.setHighwayDefaults(2, "trunk_link",    2,  30.0/3.6, 1.0, 10000);
//		//		reader.setHighwayDefaults(3, "primary",       2,  30.0/3.6, 1.0, 10000);
//		//		reader.setHighwayDefaults(3, "primary_link",  2,  30.0/3.6, 1.0, 10000);
//		//		reader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 5000);
//		//		reader.setHighwayDefaults(5, "tertiary",      1,  30.0/3.6, 1.0,  5000);
//		//		reader.setHighwayDefaults(6, "minor",         1,  30.0/3.6, 1.0,  5000);
//		//		reader.setHighwayDefaults(6, "unclassified",  1,  30.0/3.6, 1.0,  5000);
//		//		reader.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  5000);
//		//		reader.setHighwayDefaults(6, "living_street", 1,  30.0/3.6, 1.0,  5000);
//		//		reader.setHighwayDefaults(6,"path",           1,  5.0/3.6, 1.0,  2500);
//		//		reader.setHighwayDefaults(6,"cycleay",        1,  5.0/3.6, 1.0,  2500);
//		//		reader.setHighwayDefaults(6,"footway",        1,  5.0/3.6, 1.0,  1000);
//		reader.setKeepPaths(true);
//		reader.parse(gripsNetworkFile);
//
//		String networkOutputFile = gcm.getOutputDir()+"/network.xml.gz";
////		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
////		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
//		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
//		sc.getConfig().network().setInputFile(networkOutputFile);
//	}

	
	@Override
	protected void generateAndSavePopulation(Scenario sc) {
		// for now a simple ESRI shape file format is used to emulated the a more sophisticated not yet defined population meta format
		EvacuationConfigModule gcm = getEvacuationConfig(sc.getConfig());
		String gripsPopulationFile = gcm.getPopulationFileName();
		new ComplexPopulationFromESRIShapeFileGenerator(sc, gripsPopulationFile).run();

		String outputPopulationFile = gcm.getOutputDir() + "/population.xml.gz";
		new PopulationWriter(sc.getPopulation(), sc.getNetwork(), gcm.getSampleSize()).write(outputPopulationFile);
		sc.getConfig().plans().setInputFile(outputPopulationFile);

		((SimulationConfigGroup) sc.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).setStorageCapFactor(gcm.getSampleSize());
		((SimulationConfigGroup) sc.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).setFlowCapFactor(gcm.getSampleSize());

		ActivityParams pre = new ActivityParams("pre-evac");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		ActivityParams post = new ActivityParams("post-evac");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);
		
		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);
		
		//		sc.getConfig().planCalcScore().addParam("activityPriority_0", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityPriority_1", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_1", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_1", "00:00:49");


	}
	
	public static void main(String [] args) {
		if (args.length != 1) {
			printUsage();
			System.exit(-1);
		}

		new ComplexScenarioGenerator(args[0]).run();

	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("ComplexScenarioGenerator");
		System.out.println("Generates a MATSim scenario from meta format input files.");
		System.out.println();
		System.out.println("usage : ComplexScenarioGenerator config-file");
		System.out.println();
		System.out.println("config-file:   A MATSim config file that gives the location to the input files needed for creating an evacuation scenario.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2012, matsim.org");
		System.out.println();
	}
	
}
