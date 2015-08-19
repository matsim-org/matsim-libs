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

package playground.benjamin.scenarios.munich.analysis.spatialAvg;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialAveragingInputData {
	private static final Logger logger = Logger.getLogger(SpatialAveragingInputData.class);

	/*MUNICH*/
//	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
//	private static double xMin=4452550.25;
//	private static double xMax=4479483.33;
//	private static double yMin=5324955.00;
//	private static double yMax=5345696.81;
	
	/*KRASNOJARSK*/
//	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:32646");
////	private final double xMin = 458823.0;
////	private final double xMax = 544945.0;
////	private final double yMin = 6190162.0;
////	private final double yMax = 6248014.0;
//	private final double xMin = 483895.0;
//	private final double xMax = 502066.0;
//	private final double yMin = 6202213.0;
//	private final double yMax = 6212963.0;
	
	/*BERLIN-MANTEUFFELSTRASSE*/
//	private CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:31468");
////	private final double xMin = 4595288.82;
////	private final double xMax = 4598267.52;
////	private final double yMin = 5817859.97;
////	private final double yMax = 5820953.98;
//	private double xMin = 4595465.0;
//	private double xMax = 4598349.0;
//	private double yMin = 5819159.0;
//	private double yMax = 5820931.0;
//	
//	double scalingFactor;
//	private String netFile;
////	private String munichShapeFile;
//	
//	private int numberOfXBins = 160;
//	private int numberOfYBins = 120;
////	private double smoothingRadius_m = 250.;
//	private double smoothingRadius_m = 32.;
//	
//	private String runNumber1, runNumber2;
//	private String runDirectory1, runDirectory2;
//	private String configFile1;
//	private String emissionFile1, emissionFile2;
//	private Integer lastIteration1;
//	private String plansfile1, plansfile2;
//	private String eventsFile1, eventsFile2;
//	
//	private String visBoundaryShapeFile;
//	private boolean useVisBoundary = false;
//	
//	private String analysisOutPathForBaseCase;
//	private String analysisOutPathForSpatialComparison;
//	
//	private String baseCase, compareCase;
	
	private CoordinateReferenceSystem targetCRS;
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	
	private int numberOfXBins;
	private int numberOfYBins;
	private double smoothingRadius_m;
	
	private int noOfTimeBins;
	private boolean writeRoutput;
	private boolean writeGisOutput;
	private boolean useBaseCaseOnly;
	private String pollutant2analyze;
	
	private boolean useVisBoundary;
	private String visBoundaryShapeFile;
	private double scalingFactor;
	
	private String runNumber1, runNumber2;
	private String runDirectory1, runDirectory2;
	private String configFile1;
	private Integer lastIteration1;
	private String netFile;
	private String emissionFile1, emissionFile2;
	private String plansfile1, plansfile2;
	private String eventsFile1, eventsFile2;
	
//	===
	private String analysisOutPathBaseCase;
	private String analysisOutPathCompareCase;
	
	public SpatialAveragingInputData(String baseCase, String compareCase) {
		setFieldsForBaseCase(baseCase);
		setFieldsForCompareCase(baseCase, compareCase);
	}
	
	private void setFieldsForBaseCase(String baseCase) {
//		if(baseCase.equals("manteuffel")){
//			String inputPath = "../../runs-svn/manteuffelstrasse/";
//			scalingFactor = 4.;
//			useVisBoundary = true;
//			visBoundaryShapeFile = inputPath + "bau/analysis/spatialAveraging/data/shapes/analysis-area.shp";
//			runNumber1 = "bau";
////			netFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
////			netFile = inputPath + "p3/bvg.run190.25pct.dilution001.network.20150731.LP2.III.emissionNetwork.xml.gz";
//			netFile = inputPath + "p4/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.emissionNetwork.xml.gz";
//			configFile1 = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_config.xml.gz";
////			lastIteration1 = getLastIteration(configFile1);
//			lastIteration1 = 30;
////			emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEvents.xml.gz";
////			emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsMorning.xml.gz";
//			emissionFile1 = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsEvening.xml.gz";
//			
//			analysisOutPathBaseCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber1 + "." + lastIteration1;
//		}
//		else 
		if(baseCase.equals("krasnojarsk")){
			scalingFactor = 10.;
			runNumber1 = "baseCase";
			runDirectory1 = "../../runs-svn/krasnojarsk/bau/";
			netFile = runDirectory1 + "network.xml";
			configFile1 = runDirectory1 + "output_config.xml";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "emission.events.offline-10.xml.gz";
		}
		else if(baseCase.equals("exposureInternalization")){
			scalingFactor = 100.;
			runNumber1 = "baseCase";
			runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/";
			netFile = runDirectory1 + "output_network.xml.gz";
			configFile1 = runDirectory1 + "output_config.xml";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
			eventsFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".events.xml.gz";
		}
		else if(baseCase.equals("latsis")){
			scalingFactor = 100.;
			runNumber1 = "baseCase";
			runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
			netFile = runDirectory1 + "output_network.xml.gz";
			configFile1 = runDirectory1 + "output_config.xml.gz";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
			eventsFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".events.xml.gz";
		}
		else if(baseCase.equals("981")){
			scalingFactor = 10.;
		    runNumber1 = "981";
		    runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
			netFile = runDirectory1 + runNumber1 + ".output_network.xml.gz";
			configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
			eventsFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".events.xml.gz";
		}
		else {
			throw new RuntimeException("Case not defined. Aborting...");
		}
		plansfile1 = runDirectory1 + "output_plans.xml.gz";
	}
	private void setFieldsForCompareCase(String baseCase, String compareCase) {
//		if(baseCase.equals("manteuffel")){
//			String inputPath = "../../runs-svn/manteuffelstrasse/";
//			if(compareCase.equals("p1")){
//				runNumber2 = "p1";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEvents.xml.gz";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsMorning.xml.gz";
//				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsEvening.xml.gz";
//				analysisOutPathCompareCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
//			}
//		}
//		if(baseCase.equals("manteuffel")){
//			String inputPath = "../../runs-svn/manteuffelstrasse/";
//			if(compareCase.equals("p2")){
//				runNumber2 = "p2";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEvents.xml.gz";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsMorning.xml.gz";
//				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsEvening.xml.gz";
//				analysisOutPathCompareCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
//			}
//		}
//		if(baseCase.equals("manteuffel")){
//			String inputPath = "../../runs-svn/manteuffelstrasse/";
//			if(compareCase.equals("p3")){
//				runNumber2 = "p3";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEvents.xml.gz";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsMorning.xml.gz";
//				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsEvening.xml.gz";
//				analysisOutPathCompareCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
//			}
//		}
//		if(baseCase.equals("manteuffel")){
//			String inputPath = "../../runs-svn/manteuffelstrasse/";
//			if(compareCase.equals("p4")){
//				runNumber2 = "p4";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEvents.xml.gz";
////				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsMorning.xml.gz";
//				emissionFile2 = inputPath + runNumber2 + "/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsEvening.xml.gz";
//				analysisOutPathCompareCase = inputPath + "bau/analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
//			}
//		}
		
		if(baseCase.equals("exposureInternalization")){
			if(compareCase.equals("zone30")){
				runNumber2 = "zone30";
				runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_zone30/";				
			}
			if(compareCase.equals("pricing")){
				runNumber2 = "pricing";
				runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_pricing/";
			}
			if(compareCase.equals("exposurePricing")){
				runNumber2 = "exposurePricing";
				runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_exposurePricing/";
			}
			emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
			plansfile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".plans.xml.gz";
			eventsFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".events.xml.gz";
		}
		
		if(baseCase.equals("latsis")){
			if(compareCase.equals("zone30")){
				runNumber2 = "zone30";
				runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
			}
			if(compareCase.equals("pricing")){
				runNumber2 = "pricing";
				runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_pricing_newCode/";
			}
			emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
			plansfile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".plans.xml.gz";
			eventsFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".events.xml.gz";
		}
		
		if(baseCase.equals("981")){
			if(compareCase.equals("983")){
				runNumber2 = "983";
				runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
				emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + runNumber2 + "." + lastIteration1 + ".emission.events.xml.gz";
				plansfile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + runNumber2 + "." + lastIteration1 + ".plans.xml.gz";
				eventsFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + runNumber2 + "." + lastIteration1 + ".events.xml.gz";
			}
		}
		
//		if(compareCase.equals("base")){
//			runNumber2 = runNumber1;
//			runDirectory2 = runDirectory1;
//			emissionFile2 = emissionFile1;
//			plansfile2 = plansfile1;
//			eventsFile2 = eventsFile1;
//		}
	}

	private static Integer getLastIteration(String configFile) {
		Config config = ConfigUtils.createConfig();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
	

	public double getEndTime() {
		Config config = ConfigUtils.createConfig();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile1);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		return endTime;
	}

	public int getNoOfXbins() {
		return numberOfXBins;
	}

	public int getNoOfYbins() {
		return numberOfYBins;
	}

	public Double getSmoothingRadius_m() {
		return this.smoothingRadius_m;
	}
	
	public Double getNoOfBins() {
		return new Double(numberOfXBins*numberOfYBins);
	}
	
//	public String getScenarioInformation(){
//		return ("Scenario: " + baseCase + " , policy/run for comparision: " + compareCase );
//	}

	public String getNetworkFile() {
		return this.netFile;
	}

	public String getAnalysisOutPathForBaseCase() {
		return this.analysisOutPathBaseCase;
	}

	public String getAnalysisOutPathForSpatialComparison() {
		return this.analysisOutPathCompareCase;
//				(runDirectory1 + "analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta");
	}
	
	public String getSpatialAveragingOutPathForCompareCase(){
		return null;
//				(runDirectory1 + "analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1);
	}

	public String getExposureOutPathForCompareCase(){
		return null;
//				(runDirectory1 + "analysis/exposure/data/" + runNumber2 + "." + lastIteration1);
	}
	
	public String getEmissionFileForBaseCase() {
		return this.emissionFile1;
	}

	public String getEmissionFileForCompareCase() {
		return this.emissionFile2;
	}

	public Double getMinX() {
		return xMin;
	}

	public Double getMaxX() {
		return xMax;
	}

	public Double getMinY() {
		return yMin;
	}

	public Double getMaxY() {
		return yMax;
	}

	public CoordinateReferenceSystem getTargetCRS() {
		return this.targetCRS;
	}

	public double getBoundingboxSizeSquareMeter() {
		return ((xMax-xMin)*(yMax-yMin));
	}

	public String getPlansFileBaseCase() {
		return plansfile1;
	}

	public String getEventsFileBaseCase() {
		return eventsFile1;
	}

	public String getEventsFileCompareCase() {
		return eventsFile2;
	}

	public Double getScalingFactor() {
		return this.scalingFactor;
	}

	public String getPlansFileCompareCase() {
		return plansfile2;
	}
	
	public String getVisBoundaryShapeFile() {
		return visBoundaryShapeFile;
	}

	public boolean isUseVisBoundary() {
		return useVisBoundary;
	}
	
	public boolean isInResearchArea(Coord coord) {
		Double xCoord = coord.getX();
		Double yCoord = coord.getY();
		
		if(xCoord > xMin && xCoord < xMax){
			if(yCoord > yMin && yCoord < yMax){
				return true;
			}
		}
		return false;
	}

	public int getNoOfTimeBins() {
		return noOfTimeBins;
	}

	public boolean isWriteRoutput() {
		return writeRoutput;
	}

	public boolean isWriteGisOutput() {
		return writeGisOutput;
	}

	public boolean isUseBaseCaseOnly() {
		return useBaseCaseOnly;
	}

	public String getPollutant2analyze() {
		return pollutant2analyze;
	}

	public void setCRS(CoordinateReferenceSystem targetCRS) {
		this.targetCRS = targetCRS;
	}

	public void setMinX(double xMin) {
		this.xMin = xMin;
		
	}
	
	public void setMaxX(double xMax) {
		this.xMax = xMax;
	}

	public void setMinY(double yMin) {
		this.yMin = yMin;
	}

	public void setMaxY(double yMax) {
		this.yMax = yMax;
	}

	public void setNumberOfXBins(int numberOfXBins) {
		this.numberOfXBins = numberOfXBins;
	}

	public void setNumberOfYBins(int numberOfYBins) {
		this.numberOfYBins = numberOfYBins;
	}

	public void setSmoothingRadius_m(double smoothingRadius_m) {
		this.smoothingRadius_m = smoothingRadius_m;
	}

	public void setUseVisBoundary(boolean useVisBoundary) {
		this.useVisBoundary = useVisBoundary;
	}

	public void setVisBoundaryShapeFile(String visBoundaryShapeFile) {
		this.visBoundaryShapeFile = visBoundaryShapeFile;
	}

	public void setScalingFactor(double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public void setRunNumber1(String runNumber1) {
		this.runNumber1 = runNumber1;
	}

	public void setRunNumber2(String runNumber2) {
		this.runNumber2 = runNumber2;
	}

	public void setRunDirectory1(String runDirectory1) {
		this.runDirectory1 = runDirectory1;
	}

	public void setRunDirectory2(String runDirectory2) {
		this.runDirectory2 = runDirectory2;
	}

	public void setConfigFile1(String configFile1) {
		this.configFile1 = configFile1;
	}

	public void setLastIteration1(Integer lastIteration1) {
		this.lastIteration1 = lastIteration1;
	}

	public void setNetFile(String netFile) {
		this.netFile = netFile;
	}

	public void setEmissionFile1(String emissionFile1) {
		this.emissionFile1 = emissionFile1;
	}

	public void setEmissionFile2(String emissionFile2) {
		this.emissionFile2 = emissionFile2;
	}

	public void setEventsFile1(String eventsFile1) {
		this.eventsFile1 = eventsFile1;
	}

	public void setEventsFile2(String eventsFile2) {
		this.eventsFile2 = eventsFile2;
	}

	public void setPlansFile1(String plansfile1) {
		this.plansfile1 = plansfile1;
	}

	public void setPlansFile2(String plansfile2) {
		this.plansfile2 = plansfile2;
	}

	public void setOutputNameBaseCase(String outNameBaseCase) {
		this.analysisOutPathBaseCase = outNameBaseCase;
	}

	public void setOutputNameCompareCase(String outNameCompareCase) {
		this.analysisOutPathCompareCase = outNameCompareCase;
	}

	public void setNoOfTimeBins(int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
	}

	public void setWriteROutput(boolean writeRoutput) {
		this.writeRoutput = writeRoutput;
	}

	public void setWriteGISOutput(boolean writeGisOutput) {
		this.writeGisOutput = writeGisOutput;
	}

	public void setUseBaseCaseOnly(boolean useBaseCaseOnly) {
		this.useBaseCaseOnly = useBaseCaseOnly;
	}

	public void setPollutant2Analyze(String pollutant2analyze) {
		this.pollutant2analyze = pollutant2analyze;
	}
}