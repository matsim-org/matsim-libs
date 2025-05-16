///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2017 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package org.matsim.contrib.accessibility.run;
//
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.RegisterExtension;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
//import org.matsim.contrib.accessibility.AccessibilityModule;
//import org.matsim.contrib.accessibility.Modes4Accessibility;
//import org.matsim.contrib.drt.estimator.DrtEstimator;
//import org.matsim.contrib.drt.estimator.impl.DirectTripDistanceBasedDrtEstimator;
//import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
//import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
//import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
//import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
//import org.matsim.contrib.drt.run.*;
//import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
//import org.matsim.contrib.dvrp.run.DvrpModule;
//import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.AbstractModule;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.io.IOUtils;
//import org.matsim.examples.ExamplesUtils;
//import org.matsim.facilities.ActivityFacility;
//import org.matsim.testcases.MatsimTestUtils;
//import org.matsim.vis.otfvis.OTFVisConfigGroup;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.net.URL;
//
//import static org.ejml.UtilEjml.assertTrue;
//
//
///**
// * @author jbischoff
// * @author Sebastian Hörl, IRT SystemX (sebhoerl)
// */
//public class RunDrtAccessibilityExampleIT {
//
//	@RegisterExtension
//	public MatsimTestUtils utils = new MatsimTestUtils();
//
//	@Test
//	public void testRunDrtStopbasedExample() throws IOException {
//		Id.resetCaches();
//		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
//				"mielec_stop_based_drt_config.xml");
//		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
//				new OTFVisConfigGroup());
//
//
//		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
//		config.controller().setOutputDirectory(utils.getOutputDirectory());
//
//		// add accessibility config options
//		{
//			final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//			double minX = -2650.4674;
//			double maxX = 10357.8981;
//			double minY = -13215.5862;
//			double maxY = 3406.2142;
//			acg.setTileSize_m(100);
//			acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
//			acg.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);
//			acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
//			acg.setBoundingBoxBottom(minY);
//			acg.setBoundingBoxTop(maxY);
//			acg.setBoundingBoxLeft(minX);
//			acg.setBoundingBoxRight(maxX);
//			acg.setUseParallelization(false);
//
//			config.routing().setRoutingRandomness(0.);
//
//		}
//
//
//		// -----------
//		// Following is inlined from RunDrtExample.run(config, false);
//		{
//			MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
//			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());
//
//			// Added following so that agent can always walk to drt stop
//			for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
//				DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
//					(DefaultDrtOptimizationConstraintsSet) drtCfg.addOrGetDrtOptimizationConstraintsParams()
//						.addOrGetDefaultDrtOptimizationConstraintsSet();
//				defaultConstraintsSet.maxWalkDistance = Double.MAX_VALUE;
//				drtCfg.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
//
//			}
//
//			Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
//
//			// add supermarket facility
//			{
//				// Node 16 (link 222) is in the west of Mielec
//				ActivityFacility s1 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s1", ActivityFacility.class), new Coord(299.5735,-3886.1963));//Id.createLinkId("222"));
//				s1.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("supermarket"));
//				scenario.getActivityFacilities().addActivityFacility(s1);
//			}
//
//			ScenarioUtils.loadScenario(scenario);
//
//			Controler controler = new Controler(scenario);
//			controler.addOverridingModule(new DvrpModule());
//			controler.addOverridingModule(new MultiModeDrtModule());
//			controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));
//
//			//add accessibility module
//			{
//				final AccessibilityModule accModule = new AccessibilityModule();
//				accModule.setConsideredActivityType("supermarket");
//				controler.addOverridingModule(accModule);
//			}
//
//
//			controler.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					bind(DrtEstimator.class).toInstance(new DirectTripDistanceBasedDrtEstimator.Builder()
//						.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(103.34))
//						.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, 0.0))
//						.setRideDurationEstimator(new ConstantRideDurationEstimator(0.1087, 47.84)) // TODO: I'm abusing this method a bit. It's supposed to calculate drt ride duration based on car ride duration; in my case it is based on car ride **distance**
//						.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, 0.0))
//						.build());
//				}
//			});
//
//
//			controler.run();
//		}
//		// -----------
//
//		// compare results:
//		String filenameExpected = utils.getInputDirectory() + "/supermarket/accessibilities.csv";
//		String filenameActual = utils.getOutputDirectory() + "/supermarket/accessibilities.csv";
//		assertTrue(compareFilesLineByLine(filenameExpected, filenameActual), "Files are not equal");
//
//
//	}
//
//	/**
//	 * Compares two text files line by line.
//	 *
//	 * @param file1Path Path of the first file.
//	 * @param file2Path Path of the second file.
//	 * @throws IOException If an I/O error occurs.
//	 */
//	public boolean compareFilesLineByLine(String file1Path, String file2Path) throws IOException {
//		try (BufferedReader reader1 = new BufferedReader(new FileReader(file1Path));
//			 BufferedReader reader2 = new BufferedReader(new FileReader(file2Path))) {
//
//			String line1, line2;
//			int lineNumber = 1;
//
//			// Read lines from both files until the end
//			while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
//				// Compare each line
//				if (!line1.equals(line2)) {
//					System.out.println("Mismatch at line " + lineNumber + ":");
//					System.out.println("File 1: " + line1);
//					System.out.println("File 2: " + line2);
//					return false;
//				}
//				lineNumber++;
//			}
//
//			// Check if one file has extra lines
//			if (reader1.readLine() != null || reader2.readLine() != null) {
//				return false;
//			}
//
//			return true;
//		}
//	}
//}
