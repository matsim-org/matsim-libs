package org.matsim.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.*;

/**
 * In this test, we restrict the allowed modes of a link. The base case is the equil scenario.
 *
 * 		  /...			        ...\
 * ------o------------o------------o-----------
 *  l1  (n2)   l6    (n7)   l15  (n12)   l20
 */
public class ModeRestrictionTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Test if agent still can use a link if the allowed modes of it are restricted.
	 * This is because the route is already contained in the plans file and not checked again.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"6", "15", "20"})
	void testNoRouteChange_ok(String link){
		final Config config = utils.loadConfig( utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link20.xml");
		config.controller().setLastIteration(0);

		Id<Link> linkId = Id.createLinkId(link);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().get(linkId).setAllowedModes(Set.of());

		FirstLegRouteCheck firstLegRouteCheck = new FirstLegRouteCheck(linkId);
		runAndCheck(scenario, firstLegRouteCheck);
	}

	/**
	 * Test if Exception is thrown, if no route is given and the activity cannot be reached. This is because of a local dead end.
	 */
	@Test
	void testRerouteBeforeSim_toActNotPossible_throws() {
		final Config config = utils.loadConfig( utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link15.xml");
		config.controller().setLastIteration(0);

		Id<Link> linkId = Id.createLinkId("6");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().get(linkId).setAllowedModes(Set.of());
		Leg leg = (Leg) scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan().getPlanElements().get(1);
		leg.setRoute(null);

		FirstLegRouteCheck firstLegRouteCheck = new FirstLegRouteCheck(linkId);
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runAndCheck(scenario, firstLegRouteCheck));
		Assertions.assertTrue(exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	/**
	 * Test if Exception is thrown, if no route is given and the activity cannot be left. This is because of a local dead end.
	 */
	@Test
	void testRerouteBeforeSim_fromActNotPossible_throws() {
		final Config config = utils.loadConfig( utils.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile("plans_act_link6.xml");
		config.controller().setLastIteration(0);

		Id<Link> linkId = Id.createLinkId("15");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().get(linkId).setAllowedModes(Set.of());
		Leg leg = (Leg) scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan().getPlanElements().get(1);
		leg.setRoute(null);

		FirstLegRouteCheck firstLegRouteCheck = new FirstLegRouteCheck(linkId);
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> runAndCheck(scenario, firstLegRouteCheck));
		Assertions.assertTrue(exception.getMessage().contains("Exception while processing persons. Cannot guarantee that all persons have been fully processed."));
	}

	private static void runAndCheck(Scenario scenario, FirstLegRouteCheck firstLegRouteCheck) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(firstLegRouteCheck);
			}
		});
		controler.run();

		firstLegRouteCheck.assertActualContainsExpected();
	}

	private static class FirstLegRouteCheck implements LinkEnterEventHandler, ActivityEndEventHandler, PersonLeavesVehicleEventHandler {
		private final List<Id<Link>> expected;
		private final List<Id<Link>> actual = new ArrayList<>();

		private boolean firstLeg = true;

		public FirstLegRouteCheck(List<Id<Link>> expected) {
			this.expected = expected;
		}

		public FirstLegRouteCheck(Id<Link> expected){
			this.expected = Collections.singletonList(expected);
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			addLinkIdToActual(event.getLinkId());
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			addLinkIdToActual(event.getLinkId());
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			firstLeg = false;
		}

		private void addLinkIdToActual(Id<Link> event) {
			if (!firstLeg) {
				return;
			}
			actual.add(event);
		}

		public void assertActualEqualsExpected() {
			Assertions.assertEquals(expected, actual);
		}

		public void assertActualContainsExpected() {
			Assertions.assertTrue(actual.containsAll(expected));
		}
	}

}
