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

import org.matsim.core.config.Module;

public class EvacuationConfigGroup  extends Module{

	
	
	private static final long serialVersionUID = 1L;

	public enum Scenario {day,night}
	
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
	 * type of the scenario 
	 */
	private static final String SCENARIO = "scenario";
	
	/**
	 * file name of the flooding data file
	 */
	private String floodingDataFile;

	/**
	 * file name of the buildings shape file
	 */
	private String buildingsFile;
	
	/**
	 * the scenario type
	 */
	private Scenario scenario;
	

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
		}else if(SCENARIO.equals(key)){
			setScenario(value);
		}else {
			throw new IllegalArgumentException(key);
		}
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
	 * @return the scenario type (i.e. day or night)
	 */
	public Scenario getScanrio() {
		return this.scenario;
	}
	
	/**
	 * 
	 * @param scenario
	 * the type of the scenario (i.e. day or night)
	 */
	public void setScenario(String scenario) {
		if (scenario.equals("day")) {
			this.scenario = Scenario.day;
		} else if (scenario.equals("night")) {
			this.scenario = Scenario.night;
		} else {
			throw new RuntimeException("unkown scenario type:" + scenario);
		}
	}

}
