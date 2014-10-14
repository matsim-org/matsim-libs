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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

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
	private double carShareBB;
	private String commuterFileOut;
	private double carShareBE;
	private double factor;
		
	private List <CommuterRelation> commuterRelations = new ArrayList <CommuterRelation>();
	private Map <Integer, String> municipalitiesMap = new HashMap <Integer, String>();
			
	
	// constructor
	public CommuterFileReader(String shapeFile, String commuterFileIn, double carShareBB, String commuterFileOut, 
			double carShareBE, double factor, String planningAreaId) {
		this.shapeFile = shapeFile;
		this.commuterFileIn = commuterFileIn;
		this.carShareBB = carShareBB;
		this.commuterFileOut = commuterFileOut;
		this.carShareBE = carShareBE;
		this.factor = factor;
		//readShape();
		// NR = Gemeindeschluessel
		TwoAttributeShapeReader.readShape(this.shapeFile, municipalitiesMap, "NR", "NAME");
		readFile(commuterFileIn, planningAreaId);
		readFile(commuterFileOut, planningAreaId);
	}
	
	
	// read in municipality shapefile and store keys and names to a map
//	private void readShape() {
//		Collection<SimpleFeature> municipalities = ShapeFileReader.getAllFeatures(this.shapeFile);
//		for (SimpleFeature municipality : municipalities) {
//			// municipalityKey = Gemeindeschluessel
//			Integer municipalityKey = Integer.parseInt((String) municipality.getAttribute("NR"));
//			String name = (String) municipality.getAttribute("NAME");
//			municipalitiesMap.put(municipalityKey, name);			
//		}
//	}
	
		
	// read in the commuter file and extract and store relevant information
	private void readFile(final String filename, final String planningAreaId) {
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
        			if (row[0].equals(planningAreaId)) {
	            		considerRow = true;
	            		return;
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
	                
	                // commuterFileIn = "B2009Ge.txt"
	                if (filename.equals(commuterFileIn)){
	                    try {
	                        origin = Integer.parseInt(row[2]);
	                        // Berlin: 11000 (landkreisebene), 11000000 (gemeindeebene)
	                        destination = 11000000;
	                        int trips = Integer.parseInt(row[4]);
	                        int scaledTrips = scale(trips, carShareBB);
	                        String label = row[3] ;
	                        //log.info("Label = " + row[3] + " - Quelle = " + row[2] + " - Ziel = " + destination + " - Trips = " + row[4]);
	                        
	                        if (!origin.equals(destination)) {
	                        	if (!label.contains("brige ")) {
	                        		process(origin, destination, scaledTrips);
	                        	} else {
	                        		//log.info("Other municipalities of " + label + " with " + trips + " are not considered.");
	                        	}
	                        } else {
	                        	//log.info("Ignoring this entry since interior traffic will otherweise be counted twice.");
	                        }
	                    } catch (Exception e) {
	                    	log.error("Failure while reading the origin: " + origin);
	                    }
	                }
	                
	                // commuterFileOut = "B2009Ga.txt"
	                else if (filename.equals(commuterFileOut)){
	                    try {
	                    	// Berlin: 11000 (landkreisebene), 11000000 (gemeindeebene)
	                        origin = 11000000;
	                        destination = Integer.parseInt(row[2]);
	                        int trips = Integer.parseInt(row[4]);
	                        int scaledTrips = scale(trips, carShareBE);
	                        String label = row[3] ;
	                        //log.info("Label = " + row[3] + " - Quelle = " + origin + " - Ziel = " + row[2] + " - Trips = " + row[4]);
	                        
	                        if (!label.contains("brige ")) {
	                        	process(origin, destination, scaledTrips);
	                        } else {
	                            //log.info("Other municipalities of " + label + " with " + trips + " are not considered.");
	                        }
	                    } catch (Exception e) {
	                    	log.error("Failure while reading the origin: " + origin);
	                    }
	                }
	                else{
	                	log.error("ATTENTION: Check filename!") ;
	                }
        		} else {
        			log.info("Row array has less than two fields. Therefore ignore row.");
        			return;
        		}
            }
        });
        log.info("Finished reading in commuter file.");
	}
	
	
	// this method returns null if the origin or the destination municipality is not contained in the list of municipalities
	// otherwise they are included in the CommuterRelations map
	// thus counties (Kreise) and municipalities outside of the region to be considered are left out
	// considering counties would constitute doublecounting since commuter streams from/to municipalities inside the counties
	// are already considered
	private void process(int origin, int destination, int trips) {
		String source = this.municipalitiesMap.get(origin);
		String sink = this.municipalitiesMap.get(destination);
		
		if (source == null) {
			log.error("Unknown source: " + origin);
			//log.info("Unknown source: " + origin);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + destination);
			//log.info("Unknown sink: " + destination);
			return;
		}

		if (trips != 0) {
			log.info(origin + "->" + destination + ": " + trips + " car trips");
			CommuterRelation commuterRelation = new CommuterRelation(origin, source, destination, sink, trips);
			this.commuterRelations.add(commuterRelation);
		}
	}
	

	// scales trips according to input parameters
	private int scale(int trips, double carShare) {
		double factor = this.factor;
		double value = trips * factor * carShare;
		// int scaledTrips = (int) (trips * scalingFactor * carMarketShare * fullyEmployedShare);
		int scaledTrips = (int) value;
		//###################################################################################################
		if (scaledTrips == 0) {
//			Random r = new Random();
//			double randomNumber = r.nextDouble();
			// with a probability of 50% raise number of scaled trips by one
			// this is because the cast "(int)" always cuts of the decimal number
			// which may lead to skewed results particularly for weak commuter relations
//			if (randomNumber <= 1) {
				scaledTrips = scaledTrips + 1;
//			}
		}
		//###################################################################################################
		return scaledTrips;
	}

	
	// get method
	public List <CommuterRelation> getCommuterRelations() {
		return this.commuterRelations;
	}
}
