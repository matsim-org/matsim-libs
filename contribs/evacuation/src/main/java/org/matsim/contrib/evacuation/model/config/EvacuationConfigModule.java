/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConfigModule.java
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
package org.matsim.contrib.evacuation.model.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.contrib.evacuation.io.DepartureTimeDistribution;
import org.matsim.core.config.ConfigGroup;

/**
 * Config module for evacuation contrib
 * 
 * @author laemmel
 * 
 */
public class EvacuationConfigModule extends ConfigGroup {

	public static final String GROUP_NAME = "evacuation";
	public static final String NETWORK_FILE_NAME = "networkFile";
	public static final String EVACUATION_AREA_FILE_NAME = "evacuationAreaFile";
	public static final String POPULATION_FILE_NAME = "populationFile";
	public static final String OUTPUT_DIR = "outputDir";
	public static final String SAMPLE_SIZE = "sampleSize";
	private String networkFileName;
	private String evacuationAreaFileName;
	private String populationFileName;
	private String outputDir;
	private String wms;
	private String layer;
	private double sampleSize = 1;
	private String mainTrafficType = "vehicular";
	private String popDensFilename;
	private String targetCRS;


	public EvacuationConfigModule() {
		super(GROUP_NAME);
	}

	public EvacuationConfigModule(String name) {
		super(name);
	}	

	public EvacuationConfigModule(ConfigGroup evacuation) {
		super(GROUP_NAME);
		for (Entry<String, String> e : evacuation.getParams().entrySet()) {
			addParam(e.getKey(), e.getValue());
		}
	}

	@Override
	public void addParam(String param_name, String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		
		if (param_name.equals(NETWORK_FILE_NAME)) {
			setNetworkFileName(value);
		} else if (param_name.equals(EVACUATION_AREA_FILE_NAME)) {
			setEvacuationAreaFileName(value);
		} else if (param_name.equals(POPULATION_FILE_NAME)) {
			setPopulationFileName(value);
		} else if (param_name.equals(OUTPUT_DIR)) {
			setOutputDir(value);
		} else if (param_name.equals(SAMPLE_SIZE)) {
			setSampleSize(value);
		} else {
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
		} else if (param_name.equals(POPULATION_FILE_NAME)) {
			return getPopulationFileName();
		} else if (param_name.equals(OUTPUT_DIR)) {
			return getOutputDir();
		} else if (param_name.equals(SAMPLE_SIZE)) {
			return Double.toString(getSampleSize());
		} else {
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

	private DepartureTimeDistribution departureTimeDistribution;

	public void setDepartureTimeDistribution(DepartureTimeDistribution dtd) {
		this.departureTimeDistribution = dtd;
	}
	
	public DepartureTimeDistribution getDepartureTimeDistribution() {
		return this.departureTimeDistribution;
	}
	
	public String getTargetCRS(){
		return this.targetCRS;
	}
	
	public String getMainTrafficType() {
		return this.mainTrafficType;
	}

	public void setMainTrafficType(String mainTrafficType) {
		this.mainTrafficType = mainTrafficType;
	}


	public String getWms() {
		return this.wms;
	}

	public String getLayer() {
		return this.layer;
	}

	public String getPopDensFilename() {
		return this.popDensFilename;
	}

	public void setPopDensFilename(String populationDensityFile) {
		this.popDensFilename = populationDensityFile;
	}

	public void setWms(String wms) {
		this.wms = wms;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public void setTargetCRS(String targetCRS) {
		this.targetCRS = targetCRS;
	}



}
