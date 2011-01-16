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

package playground.anhorni.PLOC;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;

import playground.anhorni.LEGO.miniscenario.run.MixedControler;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private static String path = "src/main/java/playground/anhorni/PLOC/";
	private int numberOfRandomRuns = -1; // from PLOCConfigReader
	private PLOCConfigReader plocConfigReader = new PLOCConfigReader();
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.plocConfigReader.read();
    	this.numberOfRandomRuns = plocConfigReader.getNumberOfRandomRuns();
    	this.createConfigs();
    }
       
    private void createConfigs() {
    	Config config = new Config();
    	MatsimConfigReader configReader = new MatsimConfigReader(config);
    	configReader.readFile(path + "../input/PLOC/configs/config.xml");   	
    	    	
    	for (int i = 0; i < numberOfRandomRuns; i++) {
    		config.setParam("plans", "inputPlansFile", path + "../input/PLOC/plans/" + i + "_plans.xml");
        	config.setParam("controler", "runId", "run_" + String.valueOf(i));
        	String outputPath = path + "../output/PLOC/runs/";
        	new File(outputPath).mkdir();
        	config.setParam("controler", "outputDirectory", outputPath + i);
        	ConfigWriter configWriter = new ConfigWriter(config);
        	configWriter.write(path + "../input/PLOC/configs/" + i + "_config.xml");
    	}  	
    }
    
    public void run() {
    	
    	this.init();
    	    	    	   	
    	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
    		String config [] = {"src/main/java/playground/anhorni/input/PLOC/configs/" + runIndex + "_config.xml"};
    		
    		MixedControler controler = new MixedControler(config);
    		controler.setOverwriteFiles(true);
        	controler.run();
    	}	    	    	
    	log.info("Create anylsis ...");
    	
    	log.info("All runs finished ******************************");
    }
}
