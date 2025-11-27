package org.matsim.core.network.turnRestrictions;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import com.google.common.base.Verify;

/**
 * Check if turn restrictions are considered when routing agents in a
 * simulation.
 */
public class TurnRestrictionsSimulationIT {

	private static final Logger LOG = LogManager.getLogger(TurnRestrictionsSimulationIT.class);

	@Test
	void testSimulationWithTurnRestrictions() {

		System.setProperty("matsim.preferLocalDtds", "true");

		URL scenarioURL = ExamplesUtils.getTestScenarioURL("siouxfalls-2014");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(scenarioURL, "config_default.xml"));
		config.global().setCoordinateSystem("EPSG:32614");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// alter network with turn restrictions
		String mode = TransportMode.car;
		addTurnRestrictionsToSiouxFallsNetwork(scenario.getNetwork(), mode);
		Verify.verify(DisallowedNextLinksUtils.isValid(scenario.getNetwork()));

		// simulate
		Controler controller = new Controler(scenario);
		controller.run();

		// get forbidden link sequences
		List<List<Id<Link>>> disallowedLinkSequences = DisallowedNextLinksUtils
				.getDisallowedLinkIdSequences(scenario.getNetwork(), mode);
		// get link sequences from routes
		List<List<Id<Link>>> linkSequencesFromRoutes = getLinkIdSequencesFromRoutes(scenario.getPopulation(), mode);

		// check routes for forbidden sequences
		for (List<Id<Link>> linkSequence : linkSequencesFromRoutes) {
			for (List<Id<Link>> disallowedLinkSequence : disallowedLinkSequences) {
				boolean containsForbiddenSequence = containsSublist(linkSequence, disallowedLinkSequence);
				Assertions.assertFalse(containsForbiddenSequence);
			}
		}
	}

	@Test
	void testContainsSublistContains() {
		List<Integer> l0 = List.of(1, 2, 3, 4, 5);
		List<Integer> l1 = List.of(1, 2, 3, 4);
		List<Integer> l2 = List.of(2, 3, 4, 5);
		List<Integer> l3 = List.of(2, 3, 4);
		List<Integer> l4 = List.of(3);
		List<Integer> l5 = Collections.emptyList();

		Assertions.assertTrue(containsSublist(l0, l0));
		Assertions.assertTrue(containsSublist(l0, l1));
		Assertions.assertTrue(containsSublist(l0, l2));
		Assertions.assertTrue(containsSublist(l0, l3));
		Assertions.assertTrue(containsSublist(l0, l4));
		Assertions.assertTrue(containsSublist(l0, l5));
	}

	@Test
	void testContainsSublistContainsNot() {
		List<Integer> l0 = List.of(1, 2, 3, 4, 5);
		List<Integer> l1 = List.of(1, 3, 4);
		List<Integer> l2 = List.of(2, 3, 5);
		List<Integer> l3 = List.of(2, 4);
		List<Integer> l4 = List.of(0);

		Assertions.assertFalse(containsSublist(l0, l1));
		Assertions.assertFalse(containsSublist(l0, l2));
		Assertions.assertFalse(containsSublist(l0, l3));
		Assertions.assertFalse(containsSublist(l0, l4));
	}

	// Helpers

	private static void addTurnRestrictionsToSiouxFallsNetwork(Network network, String mode) {
		// 67_2 -> 44_1
		Link l67_2 = network.getLinks().get(Id.createLinkId("67_2"));
		Link l44_1 = network.getLinks().get(Id.createLinkId("44_1"));
		DisallowedNextLinks dnl0 = NetworkUtils.getOrCreateDisallowedNextLinks(l67_2);
		dnl0.addDisallowedLinkSequence(mode, List.of(l44_1.getId()));

		// 65_2 -> 68_1
		Link l65_2 = network.getLinks().get(Id.createLinkId("65_2"));
		Link l68_1 = network.getLinks().get(Id.createLinkId("68_1"));
		DisallowedNextLinks dnl1 = NetworkUtils.getOrCreateDisallowedNextLinks(l65_2);
		dnl1.addDisallowedLinkSequence(mode, List.of(l68_1.getId()));
	}

	/**
	 * Check whether a list contains a sublist anywhere.
	 * 
	 * @param <T>
	 * @param list
	 * @param sublist
	 * @return
	 */
	private static <T> boolean containsSublist(List<T> list, List<T> sublist) {
		if (sublist.isEmpty()) {
			return true;
		}

		int listSize = list.size();
		int sublistSize = sublist.size();

		if (listSize < sublistSize) {
			return false;
		} else if (listSize == sublistSize && list.equals(sublist)) {
			return true;
		}
		for (int i = 0; i <= listSize - sublistSize; ++i) {
			if (list.subList(i, i + sublistSize).equals(sublist)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get list of link id sequences of all network routes of a given mode within a
	 * population's plans.
	 * 
	 * @param population
	 * @param mode       use only routes from legs with that mode
	 * @return
	 */
	private static List<List<Id<Link>>> getLinkIdSequencesFromRoutes(Population population, String mode) {
		return population.getPersons().values().stream()
				.map(Person::getPlans).flatMap(List::stream)
				.map(TripStructureUtils::getTrips).flatMap(List::stream)
				.map(Trip::getLegsOnly).flatMap(List::stream)
				.filter(leg -> leg.getMode().equals(mode))
				.map(Leg::getRoute)
				.filter(NetworkRoute.class::isInstance)
				.map(NetworkRoute.class::cast)
				.map(networkRoute -> {
					List<Id<Link>> networkRouteLinkIds = networkRoute.getLinkIds();
					List<Id<Link>> linkIds = new ArrayList<>(networkRouteLinkIds.size() + 2);
					linkIds.add(networkRoute.getStartLinkId());
					linkIds.addAll(networkRouteLinkIds);
					linkIds.add(networkRoute.getEndLinkId());
					return linkIds;
				})
				.toList();
	}

}
