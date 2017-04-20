/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.analysis;

import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.analysis.activity.ActivityAnalyser;
import playground.southafrica.utilities.Header;

/**
 * This class is an alternative implementation of {@link ActivityAnalyser}, 
 * specifically to run the analysis of determining the percentage of activities
 * with facility Ids. This implementation, though, automates the analysis for
 * different clustering configurations, as needed for the final project of
 * Sumarie Meintjes.
 *  
 * @author jwjoubert
 */
@Deprecated
public class CheckActivityPercentages {
	final private static Logger LOG = Logger.getLogger(CheckActivityPercentages.class); 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CheckActivityPercentages.class.toString(), args);
		
		String sourceFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);
		
		double[] radii = {1, 5, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {1, 5, 10, 15, 20, 25};
		
		ActivityAnalyser aa = new ActivityAnalyser(numberOfThreads);
		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				LOG.info("================================================================================");
				LOG.info("Performing percentage-facility-id analysis for radius " + thisRadius + ", and pmin of " + thisPmin);
				LOG.info("================================================================================");
				/* Set configuration-specific filenames */
				String vehicleFolder = String.format("%s%.0f_%d/xml2/", sourceFolder, thisRadius, thisPmin);
				String outputFile = String.format("%s%.0f_%d/%.0f_%d_percentageActivities.csv", sourceFolder, thisRadius, thisPmin, thisRadius, thisPmin);
				
				/* Execute the analysis. Analysis '2' is the one calculating the
				 * percentage of activities with a facility Id. */
				aa.analyse(2, vehicleFolder, null, outputFile);
			}
		}
		
		Header.printFooter();
	}

}
