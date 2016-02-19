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
	
	private static final double startTime = 4. * 3600.;
	private static final double timeBinSize = 3600.;
	private static final double endTime = 24. * 3600.;

	private static final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/analysis_it.100/";

	private static final String[] workingDirectories = { "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/analysis_it.100/immissions/"
			, "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/analysis_it.100/consideredAgentUnits/"
			, "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/analysis_it.100/damages_receiverPoint/"};
	private static final String[] labels = { "immission" , "consideredAgentUnits" , "damages_receiverPoint" };

	private static final String receiverPointsFile = "/Users/ihab/Documents/workspace/runs-svn/cn2/output/cn1/noiseAnalysisVia/analysis_it.100/receiverPoints/receiverPoints.csv";
	private static final String separator = ";";

	private static final OutputFormat outputFormat = OutputFormat.xyt ;

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
