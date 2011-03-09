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

import org.apache.log4j.Logger;
import playground.anhorni.scenarios.analysis.SummaryWriter;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private static String path = "src/main/java/playground/anhorni/";
	private int numberOfRandomRuns = -1; // from config
	private SummaryWriter summaryWriter = null;
	private ConfigReader configReader = new ConfigReader();
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.configReader.read();
    	this.numberOfRandomRuns = configReader.getNumberOfRandomRuns();
    	this.summaryWriter = new SummaryWriter(path, this.numberOfRandomRuns);
    }
           
    public void run() {
    	
    	this.init();
    	    	
    	String config[] = {""};
		SingleRunControler controler = new SingleRunControler(config);
			    	   	
    	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {   		
    		for (int i = 0; i < 5; i++) {
    			config[0] = "src/main/java/playground/anhorni/input/PLOC/3towns/configs/configR" + runIndex + "D" + i + ".xml";
	    		controler = new SingleRunControler(config);	    		    			        	              	
	        	controler.run();
    		}
    	}
    	log.info("Create analysis ...");
    	
    	summaryWriter.run();
    	
    	
    	
//    	RandomRunsAnalyzer analyzer = new RandomRunsAnalyzer(
//    			configReader.getNumberOfCityShoppingLocs(), MultiplerunsControler.path, numberOfRandomRuns);
//    	analyzer.run(configReader.getNumberOfAnalyses());
    	
    	log.info("All runs finished ******************************");
    }
}
