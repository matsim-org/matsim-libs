/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise;

import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;

/**
 * @author ikaddoura
 *
 */
public final class MergeNoiseDataFilesIK {
	
	private static final double startTime = 1. * 3600.;
	private static final double timeBinSize = 3600.;
	private static final double endTime = 30. * 3600.;

	private static final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct/output/r_output_run0_bln_bc/ITERS/it.300/immissions/";

	private static final String[] workingDirectories = { outputDirectory };
	private static final String[] labels = { "immission" };

	private static final String receiverPointsFile = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct/output/r_output_run0_bln_bc/receiverPoints/receiverPoints.csv";
	private static final String separator = ";";

	private static final OutputFormat outputFormat = OutputFormat.xyt1t2t3etc ;

	private static final double threshold = -1. ;

	public static void main(String[] args) {
		MergeNoiseCSVFile mergeNoiseFile = new MergeNoiseCSVFile();
		mergeNoiseFile.setStartTime(startTime);
		mergeNoiseFile.setTimeBinSize(timeBinSize);
		mergeNoiseFile.setEndTime(endTime);
		mergeNoiseFile.setOutputDirectory(outputDirectory);
		mergeNoiseFile.setWorkingDirectory(workingDirectories);
		mergeNoiseFile.setLabel(labels);
		mergeNoiseFile.setReceiverPointsFile(receiverPointsFile);
		mergeNoiseFile.setSeparator(separator);
		mergeNoiseFile.setOutputFormat(outputFormat);
		mergeNoiseFile.setThreshold(threshold);		
		mergeNoiseFile.run();
	}
}
