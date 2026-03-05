package org.matsim.application.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.prepare.scenario.CreateScenarioCutOut;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "create-network-change-events", description = "Create network change events based on travel time calculated from input events.")
public class CreateNetworkChangeEvents implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(CreateNetworkChangeEvents.class);

	@CommandLine.Option(names = "--events", description = "Input events used for travel time calculation.", required = true)
	private String eventPath;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private String networkPath;

	@CommandLine.Option(names = "--output", description = "Path to network change event output", required = true)
	private String outputEvents;

	@CommandLine.Option(names = "--interval", description = "Interval of network change events. Unit is seconds.")
	private double changeEventsInterval = 900.;

	private TravelTimeCalculator tt;

	static void main(String[] args) {
		new CreateNetworkChangeEvents().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		log.info("Starting to read network and events.");
		Network network = NetworkUtils.readNetwork(networkPath);

		// this is similar to what happens in org.matsim.application.prepare.scenario.CreateScenarioCutOut
		LastTimeEvaluator lastTimeEvaluator = setUpTravelTimeCalculator(network);

		log.info("Start to create network change events and write them to file.");
		List<NetworkChangeEvent> networkChangeEvents = CreateScenarioCutOut.generateNetworkChangeEvents(network, tt, null, null, true, lastTimeEvaluator.getLastTime(), changeEventsInterval);
		new NetworkChangeEventsWriter().write(outputEvents, networkChangeEvents);

		return 0;
	}

	private @NonNull LastTimeEvaluator setUpTravelTimeCalculator(Network network) {
		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setTimeslice(changeEventsInterval);

		EventsManager manager = EventsUtils.createEventsManager();
		tt = builder.build();
		manager.addHandler(tt);
		LastTimeEvaluator lastTimeEvaluator = new LastTimeEvaluator();
		manager.addHandler(lastTimeEvaluator);
		manager.initProcessing();
		EventsUtils.readEvents(manager, eventPath);
		manager.finishProcessing();
		return lastTimeEvaluator;
	}

	private static class LastTimeEvaluator implements BasicEventHandler {
		double lastTime = -1;

		@Override
		public void handleEvent(Event event) {
			lastTime = Math.max(lastTime, event.getTime());
		}

		public double getLastTime() {
			return lastTime;
		}
	}
}
