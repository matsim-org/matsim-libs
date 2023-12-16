/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EqualVehicleDensityTargetCalculatorTest {
	private final Config config = ConfigUtils.loadConfig(
			IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "eight_shared_taxi_config.xml"),
			new MultiModeDrtConfigGroup());

	private final Network network = NetworkUtils.readNetwork(
			config.network().getInputFileURL(config.getContext()).toString());

	private final DrtZonalSystem zonalSystem = DrtZonalSystem.createFromPreparedGeometries(network,
			DrtGridUtils.createGridFromNetwork(network, 500.));

	@Test
	void calculate_oneVehiclePerZone() {
		var targetFunction = new EqualVehicleDensityTargetCalculator(zonalSystem,
				createFleetSpecification(8)).calculate(0, Map.of());
		zonalSystem.getZones().keySet().forEach(id -> assertTarget(targetFunction, zonalSystem, id, 1));
	}

	@Test
	void calculate_lessVehiclesThanZones() {
		var targetFunction = new EqualVehicleDensityTargetCalculator(zonalSystem,
				createFleetSpecification(7)).calculate(0, Map.of());
		zonalSystem.getZones().keySet().forEach(id -> assertTarget(targetFunction, zonalSystem, id, 7. / 8));
	}

	@Test
	void calculate_moreVehiclesThanZones() {
		var targetFunction = new EqualVehicleDensityTargetCalculator(zonalSystem,
				createFleetSpecification(9)).calculate(0, Map.of());
		zonalSystem.getZones().keySet().forEach(id -> assertTarget(targetFunction, zonalSystem, id, 9. / 8));
	}

	private FleetSpecification createFleetSpecification(int count) {
		FleetSpecification fleetSpecification = new FleetSpecificationImpl();
		for (int i = 0; i < count; i++) {
			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(i + "", DvrpVehicle.class))
					.startLinkId(Id.createLinkId("a"))
					.capacity(1)
					.serviceBeginTime(0)
					.serviceEndTime(100)
					.build());
		}
		return fleetSpecification;
	}

	private void assertTarget(ToDoubleFunction<DrtZone> targetFunction, DrtZonalSystem zonalSystem, String zoneId,
			double expectedValue) {
		assertThat(targetFunction.applyAsDouble(zonalSystem.getZones().get(zoneId))).isEqualTo(expectedValue);
	}
}
