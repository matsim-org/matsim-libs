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

package playground.marcel.config.groups;

import java.util.LinkedHashSet;
import java.util.Set;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

public class GlobalConfigGroup implements ConfigGroupI {

	public static final String GROUP_NAME = "global";
	public static final String RANDOM_SEED = "randomSeed";
	public static final String TIME_FORMAT = "timeFormat";
	public static final String LOCAL_DTD_BASE = "localDtdBase";
	
	private long randomSeed = 4711;
	private boolean randomSeetSet = false;
	private String timeFormat = "HH:mm:ss";
	private String localDtdBase = "";

	private Set<String> keyset = new LinkedHashSet<String>();
	
	public GlobalConfigGroup() {
		this.keyset.add(RANDOM_SEED);
		this.keyset.add(TIME_FORMAT);
		this.keyset.add(LOCAL_DTD_BASE);
	}
	
	public String getName() {
		return GROUP_NAME;
	}

	public String getValue(String key) {
		if (key.equals(RANDOM_SEED)) {
			return Long.toString(getRandomSeed());
		} else if (key.equals(TIME_FORMAT)) {
			return getTimeFormat();
		} else if (key.equals(LOCAL_DTD_BASE)) {
			return getLocalDtdBase();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public void setValue(String key, String value) {
		if (key.equals(RANDOM_SEED)) {
			setRandomSeed(Long.parseLong(value));
		} else if (key.equals(TIME_FORMAT)) {
			setTimeFormat(value);
		} else if (key.equals(LOCAL_DTD_BASE)) {
			setLocalDtdBase(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public Set<String> paramKeySet() {
		return keyset;
	}
	
	public Set<String> listKeySet() {
		return new LinkedHashSet<String>();
	}
	
	public ConfigListI getList(String key) {
		throw new UnsupportedOperationException();
	}

	/* direct access */
	
	public long getRandomSeed() {
		return this.randomSeed;
	}

	public void setRandomSeed(long seed) {
		if (this.randomSeetSet) {
			throw new UnsupportedOperationException("The randomSeed can only be set once");
		}
		this.randomSeed = seed;
		this.randomSeetSet = true;
	}
	
	public String getTimeFormat() {
		return this.timeFormat;
	}
	
	public void setTimeFormat(String format) {
		this.timeFormat = format;
	}
	
	public String getLocalDtdBase() {
		return this.localDtdBase;
	}
	
	public void setLocalDtdBase(String dtdBase) {
		this.localDtdBase = dtdBase;
	}
	
}
