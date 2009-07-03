/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.TreeMap;

import org.matsim.core.config.Module;

public class EvacuationConfigGroup  extends Module{

	
	
	private static final long serialVersionUID = 1L;

	public enum EvacuationScenario {day,night}
	
	public static final String GROUP_NAME = "evacuation";

	/**
	 * name of the evacuation area file parameter in config
	 */
	private static final String EVACUATION_AREA_FILE = "inputEvacuationAreaLinksFile";

	/**
	 * file name of the evacutation area file
	 */
	private String evacuationAreaFile;

	/**
	 * name of the flooding data file parameter in config
	 */
	private static final String FLOODING_DATA_FILE = "floodingDataFile";


	/**
	 * name of the buildings shape file in config
	 */
	private static final String BUILDINGS_FILE = "buildingsFile";

	/**
	 * name of the shoreline shape file in config
	 */
	private static final String SHORELINE_FILE = "shorelineFile";
	
	/**
	 * type of the scenario 
	 */
	private static final String SCENARIO = "scenario";
	
	/**
	 * size of the scenario
	 */
	private static final String SAMPLE_SIZE = "sampleSize";
	
	
	
	/**
	 * file name of the flooding data file
	 */
	private String floodingDataFile;

	/**
	 * file name of the buildings shape file
	 */
	private String buildingsFile;

	/**
	 * file name of the shoreline shape file
	 */
	private String shorelineFile;
	
	/**
	 * the scenario type
	 */
	private EvacuationScenario scenario = EvacuationScenario.night;
	
	/**
	 * 
	 */
	private double sampleSize = 0.;
	

	public EvacuationConfigGroup(){
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (EVACUATION_AREA_FILE.equals(key)) {
			return getEvacuationAreaFile();
		}else if (FLOODING_DATA_FILE.equals(key)) {
			return getFloodingDataFile();
		}else if (BUILDINGS_FILE.equals(key)) {
			return getBuildingsFile();
		}else if (SHORELINE_FILE.equals(key)) {
			return getShorelineFile();
		}else if (SCENARIO.equals(key)) {
			return getEvacuationScanrio().toString();
		}else if (SAMPLE_SIZE.equals(key)) {
			return Double.toString(getSampleSize());
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (EVACUATION_AREA_FILE.equals(key)) {
			setEvacuationAreaFile(value.replace('\\', '/'));
		}else if(FLOODING_DATA_FILE.equals(key)){
			setFloodingDataFile(value.replace('\\', '/'));
		}else if(BUILDINGS_FILE.equals(key)){
			setBuildingsFile(value.replace('\\', '/'));
		}else if(SHORELINE_FILE.equals(key)){
			setShorelineFile(value.replace('\\', '/'));
		}else if(SCENARIO.equals(key)){
			setEvacuationScenario(value);
		}else if(SAMPLE_SIZE.equals(key)){
			setSampleSize(value);
		}else {
			throw new IllegalArgumentException(key);
		}
	}


	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(FLOODING_DATA_FILE, getValue(FLOODING_DATA_FILE));
		map.put(BUILDINGS_FILE, getValue(BUILDINGS_FILE));
		map.put(SCENARIO, getValue(SCENARIO));
		map.put(SAMPLE_SIZE, getValue(SAMPLE_SIZE));
		return map;
	}
	
	
	/**
	 *
	 * @return the file name of the evacuation area file
	 */
	public String getEvacuationAreaFile() {
		return this.evacuationAreaFile;
	}
	/**
	 *
	 * @param evacuationAreaFile
	 * the evacuation area filename to set
	 */
	public void setEvacuationAreaFile(String evacuationAreaFile) {
		this.evacuationAreaFile = evacuationAreaFile;

	}

	/**
	 * 
	 * @return the file name of the flooding data file
	 */
	public String getFloodingDataFile() {
		return this.floodingDataFile;
	}

	/**
	 * 
	 * @param floodingDataFile
	 * the flooding data filename to set
	 */
	public void setFloodingDataFile(String floodingDataFile) {
		this.floodingDataFile = floodingDataFile;
	}

	/**
	 * 
	 * @return the shapes of the buildings
	 */
	public String getBuildingsFile() {
		return this.buildingsFile;
	}

	/**
	 * 
	 * @param buildingsFile
	 * the shapes of the buildings
	 */
	public void setBuildingsFile(String buildingsFile) {
		this.buildingsFile = buildingsFile;
	}

	
	/**
	 * 
	 * @return the shapes of the shoreline
	 */
	public String getShorelineFile() {
		return this.shorelineFile;
	}
	
	/**
	 * 
	 * @param shorelineFile
	 * the shapes of the shoreline
	 */
	public void setShorelineFile(String shorelineFile) {
		this.shorelineFile = shorelineFile;
	}
	
	/**
	 * 
	 * @return the scenario type (i.e. day or night)
	 */
	public EvacuationScenario getEvacuationScanrio() {
		return this.scenario;
	}
	
	/**
	 * 
	 * @param scenario
	 * the type of the scenario (i.e. day or night)
	 */
	public void setEvacuationScenario(String scenario) {
		if (scenario.equals("day")) {
			this.scenario = EvacuationScenario.day;
		} else if (scenario.equals("night")) {
			this.scenario = EvacuationScenario.night;
		} else {
			throw new RuntimeException("unkown scenario type:" + scenario);
		}
	}

	/**
	 * 
	 * @return the size of the scenario
	 */
	public double getSampleSize() {
		return this.sampleSize;
	}
	
	/**
	 * 
	 * @param sampleSize
	 */
	public void setSampleSize(String sampleSize) {
		this.sampleSize = Double.parseDouble(sampleSize);
	}
}
