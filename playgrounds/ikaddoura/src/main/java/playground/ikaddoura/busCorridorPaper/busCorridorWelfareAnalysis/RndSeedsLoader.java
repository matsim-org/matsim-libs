/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author ikaddoura
 *
 */
public class RndSeedsLoader {
	
	private static final Logger log = Logger.getLogger(OptSettingsReader.class);
	private String rndSeedsFile;
	private Map<Integer, Integer> runNr2rndSeed = new HashMap<Integer, Integer>();

	public RndSeedsLoader(String rndSeedsFile) {
		this.rndSeedsFile = rndSeedsFile;
		log.info("RndSeedsFile set to " + this.rndSeedsFile);
		
		loadRndSeeds();
	}

	private void loadRndSeeds() {
		log.info("Loading rndSeeds from file...");
		
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(this.rndSeedsFile)));
	            int runNr = 1;
	            String line = null;
	            while((line = br.readLine()) != null) {
	            	this.runNr2rndSeed.put(runNr, Integer.parseInt(line));
	            	runNr++;
	            }
	        } catch(FileNotFoundException e) {
	            e.printStackTrace();
	        } catch(IOException e) {
	            e.printStackTrace();
	        } finally {
	            if(br != null) {
	                try {
	                    br.close();
	                } catch(IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        log.info("Loading rndSeeds from file... Done.");
	}

	public long getRandomSeed(int rndSeedNr) {
		return this.runNr2rndSeed.get(rndSeedNr);
	}

}
