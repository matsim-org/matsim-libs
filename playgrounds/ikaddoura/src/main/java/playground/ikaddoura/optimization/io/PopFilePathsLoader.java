/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.optimization.io;

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
public class PopFilePathsLoader {

	private static final Logger log = Logger.getLogger(PopFilePathsLoader.class);
	private String popPathsFile;
	private Map<Integer, String> runNr2popPathsFile = new HashMap<Integer, String>();

	public PopFilePathsLoader(String popPathsFile) {
		this.popPathsFile = popPathsFile;
		log.info("Population paths file set to " + this.popPathsFile);
		
		loadRndSeeds();
	}

	private void loadRndSeeds() {
		log.info("Loading population paths from file...");
		
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(this.popPathsFile)));
	            int runNr = 1;
	            String line = null;
	            while((line = br.readLine()) != null) {
	            	this.runNr2popPathsFile.put(runNr, line);
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
	        log.info("Loading population paths from file... Done.");
	}

	public String getPopulationFile(int nr) {
		return this.runNr2popPathsFile.get(nr);
	}
}
