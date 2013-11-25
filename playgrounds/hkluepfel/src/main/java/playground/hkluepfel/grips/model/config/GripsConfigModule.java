/* *********************************************************************** *
 * project: org.matsim.*
 * GripsConfigModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.grips.model.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.core.config.Module;

/**
 * Config module for grips project
 * @author laemmel
 *
 */
public class GripsConfigModule extends Module {

	public static final String GROUP_NAME = "grips";

	public static final String NETWORK_FILE_NAME = "networkFile";

	public static final String EVACUATION_AREA_FILE_NAME = "evacuationAreaFile";

	public static final String POPULATION_FILE_NAME = "populationFile";

	public static final String OUTPUT_DIR = "outputDir";

	public static final String SAMPLE_SIZE = "sampleSize";

	private String networkFileName;

	private String evacuationAreaFileName;

	private String populationFileName;

	private String outputDir;

	private double sampleSize = 1;



	public GripsConfigModule(String name) {
		super(name);
	}

	public GripsConfigModule(Module grips) {
		super(GROUP_NAME);
		for (Entry<String, String> e : grips.getParams().entrySet()) {
			addParam(e.getKey(), e.getValue());
		}
	}

	@Override
	public void addParam(String param_name, String value) {
		if (param_name.equals(NETWORK_FILE_NAME)) {
			setNetworkFileName(value);
		} else if (param_name.equals(EVACUATION_AREA_FILE_NAME)) {
			setEvacuationAreaFileName(value);
		} else if (param_name.equals(POPULATION_FILE_NAME)){
			setPopulationFileName(value);
		} else if (param_name.equals(OUTPUT_DIR)){
			setOutputDir(value);
		} else if (param_name.equals(SAMPLE_SIZE)){
			setSampleSize(value);
		}else {
			throw new IllegalArgumentException(param_name);
		}
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	@Override
	public String getValue(String param_name) {
		if (param_name.equals(NETWORK_FILE_NAME)) {
			return getNetworkFileName();
		} else if (param_name.equals(EVACUATION_AREA_FILE_NAME)) {
			return getEvacuationAreaFileName();
		} else if (param_name.equals(POPULATION_FILE_NAME)){
			return getPopulationFileName();
		} else if (param_name.equals(OUTPUT_DIR)){
			return getOutputDir();
		}else if (param_name.equals(SAMPLE_SIZE)){
			return Double.toString(getSampleSize());
		}else {
			throw new IllegalArgumentException(param_name);
		}
	}

	@Override
	public Map<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(NETWORK_FILE_NAME, getValue(NETWORK_FILE_NAME));
		map.put(EVACUATION_AREA_FILE_NAME, getValue(EVACUATION_AREA_FILE_NAME));
		map.put(POPULATION_FILE_NAME, getValue(POPULATION_FILE_NAME));
		map.put(OUTPUT_DIR, getValue(OUTPUT_DIR));
		map.put(SAMPLE_SIZE, getValue(SAMPLE_SIZE));
		return map;
	}

	public String getNetworkFileName() {
		return this.networkFileName;
	}

	public void setNetworkFileName(String name) {
		this.networkFileName = name;
	}

	public String getEvacuationAreaFileName() {
		return this.evacuationAreaFileName;
	}

	public void setEvacuationAreaFileName(String evacuationAreaFileName) {
		this.evacuationAreaFileName = evacuationAreaFileName;
	}

	public String getPopulationFileName() {
		return this.populationFileName;
	}

	public void setPopulationFileName(String populationFileName) {
		this.populationFileName = populationFileName;
	}

	public double getSampleSize() {
		return this.sampleSize;
	}

	public void setSampleSize(String sampleSize) {
		this.sampleSize = Double.parseDouble(sampleSize);
	}

	//from here things only work for the xsd based config
	private DepartureTimeDistributionType distribution;

	public void setDepartureTimeDistribution(DepartureTimeDistributionType departureTimeDistributionType) {
		this.distribution = departureTimeDistributionType;
	}
	public DepartureTimeDistributionType getDepartureTimeDistribution() {
		return this.distribution;
	}

	private String mainTrafficType = "vehicular";
	public String getMainTrafficType() {
		return this.mainTrafficType;
	}
	
	public void setMainTrafficType(String mainTrafficType) {
		this.mainTrafficType = mainTrafficType;
	}
}
