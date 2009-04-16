/* *********************************************************************** *
 * project: org.matsim.*
 * DaganzoRunner
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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.run.OTFVis;


/**
 * @author dgrether
 *
 */
public class DaganzoRunner {
	
	private static final Logger log = Logger.getLogger(DaganzoRunner.class);
	
	private static final boolean visualizationOnly = false;

	public DaganzoRunner(){
	}

	public void runScenario(){
		DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
		Config config = null;
		if (visualizationOnly){
			config = new Config();
			MatsimConfigReader reader = new MatsimConfigReader(config);
			reader.readFile(DaganzoScenarioGenerator.configOut);
		}
		else {
			Controler controler = new Controler(DaganzoScenarioGenerator.configOut);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		
		
		
		//Visualize
		String[] args = {config.controler().getOutputDirectory() + 
				"/ITERS/it." + config.controler().getLastIteration() + 
				"/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		OTFVis.main(args);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DaganzoRunner().runScenario();
	}

}
