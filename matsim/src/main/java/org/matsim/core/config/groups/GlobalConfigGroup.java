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

package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class GlobalConfigGroup extends ConfigGroup {
	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	public static final String GROUP_NAME = "global";

	public GlobalConfigGroup() {
		super(GROUP_NAME);
	}

	private static final String RANDOM_SEED = "randomSeed";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String COORDINATE_SYSTEM = "coordinateSystem";

	private long randomSeed = 4711L;
	private int numberOfThreads = 2;
	private String coordinateSystem = "Atlantis";
	
	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(NUMBER_OF_THREADS, "\"global\" number of threads.  "
				+ "This number is used, e.g., for replanning, but NOT in the mobsim.  "
				+ "This can typically be set to as many cores as you have available, or possibly even slightly more.") ;
		return map ;
	}

	@Override
	public String getValue(final String key) {
		if (RANDOM_SEED.equals(key)) {
			return Long.toString(getRandomSeed());
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
		} else if (NUMBER_OF_THREADS.equals(key)) {
			setNumberOfThreads(Integer.parseInt(value));
		} else if (COORDINATE_SYSTEM.equals(key)) {
			setCoordinateSystem(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(RANDOM_SEED, getValue(RANDOM_SEED));
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

	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}
	public void setNumberOfThreads(final int numberOfThreads) {
		log.info("setting number of threads to: " + numberOfThreads ) ; // might not be so bad to do this everywhere?  benjamin/kai, oct'10
		this.numberOfThreads = numberOfThreads;
	}

	public String getCoordinateSystem() {
		return this.coordinateSystem;
	}
	public void setCoordinateSystem(final String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

}
