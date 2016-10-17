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

package playground.dziemke.cemdapMatsimCadyts;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author dziemke
 */
public class CommuterFileReaderV2 {
	private static final Logger LOG = Logger.getLogger(CommuterFileReaderV2.class);
	
	private List<CommuterRelationV2> commuterRelations = new ArrayList<>();
			
	
	public CommuterFileReaderV2(String commuterFileOutgoing, String delimiter) {
		readFile(commuterFileOutgoing, delimiter);
	}
	

	private void readFile(final String filename, String delimiter) {
		LOG.info("Start reading " + filename);
				
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterRegex(delimiter);
		new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
        	String origin = null;
        	String destination = null;

            @Override
            public void startRow(String[] row) {
            	if (row.length > 2) {
	            	try {
		            	if (row[0].length() == 8) {
		            		origin = row[0];
		            		LOG.info("New origin set to: " + origin);
		            		return;
		            	// Next check for destinations
		            	} else if (row[2].length() == 8) {
		            		destination = row[2];
		            		LOG.info("New destination set to: " + destination);
		            		LOG.info(origin + " -> " + destination + " -- Commuters: " + row[4] + " -- " + row[5] + " -- " + row[6]);
		            		int tripsMale;
		            		int tripsFemale;
		            		
		            		if (row[5].equals("X")) {
		            			tripsMale = -1; // TODO ...
		            		} else {
		            			tripsMale = Integer.parseInt(row[5]);
		            		}
		            		
		            		if (row[5].equals("X")) {
		            			tripsFemale = -1; // TODO ...
		            		} else {
		            			tripsFemale = Integer.parseInt(row[6]);
		            		}
		            		
		            		process(Integer.parseInt(origin), 
		            				Integer.parseInt(destination), 
		            				Integer.parseInt(row[4]), 
		            				tripsMale, 
		            				tripsFemale);
		            		return;
		            	} else {
		            		return;
		            	}
	            	} catch (Exception e) {
	            		System.err.println("Caught Exception: " + e.getMessage());
	                }
            	}
            };
		});
	}
	
	
	private void process(int origin, int destination, int tripsAll, int tripsMale, int tripsFemale) {
		CommuterRelationV2 commuterRelation = new CommuterRelationV2(origin, destination, tripsAll, tripsMale, tripsFemale);
		this.commuterRelations.add(commuterRelation);
	}
	

	public List <CommuterRelationV2> getCommuterRelations() {
		return this.commuterRelations;
	}
}