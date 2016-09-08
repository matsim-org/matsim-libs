/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTrafficSignalScenarioWithLanes
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package tutorial.fixedTimeSignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This class contains a simple example how to visualize a scenario with lanes and signalized intersections.
 * 
 * @author dgrether
 */
public class VisualizeSignalScenarioWithLanes {

	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/";

	private void run() {
		Config config = ConfigUtils.loadConfig(INPUT_DIR + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		OTFVisWithSignals.playScenario(scenario);
	}

	public static void main(String[] args) {
		new VisualizeSignalScenarioWithLanes().run();
	}
}
