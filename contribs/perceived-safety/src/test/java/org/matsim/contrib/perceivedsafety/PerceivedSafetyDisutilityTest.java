package org.matsim.contrib.perceivedsafety;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleParamsDefaultImpl;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.net.URL;

class PerceivedSafetyDisutilityTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPerceivedSafetyDisutility() {
		URL context = ExamplesUtils.getTestScenarioURL("equil");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

		ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(TransportMode.bike);
		modeParams.setMarginalUtilityOfTraveling(0.);
		config.scoring().addModeParams(modeParams);

//		add perceivedSafetyCfgGroup and configure
		PerceivedSafetyConfigGroup perceivedSafetyConfigGroup = ConfigUtils.addOrGetModule(config, PerceivedSafetyConfigGroup.class);

//		values taken from E_BIKE in PerceivedSafetyUtils.fillConfigWithPerceivedSafetyDefaultValues
		PerceivedSafetyConfigGroup.PerceivedSafetyModeParams perceivedSafetyModeParams = perceivedSafetyConfigGroup.getOrCreatePerceivedSafetyModeParams(TransportMode.bike);
		perceivedSafetyModeParams.setMarginalUtilityOfPerceivedSafetyPerM(0.84);
		perceivedSafetyModeParams.setMarginalUtilityOfPerceivedSafetyPerMSd(0.22);
		perceivedSafetyModeParams.setDMaxPerM(0.);

		perceivedSafetyConfigGroup.addModeParams(perceivedSafetyModeParams);
		perceivedSafetyConfigGroup.setInputPerceivedSafetyThresholdPerM(4);

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.0002);
		bicycleConfigGroup.setBicycleMode(TransportMode.bike);

		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		Network network = NetworkUtils.createNetwork();
		NetworkFactory fac = network.getFactory();

		Node from = fac.createNode(Id.createNodeId("from"), new Coord(0.,1.));
		Node to = fac.createNode(Id.createNodeId("to"), new Coord(1.,0.));

		Link link = fac.createLink(Id.createLinkId("testLink"), from, to);
		link.setLength(1000.);
		link.getAttributes().putAttribute(BicycleUtils.SURFACE, "cobblestone");
		NetworkUtils.setType(link, "tertiary");
		// assign very low perceived safety to all links
		link.getAttributes().putAttribute(TransportMode.bike + "PerceivedSafety", 1);

		network.addNode(from);
		network.addNode(to);
		network.addLink(link);
		scenario.setNetwork(network);

//		create all disutilities
		RoutingConfigGroup routingConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RoutingConfigGroup.class);
		RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.bike, scenario.getConfig());
		double sigma = routingConfigGroup.getRoutingRandomness();

		TravelTimeCalculator calculator = new TravelTimeCalculator.Builder(scenario.getNetwork()).build();
		TravelDisutility defaultTravelDisutility = defaultTravelDisutilityFactory.createTravelDisutility(calculator.getLinkTravelTimes());

		PerceivedSafetyDisutility perceivedSafetyDisutility = new PerceivedSafetyDisutility(scenario, defaultTravelDisutility, sigma);
		PerceivedSafetyAndBicycleDisutility perceivedSafetyAndBicycleDisutility =
			new PerceivedSafetyAndBicycleDisutility(scenario, defaultTravelDisutility, sigma, new BicycleParamsDefaultImpl(), 1.);

		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("test"));

		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
		vehicleType.setNetworkMode(TransportMode.bike);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("test"), vehicleType);

//		calc disutilities
		double time = 100.;
		double normal = defaultTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		double perceivedSafety = perceivedSafetyDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		double perceivedSafetyAndBicycle = perceivedSafetyAndBicycleDisutility.getLinkTravelDisutility(link, time, person, vehicle);

//		assertions
		Assertions.assertNotEquals(normal, perceivedSafety);
		Assertions.assertNotEquals(perceivedSafety, perceivedSafetyAndBicycle);
	}
}
