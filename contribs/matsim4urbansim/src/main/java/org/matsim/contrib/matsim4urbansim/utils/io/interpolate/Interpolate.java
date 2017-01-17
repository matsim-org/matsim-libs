/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matsim4urbansim.utils.io.interpolate;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interpolation.Interpolation;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;

/**
 * @author thomas
 *
 */
public class Interpolate {
	
	// logger
	private static final Logger log = Logger.getLogger(Interpolate.class);
	
	private static String inputSpatialGrid;
	private static String outputSpatialGrid;
	private static double cellSizeInMeter;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args == null || args.length != 2){
			log.info("Enter the following parameter:");
			log.info("1) input table");
			log.info("2) resolution (as side length in merter)");
			log.info("Please try again.");
			System.exit(0);
		}
		
		inputSpatialGrid = args[0];
		outputSpatialGrid = args[0].replaceAll(".txt", "_interpolated"+args[1]+"m.txt");
		cellSizeInMeter  = Double.parseDouble(args[1]);

		log.info("Reading input file (spatial grid).");
		SpatialGrid inputSG = SpatialGrid.readFromFile(inputSpatialGrid);		
		
		log.info("Putting input file (spatial grid) into interpolation routine");
		Interpolation interpol = new Interpolation(inputSG, Interpolation.BILINEAR);
		
		log.info("Creating spatial grid for interpolated values.");
		BoundingBox boundary = BoundingBox.createBoundingBox(inputSG.getXmin(), inputSG.getYmin(), inputSG.getXmax(), inputSG.getYmax());
		SpatialGrid outputSG= new SpatialGrid(boundary.getBoundingBox(), cellSizeInMeter);
		
		for(double x = outputSG.getXmin(); x < outputSG.getXmax(); x = x+cellSizeInMeter){
			for(double y = outputSG.getYmin(); y < outputSG.getYmax(); y = y+cellSizeInMeter){

				if(inputSG.getValue(x, y) == Double.NaN)
					continue;
				
				double interpolatedValue = interpol.interpolate(x, y);
				outputSG.setValue(interpolatedValue, x, y);
			}
		}
		outputSG.writeToFile(outputSpatialGrid);
		log.info("Done...");
	}
}
