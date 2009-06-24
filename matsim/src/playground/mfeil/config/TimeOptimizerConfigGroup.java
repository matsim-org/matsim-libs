/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizerConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil.config;

import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

public class TimeOptimizerConfigGroup extends Module {
	
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planomatX";
	
	/* Name of parameters */
	private static final String NEIGHBOURHOOD_SIZE = "neighbourhood_size";
	private static final String MAX_ITERATIONS = "max_iterations";
	private static final String STOP_CRITERION = "stop_criterion";
	private static final String OFFSET = "offset";

	
	//default values
	// TODO all "static" to be removed later, only bypassing solution
	private static String neighbourhood_size = "10";
	private static String max_iterations = "0";
	private static String stop_criterion = "5";
	private static String offset = "900";

	
	private final static Logger log = Logger.getLogger(TimeOptimizerConfigGroup.class);
	

	public TimeOptimizerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (NEIGHBOURHOOD_SIZE.equals(key)) {
			return getNeighbourhoodSize();
		}
		if (MAX_ITERATIONS.equals(key)) {
			return getMaxIterations();
		}
		if (STOP_CRITERION.equals(key)) {
			return getStopCriterion();
		}
		if (OFFSET.equals(key)) {
			return getOffset();
		}
		throw new IllegalArgumentException(key);
	}
	
	@Override
	public void addParam(final String key, final String value) {
		if (NEIGHBOURHOOD_SIZE.equals(key)) {
			if (Integer.parseInt(value)<1) {
				log.warn("Parameter NEIGHBOURHOOD_SIZE has been set to "+value+" but must be equal to or greater than 1. The default value of 10 will be used instead.");
			}
			else {
				setNeighbourhoodSize(value);
			}
			
			
		} else if (MAX_ITERATIONS.equals(key)) {
			if (Integer.parseInt(value) < 1) {
				log.warn("Parameter MAX_ITERATIONS has been set to "+value+" but must be equal to or greater than 1. The default value of 30 will be used instead.");
			}
			else {
				setMaxIterations(value);
			}
			
			
		} else if (STOP_CRITERION.equals(key)) {
			if ((Integer.parseInt(value)) < 1) {
				log.warn("Parameter STOP_CRITERION has been set to "+value+" but must be equal to or greater than 1. The default value of 5 will be used instead.");
			}
			else {
				setStopCriterion(value);
			}
			
		} else if (OFFSET.equals(key)) {
			if ((Double.parseDouble(value)) < 1) {
				log.warn("Parameter OFFSET has been set to "+value+"sec but must be equal to or greater than 1sec. The default value of 1800sec (=1/2hour) will be used instead.");
			}
			else {
				setOffset(value);
			}
			
		} else throw new IllegalArgumentException(key);
	}
	
	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, NEIGHBOURHOOD_SIZE);
		this.addParameterToMap(map, MAX_ITERATIONS);
		this.addParameterToMap(map, STOP_CRITERION);
		this.addParameterToMap(map, OFFSET);
		return map;
	}
	
	// TODO all "static" to be removed later, only bypassing solution
	public static String getNeighbourhoodSize() {
		return neighbourhood_size;
	}
	public void setNeighbourhoodSize(final String constrained) {
		this.neighbourhood_size = constrained;
	}
	public static String getMaxIterations() {
		return max_iterations;
	}
	public void setMaxIterations(final String maxIterations) {
		this.max_iterations = maxIterations;
	}
	public static String getStopCriterion() {
		return stop_criterion;
	}
	public void setStopCriterion(final String weight) {
		this.stop_criterion = weight;
	}
	public static String getOffset() {
		return offset;
	}
	public void setOffset(final String weight) {
		this.offset = weight;
	}
}
