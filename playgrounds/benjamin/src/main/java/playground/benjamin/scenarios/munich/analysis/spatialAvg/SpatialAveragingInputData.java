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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialAveragingInputData {
	
	private static final Logger logger = Logger.getLogger(SpatialAveragingInputData.class);
	
	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	private final double xMin = 4452550.25;
	private final double xMax = 4479483.33;
	private final double yMin = 5324955.00;
	private final double yMax = 5345696.81;
	
	double scalingFactor;
	private String netFile;
	private String munichShapeFile;
	
	private String runNumber1, runNumber2;
	private String runDirectory1, runDirectory2;
	private String configFile1;
	private String emissionFile1, emissionFile2;
	private Integer lastIteration1;
	private String plansfile;
	
	public SpatialAveragingInputData(String baseCase, String compareCase) {
		setFieldsForBaseCase(baseCase);
		setFieldsForCompareCase(baseCase, compareCase);
	}
	
	private void setFieldsForBaseCase(String baseCase) {
		munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		if(baseCase.equals("exposureInternalization")){
			scalingFactor = 100.;
			runNumber1 = "baseCase";
			runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/";
			netFile = runDirectory1 + "output_network.xml.gz";
			configFile1 = runDirectory1 + "output_config.xml";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
		}
		if(baseCase.equals("latsis")){
			scalingFactor = 100.;
			runNumber1 = "baseCase";
			runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
			netFile = runDirectory1 + "output_network.xml.gz";
			configFile1 = runDirectory1 + "output_config.xml.gz";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
		}
		if(baseCase.equals("981")){
			scalingFactor = 10.;
		    runNumber1 = "981";
		    runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
			netFile = runDirectory1 + runNumber1 + ".output_network.xml.gz";
			configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
			lastIteration1 = getLastIteration(configFile1);
			emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
		}
		plansfile = runDirectory1 + "output_plans.xml.gz";
	}
	private void setFieldsForCompareCase(String baseCase, String compareCase) {
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
		}
		
		if(baseCase.equals("981")){
			if(compareCase.equals("983")){
				runNumber2 = "983";
				runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
				emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration1 + "/" + runNumber2 + "." + lastIteration1 + ".emission.events.xml.gz";
			}
		}
	}

	private static Integer getLastIteration(String configFile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
	

	public double getEndTime() {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile1);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		return endTime;
	}
	

	public String getNetworkFile() {
		return this.netFile;
	}

	public String getAnalysisOutPathForBaseCase() {
		return (runDirectory1 + "analysis/spatialAveraging/data/" + runNumber1 + "." + lastIteration1);
	}
	

	public String getEmissionFileForBaseCase() {
		return this.emissionFile1;
	}
	

	public String getAnalysisOutPathForCompareCase() {
		return (runDirectory1 + "analysis/spatialAveraging/data/" + runNumber2 + "." + lastIteration1 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta");
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

	public String getMunichShapeFile() {
		return this.munichShapeFile;
	}

	public CoordinateReferenceSystem getTargetCRS() {
		return this.targetCRS;
	}

	public double getBoundingboxSizeSquareMeter() {
		return ((xMax-xMin)*(yMax-yMin));
	}

	public String getPlansFile() {
		return plansfile;
	}

	public String getEventsFile() {
//		return (runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".events.xml.gz");
		return (runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".events.xml.gz");
	}

	public Double getScalingFactor() {
		return this.scalingFactor;
	}

}
