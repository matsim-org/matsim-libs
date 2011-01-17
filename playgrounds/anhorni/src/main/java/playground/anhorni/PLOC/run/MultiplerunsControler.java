/* *********************************************************************** *
 * project: org.matsim.*
 * MultiplerunsControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.anhorni.PLOC.run;

import org.apache.log4j.Logger;
import playground.anhorni.LEGO.miniscenario.run.MixedControler;
import playground.anhorni.PLOC.PLOCConfigReader;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private int numberOfRandomRuns = -1; // from PLOCConfigReader
	private PLOCConfigReader plocConfigReader = new PLOCConfigReader();
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.plocConfigReader.read();
    	this.numberOfRandomRuns = plocConfigReader.getNumberOfRandomRuns();
    }
        
    public void run() {
    	
    	this.init();
    	    	    	   	
    	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
    		String config [] = {"src/main/java/playground/anhorni/input/PLOC/run" + runIndex + "/config.xml"};
    		
    		MixedControler controler = new MixedControler(config);
    		controler.setOverwriteFiles(true);
        	controler.run();
    	}	    	    	
    	log.info("Create anylsis ...");
    	
    	log.info("All runs finished ******************************");
    }
}
