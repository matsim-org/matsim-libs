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
package playground.agarwalamit.analysis.spatial;

import org.apache.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.utils.LoadMyScenarios;


/**
 * All inputs parameters required for spatial analysis are listed and set here.
 * @author amit
 */

public class SpatialDataInputs {

	public enum LinkWeightMethod {line, point};
	
	/**
	 * If analyzing only one scenario. By default, events file from last iteration and other output files (network, plans, config) are taken.
	 */
	public SpatialDataInputs(LinkWeightMethod linkWeightMethod, String initialCaseLocation) {
		this.linkWeigthMethod = linkWeightMethod;
		this.initialCase = initialCaseLocation;
		setInitialFiles();
	}
	
	/**
	 * If comparing two scenarios. By default, events file from last iteration and other output files (network, plans, config) are taken.
	 */
	public SpatialDataInputs(LinkWeightMethod linkWeightMethod, String initialCaseLocation, String compareToCaseLocation) {
		this.isComparing = true;
		this.linkWeigthMethod = linkWeightMethod;
		this.initialCase = initialCaseLocation;
		this.compareToCase = compareToCaseLocation;
		
		setInitialFiles();
		
		this.compareToCaseConfig = this.compareToCase+"/output_config.xml";
		this.compareToCaseNetwork = this.compareToCase+"/output_network.xml.gz";
		this.compareToCaseLastIteration = LoadMyScenarios.getLastIteration(this.compareToCaseConfig);
		this.compareToCaseEmissionEventsFile = this.compareToCase+"/ITERS/it."+this.compareToCaseLastIteration+"/"+this.compareToCaseLastIteration+".emission.events.xml.gz";
		this.compareToCaseEventsFile = this.compareToCase+"/ITERS/it."+this.compareToCaseLastIteration+"/"+compareToCaseLastIteration+".events.xml.gz";
		this.compareToCasePlans = this.compareToCase+"/output_plans.xml.gz";
	}

	public final static Logger LOG = Logger.getLogger(SpatialDataInputs.class);
	
	private LinkWeightMethod linkWeigthMethod;
	
	GridType gridType;
	double cellWidth;
	
	String initialCase;
	
	/**
	 * Config extension is taken .xml (not .xml.gz) because .xml have possible changes due to core changes.
	 * <p> In case, .xml is not available, matsim config reader will read .xml.gz
	 */
	public String initialCaseConfig;
	public String initialCaseNetworkFile;
	int initialCaseLastIteration; 
	public String initialCaseEmissionEventsFile;
	public String initialCaseEventsFile;
	public String initialCasePlansFile;
	
	public boolean isComparing = false;
	
	/**
	 * policy case location folder
	 */
	public String compareToCase;
	
	public String compareToCaseConfig;
	public String compareToCaseNetwork;
	int compareToCaseLastIteration ; 
	public String compareToCaseEmissionEventsFile;
	public String compareToCaseEventsFile;
	public String compareToCasePlans; 
	
	public String shapeFile ;
	public CoordinateReferenceSystem targetCRS ;
	
	double xMin;
	double xMax;
	double yMin;
	double yMax;
	
	final  double boundingBoxArea = (yMax-yMin)*(xMax-xMin);
	private double smoothingRadius = 500.;
	
	/**
	 * Hexagonal or square grids.
	 */
	public void setGridInfo(GridType gridType, double cellWidth) {
		this.gridType = gridType;
		this.cellWidth = cellWidth;
	}

	public void setShapeData(CoordinateReferenceSystem targetCRS, String shapeFile) {
		this.setShapeFile(shapeFile);
		this.setTargetCRS(targetCRS);
	}

	public void setBoundingBox(double xMin, double xMax, double yMin, double yMax){
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
	}

	public double getSmoothingRadius() {
		return smoothingRadius;
	}

	public void setSmoothingRadius(double smoothingRadius) {
		this.smoothingRadius = smoothingRadius;
	}

	public void setTargetCRS(CoordinateReferenceSystem targetCRS) {
		this.targetCRS = targetCRS;
	}

	public void setShapeFile(String shapeFile) {
		this.shapeFile = shapeFile;
	}
	
	private void setInitialFiles(){
		initialCaseConfig = initialCase+"/output_config.xml";
		initialCaseNetworkFile = initialCase+"/output_network.xml.gz";
		initialCaseLastIteration = LoadMyScenarios.getLastIteration(initialCaseConfig);
		initialCaseEmissionEventsFile = initialCase+"/ITERS/it."+initialCaseLastIteration+"/"+initialCaseLastIteration+".emission.events.xml.gz";
		initialCaseEventsFile = initialCase+"/ITERS/it."+initialCaseLastIteration+"/"+initialCaseLastIteration+".events.xml.gz";
		initialCasePlansFile = initialCase+"/output_plans.xml.gz";
	}

	LinkWeightMethod getLinkWeigthMethod() {
		return linkWeigthMethod;
	}
}
