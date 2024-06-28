package org.matsim.examples;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

// @formatter:off
/**
 * In this test, we restrict the allowed modes of a link. The base case is the equil scenario.
 * <p>
 *       /...			        ...\
 * ------o------------o------------o-----------
 * l1  (n2)   l6    (n7)   l15  (n12)   l20
 */
// @formatter:on
public class ModeRestrictionTest {
	private static final Logger log = LogManager.getLogger(ModeRestrictionTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Setting: The agent has a route from the plan. Link before and at the activity is restricted respectively.
	 * Expected behaviour: Since it is not checked, if the route contains link, which are not allowed to use anymore, it can still use the restricted link.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"6", "15", "20"})
	void testNoRouteChange_ok(String link) {
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link20.xml");
		config.controller().setLastIteration(0);

		Id<Link> linkId = Id.createLinkId(link);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().get(linkId).setAllowedModes(Set.of());

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(Id.createPersonId("car"), linkId);
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualContainsExpected();
	}

	/**
	 * Setting: The agent has no route. The mode of the link in front of the activity is restricted.
	 * Expected behaviour: By default, the network is not cleaned. The router throws an exception since there is no route from home to the activity.
	 */
	@Test
	void testRerouteBeforeSim_toActNotPossible_throws() {
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link15.xml");
		config.controller().setLastIteration(0);

		Id<Link> link = Id.createLinkId("6");
		Id<Person> person = Id.createPersonId("car");

		Scenario scenario = restrictLinkAndResetRoute(config, link, person);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(person, link);
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, firstLegRouteCheck));
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	/**
	 * Setting: The agent has no route. The mode of the link behind the activity is restricted.
	 * Expected behaviour: By default, the network is not cleaned. The router throws an exception since there is no route from the activity to home.
	 */
	@Test
	void testRerouteBeforeSim_fromActNotPossible_throws() {
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link6.xml");
		config.controller().setLastIteration(0);

		Id<Link> link = Id.createLinkId("15");
		Id<Person> person = Id.createPersonId("car");

		Scenario scenario = restrictLinkAndResetRoute(config, link, person);

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(person, link);
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, firstLegRouteCheck));
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	/**
	 * Setting: The agent has no route. The mode of the activity's link is restricted. Activity has x-y-coordinate.
	 * Expected behaviour: The agent is rerouted to the nearest link to the activity's coordinate.
	 */
	@Test
	void testRerouteBeforeSim_actOnRestrictedLinkWithCoords_ok() {
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link15.xml");
		config.controller().setLastIteration(0);

		Id<Person> person = Id.createPersonId("car");
		Scenario scenario = restrictLinkAndResetRoute(config, Id.createLinkId("15"), person);

		// New route. Note that the end link is 20, but the activity's link in the input was 15.
		// But since we restricted the mode for link 15, 20 is used as fallback.
		List<Id<Link>> newRoute = List.of(Id.createLinkId("1"), Id.createLinkId("2"), Id.createLinkId("11"), Id.createLinkId("20"));

		FirstLegVisitedLinksCheck firstLegRouteCheck = new FirstLegVisitedLinksCheck(person, newRoute);
		runController(scenario, firstLegRouteCheck);
		firstLegRouteCheck.assertActualEqualsExpected();
	}

	/**
	 * Setting: The agent has no route. The mode of the activity's link is restricted. Activity has no x-y-coordinate.
	 * Expected behaviour: The router throws an exception since there is no fallback x-y-coordinate.
	 */
	@Test
	void testRerouteBeforeSim_actOnRestrictedLinkWithoutCoord_throws(){
		final Config config = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link15.xml");
		config.controller().setLastIteration(0);

		Id<Person> person = Id.createPersonId("car");
		Scenario scenario = restrictLinkAndResetRoute(config, Id.createLinkId("15"), person);
		Activity act = (Activity) scenario.getPopulation().getPersons().get(person).getPlans().getFirst().getPlanElements().get(2);
		act.setCoord(null);

		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runController(scenario, new FirstLegVisitedLinksCheck()));
		Assertions.assertTrue(
			exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	private static Scenario restrictLinkAndResetRoute(Config config, Id<Link> link, Id<Person> person) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().get(link).setAllowedModes(Set.of());
		Leg leg = (Leg) scenario.getPopulation().getPersons().get(person).getSelectedPlan().getPlanElements().get(1);
		leg.setRoute(null);
		return scenario;
	}

	private static void runController(Scenario scenario, FirstLegVisitedLinksCheck firstLegRouteCheck) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(firstLegRouteCheck);
			}
		});
		controler.run();
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
			if (linkIds.getLast() == null || linkIds.getLast() == linkId) {
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
}
