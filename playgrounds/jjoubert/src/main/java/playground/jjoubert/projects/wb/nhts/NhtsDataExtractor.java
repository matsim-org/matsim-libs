/* *********************************************************************** *
 * project: org.matsim.*
 * NhtsDataExtractor.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb.nhts;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to parse the National Household Travel Survey (NHTS) data into a useful
 * format so that one can use it in a variety of ways.
 * 
 * @author jwjoubert
 */
public class NhtsDataExtractor {
	final private static Logger LOG = Logger.getLogger(NhtsDataExtractor.class);
	private Scenario sc;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NhtsDataExtractor.class.toString(), args);
		
		String personFile = args[0];
		String householdFile = args[1];
		
		NhtsDataExtractor nde = new NhtsDataExtractor();
		nde.parsePersons(personFile);
		
		Header.printFooter();
	}
	
	public NhtsDataExtractor() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void parsePersons(String filename){
		LOG.info("Parsing persons from " + filename);
		BufferedReader br = IOUtils.getBufferedReader(filename);
		Counter lineCounter = new Counter("  person # ");
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				
				lineCounter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		lineCounter.printCounter();
		
		LOG.info("Done parsing persons.");
	}
	
}
