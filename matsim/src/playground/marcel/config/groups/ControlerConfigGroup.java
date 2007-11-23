/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerConfigGroup.java
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

public class ControlerConfigGroup implements ConfigGroupI {

	public static final String GROUP_NAME = "controler";

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private static final String FIRST_ITERATION = "firstIteration";
	private static final String LAST_ITERATION = "lastIteration";

	private String outputDirectory = "";
	private int firstIteration = 0;
	private int lastIteration = 1001;
	
	private Set<String> keyset = new LinkedHashSet<String>();

	public ControlerConfigGroup() {
		this.keyset.add(OUTPUT_DIRECTORY);
		this.keyset.add(FIRST_ITERATION);
		this.keyset.add(LAST_ITERATION);
	}

	public String getName() {
		return GROUP_NAME;
	}

	public String getValue(String key) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			return getOutputDirectory();
		} else if (FIRST_ITERATION.equals(key)) {
			return Integer.toString(getFirstIteration());
		} else if (LAST_ITERATION.equals(key)) {
			return Integer.toString(getLastIteration());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public void setValue(String key, String value) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			setOutputDirectory(value);
		} else if (FIRST_ITERATION.equals(key)) {
			setFirstIteration(Integer.parseInt(value));
		} else if (LAST_ITERATION.equals(key)) {
			setLastIteration(Integer.parseInt(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public ConfigListI getList(String key) {
		throw new IllegalArgumentException(key);
	}

	public Set<String> listKeySet() {
		return EMPTY_LIST_SET;
	}

	public Set<String> paramKeySet() {
		return this.keyset;
	}

	/* direct access */
	
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setFirstIteration(int firstIteration) {
		this.firstIteration = firstIteration;
	}

	public int getFirstIteration() {
		return firstIteration;
	}

	public void setLastIteration(int lastIteration) {
		this.lastIteration = lastIteration;
	}

	public int getLastIteration() {
		return lastIteration;
	}	

}
