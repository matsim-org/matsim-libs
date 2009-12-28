/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXConfigGroup.java
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

public class PlanomatXConfigGroup extends Module {
	
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planomatX";
	
	/* Name of parameters */
	private static final String NEIGHBOURHOOD_SIZE = "neighbourhood_size";
	private static final String MAX_ITERATIONS = "max_iterations";
	private static final String WEIGHT_CHANGE_ORDER = "weight_change_order";
	private static final String WEIGHT_CHANGE_NUMBER = "weight_change_number";
	private static final String WEIGHT_INC_NUMBER = "weight_inc_number";
	private static final String TIMER = "timer";
	private static final String FINAL_TIMER = "final_timer";
	private static final String LC_MODE = "lc_mode";
	private static final String LC_SET_SIZE = "lc_set_size";
	private static final String ACT_TYPES = "act_types";
	
	//default values
	// TODO all "static" to be removed later, only bypassing solution
	private static String neighbourhood_size = "10";
	private static String max_iterations = "20";
	private static String weight_change_order = "0.2";
	private static String weight_change_number = "0.6";
	private static String weight_inc_number = "0.5";
	private static String timer = "TimeModeChoicer";
	private static String final_timer = "none";
	private static String lc_mode = "reducedLC";
	private static String lc_set_size = "5";
	private static String act_types = "customized";
	
	private final static Logger log = Logger.getLogger(PlanomatXConfigGroup.class);
	

	public PlanomatXConfigGroup() {
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
		if (WEIGHT_CHANGE_ORDER.equals(key)) {
			return getWeightChangeOrder();
		}
		if (WEIGHT_CHANGE_NUMBER.equals(key)) {
			return getWeightChangeNumber();
		}
		if (WEIGHT_INC_NUMBER.equals(key)) {
			return getWeightIncNumber();
		}
		if (TIMER.equals(key)) {
			return getFinalTimer();
		}
		if (FINAL_TIMER.equals(key)) {
			return getTimer();
		}
		if (LC_MODE.equals(key)) {
			return getLCMode();
		}
		if (LC_SET_SIZE.equals(key)) {
			return getLCSetSize();
		}
		if (ACT_TYPES.equals(key)) {
			return getActTypes();
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
				log.warn("Parameter MAX_ITERATIONS has been set to "+value+" but must be equal to or greater than 1. The default value of 20 will be used instead.");
			}
			else {
				setMaxIterations(value);
			}
			
			
		} else if (WEIGHT_CHANGE_ORDER.equals(key)) {
			if ((Double.parseDouble(value)) < 0 || (Double.parseDouble(value)) > 1) {
				log.warn("Parameter WEIGHT_CHANGE_ORDER has been set to "+value+" but must be between 0 and 1. The default value of 0.2 will be used instead.");
			}
			else {
				setWeightChangeOrder(value);
			}
			
		} else if (WEIGHT_CHANGE_NUMBER.equals(key)) {
			if ((Double.parseDouble(value)) < 0 || (Double.parseDouble(value)) > 1) {
				log.warn("Parameter WEIGHT_CHANGE_NUMBER has been set to "+value+" but must be between 0 and 1. The default value of 0.6 will be used instead.");
			}
			else {
				setWeightChangeNumber(value);
			}
			if ((Double.parseDouble(getWeightChangeNumber())+Double.parseDouble(getWeightChangeOrder())) > 1) {
				log.warn("Sum of weights for neighbourhood creation is greater than 1 but must not exceed 1. Weights have been scaled down proportionally.");
				double sum = Double.parseDouble(value)+Double.parseDouble(getWeightChangeOrder());
				double newOrder = Double.parseDouble(getWeightChangeOrder())/sum;
				setWeightChangeOrder(newOrder+"");
				double newNumber = Double.parseDouble(value)/sum;
				setWeightChangeNumber(newNumber+"");
			}
			
		} else if (WEIGHT_INC_NUMBER.equals(key)) {
			if ((Double.parseDouble(value)) < 0 || (Double.parseDouble(value)) > 1) {
				log.warn("Parameter WEIGHT_INC_NUMBER has been set to "+value+" but must be between 0 and 1. The default value of 0.5 will be used instead.");
			}
			else {
				setWeightIncNumber(value);
			}
			
		} else if (TIMER.equals(key)) {
			if (value.equals("Planomat") || value.equals("TimeModeChoicer") || value.equals("TimeOptimizer")) {
				setTimer(value);
			}
			else {
				log.warn(value+" is no valid TIMER. Default TimeModeChoicer will be used instead.");
			}
			
		} else if (FINAL_TIMER.equals(key)) {
			if (value.equals("Planomat") || value.equals("TimeModechoicer") || value.equals("TimeOptimizer") || value.equals("none")) {
				setFinalTimer(value);
			}
			else {
				log.warn(value+" is no valid FINAL_TIMER. Final timeing will be skipped.");
			}
			
		} else if (LC_MODE.equals(key)) {
			if (value.equals("fullLC") || value.equals("reducedLC")) {
				setLCMode(value);
			}
			else {
				log.warn(value+" is no valid LC_MODE. Default reducedLC will be used instead.");
			}
			
		} else if (LC_SET_SIZE.equals(key)) {
			if (Integer.parseInt(value)<1) {
				log.warn("Parameter LC_SET_SIZE has been set to "+value+" but must be equal to or greater than 1. The default value of 1 will be used instead.");
			}
			else {
				setLCSetSize(value);
			}
			
		} else if (ACT_TYPES.equals(key)) {
			if (value.equals("knowledge") || value.equals("all") || value.equals("customized")) {
				setActTypes(value);
			}
			else {
				log.warn(value+" is no valid ACT_TYPES parameter. \"All\" activity types will be used instead.");
			}
			
		} else throw new IllegalArgumentException(key);
	}
	
	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, NEIGHBOURHOOD_SIZE);
		this.addParameterToMap(map, MAX_ITERATIONS);
		this.addParameterToMap(map, WEIGHT_CHANGE_ORDER);
		this.addParameterToMap(map, WEIGHT_CHANGE_NUMBER);
		this.addParameterToMap(map, WEIGHT_INC_NUMBER);
		this.addParameterToMap(map, TIMER);
		this.addParameterToMap(map, FINAL_TIMER);
		this.addParameterToMap(map, LC_MODE);
		this.addParameterToMap(map, LC_SET_SIZE);
		this.addParameterToMap(map, ACT_TYPES);
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
	public static String getWeightChangeOrder() {
		return weight_change_order;
	}
	public void setWeightChangeOrder(final String weight) {
		this.weight_change_order = weight;
	}
	public static String getWeightChangeNumber() {
		return weight_change_number;
	}
	public void setWeightChangeNumber(final String weight) {
		this.weight_change_number = weight;
	}
	public static String getWeightIncNumber() {
		return weight_inc_number;
	}
	public void setWeightIncNumber(String weight) {
		this.weight_inc_number = weight;
	}
	public static String getTimer() {
		return timer;
	}
	public void setTimer(String timer) {
		this.timer = timer;
	}
	public static String getFinalTimer() {
		return final_timer;
	}
	public void setFinalTimer(String finalTimer) {
		this.final_timer = finalTimer;
	}
	public static String getLCMode() {
		return lc_mode;
	}
	public void setLCMode(String mode) {
		this.lc_mode = mode;
	}
	public static String getLCSetSize() {
		return lc_set_size;
	}
	public void setLCSetSize(String size) {
		this.lc_set_size = size;
	}
	public static String getActTypes() {
		return act_types;
	}
	public void setActTypes(String size) {
		this.act_types = size;
	}
}
