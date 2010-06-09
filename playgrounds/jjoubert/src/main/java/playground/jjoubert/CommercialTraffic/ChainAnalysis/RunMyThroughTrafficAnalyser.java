/* *********************************************************************** *
 * project: org.matsim.*
 * RunMyThroughTrafficAnalyser.java
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

package playground.jjoubert.CommercialTraffic.ChainAnalysis;

import java.util.List;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.jjoubert.CommercialTraffic.CommercialVehicle;
import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;

public class RunMyThroughTrafficAnalyser {
	private static Logger log = Logger.getLogger(RunMyThroughTrafficAnalyser.class);
	
	 /*=============================================================================
	 * String value indicating where the root where job is executed. 				|
	 * 		- Mac																	|
	 * 		- IE-Calvin														  		|
	 *=============================================================================*/
	private static String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; 	// Mac
//	private static String root = "/home/jwjoubert/MATSim/MATSimData/";					// IE-Calvin

	 /*=============================================================================
	 * String value that must be set. Allowed study areas are:						|
	 * 		- SouthAfrica															|
	 * 		- Gauteng																|
	 * 		- KZN																	|
	 * 		- WesternCape															|
	 *=============================================================================*/
	private static String studyAreaName = "Gauteng";

	/*==============================================================================
	 * The year for which the DigiCore analysis is being done. Available years are:	|
	 * 		- 2008																	|
	 *=============================================================================*/
	private static int year = 2008;

	 /*=============================================================================
	 * Version of ActivityAnalysis that should be used.								|
	 *=============================================================================*/
	private static String version = "20091202131951";

	 /*=============================================================================
	 * Integer indicating for which minor/major threshold the KDE image should be 	|
	 * calculated.																	|
	 *=============================================================================*/
	private static int threshold = 300;

	 /*=============================================================================
	 * Integer indicating for which sample the KDE image should be 	calculated.		|
	 *=============================================================================*/
	private static int sample = 1;
	
	private static double withinThreshold = 0.6;

	public static void main(String[] args) {
		/*
		 * Read study area.
		 */
		String studyAreaShapefile = String.format("%sShapefiles/%s/%s_UTM35S.shp",
				root, studyAreaName, studyAreaName);
		MyShapefileReader msr1 = new MyShapefileReader(studyAreaShapefile);
		MultiPolygon studyArea = msr1.readMultiPolygon();
		
		/*
		 * Read entry points.
		 */
		String entryPointsShapefile = String.format("%sShapefiles/%s/%sEntries_UTM35S.shp",
				root, studyAreaName, studyAreaName);
		MyShapefileReader msr2 = new MyShapefileReader(entryPointsShapefile);
		List<Point> entryPoints = msr2.readPoints();
		
		MyThroughTrafficAnalyser mtta = new MyThroughTrafficAnalyser(studyArea, entryPoints);
		
		/*
		 * Identify through-vehicles.
		 */
		String theVehicle = "211.xml";
		String vehicleXML = String.format("%sDigiCore/XML/%d/%s/%04d/Sample%02d/%s",
				root, year, version, threshold, sample, theVehicle
			);
		CommercialVehicle cv = null;
		MyXmlConverter xc = new MyXmlConverter(true);
		Object o = xc.readObjectFromFile(vehicleXML);
		if(o instanceof CommercialVehicle){
			cv = (CommercialVehicle) o;
		}
		if(cv.getFractionMinorInStudyArea() > 0 && cv.getFractionMinorInStudyArea() < withinThreshold){
			// It is a through-traffic vehicle.
			mtta.processVehicle(cv);
		}
		
		String location = String.format("%s%s/%d/%s/%04d/Sample%02d/%s_%03.0fp_", 
				root, studyAreaName, year, version, threshold, sample, studyAreaName, withinThreshold*100);
		mtta.writeListsToFile(location);
		
		log.info("----------------------------------------");
		log.info("           Process completed.");
		log.info("========================================");
		}
}
