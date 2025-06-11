package org.matsim.examples;

import com.google.common.collect.Lists;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Stream;

// @formatter:off
/**
 * In this test, we restrict the allowed modes of a link. The base case is the equil scenario.
 * <p>
 *       /...                   ...\
 * ------o------------o------------o-----------
 * l1  (n2)   l6    (n7)   l15  (n12)   l20
 */
// @formatter:on
public class ModeRestrictionTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	static Stream<Arguments> intersectLinkAndMode() {
		List<String> links = List.of("6", "15", "20");
		List<String> restrictedModes = List.of("car", "bike");

		return links.stream()
					.flatMap(link -> restrictedModes.stream().map(restrictedMode -> Arguments.of(link, restrictedMode)));
	}

	/**
	 * Setting: The agents (both car & bike) have a route from the plan. Link before and at the activity is restricted respectively. Consistency check is turned off.
	 * Expected behaviour: Since it is not checked, if the route contains link, which are not allowed to use anymore, it can still use the restricted link.
	 */
	@ParameterizedTest
	@MethodSource("intersectLinkAndMode")
	void testNoRouteChange_noConsistencyCheck_ok(String link, String restrictedMode) {
		final Config config = prepareConfig("plans_act_link20.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Id<Link> linkId = Id.createLinkId(link);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Link networkLink = scenario.getNetwork().getLinks().get(linkId);
		removeModeFromLink(networkLink, restrictedMode);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(
			Map.of(Id.createPersonId("car"), List.of(linkId), Id.createPersonId("bike"), List.of(linkId)));
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualContainsExpected();
		assertEventFilesSame();
	}

	/**
	 * Setting: The agents (both car & bike) have a route from the plan. Link before and at the activity is restricted respectively. Consistency check is turned on.
	 * Expected behaviour: An exception is thrown because the route is inconsistent and this is checked.
	 */
	@ParameterizedTest
	@MethodSource("intersectLinkAndMode")
	void testNoRouteChange_withConsistencyCheck_throws(String link, String restrictedMode) {
		final Config config = prepareConfig("plans_act_link20.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.abortOnInconsistency);

		Id<Link> linkId = Id.createLinkId(link);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Link networkLink = scenario.getNetwork().getLinks().get(linkId);
		removeModeFromLink(networkLink, restrictedMode);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck();
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, firstLegRouteCheck));
		Assertions.assertInstanceOf(ProvisionException.class, exception);
		Assertions.assertTrue(((ProvisionException) exception).getErrorMessages().stream()
															  .map(Message::getMessage)
															  .anyMatch(m -> m.equals("java.lang.RuntimeException: Network for mode '" + restrictedMode + "' has unreachable links and nodes. This may be caused by mode restrictions on certain links. Aborting.")));
	}

	/**
	 * Setting: The agents (both car & bike) have no route. The mode of the link in front of the activity is restricted. Consistency check is turned off.
	 * Expected behaviour: By default, the network is not cleaned. The router throws an exception since there is no route from home to the activity.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testRerouteBeforeSim_toActNotPossible_throws(String restrictedMode) {
		final Config config = prepareConfig("plans_act_link15.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Id<Link> link = Id.createLinkId("6");

		Scenario scenario = restrictLinkAndResetRoutes(config, link, restrictedMode);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck();
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, firstLegRouteCheck));

		//we can't check the root exception because the check is done in another thread. So we check for the more general exception.
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	/**
	 * Setting: The agents (both car & bike) have no route. The mode of the link behind the activity is restricted. Consistency check is turned off.
	 * Expected behaviour: By default, the network is not cleaned. The router throws an exception since there is no route from the activity to home.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testRerouteBeforeSim_fromActNotPossible_throws(String restrictedMode) {
		final Config config = prepareConfig("plans_act_link6.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Id<Link> link = Id.createLinkId("15");

		Scenario scenario = restrictLinkAndResetRoutes(config, link, restrictedMode);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck();
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, firstLegRouteCheck));

		//we can't check the root exception because the check is done in another thread. So we check for the more general exception.
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	/**
	 * Setting: Network and routes are cleaned before simulation. Link before the activity is restricted. Consistency check is turned off.
	 * Expected behaviour: The agent using the restricted are mode is rerouted. The new link of the activity for this agent is the one which is closest to the activity.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testCleanNetworkAndRerouteBeforeSim_toActNotPossible_withConsistencyCheck_ok(String restrictedMode){
		final Config config = prepareConfig("plans_act_link15.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.abortOnInconsistency);

		Id<Link> link = Id.createLinkId("6");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		NetworkUtils.restrictModesAndCleanNetwork(scenario.getNetwork(), l -> Map.of(link, Set.of(restrictedMode)).getOrDefault(l, Set.of()));
		PopulationUtils.checkRouteModeAndReset(scenario.getPopulation(), scenario.getNetwork());

		// New route. Note that the end link is 20, but the activity's link in the input was 15.
		// But since we restricted the mode for link 6 AND 15, 20 is used as fallback.
		List<Id<Link>> newRoute = List.of(Id.createLinkId("1"), Id.createLinkId("10"), Id.createLinkId("19"), Id.createLinkId("20"));
		List<Id<Link>> oldRoute = List.of(Id.createLinkId("1"), Id.createLinkId("6"), Id.createLinkId("15"));

		String other = restrictedMode.equals("car") ? "bike" : "car";

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(Map.of(Id.createPersonId(restrictedMode), newRoute, Id.createPersonId(other), oldRoute));
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualEqualsExpected();
		assertEventFilesSame();
	}

	/**
	 * Setting: Network and routes are cleaned before simulation. Link from the activity is restricted. Consistency check is turned off.
	 * Expected behaviour: The agent using the restricted are mode is rerouted. The new link of the activity for this agent is the one which is closest to the activity.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testCleanNetworkAndRerouteBeforeSim_fromActNotPossible_withConsistencyCheck_ok(String restrictedMode){
		final Config config = prepareConfig("plans_act_link6.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.abortOnInconsistency);

		Id<Link> link = Id.createLinkId("15");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		NetworkUtils.restrictModesAndCleanNetwork(scenario.getNetwork(),  l -> Map.of(link, Set.of(restrictedMode)).getOrDefault(l, Set.of()));
		PopulationUtils.checkRouteModeAndReset(scenario.getPopulation(), scenario.getNetwork());

		// New route. Note that the end link is 20, but the activity's link in the input was 6.
		// But since we restricted the mode for link 6 AND 15, 20 is used as fallback.
		List<Id<Link>> newRoute = List.of(Id.createLinkId("1"), Id.createLinkId("10"), Id.createLinkId("19"), Id.createLinkId("20"));
		List<Id<Link>> oldRoute = List.of(Id.createLinkId("1"), Id.createLinkId("6"));

		String other = restrictedMode.equals("car") ? "bike" : "car";

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(Map.of(Id.createPersonId(restrictedMode), newRoute, Id.createPersonId(other), oldRoute));
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualEqualsExpected();
		assertEventFilesSame();
	}

	/**
	 * Setting: The agents (both car & bike) have no route. The mode of the activity's link is restricted. Activity has x-y-coordinate. Consistency check is turned off.
	 * Expected behaviour: The agents (both car & bike) are rerouted to the nearest link to the activity's coordinate.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testRerouteBeforeSim_actOnRestrictedLinkWithCoords_ok(String restrictedMode) {
		final Config config = prepareConfig("plans_act_link15.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Scenario scenario = restrictLinkAndResetRoutes(config, Id.createLinkId("15"), restrictedMode);

		// New route. Note that the end link is 20, but the activity's link in the input was 15.
		// But since we restricted the mode for link 15, 20 is used as fallback.
		List<Id<Link>> newRoute = List.of(Id.createLinkId("1"), Id.createLinkId("10"), Id.createLinkId("19"), Id.createLinkId("20"));
		List<Id<Link>> oldRoute = List.of(Id.createLinkId("1"), Id.createLinkId("6"), Id.createLinkId("15"));

		String other = restrictedMode.equals("car") ? "bike" : "car";

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(Map.of(Id.createPersonId(restrictedMode), newRoute, Id.createPersonId(other), oldRoute));
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualEqualsExpected();
		assertEventFilesSame();
	}

	/**
	 * Setting: The agents (both car & bike) have no route. The mode of the activity's link is restricted. Activity has no x-y-coordinate. Consistency check is turned off.
	 * Expected behaviour: The router throws an exception since there is no fallback x-y-coordinate.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testRerouteBeforeSim_actOnRestrictedLinkWithoutCoord_throws(String restrictedMode){
		final Config config = prepareConfig("plans_act_link15.xml", RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Id<Person> person = Id.createPersonId(restrictedMode);
		Scenario scenario = restrictLinkAndResetRoutes(config, Id.createLinkId("15"), restrictedMode);
		Activity act = (Activity) scenario.getPopulation().getPersons().get(person).getPlans().get(0).getPlanElements().get(2);
		//Activity act = (Activity) scenario.getPopulation().getPersons().get(person).getPlans().getFirst().getPlanElements().get(2);
		act.setCoord(null);

		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, new FirstLegVisitedLinksCheck()));

		//we can't check the root exception because the check is done in another thread. So we check for the more general exception.
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	private Config prepareConfig(String plansFile, RoutingConfigGroup.NetworkRouteConsistencyCheck consistencyCheck) {
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml", MatsimTestUtils.TestMethodType.Parameterized);
		config.routing().setNetworkRouteConsistencyCheck(consistencyCheck);
		config.plans().setInputFile(plansFile);

		ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams("bike") ;
		config.scoring().addModeParams(params);

		config.qsim().setMainModes( new HashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ) ) ) ;
		config.routing().setNetworkModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );
		config.routing().removeTeleportedModeParams("bike");
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.controller().setLastIteration(0);
		return config;
	}

	private static Scenario restrictLinkAndResetRoutes(Config config, Id<Link> link, String restrictedMode) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		removeModeFromLink(scenario.getNetwork().getLinks().get(link), restrictedMode);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
			leg.setRoute(null);
		}
		return scenario;
	}

	private static void runController(Scenario scenario, FirstLegVisitedLinksCheck firstLegRouteCheck) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(firstLegRouteCheck);
				this.addTravelTimeBinding(TransportMode.bike).to(BikeTravelTime.class);
			}
		});
		controler.run();
	}

	private static void removeModeFromLink(Link networkLink, String restrictedMode) {
		Set<String> allowedModes = new HashSet<>(networkLink.getAllowedModes());
		allowedModes.remove(restrictedMode);
		networkLink.setAllowedModes(allowedModes);
	}

	/**
	 * Event handler that checks the visited Links of a person on the first leg.
	 */
	private static class FirstLegVisitedLinksCheck implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
		private final Map<Id<Person>, List<Id<Link>>> expected = new HashMap<>();
		private final Map<Id<Person>, List<Id<Link>>> actual = new HashMap<>();
		private final Map<Id<Vehicle>, Id<Person>> currentPersonByVehicle = new HashMap<>();
		private final Map<Id<Person>, Boolean> onFirstLeg = new HashMap<>();

		public FirstLegVisitedLinksCheck() {
		}

		public FirstLegVisitedLinksCheck(Map<Id<Person>, List<Id<Link>>> expected) {
			this.expected.putAll(expected);
		}

		public FirstLegVisitedLinksCheck(Id<Person> personId, List<Id<Link>> expected) {
			this.expected.put(personId, expected);
		}

		public FirstLegVisitedLinksCheck(Id<Person> personId, Id<Link> expected) {
			this.expected.put(personId, Collections.singletonList(expected));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			addLinkIdToActual(this.currentPersonByVehicle.get(event.getVehicleId()), event.getLinkId());
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			addLinkIdToActual(this.currentPersonByVehicle.get(event.getVehicleId()), event.getLinkId());
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			this.currentPersonByVehicle.remove(event.getVehicleId());
			this.onFirstLeg.put(event.getPersonId(), false);
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			this.currentPersonByVehicle.put(event.getVehicleId(), event.getPersonId());
			this.onFirstLeg.putIfAbsent(event.getPersonId(), true);
		}

		private void addLinkIdToActual(Id<Person> personId, Id<Link> linkId) {
			if (!this.onFirstLeg.get(personId)) {
				return;
			}
			actual.putIfAbsent(personId, Lists.newArrayList(linkId));
			List<Id<Link>> linkIds = actual.get(personId);
			if (linkIds.get(linkIds.size()-1) == null || linkIds.get(linkIds.size()-1) == linkId) {
			//if (linkIds.getLast() == null || linkIds.getLast() == linkId) {
				return;
			}
			linkIds.add(linkId);
		}

		public void assertActualEqualsExpected() {
			Assertions.assertEquals(expected, actual);
		}

		public void assertActualContainsExpected() {
			for (Map.Entry<Id<Person>, List<Id<Link>>> entry : expected.entrySet()) {
				Id<Person> personId = entry.getKey();
				List<Id<Link>> expected = entry.getValue();
				List<Id<Link>> actual = this.actual.get(personId);
				Assertions.assertNotNull(actual);
				Assertions.assertTrue(actual.containsAll(expected));
			}
		}
	}


	private static class BikeTravelTime implements TravelTime {
		@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){
			return 1. ;
		}
	}

	/**
	 * Note that this method alone would be sufficient to check the simulation behaviour. But since I have already written the eventHandler, I will use it as well.
	 * If at some point it is too nervig, to maintain both test methodologies here, the eventHandler can be removed.
	 * But, I think it makes clearer what is the relevant thing to check. paul aug '24
	 */
	private void assertEventFilesSame() {
		String fileName = "output_events.xml.gz";

		String inputFile = utils.getInputDirectory() + utils.getParameterizedTestInputString() + "/" + fileName;
		String outputFile = utils.getOutputDirectory() + "/" + fileName;

		ComparisonResult comparisonResult = EventsUtils.compareEventsFiles(inputFile, outputFile);
		if (comparisonResult != ComparisonResult.FILES_ARE_EQUAL)
			Assertions.fail("Event files are not equal: " + comparisonResult);
	}
}
