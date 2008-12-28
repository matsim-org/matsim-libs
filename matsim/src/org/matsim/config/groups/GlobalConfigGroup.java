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

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "global";

	public GlobalConfigGroup() {
		super(GROUP_NAME);
	}

	private static final String RANDOM_SEED = "randomSeed";
	private static final String OUTPUT_TIME_FORMAT = "outputTimeFormat"; // usage deprecated
	private static final String GLOBAL_DTD_BASE = "globalDTDBase";  // usage deprecated
	private static final String LOCAL_DTD_BASE = "localDTDBase";
	private static final String USE_ROAD_PRICING = "useRoadPricing"; // usage deprecated
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String COORDINATE_SYSTEM = "coordinateSystem";

	private long randomSeed = 4711L;
	private String localDtdBase = "dtd/";
	private int numberOfThreads = 2;
	private String coordinateSystem = "Atlantis";

	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	@Override
	public String getValue(final String key) {
		if (RANDOM_SEED.equals(key)) {
			return Long.toString(getRandomSeed());
		} else if (LOCAL_DTD_BASE.equals(key)) {
			return getLocalDtdBase();
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
		} else if (OUTPUT_TIME_FORMAT.equals(key) || GLOBAL_DTD_BASE.equals(key) || USE_ROAD_PRICING.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer supported and should be removed from the configuration file.");
		} else if (LOCAL_DTD_BASE.equals(key)) {
			setLocalDtdBase(value);
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
		addParameterToMap(map, LOCAL_DTD_BASE);
		addParameterToMap(map, COORDINATE_SYSTEM);
		addParameterToMap(map, NUMBER_OF_THREADS);
		return map;
	}

	/* direct access */

	public long getRandomSeed() {
		return this.randomSeed;
	}
	public void setRandomSeed(final long randomSeed) {
		this.randomSeed = randomSeed;
	}

	public String getLocalDtdBase() {
		return this.localDtdBase;
	}
	public void setLocalDtdBase(final String localDtdBase) {
		this.localDtdBase = localDtdBase;
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
