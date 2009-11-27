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

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.run.OTFVis;


/**
 * @author dgrether
 *
 */
public class VisLastIteration {

	/**
	 * @param args
	 */
	public static void main(String[] a) {
		DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
		Config config = null;
		config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(scenarioGenerator.configOut);
		String mvi = config.controler().getOutputDirectory() + 
		"/ITERS/it." + config.controler().getLastIteration() + 
		"/" + config.controler().getLastIteration() + ".otfvis.mvi";
		System.out.println(mvi);
		String[] args = {mvi};
		OTFVis.main(args);
		
	}

}
