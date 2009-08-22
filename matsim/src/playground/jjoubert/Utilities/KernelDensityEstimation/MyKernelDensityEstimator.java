/* *********************************************************************** *
 * project: org.matsim.*
 * MyGridBuilder.java
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

package playground.jjoubert.Utilities.KernelDensityEstimation;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.MultiPolygon;

import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.MyKdeWriter;
import playground.jjoubert.Utilities.MyShapefileReader;


public class MyKernelDensityEstimator {

	/* 
	 * String value that must be set. Allowed area names are:
	 * 		- SouthAfrica
	 * 		- Gauteng
	 * 		- KZN
	 * 		- WesternCape
	 */
	static String mainAreaName = "SouthAfrica";
	static String studyAreaName = "Gauteng";

	// Set the home directory, depending on where the job is executed.
//	static String root = "~/MATSim/workspace/MATSimData/"; // Mac
//	static String root = "~/";									// IVT-Sim0
	static String root = "~/data/";								// Satawal

	private final static Logger log = Logger.getLogger(MyGrid.class);

	public static void main(String args[]){
		log.info("=======================================================================");
		log.info("  Performing a kernel density estimation on " + mainAreaName);
		log.info("  using the activities from " + studyAreaName);
		log.info("=======================================================================");
		log.info(null);
		DateString ds = new DateString();
		
		String mainAreaShapefileName = root + "Shapefiles/" + mainAreaName + "/" + mainAreaName + "_UTM35S.shp";
		MyShapefileReader msr = new MyShapefileReader(mainAreaShapefileName);
		MultiPolygon studyArea = msr.readMultiPolygon();
		MyGrid grid = new MyGrid(studyArea, 1000, 1000, 24);
		String activityFilename = root + studyAreaName + "/Activities/" + studyAreaName + "MinorLocations.txt";
		grid.estimateActivities(activityFilename, 5000);
		
		String kdeOutputFilename = root + studyAreaName + "/Activities/" + studyAreaName + "MinorKDE_" + ds.toString() + ".txt";
		log.info("Writing minor activities to " + kdeOutputFilename);
		MyKdeWriter kw = new MyKdeWriter();
		kw.writeKdeToFile(grid, kdeOutputFilename);
		
		log.info("Completed kernel density estimation.");		
	}

}
