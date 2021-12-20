/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.alonso_mora.run;

import org.assertj.core.data.Percentage;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigurator;
import org.matsim.contrib.drt.extension.alonso_mora.MultiModeAlonsoMoraConfigGroup;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.*;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeModule;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.extension.shifts.run.ShiftDvrpFleetQsimModule;
import org.matsim.contrib.drt.extension.shifts.shift.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeQSimModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sebhoerl
 */
public class AlonsoMoraExamplesIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunAlonsoMora() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new MultiModeAlonsoMoraConfigGroup(), new OTFVisConfigGroup());

		AlonsoMoraConfigGroup amConfig = new AlonsoMoraConfigGroup();
		MultiModeAlonsoMoraConfigGroup.get(config).addParameterSet(amConfig);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		// Remove DRT rebalancer as we want to use AM rebalancer
		DrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();
		drtConfig.removeParameterSet(drtConfig.getRebalancingParams().get());

		// Load scenario
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		// Set up controller
		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));

		AlonsoMoraConfigurator.configure(controller, amConfig.getMode());
		controller.run();

		var expectedStats = Stats.newBuilder() //
				.rejectionRate(0.2) //
				.rejections(78) //
				.waitAverage(215.88) //
				.inVehicleTravelTimeMean(347.02) //
				.totalTravelTimeMean(562.9) //
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunAlonsoMoraWithShifts() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new MultiModeAlonsoMoraConfigGroup(), new OTFVisConfigGroup());

		AlonsoMoraConfigGroup amConfig = new AlonsoMoraConfigGroup();
		MultiModeAlonsoMoraConfigGroup.get(config).addParameterSet(amConfig);

		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		// Remove DRT rebalancer as we want to use AM rebalancer
		DrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();
		drtConfig.removeParameterSet(drtConfig.getRebalancingParams().get());

		// Load scenario
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		// Set up shifts and shift facilities

		DrtShiftsSpecification shifts = new DrtShiftsSpecificationImpl();
		{
			double baseStartTime = 6.0 * 3600.0;
			double shiftDuration = 8.0 * 3600.0;
			double breakDuration = 1800.0;
			double breakBuffer = 3.0 * 3600.0;
			int parallelShifts = 1;

			int shiftIndex = 0;

			Random random = new Random(0);
			double variability = 3600.0;

			while (baseStartTime + shiftDuration < 7.0 * 24.0 * 3600.0) {
				double shiftStartTime = baseStartTime + Math.round(variability * random.nextDouble());
				double shiftEndTime = shiftStartTime + shiftDuration;

				for (int k = 0; k < parallelShifts; k++) {
					double breakMiddleTime = shiftStartTime + 0.5 * shiftDuration;
					double earliestBreakStartTime = breakMiddleTime - 0.5 * breakBuffer;
					double earliestBreakEndTime = breakMiddleTime + 0.5 * breakBuffer;

					earliestBreakStartTime = Math.floor(earliestBreakStartTime);
					earliestBreakEndTime = Math.floor(earliestBreakEndTime);

					shifts.addShiftSpecification(DrtShiftSpecificationImpl.newBuilder() //
							.id(Id.create(shiftIndex, DrtShift.class)) //
							.start(shiftStartTime) //
							.end(shiftEndTime) //
							.shiftBreak(DrtShiftBreakSpecificationImpl.newBuilder() //
									.earliestStart(earliestBreakStartTime) //
									.latestEnd(earliestBreakEndTime) //
									.duration(breakDuration) //
									.build()) //
							.build());

					shiftIndex++;
				}

				baseStartTime += shiftDuration;
			}
		}

		List<Id<Link>> hubLinkIds = Arrays.asList("385", "499", "277", "52", "449").stream().map(Id::createLinkId)
				.collect(Collectors.toList());

		OperationFacilitiesSpecification operationFacilities = new OperationFacilitiesSpecificationImpl();
		int hubIndex = 0;
		for (Id<Link> hubLinkId : hubLinkIds) {
			operationFacilities.addOperationFacilitySpecification(OperationFacilitySpecificationImpl.newBuilder()//
					.id(Id.create("hub" + (++hubIndex), OperationFacility.class)) //
					.linkId(hubLinkId) //
					.coord(scenario.getNetwork().getLinks().get(hubLinkId).getCoord()) //
					.capacity(50) //
					.type(OperationFacilityType.hub) //
					.build());
		}

		// Set up controller
		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			controller.addOverridingModule(new ShiftDrtModeModule(drtCfg, shiftDrtConfigGroup));
			controller.addOverridingQSimModule(new DrtModeQSimModule(drtCfg, new ShiftDrtModeOptimizerQSimModule(drtCfg, shiftDrtConfigGroup)));
			controller.addOverridingQSimModule(new ShiftDvrpFleetQsimModule(drtCfg.getMode()));
		}

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(DrtShiftsSpecification.class).toInstance(shifts);
				bindModal(OperationFacilitiesSpecification.class).toInstance(operationFacilities);
			}
		});

		AlonsoMoraConfigurator.configure(controller, amConfig.getMode());
		controller.run();

		var expectedStats = Stats.newBuilder() //
				.rejectionRate(0.2) //
				.rejections(78) //
				.waitAverage(215.88) //
				.inVehicleTravelTimeMean(347.02) //
				.totalTravelTimeMean(562.9) //
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	/**
	 * Early warning system: if customer stats vary more than the defined percentage
	 * above or below the expected values then the following unit tests will fail.
	 * This is meant to serve as a red flag. The following customer parameter
	 * checked are: rejectionRate, rejections, waitAverage, inVehicleTravelTimeMean,
	 * & totalTravelTimeMean
	 */

	private void verifyDrtCustomerStatsCloseToExpectedStats(String outputDirectory, Stats expectedStats) {

		String filename = outputDirectory + "/drt_customer_stats_drt.csv";

		final List<String> collect;
		try {
			collect = Files.lines(Paths.get(filename)).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int size = collect.size();
		List<String> keys = List.of(collect.get(0).split(";"));
		List<String> lastIterationValues = List.of(collect.get(size - 1).split(";"));

		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			params.put(keys.get(i), lastIterationValues.get(i));
		}

		double inVehicleTravelTimeMean = Double.parseDouble(params.get("inVehicleTravelTime_mean"));
		double waitAverage = Double.parseDouble(params.get("wait_average"));
		double rejections = Double.parseDouble(params.get("rejections"));
		double rejectionRate = Double.parseDouble(params.get("rejectionRate"));
		double totalTravelTimeMean = Double.parseDouble(params.get("totalTravelTime_mean"));

		var percentage = Percentage.withPercentage(0.00001);
		assertThat(rejectionRate).isCloseTo(expectedStats.rejectionRate, percentage);
		assertThat(rejections).isCloseTo(expectedStats.rejections, percentage);
		assertThat(waitAverage).isCloseTo(expectedStats.waitAverage, percentage);
		assertThat(inVehicleTravelTimeMean).isCloseTo(expectedStats.inVehicleTravelTimeMean, percentage);
		assertThat(totalTravelTimeMean).isCloseTo(expectedStats.totalTravelTimeMean, percentage);
	}

	private static class Stats {
		private final double rejectionRate;
		private final double rejections;
		private final double waitAverage;
		private final double inVehicleTravelTimeMean;
		private final double totalTravelTimeMean;

		private Stats(Builder builder) {
			rejectionRate = builder.rejectionRate;
			rejections = builder.rejections;
			waitAverage = builder.waitAverage;
			inVehicleTravelTimeMean = builder.inVehicleTravelTimeMean;
			totalTravelTimeMean = builder.totalTravelTimeMean;
		}

		public static Builder newBuilder() {
			return new Builder();
		}

		public static final class Builder {
			private double rejectionRate;
			private double rejections;
			private double waitAverage;
			private double inVehicleTravelTimeMean;
			private double totalTravelTimeMean;

			private Builder() {
			}

			public Builder rejectionRate(double val) {
				rejectionRate = val;
				return this;
			}

			public Builder rejections(double val) {
				rejections = val;
				return this;
			}

			public Builder waitAverage(double val) {
				waitAverage = val;
				return this;
			}

			public Builder inVehicleTravelTimeMean(double val) {
				inVehicleTravelTimeMean = val;
				return this;
			}

			public Builder totalTravelTimeMean(double val) {
				totalTravelTimeMean = val;
				return this;
			}

			public Stats build() {
				return new Stats(this);
			}
		}
	}
}
