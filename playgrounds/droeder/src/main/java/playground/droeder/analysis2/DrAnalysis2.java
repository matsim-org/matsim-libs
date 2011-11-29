/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.analysis2;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author droeder
 *
 */
public class DrAnalysis2 {
	private static final String PLANS = ".plans.xml.gz";
	private static final String EVENTS = ".events.xml.gz";
	private String outputDir;
	private String iteration;
	private Scenario sc;
	
	private Set<AbstractDrAnalysisModule> modules;

	/**
	 * 
	 * @param configFile
	 * @param iteration
	 */
	public DrAnalysis2(String configFile, Integer iteration){
		Config c = ConfigUtils.loadConfig(configFile);
		if(iteration == null){
			this.iteration = "ITERS/it." + String.valueOf(c.controler().getLastIteration()) + "/" +
					String.valueOf(c.controler().getLastIteration());
		}else{
			this.iteration = "ITERS/it." + String.valueOf(iteration) + "/" + String.valueOf(iteration);
		}
		this.outputDir = c.controler().getOutputDirectory();
		String plans = getFileDir(this.PLANS);
		c.plans().setInputFile(plans);
		this.sc = ScenarioUtils.loadScenario(c);
	}
	
	private String getFileDir(String type) {
		String file = this.outputDir + this.iteration + type;
		if(!new File(file).exists()){
			try {
				throw new FileNotFoundException(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return file;
	}

}
