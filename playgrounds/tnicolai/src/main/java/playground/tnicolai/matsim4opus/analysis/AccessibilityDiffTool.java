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
package playground.tnicolai.matsim4opus.analysis;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * @author thomas
 *
 */
public class AccessibilityDiffTool {
	
	private static final Logger log = Logger.getLogger(AccessibilityDiffTool.class);
	
	public static void main(String args[]){
		
		String policyAccessibilityGridFile = args[0];
		String baseCaseAccessibilityGridFile = args[1];
		String outputFilename = args[2];
		
		SpatialGrid policy = SpatialGrid.readFromFile(policyAccessibilityGridFile);
		SpatialGrid basecase = SpatialGrid.readFromFile(baseCaseAccessibilityGridFile);
		SpatialGrid diff;
		
		// check if grid files have equal size
		if(! isSameSize(policy, basecase) ){
			log.error("Given gird fiels have different sizes!");
			System.exit(0);
		}
		
		// init diff
		diff = new SpatialGrid(basecase);
		
		double xmin = basecase.getXmin();
		double xmax = basecase.getXmax();
		double ymin = basecase.getYmin();
		double ymax = basecase.getYmax();
		double resolution = basecase.getResolution();
		
		
		for (double y = ymin; y <= ymax; y += resolution){
			for (double x = xmin; x <= xmax; x += resolution){
				
				double pValue = policy.getValue(x, y);
				double bValue= basecase.getValue(x, y);
				double diffValue;
				//calculate difference only in the zurich area
				if(!Double.isNaN(pValue) && !Double.isNaN(bValue))
					diffValue = pValue - bValue;
				else
					diffValue = Double.NaN;
				
				// set diff value
				diff.setValue(diffValue, x, y);
			}
		}
		
		diff.writeToFile(outputFilename);
		
		log.info("Done!");
	}

	/**
	 * @param policy
	 * @param basecase
	 */
	protected static boolean isSameSize(SpatialGrid policy, SpatialGrid basecase) {
		return (policy.getNumRows() == basecase.getNumRows() && 
			    policy.getNumCols(0) == basecase.getNumCols(0) );
	}

}
