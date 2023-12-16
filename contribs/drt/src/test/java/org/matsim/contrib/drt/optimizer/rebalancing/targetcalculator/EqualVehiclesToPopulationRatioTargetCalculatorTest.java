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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EqualVehiclesToPopulationRatioTargetCalculatorTest {
	private final Config config = ConfigUtils.loadConfig(
			IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "eight_shared_taxi_config.xml"),
			new MultiModeDrtConfigGroup());

	private final Network network = NetworkUtils.readNetwork(
			config.network().getInputFileURL(config.getContext()).toString());

	private final DrtZonalSystem zonalSystem = DrtZonalSystem.createFromPreparedGeometries(network,
			DrtGridUtils.createGridFromNetwork(network, 500.));

	private final Population population = PopulationUtils.createPopulation(config);
	private final PopulationFactory factory = population.getFactory();

	@Test
	void testCalculate_oneVehiclePerZone() {
		initPopulation(Map.of("2", 1, "4", 1, "8", 1));
		var targetFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem, population,
				createFleetSpecification(8)).calculate(0, Map.of());

		assertTarget(targetFunction, zonalSystem, "1", 0);
		assertTarget(targetFunction, zonalSystem, "2", 8. * (1. / 3));
		assertTarget(targetFunction, zonalSystem, "3", 0);
		assertTarget(targetFunction, zonalSystem, "4", 8. * (1. / 3));
		assertTarget(targetFunction, zonalSystem, "5", 0);
		assertTarget(targetFunction, zonalSystem, "6", 0);
		assertTarget(targetFunction, zonalSystem, "7", 0);
		assertTarget(targetFunction, zonalSystem, "8", 8. * (1. / 3));
	}

	@Test
	void testCalculate_twoVehiclesPerZone() {
		initPopulation(Map.of("2", 1, "4", 1, "8", 1));
		var targetFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem, population,
				createFleetSpecification(16)).calculate(0, Map.of());

		assertTarget(targetFunction, zonalSystem, "1", 0);
		assertTarget(targetFunction, zonalSystem, "2", 16 * (1. / 3));
		assertTarget(targetFunction, zonalSystem, "3", 0);
		assertTarget(targetFunction, zonalSystem, "4", 16 * (1. / 3));
		assertTarget(targetFunction, zonalSystem, "5", 0);
		assertTarget(targetFunction, zonalSystem, "6", 0);
		assertTarget(targetFunction, zonalSystem, "7", 0);
		assertTarget(targetFunction, zonalSystem, "8", 16. * (1. / 3));
	}

	@Test
	void testCalculate_noPopulation() {
		initPopulation(Map.of());
		var targetFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem, population,
				createFleetSpecification(16)).calculate(0, Map.of());

		assertTarget(targetFunction, zonalSystem, "1", 0);
		assertTarget(targetFunction, zonalSystem, "2", 0);
		assertTarget(targetFunction, zonalSystem, "3", 0);
		assertTarget(targetFunction, zonalSystem, "4", 0);
		assertTarget(targetFunction, zonalSystem, "5", 0);
		assertTarget(targetFunction, zonalSystem, "6", 0);
		assertTarget(targetFunction, zonalSystem, "7", 0);
		assertTarget(targetFunction, zonalSystem, "8", 0);
	}

	@Test
	void testCalculate_unevenDistributionOfActivitiesInPopulatedZones() {
		initPopulation(Map.of("2", 2, "4", 4, "8", 8));
		var targetFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem, population,
				createFleetSpecification(16)).calculate(0, Map.of());

		assertTarget(targetFunction, zonalSystem, "1", 0);
		assertTarget(targetFunction, zonalSystem, "2", 16 * (2. / 14));
		assertTarget(targetFunction, zonalSystem, "3", 0);
		assertTarget(targetFunction, zonalSystem, "4", 16 * (4. / 14));
		assertTarget(targetFunction, zonalSystem, "5", 0);
		assertTarget(targetFunction, zonalSystem, "6", 0);
		assertTarget(targetFunction, zonalSystem, "7", 0);
		assertTarget(targetFunction, zonalSystem, "8", 16 * (8. / 14));
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

	/**
	 * we have eight zones, 2 rows 4 columns.
	 * order of zones:
	 * 2	4	6	8
	 * 1	3	5	7
	 */
	private void initPopulation(Map<String, Integer> populationPerZone) {
		populationPerZone.forEach((zoneId, population) -> {
			for (int i = 0; i < population; i++) {
				createAndAddPerson(zoneId + "_" + i, zoneId);
			}
		});
	}

	private void createAndAddPerson(String id, String zoneId) {
		Id<Link> linkId = zonalSystem.getZones().get(zoneId).getLinks().get(0).getId();
		Person person = factory.createPerson(Id.createPersonId(id));
		Plan plan = factory.createPlan();
		plan.addActivity(factory.createActivityFromLinkId("dummy", linkId));
		person.addPlan(plan);
		population.addPerson(person);
	}

	private void assertTarget(ToDoubleFunction<DrtZone> targetFunction, DrtZonalSystem zonalSystem, String zoneId,
			double expectedValue) {
		assertThat(targetFunction.applyAsDouble(zonalSystem.getZones().get(zoneId))).isEqualTo(expectedValue);
	}
}
