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

import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.anhorni.PLOC.analysis.SummaryWriter;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private static String path = "src/main/java/playground/anhorni/";
	private int numberOfRandomRuns = -1; // from config
	private SummaryWriter summaryWriter = null;
	private ConfigReader configReader = new ConfigReader();
	private ObjectAttributes personAttributes = new ObjectAttributes();
	
	public static int shoppingFacilities[] = {1, 2, 4, 5, 6, 7, 8, 9};
	public static double dayExpenditureFactor[] = {1.0, 1.0, 1.0, 1.0, 1.0};
	public static double share[] = {0.9, 0.9, 0.9, 0.8, 0.5};
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.configReader.read();
    	this.numberOfRandomRuns = configReader.getNumberOfRandomRuns();
    	this.summaryWriter = new SummaryWriter(path);
    	
    	ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personAttributes);
			attributesReader.parse("src/main/java/playground/anhorni/input/PLOC/3towns/personExpenditures.xml");
    }
           
    public void run() {
    	
    	this.init();
 	    	
    	String config[] = {""};
		SingleRunControler controler;
					    	   	
    	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {   		
    		for (int i = 0; i < 5; i++) {
    			config[0] = "src/main/java/playground/anhorni/input/PLOC/3towns/runs/run"+ runIndex + "/day" + i + "/config.xml";
	    		controler = new SingleRunControler(config);	 
	    		controler.setPersonAttributes(this.personAttributes);
	    		controler.setTempVar(configReader.isTemporalVar());
	    		controler.setDay(i);
	        	controler.run();
    		}
    	}
    	log.info("Create analysis ...");   	
    	summaryWriter.run(numberOfRandomRuns);   	
    	log.info("All runs finished ******************************");
    }
}
