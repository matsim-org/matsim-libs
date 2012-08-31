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
		String outputDir = args[2];
		
		SpatialGrid policy = SpatialGrid.readFromFile(policyAccessibilityGridFile);
		SpatialGrid basecase = SpatialGrid.readFromFile(baseCaseAccessibilityGridFile);
		SpatialGrid diff;
		SpatialGrid percentage;
		
		// check if grid files have equal size
		if(! isSameSize(policy, basecase) ){
			log.error("Given gird fiels have different sizes!");
			System.exit(0);
		}
		
		// init diff
		diff = new SpatialGrid(basecase);
		percentage = new SpatialGrid(basecase);
		
		double xmin = basecase.getXmin();
		double xmax = basecase.getXmax();
		double ymin = basecase.getYmin();
		double ymax = basecase.getYmax();
		double resolution = basecase.getResolution();
		
		double minDiffValue = Double.MAX_VALUE;
		double maxDiffValue = Double.MIN_VALUE;
		double minPercentangeValue = Double.MAX_VALUE;
		double maxPercentangeValue = Double.MIN_VALUE;
		
		for (double y = ymin; y <= ymax; y += resolution){
			for (double x = xmin; x <= xmax; x += resolution){
				
				double pValue = policy.getValue(x, y);
				double bValue= basecase.getValue(x, y);
				double diffValue;
				double percentageValue;
				//calculate difference only in the zurich area
				if(!Double.isNaN(pValue) && !Double.isNaN(bValue)){
					// compute difference on values
					diffValue = pValue - bValue;
					minDiffValue = Math.min(minDiffValue, diffValue);
					maxDiffValue = Math.max(maxDiffValue, diffValue);
					
					// compute derivation in per cent
					percentageValue = ((pValue * 100.) / bValue) - 100.;

					minPercentangeValue = Math.min(minPercentangeValue, percentageValue);
					maxPercentangeValue = Math.max(maxPercentangeValue, percentageValue);
					// limit values at +/- 20% for visualization reasons
					if(percentageValue > 20)
						percentageValue = 20.;
					if(percentageValue < -20)
						percentageValue = -20.;
				}
				else{
					diffValue = Double.NaN;
					percentageValue = Double.NaN;
				}
				
				// set diff and percentage value
				diff.setValue(diffValue, x, y);
				percentage.setValue(percentageValue, x, y);
			}
		}
		
		diff.writeToFile(outputDir + "/diff.txt");
		percentage.writeToFile(outputDir + "/percentage.txt");
		
		log.info("Diff: Max value= " + maxDiffValue + " Min value= " + minDiffValue + " in diff grid-layer");
		log.info("Percentage: Max value= " + maxPercentangeValue + " Min value= " + minPercentangeValue + " in percentage grid-layer");
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
