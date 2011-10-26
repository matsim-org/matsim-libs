/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.fhuelsmann.noise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


public class NoiseTool {
	private static String runDirectory = "../../run981/";
	private static String eventsFile = runDirectory
			+ "ITERS/it.1500/981.1500.events.xml.gz";
	private static String netfile = runDirectory + "981.output_network.xml.gz";
	private final Scenario scenario;
	// private final Network net ;
	String outputfile = runDirectory + "noiseEvents.xml";

	public NoiseTool() {
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		NoiseTool noise = new NoiseTool();
		noise.run(args);
	}

	private void run(String[] args) {

		loadScenario();
		Network network = scenario.getNetwork();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		NoiseHandler handler = new NoiseHandler(network);
		eventsManager.addHandler(handler);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(
				eventsManager);
		matsimEventsReader.readFile(eventsFile);

		Map<Id, Map<String, double[]>> linkInfo = handler
				.getlinkToTrafficInfo();
		// Map<Id,List<Double>> linkTimes = handler.getlinkTimes();
		// Map <Id ,Map<String ,Double []>> linkInfos =
		// Test.generateLinkInfos();
		Calculation calculation = new Calculation();

		Map<Id, Map<String, Double>> res = calculation.Cal(linkInfo);

		// this Java code could be placed in another class
		List<NoiseEventImpl> eventsList = new ArrayList<NoiseEventImpl>();
		for (Entry<Id, Map<String, Double>> entry : res.entrySet()) {
			Id linkId = entry.getKey();
			Map<String, Double> L_mE = entry.getValue();
			double time = 0.0;
			NoiseEventImpl event = new NoiseEventImpl(time, linkId, L_mE);
			eventsList.add(event);
		}
		NoiseWriter noiseWriter = new NoiseWriter();
		noiseWriter.writeEvents(eventsList);

	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netfile);
		// config.plans().setInputFile(plansfile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();

	}

}
