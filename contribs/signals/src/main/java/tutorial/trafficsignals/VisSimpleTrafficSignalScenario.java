/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSignalSystemScenario
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
package tutorial.trafficsignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * This class contains a simple example how to visualize a simple scenario
 * with signalized intersections.
 * 
 * @author dgrether
 *
 * @see org.matsim.signals
 * @see http://matsim.org/node/384
 *
 */
public class VisSimpleTrafficSignalScenario {

	
	private void run() {
		String configFile = new RunCreateTrafficSignalScenarioExample().run();
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, 
				new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		OTFVisWithSignals.playScenario(scenario);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new VisSimpleTrafficSignalScenario().run();
	}


}
