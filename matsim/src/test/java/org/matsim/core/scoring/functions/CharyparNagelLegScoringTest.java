package org.matsim.core.scoring.functions;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CharyparNagelLegScoringTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void scoreTeleportedLeg() {

		var population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		var person = createPerson(population.getFactory());
		var route = new GenericRouteImpl(Id.createLinkId("start"), Id.createLinkId("end"));
		route.setTravelTime(41);
		route.setDistance(1009);
		var scoringParams = createScoringParams("leg-mode");
		var tripBasedFunction = createScoringFunction(scoringParams, Set.of());
		var trips = TripStructureUtils.getTrips(person.getSelectedPlan());
		assertEquals(1, trips.size());
		var trip = trips.getFirst();
		assertEquals(1, trip.getTripElements().size());
		var leg = (Leg) trip.getTripElements().getFirst();
		leg.setRoute(route);

		tripBasedFunction.handleTrip(trip);
		tripBasedFunction.finish();

		var expectedScore = calcBaseScore(scoringParams, leg.getMode(), leg.getTravelTime().seconds(), route.getDistance())
			+ calcDailyConstant(scoringParams, leg.getMode())
			+ calcTripConstant(scoringParams, leg.getMode());

		assertEquals(expectedScore, tripBasedFunction.getScore());
	}

	@Test
	void scoreSinglePtTrip() {

		var scenarioUrl = ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch").toString();
		var config = ConfigUtils.loadConfig(scenarioUrl + "config.xml");
		var plansPath = utils.getClassInputDirectory() + "one-routed-pt-plan.xml.gz";
		config.plans().setInputFile(plansPath);
		var scenario = ScenarioUtils.loadScenario(config);
		assertEquals(1, config.transit().getTransitModes().size());
		var transitMode = config.transit().getTransitModes().iterator().next();
		var scoringParams = createScoringParams(transitMode);
		var tripBasedFunction = createScoringFunction(scoringParams, config.transit().getTransitModes());
		var person = getSinglePerson(scenario);

		replayPlanTripBased(tripBasedFunction, person);

		var expectedScore = TripStructureUtils.getLegs(person.getSelectedPlan()).stream()
			.mapToDouble(l -> {
				var legScore = calcBaseScore(scoringParams, l.getMode(), l.getTravelTime().seconds(), l.getRoute().getDistance());
				var waitScore = calcWaitScore(scoringParams, l);
				return legScore + waitScore;
			})
			.sum() +
			calcTripConstant(scoringParams, transitMode) +
			calcDailyConstant(scoringParams, transitMode);
		assertEquals(expectedScore, tripBasedFunction.getScore(), 0.1);
	}

	@Test
	void scoreMultiplePtTripsWithLineSwitch() {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch").toString();
		var config = ConfigUtils.loadConfig(scenarioUrl + "config.xml");
		var plansPath = utils.getClassInputDirectory() + "one-routed-pt-plan-line-switch.xml.gz";
		config.plans().setInputFile(plansPath);
		var scenario = ScenarioUtils.loadScenario(config);
		assertEquals(1, config.transit().getTransitModes().size());
		var transitMode = config.transit().getTransitModes().iterator().next();
		var scoringParams = createScoringParams(transitMode);
		var tripBasedFunction = createScoringFunction(scoringParams, config.transit().getTransitModes());
		var person = getSinglePerson(scenario);

		replayPlanTripBased(tripBasedFunction, person);

		// the scoring puts one pt constant onto each trip
		var expectedConstantPerTrip = TripStructureUtils.getTrips(person.getSelectedPlan()).stream()
			.mapToDouble(_ -> calcTripConstant(scoringParams, transitMode))
			.sum();

		var expectedLegScore = TripStructureUtils.getLegs(person.getSelectedPlan()).stream()
			.mapToDouble(l -> {
				var legScore = calcBaseScore(scoringParams, l.getMode(), l.getTravelTime().seconds(), l.getRoute().getDistance());
				var waitScore = calcWaitScore(scoringParams, l);
				return legScore + waitScore;
			})
			.sum();
		// we know that the second pt trip has a line switch this is rather difficult to figure out otherwise
		var expectedLineSwitch = scoringParams.getUtilityOfLineSwitch();
		// we know that we have one daily constant in this scenario
		var dailyScore = calcDailyConstant(scoringParams, transitMode);
		var expectedScore = dailyScore + expectedConstantPerTrip + expectedLegScore + expectedLineSwitch;
		assertEquals(expectedScore, tripBasedFunction.getScore(), 0.1);
	}

	private static void replayPlanTripBased(CharyparNagelLegScoring function, Person person) {
		var trips = TripStructureUtils.getTrips(person.getSelectedPlan());
		for (var trip : trips) {
			function.handleTrip(trip);
		}
		function.finish();
	}

	private static double calcBaseScore(ScoringConfigGroup scoringParams, String mode, double travelTime, double travelDist) {
		var modeParams = scoringParams.getOrCreateModeParams(mode);
		// trave time: travelTime * marginalUtilityOfTraveling / 3600
		var utilTravelTime = travelTime * modeParams.getMarginalUtilityOfTraveling() / 3600;
		// distance: distance * marginalUtilityOfDistance
		var utilDist = travelDist * modeParams.getMarginalUtilityOfDistance();
		// distance costs: distance * monetaryDistanceCosts * marginalUtilityOfMoney
		var utilDistCosts = travelDist * modeParams.getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney();

		return utilTravelTime + utilDist + utilDistCosts;
	}

	private static double calcWaitScore(ScoringConfigGroup scoringParams, Leg leg) {
		if (leg.getRoute() instanceof TransitPassengerRoute tpr) {
			var waitTime = tpr.getBoardingTime().seconds() - leg.getDepartureTime().seconds();
			var waitUtil = scoringParams.getMarginalUtlOfWaitingPt_utils_hr() / 3600 - scoringParams.getOrCreateModeParams(leg.getMode()).getMarginalUtilityOfTraveling() / 3600;
			return waitTime * waitUtil;
		} else {
			return 0.;
		}
	}

	private static double calcDailyConstant(ScoringConfigGroup scoringParams, String mode) {
		var modeParams = scoringParams.getOrCreateModeParams(mode);
		return modeParams.getDailyUtilityConstant() + scoringParams.getMarginalUtilityOfMoney() * modeParams.getDailyMonetaryConstant();
	}

	private static double calcTripConstant(ScoringConfigGroup scoringParams, String mode) {
		return scoringParams.getOrCreateModeParams(mode).getConstant();
	}

	private static Person createPerson(PopulationFactory factory) {
		var person = factory.createPerson(Id.createPersonId("1"));
		var plan = factory.createPlan();
		var startAct = factory.createActivityFromLinkId("start", Id.createLinkId("start"));
		startAct.setEndTime(7);
		startAct.setCoord(new Coord(0, 0));
		plan.addActivity(startAct);

		var leg = factory.createLeg("main-leg");
		leg.setTravelTime(41);
		leg.setDepartureTime(7);
		leg.setRoutingMode("routing-mode");
		leg.setMode("leg-mode");
		plan.addLeg(leg);

		var endAct = factory.createActivityFromLinkId("end", Id.createLinkId("end"));
		endAct.setCoord(new Coord(0, 1009));
		plan.addActivity(endAct);

		person.addPlan(plan);
		return person;
	}

	private static ScoringConfigGroup createScoringParams(String mode) {
		var config = new ScoringConfigGroup();
		config.setMarginalUtlOfWaitingPt_utils_hr(-3);
		config.setMarginalUtilityOfMoney(5);
		config.setEarlyDeparture_utils_hr(-7);
		config.setLateArrival_utils_hr(-11);
		config.setPerforming_utils_hr(13);
		config.setUtilityOfLineSwitch(-17);

		config.getOrCreateModeParams(mode)
			.setConstant(-19)
			.setMarginalUtilityOfDistance(-23)
			.setMonetaryDistanceRate(-29)
			.setMarginalUtilityOfTraveling(-31)
			.setDailyMonetaryConstant(-37)
			.setDailyUtilityConstant(-41);
		return config;
	}

	private static CharyparNagelLegScoring createScoringFunction(ScoringConfigGroup config, Set<String> ptModes) {

		var scenarioConfig = new ScenarioConfigGroup();
		var scoringParams = new ScoringParameters.Builder(config, config.getScoringParameters(null), Map.of(), scenarioConfig)
			.build();
		return new CharyparNagelLegScoring(scoringParams, ptModes);
	}

	private static @NonNull Person getSinglePerson(Scenario scenario) {
		var size = scenario.getPopulation().getPersons().size();
		assertEquals(1, size, "Expected scenario with one Person, but has: " + size);
		return scenario.getPopulation().getPersons().values().iterator().next();
	}
}
