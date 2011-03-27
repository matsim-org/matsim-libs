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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.xml.sax.SAXException;

import playground.anhorni.PLOC.analysis.SummaryWriter;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private static String path = "src/main/java/playground/anhorni/";
	private int numberOfRandomRuns = -1; // from config
	private SummaryWriter summaryWriter = null;
	private ConfigReader configReader = new ConfigReader();
	private ObjectAttributes personAttributes = new ObjectAttributes();
	
	public static int shoppingFacilities[] = {1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
	
    public static void main (final String[] args) { 
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	runControler.run();
    }
    
    private void init() {
    	this.configReader.read();
    	this.numberOfRandomRuns = configReader.getNumberOfRandomRuns();
    	this.summaryWriter = new SummaryWriter(path, this.numberOfRandomRuns);
    	
    	ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personAttributes);
		try {
			attributesReader.parse("src/main/java/playground/anhorni/input/PLOC/3towns/personExpenditures.xml");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	        	controler.run();
    		}
    	}
    	log.info("Create analysis ...");   	
    	summaryWriter.run();   	
    	log.info("All runs finished ******************************");
    }
}
