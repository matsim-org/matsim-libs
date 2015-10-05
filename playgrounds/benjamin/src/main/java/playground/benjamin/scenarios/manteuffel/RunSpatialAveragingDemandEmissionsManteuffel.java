/* *********************************************************************** *
 * project: org.matsim.*
 * RunSpatialAveragingDemandEmissionsManteuffel.java
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
package playground.benjamin.scenarios.manteuffel;

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
public class RunSpatialAveragingDemandEmissionsManteuffel {

	String inputPath = "../../../runs-svn/manteuffelstrasse/";
	
	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:31468");
//	private final double xMin = 4595288.82;
//	private final double xMax = 4598267.52;
//	private final double yMin = 5817859.97;
//	private final double yMax = 5820953.98;
	private final double xMin = 4595465.0;
	private final double xMax = 4598349.0;
	private final double yMin = 5819159.0;
	private final double yMax = 5820931.0;
	
	private final int numberOfXBins = 160;
	private final int numberOfYBins = 120;
	private final double smoothingRadius_m = 32.;
	
	private final int noOfTimeBins = 1;
	private boolean writeRoutput = true;
	private boolean writeGisOutput = false;
	private boolean useBaseCaseOnly = false;
	private final String pollutant2analyze = WarmPollutant.NOX.toString();
	
	private final boolean useVisBoundary = true;
	private final String visBoundaryShapeFile = inputPath + "bau/analysis/spatialAveraging/data/shapes/analysis-area.shp";
	private final double scalingFactor = 4.;
	
	
	// ***BASE CASE***
	private final String runNumber1 = "bau";
	
	private final String configFile1 = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_config.xml.gz";
	private final Integer lastIteration1 = 30;
	
	private final String netFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
//	private final String netFile = inputPath + "p3/bvg.run190.25pct.dilution001.network.20150731.LP2.III.emissionNetwork.xml.gz";
//	private final String netFile = inputPath + "p4/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.emissionNetwork.xml.gz";;
	
	private final String emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsDay.xml.gz";
//	private final String emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsMorning.xml.gz";
//	private final String emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsEvening.xml.gz";;
	
	private final String fileNameAnalysisBaseCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber1 + "." + lastIteration1;
	
	
	// ***COMPARE CASE***
	private final String runNumber2 = "p1";
	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsDay.xml.gz";
//	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsMorning.xml.gz";
//	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsEvening.xml.gz";
	
//	private final String runNumber2 = "p2";
//	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsDay.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsMorning.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsEvening.xml.gz";
	
//	private final String runNumber2 = "p3";
//	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsDay.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsMorning.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsEvening.xml.gz";
	
//	private final String runNumber2 = "p4";
//	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsDay.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsMorning.xml.gz";
////	private final String emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsEvening.xml.gz";
	
	private final String fileNameAnalysisCompareCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
	
//  ===
	private SpatialAveragingInputData inputData = new SpatialAveragingInputData();

	public static void main(String[] args) throws IOException {
		new RunSpatialAveragingDemandEmissionsManteuffel().configure();
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
		this.inputData.setVisBoundaryShapeFile(visBoundaryShapeFile);
		this.inputData.setScalingFactor(scalingFactor);
		
		this.inputData.setConfigFile1(configFile1);
		this.inputData.setNetFile(netFile);
		
		this.inputData.setEmissionFile1(emissionFile1);
		this.inputData.setEmissionFile2(emissionFile2);
		
		this.inputData.setOutputNameBaseCase(fileNameAnalysisBaseCase);
		this.inputData.setOutputNameCompareCase(fileNameAnalysisCompareCase);
		
		SpatialAveragingDemandEmissions sade = new SpatialAveragingDemandEmissions();
		sade.setInputData(inputData);
		sade.run();
	}
}