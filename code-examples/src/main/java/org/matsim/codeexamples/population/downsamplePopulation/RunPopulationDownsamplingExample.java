/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.population.downsamplePopulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author kn
 * @author jlaudan
 */
class RunPopulationDownsamplingExample {

	private final String inputPopFilename;
	private final String outputPopFilename;

	private RunPopulationDownsamplingExample(String inputPopFilename, String outputPopFilename) {
		this.inputPopFilename = inputPopFilename;
		this.outputPopFilename = outputPopFilename;
	}

	public static void main(final String[] args) {

		String outputPopFilename = "";
		String inputPopFilename = "";

		if ( args!=null ) {
			if (args.length != 2) {
				System.err.println("Usage: cmd inputPop.xml.gz outputPop.xml.gz");
				System.exit(401);
			} else {
				inputPopFilename = args[0] ;
				outputPopFilename = args[1] ;
			}
		}

		RunPopulationDownsamplingExample app = new RunPopulationDownsamplingExample(inputPopFilename, outputPopFilename);
		app.run();
	}

	private void run() {

		// create an empty scenario using an empty configuration
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// the writer will be called by the reader and write the new population file. As parameter the fraction of the
		// input population is passed. In our case we will downsize the population to 1%.
		StreamingPopulationWriter writer = new StreamingPopulationWriter(0.1);

		// the reader will read in an existing population file
		StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
		reader.addAlgorithm(writer);

		try {
			writer.startStreaming(outputPopFilename);
			reader.readFile(inputPopFilename);
		} finally {
			writer.closeStreaming();
		}
	}
}
