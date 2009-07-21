/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToGAP.java
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

package playground.jjoubert.CommercialModel.Postprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.AnalyseGAPDensity;
import playground.jjoubert.CommercialTraffic.SAZone;
import playground.jjoubert.Utilities.DateString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class EventsToGAP {

	// String value that must be set
	final static String PROVINCE = "Gauteng";
	final static int GAP_ID_INDEX = 1;
	/*
	 * The following GAP_ID indices should be used:
	 * 		Gauteng: 		1
	 * 		KZN: 			2
	 * 		WesternCape: 	2
	 */
	
	// Mac
	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "/home/jjoubert/";
	// Derived string values
	final static String GAP_SHAPEFILE = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "GAP_UTM35S.shp";
	final static String SHAPEFILE = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "_UTM35S.shp";
	final static String INPUT = ROOT + "Commercial/PostProcess/Run06/" + "100.eventsTruckMinor.txt";
	final static String OUTPUT_PRE = ROOT + "Commercial/PostProcess/Run06/" + "SimulatedCommercialMinorGAP_Normalized_";
	final static String OUTPUT_POST = ".txt";

	public static final String DELIMITER = ",";
	final static int GAP_SEARCH_AREA = 20000; // in METERS
	

	public static void main( String args[] ) {
		System.out.println("==========================================================================================");
		System.out.println("   Converting " + PROVINCE + " MATSim simulation events ('minor') to GAP densities" );
		System.out.println();
		
		DateString date = new DateString();
		date.setTimeInMillis(System.currentTimeMillis());
		String now = date.toString();
		String output = OUTPUT_PRE + now + OUTPUT_POST;
		
		ArrayList<SAZone> zoneList = AnalyseGAPDensity.readGAPShapeFile( GAP_SHAPEFILE, GAP_ID_INDEX );
	
		QuadTree<SAZone> zoneTree = AnalyseGAPDensity.buildQuadTree( zoneList, SHAPEFILE, PROVINCE );

		assignActivityToZone(zoneList, zoneTree );
		
		ArrayList<ArrayList<Double>> normalizedList = normalizeZoneCounts( zoneList );
		
		writeZoneStatsToFile( normalizedList, output );
	}
	
			
	private static ArrayList<ArrayList<Double>> normalizeZoneCounts(ArrayList<SAZone> zoneList) {
		ArrayList<ArrayList<Double>> allZones = new ArrayList<ArrayList<Double>>();
		
		System.out.print("Normalizing the minor activity counts... ");
		double maxActivity = Double.NEGATIVE_INFINITY;
		for (SAZone zone : zoneList) {
			for (int i = 0; i < zone.getTimeBins(); i++) {
				maxActivity = Math.max(maxActivity, zone.getMinorActivityCountDetail(i));
			}
		}
		
		ArrayList<Double> thisZone = null;
		for (SAZone zone : zoneList) {
			thisZone = new ArrayList<Double>();
			/*
			 * Add GAP_ID
			 */
			thisZone.add(Double.parseDouble(zone.getName()));
			
			for (int i = 0; i < zone.getTimeBins(); i++) {
				double dummy = (double) zone.getMinorActivityCountDetail(i);
				thisZone.add((dummy / maxActivity)*100);
			}
			allZones.add(thisZone);
		}		
		System.out.printf("Done.\n\n");
		return allZones;
	}


	private static void assignActivityToZone(ArrayList<SAZone> list, QuadTree<SAZone> tree ){
		System.out.println("Assigning activity locations to GAP mesozones.");

		GeometryFactory gf = new GeometryFactory();
		int events = 0;
		int eventsOut = 0;
		int eventProgress = 1;
		
		try { // Minor activities
			Scanner inputMinor = new Scanner(new BufferedReader(new FileReader(new File( INPUT ) ) ) );
			@SuppressWarnings("unused")
			String header = inputMinor.nextLine();

			try {
				while(inputMinor.hasNextLine() ){
					String[] thisLine = inputMinor.nextLine().split( DELIMITER );
					if( thisLine.length == 3 ){
						double x = Double.parseDouble( thisLine[0] );
						double y = Double.parseDouble( thisLine[1] );
						int timeOfDay = Integer.parseInt( thisLine[2] );

						Point thisActivity = gf.createPoint(new Coordinate(x, y) );

						ArrayList<SAZone> shortlist = (ArrayList<SAZone>) tree.get(x, y, GAP_SEARCH_AREA );
						SAZone minorZone = findZoneInArrayList(thisActivity, shortlist );
						if ( minorZone != null ){
							minorZone.incrementMinorActivityCountDetail( timeOfDay );
						} else{
							eventsOut++;
//							System.err.println("The event is not inside the study area");		
						}
					}
					// Report progress
					if( events == eventProgress){
						System.out.printf("     ...minor events %8d\n", events );
						eventProgress*=2;
					}
					events++;
				} 
			} finally {
				inputMinor.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.printf("     ...minor events %8d. Done (%d (%3.4f%%) was outside the study area)\n\n", events, eventsOut, ((float)eventsOut / (float)events)*100 );
	}

	private static SAZone findZoneInArrayList(Point p, ArrayList<SAZone> list ) {
	SAZone zone = null;
	int i = 0;
	while( (i < list.size() ) & (zone == null) ){
		SAZone thisZone = list.get(i);
		if( thisZone.contains( p ) ){
			zone = thisZone;				
		} else{
			i++;
		}
	}
	return zone;
}
	
	private static void writeZoneStatsToFile(ArrayList<ArrayList<Double>> allZones, String output) {
		System.out.print("Writing mesozone statistics to file... ");
		try{
			BufferedWriter outputMinor = new BufferedWriter(new FileWriter( new File ( output ) ) );
			
			String header = createHeaderString();
	
			// Write minor activities
			try{
				/*
				 * Write the output header.
				 */
				outputMinor.write( header );
				outputMinor.newLine();
				for (ArrayList<Double> thisZone : allZones) {
					String thisLine = new String();
					/*
					 * Convert the GAP_ID to integer, and add to output string.
					 */
					int gapID = (int) Math.floor(thisZone.get(0));
					thisLine += Integer.valueOf(gapID) + DELIMITER;	
					/*
					 * Add the double values for hours 0 through 22 to output string.
					 */
					for(int i = 1; i < thisZone.size()-1; i++ ){
						thisLine += thisZone.get(i).toString() + DELIMITER;
					}
					thisLine += thisZone.get(thisZone.size()-1);
					outputMinor.write( thisLine );
					outputMinor.newLine();
				}
			} finally{
				outputMinor.close();
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
		System.out.print("Done.\n\n");
	}
		
	/*
	 * Returns a standard header string for the normalized GAP statistics file:
	 * 		GAP_ID; and
	 * 		One column for each hour, starting with 0 and ending with 23. 
	 */
	public static String createHeaderString(){
		String headerString = "Name" + DELIMITER;
		for(int i = 0; i < 23; i++){
			headerString += "H" + i + DELIMITER;
		}
		headerString += "H23";
		
		return headerString;
	}
	
}
