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

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.FileType;
import org.matsim.core.config.Module;

/**
 * Config module for grips project
 * 
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
	private String scenarioFileName;
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


	public GripsConfigModule(String name, String scenariofile) {
		super(name);
		scenarioFileName = scenariofile;
	}
	
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


	private String getAbsolute(String filename) {
		String rv = filename;
		File file = new File(filename);
		
		if (!file.isAbsolute()) {
			File sf = new File(scenarioFileName);
			if (filename.startsWith("."))
				filename = filename.substring(1);
			rv = sf.getParent() + "/" + filename; // filename can be something
													// like /osm/map.osm
		}
//		System.out.println("filename: " + filename);
		System.out.println("rv: " + rv);
		
		return rv;
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

	// from here things only work for the xsd based config
	private DepartureTimeDistributionType distribution;

	public void setDepartureTimeDistribution(
			DepartureTimeDistributionType departureTimeDistributionType) {
		this.distribution = departureTimeDistributionType;
	}

	public DepartureTimeDistributionType getDepartureTimeDistribution() {
		return this.distribution;
	}

	public String getTargetCRS(){
		return targetCRS;
	}
	
	public String getMainTrafficType() {
		return this.mainTrafficType;
	}

	public void setMainTrafficType(String mainTrafficType) {
		this.mainTrafficType = mainTrafficType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((distribution == null) ? 0 : distribution.hashCode());
		result = prime
				* result
				+ ((evacuationAreaFileName == null) ? 0
						: evacuationAreaFileName.hashCode());
		result = prime * result
				+ ((mainTrafficType == null) ? 0 : mainTrafficType.hashCode());
		result = prime * result
				+ ((networkFileName == null) ? 0 : networkFileName.hashCode());
		result = prime * result
				+ ((outputDir == null) ? 0 : outputDir.hashCode());
		result = prime
				* result
				+ ((populationFileName == null) ? 0 : populationFileName
						.hashCode());
		long temp;
		temp = Double.doubleToLongBits(sampleSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GripsConfigModule other = (GripsConfigModule) obj;
		if (distribution == null) {
			if (other.distribution != null)
				return false;
		} else if (!distribution.equals(other.distribution))
			return false;
		if (evacuationAreaFileName == null) {
			if (other.evacuationAreaFileName != null)
				return false;
		} else if (!evacuationAreaFileName.equals(other.evacuationAreaFileName))
			return false;
		if (mainTrafficType == null) {
			if (other.mainTrafficType != null)
				return false;
		} else if (!mainTrafficType.equals(other.mainTrafficType))
			return false;
		if (networkFileName == null) {
			if (other.networkFileName != null)
				return false;
		} else if (!networkFileName.equals(other.networkFileName))
			return false;
		if (outputDir == null) {
			if (other.outputDir != null)
				return false;
		} else if (!outputDir.equals(other.outputDir))
			return false;
		if (populationFileName == null) {
			if (other.populationFileName != null)
				return false;
		} else if (!populationFileName.equals(other.populationFileName))
			return false;
		if (Double.doubleToLongBits(sampleSize) != Double
				.doubleToLongBits(other.sampleSize))
			return false;
		return true;
	}


	public String getWms() {
		return this.wms;
	}

	public String getLayer() {
		return this.layer;
	}

	public String getPopDensFilename() {
		return popDensFilename;
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
