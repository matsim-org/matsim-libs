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

package playground.benjamin.utils.spatialAvg;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialAveragingInputData {
	private static final Logger logger = Logger.getLogger(SpatialAveragingInputData.class);
	
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
	
	private String configFile1;
	private String netFile;
	private String emissionFile1, emissionFile2;
	private String plansfile1, plansfile2;
	private String eventsFile1, eventsFile2;
	
//	===
	private String analysisOutPathBaseCase;
	private String analysisOutPathCompareCase;
	
	// TODO: somehow remove this, its bulky...
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

	public String getAnalysisOutPathBaseCase() {
		return this.analysisOutPathBaseCase;
	}

	public String getAnalysisOutPathCompareCase() {
		return this.analysisOutPathCompareCase;
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

	public void setConfigFile1(String configFile1) {
		this.configFile1 = configFile1;
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