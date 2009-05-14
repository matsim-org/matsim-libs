/* *********************************************************************** *
 * project: org.matsim.*
 * BKickHouseholdsCreator
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
package playground.benjamin.income;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.households.BasicHousehold;
import org.matsim.core.basic.v01.households.BasicHouseholdBuilder;
import org.matsim.core.basic.v01.households.BasicHouseholds;
import org.matsim.core.basic.v01.households.BasicIncome;
import org.matsim.core.basic.v01.households.HouseholdsWriterV1;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class BKickHouseholdsCreatorZurich {

	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
  
		/*
		 * This file has the following attributes:
		 * the_geom
     * GMDEQNR
     * NR
     * NAME
     * GMDE
     * FLAECHE_HA
		 */
		String quartiereZurichShapeFile = DgPaths.WORKBASE + "externedaten/Schweiz/Gemeindegrenzen/quartiergrenzen2006/quart06_shp_070824/quart06.shp";
		
		Map<String, Feature> featuresByName = new HashMap<String, Feature>();
		Map<String, Double> gemeindeIncome = new HashMap<String, Double>();
		
		FeatureSource fts = ShapeFileReader.readDataFile(quartiereZurichShapeFile);
		
		//Iterator to iterate over the features from the shape file
		Iterator<Feature> it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
			//of other attributes
			String ftname = (String) ft.getAttribute("NAME");
			System.out.println("Found feature for Gemeinde: " + ftname);
			featuresByName.put(ftname, ft);
		}
		
		//read the average income
		
		String einkommenZurichTextfile = DgPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/einkommenKantonZurichPlainDataEdited.txt";
		BufferedReader reader = IOUtils.getBufferedReader(einkommenZurichTextfile);
		String line = reader.readLine();
		//skip the header
		for (int i = 0; i < 6; i++) {
			line = reader.readLine();
		}
		
		while (line != null) {
//			System.out.println(line);
			String[] columns = line.split("\t");
//			int firstNumberIndex = 1;
//			Exception e = null;
//			do {
//				try {
//					e = null;
//					Double.parseDouble(columns[firstNumberIndex].replace(",", "."));
//				} catch(Exception ex) {
//					System.err.println(ex.getMessage());
//					e = ex;
//					firstNumberIndex++;
//				}
//			} while (e != null);
//			
			int incomeIndex = 7;
			gemeindeIncome.put(columns[0], Double.parseDouble(columns[incomeIndex].replace(",", ".")) * 1000);
//			System.out.println(columns[0] + " " + gemeindeIncome.get(columns[0]));
			line = reader.readLine();
		} 
		
		int gemeindeNotFound = 0;
		for (Entry entry : gemeindeIncome.entrySet()){
			System.out.println("Gemeinde: " + entry.getKey());
			Feature ft = featuresByName.get(entry.getKey());
			if (ft == null) {
				gemeindeNotFound++;
				System.err.println("Can not find feature for " + entry.getKey());
			}
		}
		System.err.println("Gemeinden nicht gefunden: " + gemeindeNotFound);
		System.err.println("Gemeinden mit Einkommen: "+ gemeindeIncome.size());
		
		//create the households
		BasicScenarioImpl sc = new BasicScenarioImpl();
    Id id1 = sc.createId("1");
    Id id2 = sc.createId("2");
    BasicHouseholds<BasicHousehold> hhs = sc.getHouseholds();
    BasicHouseholdBuilder b = hhs.getHouseholdBuilder();
    
    BasicHousehold hh = b.createHousehold(id1);
    hh.setIncome(b.createIncome(40000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id1);
    hhs.getHouseholds().put(id1, hh);
    
    hh = b.createHousehold(id2);
    hh.setIncome(b.createIncome(120000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id2);
    hhs.getHouseholds().put(id2, hh);
    
    
    //TODO uncomment hh.write file 
    HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(hhs);
//    hhwriter.writeFile("test/input/playground/benjamin/BKickScoringTest/households.xml");
    System.out.println("Households written!");
    
	}

}
