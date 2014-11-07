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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.dziemke.utils.TwoAttributeShapeReader;


/**
 * @author dziemke
 * Based on "playground.vsp.demandde.pendlermatrix.PendlerMatrixReader.java"
 */
public class CommuterFileReader {
	private static final Logger log = Logger.getLogger(CommuterFileReader.class);
	
	// class variables
	private String shapeFile;
	private String commuterFileIn;
	private double carShareExterior;
	private String commuterFileOut;
	private double carShareInterior;
	private double factor;
		
	private List <CommuterRelation> commuterRelations = new ArrayList <CommuterRelation>();
	private Map <Integer, String> municipalitiesMap = new HashMap <Integer, String>();
			
	
	// constructor
	public CommuterFileReader(String shapeFile, String commuterFileIn, double carShareExterior, String commuterFileOut, 
			//double carShareBE, double factor, String planningAreaId) {
			double carShareInterior, double factor, int planningAreaId) {
		this.shapeFile = shapeFile;
		this.commuterFileIn = commuterFileIn;
		this.carShareExterior = carShareExterior;
		this.commuterFileOut = commuterFileOut;
		this.carShareInterior = carShareInterior;
		this.factor = factor;
		// NR = Gemeindeschluessel
		TwoAttributeShapeReader.readShape(this.shapeFile, municipalitiesMap, "NR", "NAME");
		readFile(commuterFileIn, planningAreaId);
		readFile(commuterFileOut, planningAreaId);
	}
	
		
	// read in the commuter file and extract and store relevant information
	private void readFile(final String filename, final Integer planningAreaId) {
	//private void readFile(final String filename, final String planningAreaId) {
		log.info("======================" + "\n"
						   + "Start reading " + filename + "\n"
						   + "======================" + "\n");
				
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterRegex("\t");
        new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
        	
        	boolean considerRow = false;

            // this method is called whenever a row has been; see "TabularFileHandler.java"
        	@Override
            public void startRow(String[] row) {
        		// if the string array has less than 2 entries it is no commuter relation line, so that the return command
        		// is called directly
        		if (row.length >= 2) {
	            	// switch on processing of row in first line after the planning area id is given in the first column
        			if (row[0].equals(planningAreaId.toString())) {
	            		considerRow = true;
	            		return;
        			} else {
        				log.info("Row does not start with planning area ID.");
	            	}
        			
        			// if considerRow is false do nothing and return
        			if (considerRow == false) {
        				log.info("ConsiderRow is set to false. Therefore ignore row.");
	                    return;
	                }
        			
        			// "16" is always the last entry. therefore, it seems okay to hard-code this number here
        			// once it is reached, set considerRow to false so that this is the last row to be considered
        			if (row[2].equals("16")) {
	            		considerRow = false;
	            	}
        			
	                Integer origin = null ;
	                Integer destination = 0;
	                	                
	                if (filename.equals(commuterFileIn)){ // commuterFileIn = "B2009Ge.txt"
	                    try {
	                        origin = Integer.parseInt(row[2]);
	                        destination = planningAreaId;
	                        int trips = Integer.parseInt(row[4]);
	                        // It is important that interior traffic is left out in this case (commuterFileIn), since the car share for
	                        // exterior is used here and this car share relates to people LIVING exterior of planning area.
	                        int scaledTrips = scale(trips, carShareExterior);
	                        String label = row[3];
	                        log.info("Label = " + row[3] + " - Quelle = " + row[2] + " - Ziel = " + destination + " - Trips = " + row[4]);
	                        
	                        if (!origin.equals(destination)) {
	                        	if (!label.contains("brige ")) {
	                        		process(origin, destination, scaledTrips);
	                        	} else {
	                        		log.info("Other municipalities of " + label + " with " + trips + " trips are not considered.");
	                        	}
	                        } else {
	                        	// IMPORTANT!!!
	                        	log.info("Ignoring this entry since interior traffic will otherweise be counted twice.");
	                        }
	                    } catch (Exception e) {
	                    	log.error("Failure while reading the origin: " + origin);
	                    }
	                }
	                	                
	                else if (filename.equals(commuterFileOut)){ // commuterFileOut = "B2009Ga.txt"
	                    try {
	                    	origin = planningAreaId;
	                        destination = Integer.parseInt(row[2]);
	                        int trips = Integer.parseInt(row[4]);
	                        int scaledTrips = scale(trips, carShareInterior);
	                        String label = row[3] ;
	                        log.info("Label = " + row[3] + " - Quelle = " + origin + " - Ziel = " + row[2] + " - Trips = " + row[4]);
	                        
	                        if (!label.contains("brige ")) {
	                        	process(origin, destination, scaledTrips);
	                        } else {
	                            log.info("Other municipalities of " + label + " with " + trips + " trips are not considered.");
	                        }
	                    } catch (Exception e) {
	                    	log.error("Failure while reading the origin: " + origin);
	                    }
	                }
	                else{
	                	log.error("ATTENTION: Check filename!") ;
	                }
        		} else {
        			log.info("Row array has less than two fields. This means it cannot be a commuter entry. Therefore, ignore row.");
        			return;
        		}
            }
        });
        log.info("Finished reading in commuter file.");
	}
	
	
	// This method returns null if the origin or the destination municipality is not contained in the list of municipalities
	// otherwise they are included in the CommuterRelationMap
	// Thus counties (Kreise) and municipalities outside of the region to be considered are left out.
	// Considering counties would constitute doublecounting since commuter streams from/to municipalities inside the counties
	// are already considered.
	private void process(int origin, int destination, int trips) {
		String source = this.municipalitiesMap.get(origin);
		String sink = this.municipalitiesMap.get(destination);
		
		if (source == null) {
			log.info("Source: " + origin + " unknown. Therefore not including its commuter relations.");
			return;
		}
		if (sink == null) {
			log.info("Sink: " + destination + " unknown. Therefore not including its commuter relations.");
			return;
		}

		log.info(origin + "->" + destination + ": " + trips + " car trips");
		CommuterRelation commuterRelation = new CommuterRelation(origin, source, destination, sink, trips);
		this.commuterRelations.add(commuterRelation);
	}
	

	// Scales trips according to input parameters
	private int scale(int trips, double carShare) {
		double factor = this.factor;
		double value = trips * factor * carShare;
		int scaledTrips = (int) value;
		if (scaledTrips == 0) {
//			Random r = new Random();
//			double randomNumber = r.nextDouble();
			
			// With a probability of 50% raise number of scaled trips by one. This is because the cast to int
			// always cuts of the decimal number which may lead to skewed results particularly for weak commuter
			// relations.
//			if (randomNumber <= 1) {
				scaledTrips = scaledTrips + 1;
//			}
		}
		return scaledTrips;
	}

	
	// Get method
	public List <CommuterRelation> getCommuterRelations() {
		return this.commuterRelations;
	}
}
