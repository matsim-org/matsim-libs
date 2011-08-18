/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionMain.java
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
package playground.fhuelsmann.emission.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


/**
 * @author friederike
 *
 */

public class CongestionMain {
	

	private final static String runDirectory = "../../run980/";
	private final static String netFile = runDirectory + "980.output_network.xml.gz";
	
	private final static String eventsFile = runDirectory + "ITERS/it.1000/980.1000.events.xml.gz";
	
	private final Scenario scenario;

	public CongestionMain(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	private void run(String[] args) {
		loadScenario();
		Network network = scenario.getNetwork();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		
		Congestion cong = new Congestion(network);
		
		eventsManager.addHandler(cong);
	
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
	}
	
	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}

	public static void main (String[] args) throws Exception{
		CongestionMain congestionMain = new CongestionMain();
		congestionMain.run(args);
	}

}
