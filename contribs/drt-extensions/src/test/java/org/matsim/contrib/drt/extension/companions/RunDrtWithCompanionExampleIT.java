/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.companions;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Steffen Axer
 */
public class RunDrtWithCompanionExampleIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunDrtWithCompanions() {
		MatsimRandom.reset();
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, new OTFVisConfigGroup(), new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), dvrpConfigGroup);

		// Add DrtCompanionParams with some default values into existing Drt configurations
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.getModalElements().iterator().next();

		DrtCompanionParams crtCompanionParams = new DrtCompanionParams();
		crtCompanionParams.setDrtCompanionSamplingWeights(List.of(0.5,0.2,0.1,0.1,0.1));

		drtWithExtensionsConfigGroup.addParameterSet(crtCompanionParams);

		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		ConfigGroup zoneSystemParams = matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		matrixParams.addParameterSet(zoneSystemParams);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = DrtCompanionControlerCreator.createControler(config);
		controler.run();

		int[] actualRides = getTotalNumberOfDrtRides();
		Assertions.assertThat(actualRides[0]).isEqualTo(378);
		Assertions.assertThat(actualRides[1]).isEqualTo(706);
	}

	@Test
	void testRunDrtWithCompanionsMultiThreaded() {
		MatsimRandom.reset();
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, new OTFVisConfigGroup(), new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), dvrpConfigGroup);

		// Add DrtCompanionParams with some default values into existing Drt configurations
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.getModalElements().iterator().next();

		DrtCompanionParams crtCompanionParams = new DrtCompanionParams();
		crtCompanionParams.setDrtCompanionSamplingWeights(List.of(0.5,0.2,0.1,0.1,0.1));

		drtWithExtensionsConfigGroup.addParameterSet(crtCompanionParams);

		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		ConfigGroup zoneSystemParams = matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		matrixParams.addParameterSet(zoneSystemParams);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.qsim().setNumberOfThreads(2);

		Controler controler = DrtCompanionControlerCreator.createControler(config);
		controler.run();

		int[] actualRides = getTotalNumberOfDrtRides();
		Assertions.assertThat(actualRides[0]).isEqualTo(375);
		Assertions.assertThat(actualRides[1]).isEqualTo(699);
	}

	private int[] getTotalNumberOfDrtRides() {
		String filename = utils.getOutputDirectory() + "/drt_customer_stats_drt.csv";

		final List<String> collect;
		try {
			collect = Files.lines(Paths.get(filename)).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int size = collect.size();
		List<String> keys = List.of(collect.get(0).split(";"));
		List<String> lastIterationValues = List.of(collect.get(size - 1).split(";"));

		int ridesRequestIndex = keys.indexOf("rides");
        int ridesRequest = Integer.parseInt(lastIterationValues.get(ridesRequestIndex));

		int ridesPaxIndex = keys.indexOf("rides_pax");
		int ridesPax = Integer.parseInt(lastIterationValues.get(ridesPaxIndex));

		return new int[]{ridesRequest, ridesPax};
	}
}
