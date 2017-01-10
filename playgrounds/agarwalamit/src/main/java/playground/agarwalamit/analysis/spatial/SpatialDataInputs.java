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

import java.io.File;
import org.apache.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * All inputs parameters required for spatial analysis are listed and set here.
 * @author amit
 */

public class SpatialDataInputs {

	public enum LinkWeightMethod {line, point}

    public final static Logger LOG = Logger.getLogger(SpatialDataInputs.class);
	
	private final LinkWeightMethod linkWeigthMethod;
	
	private GridType gridType;
	private double cellWidth;
	private final String initialCase;
	
	/**
	 * Config extension is taken .xml (not .xml.gz) because .xml have possible changes due to core changes.
	 * <p> In case, .xml is not available, matsim config reader will read .xml.gz
	 */
	public String initialCaseConfig;
	public String initialCaseNetworkFile;
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
	public String compareToCaseEmissionEventsFile;
	public String compareToCaseEventsFile;
	public String compareToCasePlans; 
	
	public String shapeFile ;
	public CoordinateReferenceSystem targetCRS ;
	
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	
	private final  double boundingBoxArea = (yMax-yMin)*(xMax-xMin);
	private double smoothingRadius = 500.;
	
	/**
	 * If analyzing only one scenario. By default, events file from last iteration and other output files (network, plans, config) are taken.
	 */
	public SpatialDataInputs(final LinkWeightMethod linkWeightMethod, final String initialCaseLocation) {
		this.linkWeigthMethod = linkWeightMethod;
		this.initialCase = initialCaseLocation;
		setInitialFiles();
	}
	
	/**
	 * If comparing two scenarios. By default, events file from last iteration and other output files (network, plans, config) are taken.
	 */
	public SpatialDataInputs(final LinkWeightMethod linkWeightMethod, final String initialCaseLocation, final String compareToCaseLocation) {
		this.isComparing = true;
		this.linkWeigthMethod = linkWeightMethod;
		this.initialCase = initialCaseLocation;
		this.compareToCase = compareToCaseLocation;
		
		setInitialFiles();
		
		this.compareToCaseConfig = this.compareToCase+"/output_config.xml";
		this.compareToCaseNetwork = this.compareToCase+"/output_network.xml.gz";
		int compareToCaseLastIteration = LoadMyScenarios.getLastIteration(this.compareToCaseConfig);

		// check for output events file
		if ( new File(compareToCase+"/output_emissions_events.xml.gz").exists() ) {
			compareToCaseEmissionEventsFile = compareToCase+"/output_emissions_events.xml.gz";
		} else {
			compareToCaseEmissionEventsFile = compareToCase+"/ITERS/it."+compareToCaseLastIteration+"/"+compareToCaseLastIteration+".emission.events.xml.gz";
		}

		if ( new File(compareToCase+"/output_events.xml.gz").exists() ) {
			compareToCaseEventsFile = compareToCase+"/output_events.xml.gz";
		} else {
			compareToCaseEventsFile = compareToCase+"/ITERS/it."+compareToCaseLastIteration+"/"+compareToCaseLastIteration+".events.xml.gz";
		}

		this.compareToCasePlans = this.compareToCase+"/output_plans.xml.gz";
	}
	
	/**
	 * Hexagonal or square grids.
	 */
	public void setGridInfo(final GridType gridType, final double cellWidth) {
		this.gridType = gridType;
		this.cellWidth = cellWidth;
	}

	public void setShapeData(final CoordinateReferenceSystem targetCRS, final String shapeFile) {
		this.setShapeFile(shapeFile);
		this.setTargetCRS(targetCRS);
	}

	public void setBoundingBox(final double xMin, final double xMax,final double yMin, final double yMax){
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
	}

	public double getSmoothingRadius() {
		return smoothingRadius;
	}

	public void setSmoothingRadius(final double smoothingRadius) {
		this.smoothingRadius = smoothingRadius;
	}

	public void setTargetCRS(final CoordinateReferenceSystem targetCRS) {
		this.targetCRS = targetCRS;
	}

	public void setShapeFile(final String shapeFile) {
		this.shapeFile = shapeFile;
	}
	
	private void setInitialFiles(){
		initialCaseConfig = initialCase+"/output_config.xml";
		initialCaseNetworkFile = initialCase+"/output_network.xml.gz";
		int initialCaseLastIteration = LoadMyScenarios.getLastIteration(initialCaseConfig);

		// check for output events file
		if ( new File(initialCase+"/output_emissions_events.xml.gz").exists() ) {
			initialCaseEmissionEventsFile = initialCase+"/output_emissions_events.xml.gz";
		} else {
			initialCaseEmissionEventsFile = initialCase+"/ITERS/it."+initialCaseLastIteration+"/"+initialCaseLastIteration+".emission.events.xml.gz";
		}

		if ( new File(initialCase+"/output_events.xml.gz").exists() ) {
			initialCaseEventsFile = initialCase+"/output_events.xml.gz";
		} else {
			initialCaseEventsFile = initialCase+"/ITERS/it."+initialCaseLastIteration+"/"+initialCaseLastIteration+".events.xml.gz";
		}

		initialCasePlansFile = initialCase+"/output_plans.xml.gz";
	}

	public LinkWeightMethod getLinkWeigthMethod() {
		return linkWeigthMethod;
	}

	public GridType getGridType() {
		return gridType;
	}

	public double getCellWidth() {
		return cellWidth;
	}

	public String getInitialCase() {
		return initialCase;
	}

	public double getxMin() {
		return xMin;
	}

	public double getxMax() {
		return xMax;
	}

	public double getyMin() {
		return yMin;
	}

	public double getyMax() {
		return yMax;
	}

	public double getBoundingBoxArea() {
		return boundingBoxArea;
	}
}