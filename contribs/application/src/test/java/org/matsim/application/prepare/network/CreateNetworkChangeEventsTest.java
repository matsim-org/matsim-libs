package org.matsim.application.prepare.network;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateNetworkChangeEventsTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generatesValidEventsForDefaultInterval() {

		String scenarioRoot = ExamplesUtils.getTestScenarioURL("equil").toString();
		String networkPath = scenarioRoot + "network.xml";
		String inputEventsPath = scenarioRoot + "output_events.xml.gz";
		String outputChangeEventsPath = utils.getOutputDirectory() + "network-change-events.xml";
		String inputChangeEventsPath = utils.getInputDirectory() + "network-change-events.xml";

		new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath,
			"--output", outputChangeEventsPath
		);

		List<NetworkChangeEvent> generatedEvents = parseNetworkChangeEvents(networkPath, outputChangeEventsPath);
		List<NetworkChangeEvent> referenceEvents = parseNetworkChangeEvents(networkPath, inputChangeEventsPath);

		Assertions.assertEquals(generatedEvents.size(), referenceEvents.size());
		for (int i = 0; i < generatedEvents.size(); i++) {
			Assertions.assertTrue(eventsEqual(generatedEvents.get(i), referenceEvents.get(i)));
		}
	}

	@Test
	void respectsConfiguredIntervalAndProducesNoMoreEventsThanDefault() {
		String scenarioRoot = ExamplesUtils.getTestScenarioURL("equil").toString();
		String networkPath = scenarioRoot + "network.xml";
		String inputEventsPath = scenarioRoot + "output_events.xml.gz";
		String customOutput = utils.getOutputDirectory() + "custom-network-change-events.xml";
		String customInput = utils.getInputDirectory() + "custom-network-change-events.xml";
		double customInterval = 3_600.;

		new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath,
			"--output", customOutput,
			"--interval", String.valueOf(customInterval)
		);

		List<NetworkChangeEvent> outputEvents = parseNetworkChangeEvents(networkPath, customOutput);
		List<NetworkChangeEvent> referenceEvents = parseNetworkChangeEvents(networkPath, customInput);
		Assertions.assertEquals(outputEvents.size(), referenceEvents.size());
		for (int i = 0; i < outputEvents.size(); i++) {
			Assertions.assertTrue(eventsEqual(outputEvents.get(i), referenceEvents.get(i)));
		}
	}

	private List<NetworkChangeEvent> parseNetworkChangeEvents(String networkPath, String networkChangeEventsPath) {
		List<NetworkChangeEvent> generatedEvents = new ArrayList<>();
		try (InputStream eventsStream = Files.newInputStream(Path.of(networkChangeEventsPath))) {
			new NetworkChangeEventsParser(NetworkUtils.readNetwork(networkPath), generatedEvents).parse(eventsStream);
		} catch (IOException e) {
			throw new RuntimeException("Could not parse network change events from " + networkChangeEventsPath, e);
		}
		return generatedEvents;
	}

	private boolean eventsEqual(NetworkChangeEvent o1, NetworkChangeEvent o2) {
		boolean time = Double.compare(o1.getStartTime(), o2.getStartTime()) == 0;
		boolean freespeed = o1.getFreespeedChange().equals(o2.getFreespeedChange());
		boolean changes = o1.getAttributesChanges().equals(o2.getAttributesChanges());
		boolean lanesChange = (o1.getLanesChange() == null && o2.getLanesChange() == null) || o1.getLanesChange().equals(o2.getLanesChange());
		boolean flow = (o1.getFlowCapacityChange() == null && o2.getFlowCapacityChange() == null) || o1.getFlowCapacityChange().equals(o2.getFlowCapacityChange());
		return time && freespeed && changes && lanesChange && flow;
	}
}
