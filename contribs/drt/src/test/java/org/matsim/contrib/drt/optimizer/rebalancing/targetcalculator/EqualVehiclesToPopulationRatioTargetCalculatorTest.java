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
import java.util.function.ToIntFunction;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
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

	@Test
	public void testCalculate_oneVehiclePerZone() {
		ToIntFunction<DrtZone> demandFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem,
				createPopulation(), createFleetSpecification(8)).calculate(0, Map.of());

		assertTarget(demandFunction, zonalSystem, "1", 0);
		assertTarget(demandFunction, zonalSystem, "2", 2);
		assertTarget(demandFunction, zonalSystem, "3", 0);
		assertTarget(demandFunction, zonalSystem, "4", 2);
		assertTarget(demandFunction, zonalSystem, "5", 0);
		assertTarget(demandFunction, zonalSystem, "6", 0);
		assertTarget(demandFunction, zonalSystem, "7", 0);
		assertTarget(demandFunction, zonalSystem, "8", 2);
	}

	@Test
	public void testCalculate_twoVehiclesPerZone() {
		ToIntFunction<DrtZone> demandFunction = new EqualVehiclesToPopulationRatioTargetCalculator(zonalSystem,
				createPopulation(), createFleetSpecification(16)).calculate(0, Map.of());

		assertTarget(demandFunction, zonalSystem, "1", 0);
		assertTarget(demandFunction, zonalSystem, "2", 5);
		assertTarget(demandFunction, zonalSystem, "3", 0);
		assertTarget(demandFunction, zonalSystem, "4", 5);
		assertTarget(demandFunction, zonalSystem, "5", 0);
		assertTarget(demandFunction, zonalSystem, "6", 0);
		assertTarget(demandFunction, zonalSystem, "7", 0);
		assertTarget(demandFunction, zonalSystem, "8", 5);
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
	 * <p>
	 * 1) in the left column, there are half of the people, performing dummy - > car -> dummy
	 * That should lead to half of the drt vehicles rebalanced to the left column when using FleetSizeWeightedByActivityEndsDemandEstimator.
	 * 2) in the right column, the other half of the people perform dummy -> drt -> dummy from top row to bottom row.
	 * That should lead to all drt vehicles rebalanced to the right column when using PreviousIterationDRTDemandEstimator.
	 * 3) in the center, there is nothing happening.
	 * But, when using EqualVehicleDensityZonalDemandEstimator, one vehicle should get sent to every zone..
	 */
	private Population createPopulation() {
		Population population = PopulationUtils.createPopulation(config);
		//delete what's there
		population.getPersons().clear();

		PopulationFactory factory = population.getFactory();

		Id<Link> left1 = Id.createLinkId(344);
		Id<Link> left2 = Id.createLinkId(112);

		for (int i = 1; i < 100; i++) {
			Person person = factory.createPerson(Id.createPersonId("leftColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", left1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg(TransportMode.car));
			plan.addActivity(factory.createActivityFromLinkId("dummy", left2));

			person.addPlan(plan);
			population.addPerson(person);
		}

		Id<Link> right1 = Id.createLinkId(151);
		Id<Link> right2 = Id.createLinkId(319);

		for (int i = 1; i < 100; i++) {
			Person person = factory.createPerson(Id.createPersonId("rightColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", right1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg(TransportMode.drt));
			plan.addActivity(factory.createActivityFromLinkId("dummy", right2));

			person.addPlan(plan);
			population.addPerson(person);
		}

		Id<Link> center1 = Id.createLinkId(147);
		Id<Link> center2 = Id.createLinkId(315);

		for (int i = 1; i < 100; i++) {
			Person person = factory.createPerson(Id.createPersonId("centerColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", center1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg("drt_teleportation"));
			plan.addActivity(factory.createActivityFromLinkId("dummy", center2));

			person.addPlan(plan);
			population.addPerson(person);
		}
		return population;
	}

	private void assertTarget(ToIntFunction<DrtZone> targetFunction, DrtZonalSystem zonalSystem, String zoneId,
			int expectedValue) {
		assertThat(targetFunction.applyAsInt(zonalSystem.getZones().get(zoneId))).isEqualTo(expectedValue);
	}
}
