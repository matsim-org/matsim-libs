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
package playground.agarwalamit.spatial;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.spatial.GeneralGrid.GridType;


/**
 * All inputs parameters required for spatial analysis are listed and set here.
 * @author amit
 */

public class SpatialDataInputs {

	public final static Logger LOG = Logger.getLogger(SpatialDataInputs.class);
	
	/**
	 * Line or point
	 */
	public final static String linkWeigthMethod = "line"; 
	
	public final static GridType gridType = GridType.SQUARE;
	
	/**
	 * Hexagonal or square grids will depend on this
	 */
	public final static double cellWidth = 170.;
	
	/**
	 * same as count scale factor
	 */
	public final static double scalingFactor = 100.; 
	
	public final static String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	
	
	/**
	 * base case ctd location folder
	 */
	public final static String BAU = runDir+"/baseCaseCtd/";
	public final static String BAUConfig = BAU+"/output_config.xml.gz";
	public final static String BAUNetwork = BAU+"/output_network.xml.gz";
	public final static int BAULastIteration = LoadMyScenarios.getLastIteration(BAUConfig); 
	public final static String BAUEmissionEventsFile = BAU+"/ITERS/it."+BAULastIteration+"/"+BAULastIteration+".emission.events.xml.gz";
	public final static String BAUEventsFile = BAU+"/ITERS/it."+BAULastIteration+"/"+BAULastIteration+".events.xml.gz";
	public final static String BAUPlans = BAU+"/output_plans.xml.gz";
	
	/**
	 * policy case location folder
	 */
	public final static String compareToCase = runDir+"/ei/";
	public final static String compareToCaseConfig = compareToCase+"/output_config.xml.gz";
	public final static String compareToCaseNetwork = compareToCase+"/output_network.xml.gz";
	public final static int compareToCaseLastIteration = LoadMyScenarios.getLastIteration(compareToCaseConfig); 
	public final static String compareToCaseEmissionEventsFile = compareToCase+"/ITERS/it."+compareToCaseLastIteration+"/"+compareToCaseLastIteration+".emission.events.xml.gz";
	public final static String compareToCaseEventsFile = compareToCase+"/ITERS/it."+compareToCaseLastIteration+"/"+compareToCaseLastIteration+".events.xml.gz";
	public final static String compareToCasePlans = compareToCase+"/output_plans.xml.gz";
	
	public final static String shapeFile = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	public final static CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	public final static double xMin =4452550.25;
	public final static double xMax =4479483.33;
	public final static double yMin =5324955.00;
	public final static double yMax =5345696.81;
	public final static double boundingBoxArea = (yMax-yMin)*(xMax-xMin);
	public final static double smoothingRadius = 500.;
	
	public final static int noOfTimeBin = 1;
	
	public final static boolean compareWithBAU = false;
	
	public final static String outputDir = runDir+"/analysis/spatialPlots/"; 
}
