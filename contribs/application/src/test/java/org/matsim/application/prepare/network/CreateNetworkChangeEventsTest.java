package org.matsim.application.prepare.network;

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

import java.util.ArrayList;
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

		new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath,
			"--output", outputChangeEventsPath
		);

		List<NetworkChangeEvent> generatedEvents = parseNetworkChangeEvents(networkPath, outputChangeEventsPath);
		double lastInputEventTime = getLastEventTime(inputEventsPath);

		assertThat(generatedEvents).isNotEmpty();
		assertThat(generatedEvents).allSatisfy(event -> {
			assertThat(event.getStartTime()).isGreaterThanOrEqualTo(0);
			assertThat(event.getStartTime()).isLessThan(lastInputEventTime);
			assertThat(event.getStartTime() % 900.).isCloseTo(0., org.assertj.core.data.Offset.offset(1e-9));
			assertThat(event.getFreespeedChange()).isNotNull();
			assertThat(event.getFreespeedChange().getValue()).isGreaterThan(0.);
			assertThat(event.getLinks()).hasSize(1);
			assertThat(event.getFreespeedChange().getValue()).isLessThanOrEqualTo(event.getLinks().iterator().next().getFreespeed());
		});
	}

	@Test
	void respectsConfiguredIntervalAndProducesNoMoreEventsThanDefault() {
		String scenarioRoot = ExamplesUtils.getTestScenarioURL("equil").toString();
		String networkPath = scenarioRoot + "network.xml";
		String inputEventsPath = scenarioRoot + "output_events.xml.gz";
		String defaultOutput = utils.getOutputDirectory() + "default-network-change-events.xml";
		String customOutput = utils.getOutputDirectory() + "custom-network-change-events.xml";
		double customInterval = 3_600.;

		new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath,
			"--output", defaultOutput
		);

		new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath,
			"--output", customOutput,
			"--interval", String.valueOf(customInterval)
		);

		List<NetworkChangeEvent> defaultEvents = parseNetworkChangeEvents(networkPath, defaultOutput);
		List<NetworkChangeEvent> customIntervalEvents = parseNetworkChangeEvents(networkPath, customOutput);

		assertThat(defaultEvents).isNotEmpty();
		assertThat(customIntervalEvents).isNotEmpty();
		assertThat(customIntervalEvents.size()).isLessThanOrEqualTo(defaultEvents.size());
		assertThat(customIntervalEvents).allSatisfy(event ->
			assertThat(event.getStartTime() % customInterval).isCloseTo(0., org.assertj.core.data.Offset.offset(1e-9))
		);
	}

	@Test
	void failsIfRequiredArgumentsAreMissing() {
		String scenarioRoot = ExamplesUtils.getTestScenarioURL("equil").toString();
		String networkPath = scenarioRoot + "network.xml";
		String inputEventsPath = scenarioRoot + "output_events.xml.gz";

		assertThatThrownBy(() -> new CreateNetworkChangeEvents().execute(
			"--events", inputEventsPath,
			"--network", networkPath
		))
			.isInstanceOf(RuntimeException.class);
	}

	private List<NetworkChangeEvent> parseNetworkChangeEvents(String networkPath, String networkChangeEventsPath) {
		List<NetworkChangeEvent> generatedEvents = new ArrayList<>();
		new NetworkChangeEventsParser(NetworkUtils.readNetwork(networkPath), generatedEvents).parse(networkChangeEventsPath);
		return generatedEvents;
	}

	private double getLastEventTime(String eventsPath) {
		double[] lastTime = new double[]{-1.};
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler((BasicEventHandler) event -> lastTime[0] = Math.max(lastTime[0], event.getTime()));
		manager.initProcessing();
		EventsUtils.readEvents(manager, eventsPath);
		manager.finishProcessing();
		return lastTime[0];
	}
}
