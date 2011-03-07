/* *********************************************************************** *
 * project: org.matsim.*
 * JointReplanningConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.run.config;

import java.lang.String;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.core.config.Module;

/**
 * @author thibautd
 */
public class JointReplanningConfigGroup extends Module {

	private static final Logger log = Logger.getLogger(JointReplanningConfigGroup.class);

	private static final long serialVersionUID = 1L;
	public static final String GROUP_NAME = "JointReplanning";

	//parameter names
	private static final String DUMMY_PARAM = "dummy_parameter";
	private static final String NUM_TIME_INTERVALS = "num_time_intervals";
	private static final String POP_SIZE = "ga_population_size";

	//parameter values
	private int numTimeIntervals;
	private int populationSize;

	public JointReplanningConfigGroup() {
		super(GROUP_NAME);
		log.debug("joint replanning config group initialized");
	}

	/*
	 * =========================================================================
	 * base class methods
	 * =========================================================================
	 */
	@Override
	public void addParam(String param_name, String value) {
		//TODO: invoque the corresponding setters.
	}

	@Override
	public String getValue(String param_name) {
		//TODO
		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		this.addParameterToMap(map, DUMMY_PARAM);
		this.addParameterToMap(map, NUM_TIME_INTERVALS);
		return map;
	}

	/*
	 * =========================================================================
	 * getters/setters
	 * =========================================================================
	 */

	public int getNumTimeIntervals() {
		return this.numTimeIntervals;
	}

	public int getPopulationSize() {
		return this.populationSize;
	}
	
}

