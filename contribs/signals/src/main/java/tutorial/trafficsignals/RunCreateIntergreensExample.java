/* *********************************************************************** *
 * project: org.matsim.*
 * CreateIntergreens
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesWriter10;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.signals.data.SignalsData;
import org.matsim.signals.data.intergreens.v10.IntergreenTimesData;
import org.matsim.signals.data.intergreens.v10.IntergreenTimesDataFactory;
import org.matsim.signals.data.intergreens.v10.IntergreensForSignalSystemData;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalSystem;


/**
 * This Example shows how to create an intergreens input file for the CreateSimpleTrafficSignalScenario Example. 
 * @author dgrether
 *
 */
public class RunCreateIntergreensExample {

	private static final Logger log = Logger.getLogger(RunCreateIntergreensExample.class);
	
	private static void createIntergreens(SignalsData sd){
		IntergreenTimesData ig = sd.getIntergreenTimesData();
		IntergreenTimesDataFactory igdf = ig.getFactory();
		// Create a data object for signal system with id 3
		IntergreensForSignalSystemData ig3 = igdf.createIntergreensForSignalSystem(Id.create("3", SignalSystem.class));
		// Request at least 10 seconds red between the end of green of signal group 1 
		// and the beginning of green of signal group 2 (signal system 3)
		ig3.setIntergreenTime(10, Id.create("1", SignalGroup.class), Id.create("2", SignalGroup.class));
		// add the data object to the container
		ig.addIntergreensForSignalSystem(ig3);
		// same as above for signal system 4...
		IntergreensForSignalSystemData ig4 = igdf.createIntergreensForSignalSystem(Id.create("4", SignalSystem.class));
		ig4.setIntergreenTime(10, Id.create("1", SignalGroup.class), Id.create("2", SignalGroup.class));
		ig.addIntergreensForSignalSystem(ig4);
	}
	
	
	public static void main(String[] args) {
		String configFile = new RunCreateTrafficSignalScenarioExample().run();
		Config config = ConfigUtils.loadConfig(configFile);
		config.signalSystems().setUseIntergreenTimes(true);
		SignalsScenarioLoader loader = new SignalsScenarioLoader(config.signalSystems());
		SignalsData signalsData = loader.loadSignalsData();
		createIntergreens(signalsData);
		IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(signalsData.getIntergreenTimesData());
		String intergreensFilename = "output/example90TrafficLights/intergreens.xml";
		writer.write(intergreensFilename);
		log.info("Intergreens written to " + intergreensFilename);
	}

}
