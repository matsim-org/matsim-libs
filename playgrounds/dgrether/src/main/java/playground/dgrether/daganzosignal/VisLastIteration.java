/* *********************************************************************** *
 * project: org.matsim.*
 * VisLastIteration
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
package playground.dgrether.daganzosignal;

import java.io.FileNotFoundException;
import java.io.IOException;

import playground.dgrether.DgOTFVisReplayLastIteration;


/**
 * @author dgrether
 *
 */
public class VisLastIteration {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] a) throws FileNotFoundException, IOException {
		DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
		String outputConfig = null ;
		if (scenarioGenerator.runId == null) {
			outputConfig = scenarioGenerator.outputDirectory + "output_config.xml.gz";
		}
		else {
			outputConfig = scenarioGenerator.outputDirectory +  scenarioGenerator.runId + ".output_config.xml.gz";
		}
		String[] args = {outputConfig};
		DgOTFVisReplayLastIteration.main(args);
		
	}

}
