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
package tutorial.fixedTimeSignals;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * This class contains a simple example how to visualize a simple scenario
 * with signalized intersections.
 * 
 * @author dgrether
 */
public class VisualizeSignalScenario {	

	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/useSignalInput/woLanes/";
	
	public static void run(boolean startOtfvis) throws IOException {
		// --- load the configuration file
		Config config = ConfigUtils.loadConfig(INPUT_DIR + "config.xml");
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		// --- create the scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// load the information about signals data (i.e. fill the SignalsData object) and add it to the scenario as scenario element
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		// --- create the controler
		Controler c = new Controler(scenario);
		// add the signals module to the simulation such that SignalsData is not only
		// contained in the scenario but also used in the simulation
		c.addOverridingModule(new SignalsModule());
		if (startOtfvis) {
			// add the module that start the otfvis visualization with signals
			c.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}
		
		// --- run the simulation
		c.run();
	}
	
	public static void main(String[] args) throws IOException {
		run(true);
	}
}
