/* *********************************************************************** *
 * project: org.matsim.*
 * RunSpatialAveragingDemandEmissionsKrasnojarsk.java
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
package playground.benjamin.scenarios.krasnojarsk;

import java.io.IOException;

import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.utils.spatialAvg.SpatialAveragingDemandEmissions;
import playground.benjamin.utils.spatialAvg.SpatialAveragingInputData;

/**
 * @author benjamin
 *
 */
public class RunSpatialAveragingDemandEmissionsKrasnojarsk {

	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:32646");
	//private final double xMin = 458823.0;
	//private final double xMax = 544945.0;
	//private final double yMin = 6190162.0;
	//private final double yMax = 6248014.0;
	private final double xMin = 483895.0;
	private final double xMax = 502066.0;
	private final double yMin = 6202213.0;
	private final double yMax = 6212963.0;
	
	private final int numberOfXBins = 160;
	private final int numberOfYBins = 120;
	private final double smoothingRadius_m = 250.;
	
	private final int noOfTimeBins = 1;
	private final boolean writeRoutput = true;
	private final boolean writeGisOutput = false;
	private final boolean useBaseCaseOnly = true;
	private final String pollutant2analyze = WarmPollutant.NOX.toString();
	
	private final boolean useVisBoundary = false;
//	private final String visBoundaryShapeFile;
	private final double scalingFactor = 10.;
	
	
	// ***BASE CASE***
	private final String runNumber1 = "baseCase";
	
	private final String runDirectory1 = "../../runs-svn/krasnojarsk/bau/";
	private final String netFile = runDirectory1 + "network.xml";
	private final String configFile1 = runDirectory1 + "output_config.xml";
	private final int lastIteration1 = 400;
	private final String emissionFile1 = runDirectory1 + "emission.events.offline-10.xml.gz";
	
	private final String fileNameAnalysisBaseCase = runDirectory1 + "analysis/spatialAveraging/data/" + runNumber1 + "." + lastIteration1;
	
	
	// ***COMPARE CASE***
	// none so far...
	
//  ===
	private SpatialAveragingInputData inputData = new SpatialAveragingInputData();

	public static void main(String[] args) throws IOException {
		new RunSpatialAveragingDemandEmissionsKrasnojarsk().configure();
	}

	private void configure() throws IOException {
		this.inputData.setCRS(targetCRS);
		this.inputData.setMinX(xMin);
		this.inputData.setMaxX(xMax);
		this.inputData.setMinY(yMin);
		this.inputData.setMaxY(yMax);
		this.inputData.setNumberOfXBins(numberOfXBins);
		this.inputData.setNumberOfYBins(numberOfYBins);
		this.inputData.setSmoothingRadius_m(smoothingRadius_m);
		
		this.inputData.setNoOfTimeBins(noOfTimeBins);
		this.inputData.setWriteROutput(writeRoutput);
		this.inputData.setWriteGISOutput(writeGisOutput);
		this.inputData.setUseBaseCaseOnly(useBaseCaseOnly);
		this.inputData.setPollutant2Analyze(pollutant2analyze);
		
		this.inputData.setUseVisBoundary(useVisBoundary);
//		this.inputData.setVisBoundaryShapeFile(visBoundaryShapeFile);
		this.inputData.setScalingFactor(scalingFactor);
		
		this.inputData.setConfigFile1(configFile1);
		this.inputData.setNetFile(netFile);
		
		this.inputData.setEmissionFile1(emissionFile1);
//		this.inputData.setEmissionFile2(emissionFile2);
		
		this.inputData.setOutputNameBaseCase(fileNameAnalysisBaseCase);
//		this.inputData.setOutputNameCompareCase(fileNameAnalysisCompareCase);
		
		SpatialAveragingDemandEmissions sade = new SpatialAveragingDemandEmissions();
		sade.setInputData(inputData);
		sade.run();
	}
}