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

package playground.dziemke.demand;

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


public class PendlerMatrixReader {
	
	private static final Logger log = Logger.getLogger(PendlerMatrixReader.class);
		
	private String shapeFile;
	private String commuterFileIn;
	private String commuterFileOut;	
	private double scalingFactor;
	private double carMarketShare;
	private double fullyEmployedShare;
	
	private List <CommuterRelation> commuterRelations = new ArrayList <CommuterRelation>();
	private Map <Integer, String> kreise = new HashMap <Integer, String>();
			
	
	public PendlerMatrixReader(String shapeFile, String commuterFileIn, String commuterFileOut, double scalingFactor,
			double carMarketShare, double fullyEmployedShare) {
		this.shapeFile = shapeFile;
		this.commuterFileIn = commuterFileIn;
		this.commuterFileOut = commuterFileOut;		
		this.scalingFactor = scalingFactor;
		this.carMarketShare = carMarketShare;
		this.fullyEmployedShare = fullyEmployedShare;
		readShape();
		readMatrix(commuterFileIn);
		readMatrix(commuterFileOut);
	}
	
	
	private void readShape() {
		Collection<SimpleFeature> municipalities = ShapeFileReader.getAllFeatures(this.shapeFile);
		for (SimpleFeature municipality : municipalities) {
			Integer gemeindeschluessel = Integer.parseInt((String) municipality.getAttribute("NR"));
			String name = (String) municipality.getAttribute("NAME");
			kreise.put(gemeindeschluessel, name);			
		}
	}
	
	
	private void readMatrix(final String filename) {
		
		Logger.getLogger(this.getClass()).warn("this method may read double entries in the Pendlermatrix (such as Nuernberg) twice. " +
				"If this may be a problem, you need to check.  kai, apr'11" ) ;
		
		System.out.println("======================" + "\n"
						   + "Start reading " + filename + "\n"
						   + "======================" + "\n");
		
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","});
        new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {

            @Override
            public void startRow(String[] row) {
                if (row[0].startsWith("#")) {
                    return;
                }
                Integer quelle = null ;
                Integer ziel = 0;
                // car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
                // double carMarketShare = 0.67;
                // scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
                // double scaleFactor = 1.29;

                if (filename.equals(commuterFileIn)){
                    try {
                        quelle = Integer.parseInt(row[2]);
                        
                        // as of now, the following value has to be manually adapted to the zone under consideration
                        // muenchen: 9162; berlin: 11000 (landkreisebene), 11000000 (gemeindeebene)
                        ziel = 11000000;
                        
                        int trips = Integer.parseInt(row[4]);
                        int scaledTrips = scale(trips);
                        
                        // int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
                        // int workCar = (int) (carMarketShare * totalTrips);
                        String label = row[3] ;
                        if ( !label.contains("brige ") && !quelle.equals(ziel)) {
                        	// quelle may not be equal to ziel because internal commuter traffic would be read in twice otherwise
                        	// now call method createHouseholdAndPerson
                            process(quelle, ziel, scaledTrips);
                        } else {
                            System.out.println( " uebrige? : " + label ) ;
                        }
                    } catch ( Exception ee ) {
                        System.err.println("we are trying to read quelle: " + quelle ) ;
                        //						System.exit(-1) ;
                    }
                }
                else if (filename.equals(commuterFileOut)){
                    try {
                    	// as of now, the following value has to be manually adapted to the zone under consideration
                        // muenchen: 9162; berlin: 11000 (landkreisebene), 11000000 (gemeindeebene)
                    	quelle = 11000000;
                        
                        ziel = Integer.parseInt(row[2]);

                        int trips = Integer.parseInt(row[4]);
                        int scaledTrips = scale(trips);
                        
                        // int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
                        // int workCar = (int) (carMarketShare * totalTrips);
                        String label = row[3] ;
                        if ( !label.contains("brige ")) {
                        	// !quelle.equals(ziel)
                        	// now call method createHouseholdAndPerson
                        	process(quelle, ziel, scaledTrips);
                        } else {
                            System.out.println( " uebrige? : " + label ) ;
                        }
                    } catch ( Exception ee ) {
                        System.err.println("we are trying to read quelle: " + quelle ) ;
                        //						System.exit(-1) ;
                    }
                }
                else{
                    System.err.println("ATTENTION: check filename!") ;
                }
            }
        });
        System.out.println("Pendlerdateien vollständig eingelesen.");
	}
	
	
	private void process(int quelle, int ziel, int trips) {
		String source = this.kreise.get(quelle);
		String sink = this.kreise.get(ziel);
		
		if (source == null) {
			log.error("Unknown source: " + quelle);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + ziel);
			return;
		}
				
//		int scaledCarQuantity = scale(workCar);
				
		if (trips != 0) {
			log.info(quelle + "->" + ziel + ": " + trips + " car trips");
			CommuterRelation commuterRelation = new CommuterRelation(quelle, source, ziel, sink, trips);
			this.commuterRelations.add(commuterRelation);
		}
	}
	
	
//	private int scaleOld(int quantity) {
//		double scalingFactor = this.scalingFactor;
//		int scaled = (int) (quantity * scalingFactor );
//		return scaled;
//	}
	
	
	private int scale(int trips) {
		double scalingFactor = this.scalingFactor;
		double carMarketShare = this.carMarketShare;
		double fullyEmployedShare = this.fullyEmployedShare;
		int scaledTrips = (int) (trips * scalingFactor * carMarketShare * fullyEmployedShare);
		return scaledTrips;
	}

		
	public List <CommuterRelation> getCommuterRelations() {
		return this.commuterRelations;
	}
}
