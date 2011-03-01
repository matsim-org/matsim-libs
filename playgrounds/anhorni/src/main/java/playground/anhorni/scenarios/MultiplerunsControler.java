/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.scenarios;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;

import playground.anhorni.scenarios.analysis.RandomRunsAnalyzer;
import playground.anhorni.scenarios.analysis.SummaryWriter;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private static String path = "src/main/java/playground/anhorni/";
	private int numberOfRandomRuns = -1; // from config
	private SummaryWriter summaryWriter = null;
	private ConfigReader myConfigReader = new ConfigReader();
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.myConfigReader.read();
    	this.numberOfRandomRuns = myConfigReader.getNumberOfRandomRuns();
    	this.summaryWriter = new SummaryWriter(myConfigReader.getNumberOfCityShoppingLocs(), path, this.numberOfRandomRuns);
    	this.createConfigs();
    }
       
    private void createConfigs() {
    	Config config = new Config();
    	MatsimConfigReader configReader = new MatsimConfigReader(config);
    	configReader.readFile(path + "/input/PLOC/3towns/config.xml");   	
    	config.setParam("network", "inputNetworkFile", path + "input/PLOC/3towns/networks/" + 
    			this.myConfigReader.getPopulationSize() + "_network.xml");
    	
    	String outputPath = path + "";
    	ConfigWriter configWriter = new ConfigWriter(config);
    	
    	// random -------------------------------------------------------------
    	for (int i = 0; i < numberOfRandomRuns; i++) {
    		config.setParam("plans", "inputPlansFile", path + "input/PLOC/3towns/plans/" + i + "_plans_random.xml");
        	config.setParam("controler", "runId", i + "_random");
        	outputPath = path + "/output/PLOC/3towns/matsim/random/";
        	new File(outputPath).mkdir();
        	config.setParam("controler", "outputDirectory", outputPath + i + "_random");
        	configWriter.write(path + "/input/PLOC/3towns/configs/" + i + "_config_random.xml");
    	}
    	
    	//  ---------------------------------------------------------------- 	 	
    }
    
    public void run() {
    	
    	this.init();
    	    	
    	String config[] = {""};
		SingleRunControler controler = new SingleRunControler(config);
			    	   	
    	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
    		config[0] = "src/main/java/playground/anhorni/input/PLOC/3towns/configs/" + runIndex + "_config_random.xml";
    		
    		controler = new SingleRunControler(config);
    		controler.setOverwriteFiles(true);
    		controler.setNumberOfCityShoppingLocations(myConfigReader.getNumberOfCityShoppingLocs());
        	controler.run();
        	
        	this.summaryWriter.write2Summary(runIndex);
    	}	    	
    	summaryWriter.finish();
    	
    	log.info("Create analysis ...");
    	
    	RandomRunsAnalyzer analyzer = new RandomRunsAnalyzer(myConfigReader.getNumberOfCityShoppingLocs(), MultiplerunsControler.path, numberOfRandomRuns);
    	analyzer.run(myConfigReader.getNumberOfAnalyses());
    	
    	log.info("All runs finished ******************************");
    }
}
