package org.matsim.application.prepare.scenario;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CreateScenarioCutOutTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Test the cutout without facilities and without events.
	 */
	@Test
	void testBasicCutout() {
		// TODO Rueckfrage: Soll die cap auch auf inf gesetzt werden, wenn es keine Events gibt, die die freespeed anpassen?
		new CreateScenarioCutOut().execute(
			"--buffer", "100",
			"--population", utils.getClassInputDirectory() + "plans_without_facilities.xml",
			"--network", ExamplesUtils.getTestScenarioURL("chessboard") + "network.xml",
			"--output-network", utils.getOutputDirectory() + "cut_network.xml",
			"--output-population", utils.getOutputDirectory() + "cut_population.xml",
			"--output-network-change-events", utils.getOutputDirectory() + "cut_change_events.xml",
			"--input-crs", "EPSG:25832",
			"--target-crs", "EPSG:25832",
			"--shp", utils.getClassInputDirectory() + "chessboard-cutout.shp",
			"--network-change-events-interval", "3600"
		);

		Scenario referenceScenario = getReferenceScenario(false, false);
		Scenario outputScenario = getOutputScenario(false, false);

		// Network-Change event should only be generated, if an events file is given
		assertThat(new File(utils.getOutputDirectory() + "cut_change_events.xml"))
			.doesNotExist();
		assertThat(new File(utils.getOutputDirectory() + "cut_facilities.xml"))
			.doesNotExist();

		Assertions.assertTrue(NetworkUtils.compare(referenceScenario.getNetwork(), outputScenario.getNetwork()), "Network was not cut properly!");
		Assertions.assertSame(PopulationComparison.Result.equal, PopulationComparison.compare(referenceScenario.getPopulation(), outputScenario.getPopulation()), "Population was not cut properly!");
	}

	/**
	 * Test the cutout with facilities but without events.
	 */
	@Test
	void testFacilitiesCutout() {
		new CreateScenarioCutOut().execute(
			"--buffer", "100",
			"--population", ExamplesUtils.getTestScenarioURL("chessboard") + "plans.xml",
			"--network", ExamplesUtils.getTestScenarioURL("chessboard") + "network.xml",
			"--facilities", ExamplesUtils.getTestScenarioURL("chessboard") + "facilities.xml",
			"--output-network", utils.getOutputDirectory() + "cut_network.xml",
			"--output-population", utils.getOutputDirectory() + "cut_population.xml",
			"--output-network-change-events", utils.getOutputDirectory() + "cut_change_events.xml",
			"--output-facilities", utils.getOutputDirectory() + "cut_facilities.xml",
			"--input-crs", "EPSG:25832",
			"--target-crs", "EPSG:25832",
			"--shp", utils.getClassInputDirectory() + "chessboard-cutout.shp",
			"--network-change-events-interval", "3600"
		);

		Scenario referenceScenario = getReferenceScenario(false, true);
		Scenario outputScenario = getOutputScenario(false, true);

		// Network-Change event should only be generated, if an events file is given
		assertThat(new File(utils.getOutputDirectory() + "cut_change_events.xml"))
			.doesNotExist();

		Assertions.assertTrue(NetworkUtils.compare(referenceScenario.getNetwork(), outputScenario.getNetwork()), "Network was not cut properly!");
		Assertions.assertSame(PopulationComparison.Result.equal, PopulationComparison.compare(referenceScenario.getPopulation(), outputScenario.getPopulation()), "Population was not cut properly!");
		Assertions.assertTrue(outputScenario.getActivityFacilities().getFacilities().size() <= referenceScenario.getActivityFacilities().getFacilities().size());
		Assertions.assertTrue(outputScenario.getActivityFacilities().getFacilities().keySet().stream().allMatch(id -> referenceScenario.getActivityFacilities().getFacilities().containsKey(id)));
	}

	/**
	 * Test the cutout without facilities but with events.
	 */
	@Test
	void testEventsCutout() throws IOException {
		new CreateScenarioCutOut().execute(
			"--buffer", "100",
			"--population", utils.getClassInputDirectory() + "plans_without_facilities.xml",
			"--network", ExamplesUtils.getTestScenarioURL("chessboard") + "network.xml",
			"--events", utils.getClassInputDirectory() + "events.xml.gz",
			"--output-network", utils.getOutputDirectory() + "cut_network.xml",
			"--output-population", utils.getOutputDirectory() + "cut_population.xml",
			"--output-network-change-events", utils.getOutputDirectory() + "cut_change_events.xml",
			"--input-crs", "EPSG:25832",
			"--target-crs", "EPSG:25832",
			"--shp", utils.getClassInputDirectory() + "chessboard-cutout.shp",
			"--network-change-events-interval", "3600"
		);

		Scenario referenceScenario = getReferenceScenario(true, false);
		Scenario outputScenario = getOutputScenario(true, false);

		assertThat(new File(utils.getOutputDirectory() + "cut_facilities.xml"))
			.doesNotExist();

		Assertions.assertTrue(NetworkUtils.compare(referenceScenario.getNetwork(), outputScenario.getNetwork()), "Network was not cut properly!");
		Assertions.assertTrue(checkIfNetworkChangeEventsAreSame(referenceScenario.getNetwork(), outputScenario.getNetwork()), "NetworkChangeEvents were not generated properly!");
		Assertions.assertSame(PopulationComparison.Result.equal, PopulationComparison.compare(referenceScenario.getPopulation(), outputScenario.getPopulation()), "Population was not cut properly!");
	}

	/**
	 * Test the cutout with facilities and events.
	 */
	@Test
	void testFullCutOut() throws IOException {
		new CreateScenarioCutOut().execute(
			"--buffer", "100",
			"--population", ExamplesUtils.getTestScenarioURL("chessboard") + "plans.xml",
			"--network", ExamplesUtils.getTestScenarioURL("chessboard") + "network.xml",
			"--events", utils.getClassInputDirectory() + "events.xml.gz",
			"--facilities", ExamplesUtils.getTestScenarioURL("chessboard") + "facilities.xml",
			"--output-network", utils.getOutputDirectory() + "cut_network.xml",
			"--output-population", utils.getOutputDirectory() + "cut_population.xml",
			"--output-network-change-events", utils.getOutputDirectory() + "cut_change_events.xml",
			"--output-facilities", utils.getOutputDirectory() + "cut_facilities.xml",
			"--input-crs", "EPSG:25832",
			"--target-crs", "EPSG:25832",
			"--shp", utils.getClassInputDirectory() + "chessboard-cutout.shp",
			"--network-change-events-interval", "3600"
		);

		Scenario referenceScenario = getReferenceScenario(true, true);
		Scenario outputScenario = getOutputScenario(true, true);

		Assertions.assertTrue(NetworkUtils.compare(referenceScenario.getNetwork(), outputScenario.getNetwork()), "Network was not cut properly!");
		Assertions.assertTrue(checkIfNetworkChangeEventsAreSame(referenceScenario.getNetwork(), outputScenario.getNetwork()), "NetworkChangeEvents were not generated properly!");
		Assertions.assertSame(PopulationComparison.Result.equal, PopulationComparison.compare(referenceScenario.getPopulation(), outputScenario.getPopulation()), "Population was not cut properly!");
		Assertions.assertTrue(outputScenario.getActivityFacilities().getFacilities().size() <= referenceScenario.getActivityFacilities().getFacilities().size());
		Assertions.assertTrue(outputScenario.getActivityFacilities().getFacilities().keySet().stream().allMatch(id -> referenceScenario.getActivityFacilities().getFacilities().containsKey(id)));
	}

	/**
	 * Get the reference-output as a scenario
	 *
	 * @param withEvents If you executed the test with an input Events file, set this to true
	 */
	private Scenario getReferenceScenario(boolean withEvents, boolean withFacilities) {
		String folder = withEvents ? "withEvents/" : "withoutEvents/";

		Config referenceConfig = ConfigUtils.createConfig();
		referenceConfig.global().setCoordinateSystem("EPSG:25832");
		referenceConfig.network().setTimeVariantNetwork(true);
		referenceConfig.network().setInputFile(utils.getClassInputDirectory() + folder + "cut_network.xml");
		if (withEvents) referenceConfig.network().setChangeEventsInputFile(utils.getClassInputDirectory() + folder + "cut_change_events.xml");
		referenceConfig.plans().setInputFile(utils.getClassInputDirectory() + folder + "cut_population.xml");
		if (withFacilities) referenceConfig.facilities().setInputFile(utils.getClassInputDirectory() + folder + "cut_facilities.xml");
		return ScenarioUtils.loadScenario(referenceConfig);
	}

	/**
	 * Get the output as a scenario
	 */
	private Scenario getOutputScenario(boolean withEvents, boolean withFacilities) {
		Config outputConfig = ConfigUtils.createConfig();
		outputConfig.global().setCoordinateSystem("EPSG:25832");
		outputConfig.network().setTimeVariantNetwork(true);
		outputConfig.network().setInputFile(utils.getOutputDirectory() + "cut_network.xml");
		if (withEvents) outputConfig.network().setChangeEventsInputFile(utils.getOutputDirectory() + "cut_change_events.xml");
		outputConfig.plans().setInputFile(utils.getOutputDirectory() + "cut_population.xml");
		if (withFacilities) outputConfig.facilities().setInputFile(utils.getOutputDirectory() + "cut_facilities.xml");
		return ScenarioUtils.loadScenario(outputConfig);
	}

	/**
	 * Checks if two Networks have the same NetworkChangeEvents by checking the ids.
	 */
	private boolean checkIfNetworkChangeEventsAreSame(Network firstNetwork, Network secondNetwork) throws IOException {
		List<NetworkChangeEvent> firstList = new LinkedList<>();
		List<NetworkChangeEvent> secondList = new LinkedList<>();
		new NetworkChangeEventsParser(firstNetwork, firstList).parse(Files.newInputStream(Path.of(utils.getClassInputDirectory() + "withEvents/cut_change_events.xml")));
		new NetworkChangeEventsParser(secondNetwork, secondList).parse(Files.newInputStream(Path.of(utils.getOutputDirectory() + "cut_change_events.xml")));
		;

		if (firstList.size() != secondList.size()) return false;

		// This loop is unefficient, but I did not found a better way, to compare the NetworkChangeEvents, as they have no deterministic order
		int matches = 0;
		for (NetworkChangeEvent firstEvent : firstList) {
			// Find an event in the secondList, that matches this event
			for (NetworkChangeEvent secondEvent : secondList) {
				if (firstEvent.getStartTime() != secondEvent.getStartTime()) continue;
				if (firstEvent.getFreespeedChange() != null && secondEvent.getFreespeedChange() != null && firstEvent.getFreespeedChange().getValue() != secondEvent.getFreespeedChange().getValue())
					continue;
				if (firstEvent.getFlowCapacityChange() != null && secondEvent.getFlowCapacityChange() != null && firstEvent.getFlowCapacityChange().getValue() != secondEvent.getFlowCapacityChange().getValue())
					continue;
				if (!equalsIds(firstEvent.getLinks(), secondEvent.getLinks())) continue;

				matches++;
				break;
			}
		}

		return matches == firstList.size();
	}

	/**
	 * Checks if two List have the same elements by checking the ids.
	 */
	private <T> boolean equalsIds(Collection<? extends Identifiable<T>> firstList, Collection<? extends Identifiable<T>> secondList) {
		return new HashSet<>(firstList.stream().map(Identifiable::getId).toList()).equals(new HashSet<>(secondList.stream().map(Identifiable::getId).toList()));
	}
}
