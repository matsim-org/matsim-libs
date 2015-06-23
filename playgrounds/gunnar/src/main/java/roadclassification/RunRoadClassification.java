/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TunnelMain.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package roadclassification;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import opdytsintegration.MATSimDecisionVariableSetEvaluator;
import optdyts.ObjectiveFunction;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.OsmNetworkReader;

public class RunRoadClassification {

	static void justRun() {
		System.out.println("STARTED ...");
		Scenario scenario = ScenarioUtils.createScenario(createConfig());
		try (InputStream is = new FileInputStream(
				DownloadExampleData.SIOUX_FALLS)) {
			OsmNetworkReader osmNetworkReader = new OsmNetworkReader(
					scenario.getNetwork(),
					DownloadExampleData.COORDINATE_TRANSFORMATION);
			osmNetworkReader.parse(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final Controler controler = new Controler(scenario);
		controler.run();
		System.out.println("... DONE.");
	}

	static void optimize() {
		System.out.println("STARTED ...");
		Scenario scenario = ScenarioUtils.createScenario(createConfig());
		try (InputStream is = new FileInputStream(
				DownloadExampleData.SIOUX_FALLS)) {
			OsmNetworkReader osmNetworkReader = new OsmNetworkReader(
					scenario.getNetwork(),
					DownloadExampleData.COORDINATE_TRANSFORMATION);
			osmNetworkReader.parse(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final Controler controler = new Controler(scenario);
		final RoadClassificationStateFactory stateFactory = new RoadClassificationStateFactory();
		final ObjectiveFunction<RoadClassificationState> objectiveFunction = new RoadClassificationObjectiveFunction();
		final Set<AbstractRoadClassificationDecisionVariable> decisionVariables = new LinkedHashSet<>();

		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// default capacities
				// motorway 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 2000, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,
						80.0 / 3.6, 1.0, 1500, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 1, 80.0 / 3.6,
						1.0, 2000);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1000);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 300);

			}
		});
		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// Derived capacities (from triangular fundamental diagram)
				// motorway 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 1900, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,
						80.0 / 3.6, 1.0, 1800, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 1, 80.0 / 3.6,
						1.0, 1800);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1800);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 1400);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 1000);
			}
		});

		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// default capacities
				// no multi-lane roads
				osmNetworkReader.setHighwayDefaults(1, "motorway", 1,
						120.0 / 3.6, 1.0, 2000, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,
						80.0 / 3.6, 1.0, 1500, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 1, 80.0 / 3.6,
						1.0, 2000);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1000);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 300);

			}
		});
		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// Derived capacities (from triangular fundamental diagram)
				// no multi-lane roads
				osmNetworkReader.setHighwayDefaults(1, "motorway", 1,
						120.0 / 3.6, 1.0, 1900, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,
						80.0 / 3.6, 1.0, 1800, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 1, 80.0 / 3.6,
						1.0, 1800);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1800);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 1400);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 1000);
			}
		});

		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// default capacities
				// motorway, trunk 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 2000, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 2,
						80.0 / 3.6, 1.0, 1500, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 2, 80.0 / 3.6,
						1.0, 2000);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1000);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 300);

			}
		});
		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// Derived capacities (from triangular fundamental diagram)
				// motorway, trunk 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 1900, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 2,
						80.0 / 3.6, 1.0, 1800, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 2, 80.0 / 3.6,
						1.0, 1800);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 1,
						50.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(3, "primary", 1,
						80.0 / 3.6, 1.0, 1800);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 1400);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 1000);
			}
		});

		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// default capacities
				// motorway, trunk, primary 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 2000, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 2,
						80.0 / 3.6, 1.0, 1500, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 2, 80.0 / 3.6,
						1.0, 2000);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 2,
						50.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary", 2,
						80.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1500);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1000);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 600);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 300);

			}
		});
		decisionVariables.add(new AbstractRoadClassificationDecisionVariable(
				scenario.getNetwork()) {
			@Override
			void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader) {
				// Derived capacities (from triangular fundamental diagram)
				// motorway, trunk, primary 2 instead of 1 lane
				osmNetworkReader.setHighwayDefaults(1, "motorway", 2,
						120.0 / 3.6, 1.0, 1900, true);
				osmNetworkReader.setHighwayDefaults(1, "motorway_link", 2,
						80.0 / 3.6, 1.0, 1800, true);
				osmNetworkReader.setHighwayDefaults(2, "trunk", 2, 80.0 / 3.6,
						1.0, 1800);
				osmNetworkReader.setHighwayDefaults(2, "trunk_link", 2,
						50.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(3, "primary", 2,
						80.0 / 3.6, 1.0, 1800);
				osmNetworkReader.setHighwayDefaults(3, "primary_link", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(4, "secondary", 1,
						60.0 / 3.6, 1.0, 1700);
				osmNetworkReader.setHighwayDefaults(5, "tertiary", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "minor", 1, 45.0 / 3.6,
						1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "unclassified", 1,
						45.0 / 3.6, 1.0, 1600);
				osmNetworkReader.setHighwayDefaults(6, "residential", 1,
						30.0 / 3.6, 1.0, 1400);
				osmNetworkReader.setHighwayDefaults(6, "living_street", 1,
						15.0 / 3.6, 1.0, 1000);
			}
		});

		// AND RUN THE ENTIRE THING

		// final double convergenceNoiseVarianceScale = 0.01;
		final double maximumRelativeGap = 0.05;
		
		final MATSimDecisionVariableSetEvaluator<RoadClassificationState, AbstractRoadClassificationDecisionVariable> predictor = new MATSimDecisionVariableSetEvaluator<RoadClassificationState, AbstractRoadClassificationDecisionVariable>(
				decisionVariables, objectiveFunction,
				// convergenceNoiseVarianceScale, 
				stateFactory, 5, maximumRelativeGap);
		predictor.setLogFileName("roadclassification-log.txt");
		predictor.setMemory(1);
		predictor.setBinSize_s(15 * 60);
		predictor.setBinCnt(24 * 4);

		controler.addControlerListener(predictor);
		controler.run();

		System.out.println("... DONE.");
	}

	public static void main(String[] args) throws FileNotFoundException {

		// justRun();
		optimize();

	}

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/roadclassification");
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		return config;
	}

}
