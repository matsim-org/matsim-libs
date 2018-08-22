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
package tutorial.fixedTimeSignals;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesDataFactory;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesWriter10;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreensForSignalSystemData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;

/**
 * This example shows how to create an intergreens input file for a given scenario.
 *
 * If there is no intergreen data available for your scenario you may use the
 * intergreen times of a correct fixed time signal control, i.e. a signal control
 * with realistic intergreen times where no collisions may occur.
 * @link TtCalculateSimplifiedIntergreens for how to extract intergreen times from a fixed time control
 *  
 * @author dgrether
 */
public class CreateIntergreensExample {

	private static final Logger log = Logger.getLogger(CreateIntergreensExample.class);
	
	private static final String INPUT_DIR = "examples/tutorial/example90TrafficLights/useSignalInput/woLanes/";
	private static String outputDir = "output/example90TrafficLights/";
	
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
	
	/**
	 * @param args if not null it gives the output directory for the intergreens file
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args != null){
			// use the given output if args is not null
			outputDir = args[0];
		}
		// read in the existing scenario and add the intergreens data
		Config config = ConfigUtils.loadConfig(INPUT_DIR + "config.xml");
		SignalSystemsConfigGroup signalSystemsConfigGroup = 
				ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setUseIntergreenTimes(true);
		SignalsDataLoader signalsDataLoader = new SignalsDataLoader(config);
		SignalsData signalsData = signalsDataLoader.loadSignalsData();
		createIntergreens(signalsData);
		// write the intergreens file
		IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(signalsData.getIntergreenTimesData());
		String intergreensFilename = outputDir + "intergreens.xml";
		writer.write(intergreensFilename);
		log.info("Intergreens written to " + intergreensFilename);
	}
}
