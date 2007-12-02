/* *********************************************************************** *
 * project: org.matsim.*
 * GlobalConfigGroup.java
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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;

public class GlobalConfigGroup extends Module {

	public static final String GROUP_NAME = "global";

	public GlobalConfigGroup() {
		super(GROUP_NAME);
	}

	private static final String RANDOM_SEED = "randomSeed";
	private static final String OUTPUT_TIME_FORMAT = "outputTimeFormat";
	private static final String GLOBAL_DTD_BASE = "globalDTDBase";
	private static final String LOCAL_DTD_BASE = "localDTDBase";
	private static final String USE_ROAD_PRICING = "useRoadPricing";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String COORDINATE_SYSTEM = "coordinateSystem";

	private long randomSeed = 4711L;
	private String outputTimeFormat = "HH:mm:ss";
	private String localDtdBase = "dtd/";
	private boolean useRoadPricing = false;
	private int numberOfThreads = 2;
	private String coordinateSystem = "Atlantis";

	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	@Override
	public String getValue(final String key) {
		if (RANDOM_SEED.equals(key)) {
			return Long.toString(getRandomSeed());
		} else if (OUTPUT_TIME_FORMAT.equals(key)) {
			return getOutputTimeFormat();
		} else if (LOCAL_DTD_BASE.equals(key)) {
			return getLocalDtdBase();
		} else if (USE_ROAD_PRICING.equals(key)) {
			return (useRoadPricing() ? "yes" : " no");
		} else if (NUMBER_OF_THREADS.equals(key)) {
			return Integer.toString(getNumberOfThreads());
		} else if (COORDINATE_SYSTEM.equals(key)) {
			return getCoordinateSystem();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (RANDOM_SEED.equals(key)) {
			setRandomSeed(Long.parseLong(value));
		} else if (OUTPUT_TIME_FORMAT.equals(key)) {
			setOutputTimeFormat(value);
		} else if (GLOBAL_DTD_BASE.equals(key)) {
			log.info("The parameter " + GLOBAL_DTD_BASE + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else if (LOCAL_DTD_BASE.equals(key)) {
			setLocalDtdBase(value);
		} else if (USE_ROAD_PRICING.equals(key)) {
			useRoadPricing("yes".equals(value) || "true".equals(value));
		} else if (NUMBER_OF_THREADS.equals(key)) {
			setNumberOfThreads(Integer.parseInt(value));
		} else if (COORDINATE_SYSTEM.equals(key)) {
			setCoordinateSystem(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(RANDOM_SEED, getValue(RANDOM_SEED));
		map.put(USE_ROAD_PRICING, getValue(USE_ROAD_PRICING));
		addNotNullParameterToMap(map, OUTPUT_TIME_FORMAT);
		addNotNullParameterToMap(map, LOCAL_DTD_BASE);
		addNotNullParameterToMap(map, COORDINATE_SYSTEM);
		addNotNullParameterToMap(map, NUMBER_OF_THREADS);
		return map;
	}

	/* direct access */

	public long getRandomSeed() {
		return this.randomSeed;
	}
	public void setRandomSeed(final long randomSeed) {
		this.randomSeed = randomSeed;
	}

	public String getOutputTimeFormat() {
		return this.outputTimeFormat;
	}
	public void setOutputTimeFormat(final String outputTimeFormat) {
		this.outputTimeFormat = outputTimeFormat;
	}

	public String getLocalDtdBase() {
		return this.localDtdBase;
	}
	public void setLocalDtdBase(final String localDtdBase) {
		this.localDtdBase = localDtdBase;
	}

	public boolean useRoadPricing() {
		return this.useRoadPricing;
	}
	public void useRoadPricing(final boolean useRoadPricing) {
		this.useRoadPricing = useRoadPricing;
	}

	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}
	public void setNumberOfThreads(final int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public String getCoordinateSystem() {
		return this.coordinateSystem;
	}
	public void setCoordinateSystem(final String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

}
