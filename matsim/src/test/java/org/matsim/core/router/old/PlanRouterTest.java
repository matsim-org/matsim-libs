/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanRouterTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.core.router.old;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.DefaultPrepareForSimModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PlanRouterTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void passesVehicleFromOldPlan(RoutingConfigGroup.AccessEgressType type) {
		final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.routing().setAccessEgressType(type);

		config.plans().setInputFile("plans1.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(new TripRouterModule());
				install(new ScenarioByInstanceModule(scenario));
				install(new TimeInterpretationModule());
				addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility(config.scoring()));
				addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());

				install(new DefaultPrepareForSimModule());
			}
		});
		injector.getInstance(PrepareForSim.class).run();

		TripRouter tripRouter = injector.getInstance(TripRouter.class);
		PlanRouter testee = new PlanRouter(tripRouter, TimeInterpretation.create(config));
		Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
		Id<Vehicle> vehicleId = Id.create(1, Vehicle.class);
		((NetworkRoute) TripStructureUtils.getLegs(plan).get(0).getRoute()).setVehicleId(vehicleId);

		testee.run(plan);

		if (config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none)) {
			Assertions.assertEquals(vehicleId, ((NetworkRoute) TripStructureUtils.getLegs(plan).get(0).getRoute()).getVehicleId(), "Vehicle Id transferred to new Plan");
		} else {
			Assertions.assertEquals(vehicleId, ((NetworkRoute) TripStructureUtils.getLegs(plan).get(1).getRoute()).getVehicleId(), "Vehicle Id transferred to new Plan");
			// yy I changed get(0) to get(1) since in the input file there is no intervening walk leg, but in the output there is. kai, feb'16
		}
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void keepsVehicleIfTripRouterUsesOneAlready(RoutingConfigGroup.AccessEgressType type) {
		final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.routing().setAccessEgressType(type);

		config.plans().setInputFile("plans1.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final LeastCostPathCalculatorFactory leastCostAlgoFactory = new SpeedyALTFactory();
		final OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
		final FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();

		Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
		Id<Vehicle> oldVehicleId = Id.create(1, Vehicle.class);
		((NetworkRoute) TripStructureUtils.getLegs(plan).get(0).getRoute()).setVehicleId(oldVehicleId);


		// A trip router which provides vehicle ids by itself.
		final Id<Vehicle> newVehicleId = Id.create(2, Vehicle.class);
		final RoutingModule routingModule = new RoutingModule() {
			@Override
			public List<? extends PlanElement> calcRoute(RoutingRequest request) {
				final Facility fromFacility = request.getFromFacility();
				final Facility toFacility = request.getToFacility();
				final double departureTime = request.getDepartureTime();
				final Person person = request.getPerson();


				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
				Network carOnlyNetwork = NetworkUtils.createNetwork();
				filter.filter(carOnlyNetwork, Set.of("car"));

				// We create a teleport with beelineDistanceFactor 0, so that the access and egress do not change the overall plan-stats
				RoutingModule teleportModule = new TeleportationRoutingModule(
					"walk",
					scenario,
					1,
					0,
					null);

				// TODO I have used the default impls for all needed interfaces. Check if this is okay # aleks

				List<? extends PlanElement> trip = null;

				if (type == RoutingConfigGroup.AccessEgressType.accessEgressModeToLink) {
					trip = DefaultRoutingModules.createAccessEgressNetworkRouter(
						"car",
						leastCostAlgoFactory.createPathCalculator(scenario.getNetwork(), disutilityFactory.createTravelDisutility(travelTime), travelTime),
						scenario,
						carOnlyNetwork,
						teleportModule,
						teleportModule,
						TimeInterpretation.create(config),
						RouterUtils.getMultimodalLinkChooserDefault()
					).calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, departureTime, person));
					((NetworkRoute) TripStructureUtils.getLegs(trip).get(1).getRoute()).setVehicleId(newVehicleId);
				} else {
					trip = DefaultRoutingModules.createPureNetworkRouter("car", scenario.getPopulation().getFactory(),
							scenario.getNetwork(),
							leastCostAlgoFactory.createPathCalculator(scenario.getNetwork(), disutilityFactory.createTravelDisutility(travelTime), travelTime))
						.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, departureTime, person));
					((NetworkRoute) TripStructureUtils.getLegs(trip).get(0).getRoute()).setVehicleId(newVehicleId);
				}

				return trip;

			}

		};
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioByInstanceModule(scenario));
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility(config.scoring()));
						addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
						addRoutingModuleBinding("car").toInstance(routingModule);
					}
				}));

				install(new TimeInterpretationModule());
				install(new DefaultPrepareForSimModule());
			}
		});
		injector.getInstance(PrepareForSim.class).run();

		TripRouter tripRouter = injector.getInstance(TripRouter.class);

		PlanRouter testee = new PlanRouter(tripRouter, TimeInterpretation.create(config));
		testee.run(plan);

		if (config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none)) {
			Assertions.assertEquals(newVehicleId, ((NetworkRoute) TripStructureUtils.getLegs(plan).get(0).getRoute()).getVehicleId(), "Vehicle Id from TripRouter used");
		} else {
			Assertions.assertEquals(newVehicleId, ((NetworkRoute) TripStructureUtils.getLegs(plan).get(1).getRoute()).getVehicleId(), "Vehicle Id from TripRouter used");
			// yy I changed get(0) to get(1) since in the input file there is no intervening walk leg, but in the output there is. kai, feb'16
		}
	}

}
