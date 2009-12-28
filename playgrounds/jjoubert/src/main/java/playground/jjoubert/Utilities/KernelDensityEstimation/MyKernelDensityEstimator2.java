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

import playground.jjoubert.Utilities.MyShapefileReader;

import com.vividsolutions.jts.geom.MultiPolygon;


public class MyKernelDensityEstimator2 {

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
//		DateString ds = new DateString();
		
		String mainAreaShapefileName = root + "Shapefiles/" + mainAreaName + "/" + mainAreaName + "_UTM35S.shp";
		MyShapefileReader msr = new MyShapefileReader(mainAreaShapefileName);
		MultiPolygon studyArea = msr.readMultiPolygon();
		String activityFilename = root + studyAreaName + "/Activities/" + studyAreaName + "MajorLocations.txt";

//		MyGrid grid = new MyGrid(studyArea, 1000, 1000, 24);
		MyGrid2 grid = new MyGrid2(studyArea, 500, 500, 24, activityFilename);
		grid.readPoints();
		grid.processCells(root + studyAreaName + "/Activities/" + studyAreaName + "KDE.txt", 5000);

//		QuadTree<ActivityPoint> qt = grid.getPointTree();
				
		log.info("Completed.");		
	}

}
