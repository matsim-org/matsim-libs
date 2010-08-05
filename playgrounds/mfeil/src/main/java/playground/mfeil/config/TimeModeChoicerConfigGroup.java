/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChoicerConfigGroup.java
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


public class TimeModeChoicerConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "TimeModeChoicer";

	/* Name of parameters */
	private static final String POSSIBLE_MODES = "possible_modes";
	private static final String NEIGHBOURHOOD_SIZE = "neighbourhood_size";
	private static final String MAX_ITERATIONS = "max_iterations";
	private static final String STOP_CRITERION = "stop_criterion";
	private static final String OFFSET = "offset";
	private static final String MAXIMUM_WALKING_DISTANCE = "maximum_walking_distance";
	private static final String MODE_CHOICE = "mode_choice";
	private static final String MINTIME_HOME = "mintime_home";
	private static final String MINTIME_WORK = "mintime_work";
	private static final String MINTIME_SHOPPING = "mintime_shopping";
	private static final String MINTIME_LEISURE = "mintime_leisure";
	private static final String MINTIME_EDUCATION = "mintime_education";


	//default values
	// TODO all "static" to be removed later, only bypassing solution
	private static String possible_modes = "car,pt,bike,walk";
	private static String neighbourhood_size = "10";
	private static String max_iterations = "30";
	private static String stop_criterion = "5";
	private static String offset = "1800";
	private static String maximum_walking_distance = "2000";
	private static String mode_choice = "standard";
	private static String mintime_home = "3600";
	private static String mintime_work = "3600";
	private static String mintime_shopping = "1800";
	private static String mintime_leisure = "3600";
	private static String mintime_education = "3600";


	private final static Logger log = Logger.getLogger(TimeModeChoicerConfigGroup.class);


	public TimeModeChoicerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (POSSIBLE_MODES.equals(key)) {
			return possible_modes;
		}
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
		if (MAXIMUM_WALKING_DISTANCE.equals(key)) {
			return getModeChoice();
		}
		if (MODE_CHOICE.equals(key)) {
			return getMaximumWalkingDistance();
		}
		if (MINTIME_HOME.equals(key)) {
			return getMaximumWalkingDistance();
		}
		if (MINTIME_WORK.equals(key)) {
			return getMaximumWalkingDistance();
		}
		if (MINTIME_SHOPPING.equals(key)) {
			return getMaximumWalkingDistance();
		}
		if (MINTIME_LEISURE.equals(key)) {
			return getMaximumWalkingDistance();
		}
		if (MINTIME_EDUCATION.equals(key)) {
			return getMaximumWalkingDistance();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {

		if (POSSIBLE_MODES.equals(key)) {
			setPossibleModes(value); // no quality check yet
		}

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

		} else if (MAXIMUM_WALKING_DISTANCE.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MAXIMUM_WALKING_DISTANCE has been set to "+value+"m but must be equal to or greater than 1m. Default TimeModeChoicer will be used instead.");
			}
			else {
				setMaximumWalkingDistance(value);
			}

		} else if (MODE_CHOICE.equals(key)) {
			if (value.equals("standard") || value.equals("extended_1") || value.equals("extended_2") || value.equals("extended_3") || value.equals("none")) {
				setModeChoice(value);
			}
			else {
				log.warn(value+" is no valid MODE_CHOICE. Standard mode choice will be used instead.");
			}
		} else if (MINTIME_HOME.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MINTIME_HOME has been set to "+value+" seconds but must be equal to or greater than 1 second. Default value of 3600 seconds will be used instead.");
			}
			else {
				setMintimeHome(value);
			}
		} else if (MINTIME_WORK.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MINTIME_WORK has been set to "+value+" seconds but must be equal to or greater than 1 second. Default value of 3600 seconds will be used instead.");
			}
			else {
				setMintimeHome(value);
			}
		} else if (MINTIME_SHOPPING.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MINTIME_SHOPPING has been set to "+value+" seconds but must be equal to or greater than 1 second. Default value of 1800 seconds will be used instead.");
			}
			else {
				setMintimeHome(value);
			}
		} else if (MINTIME_LEISURE.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MINTIME_LEISURE has been set to "+value+" seconds but must be equal to or greater than 1 second. Default value of 3600 seconds will be used instead.");
			}
			else {
				setMintimeHome(value);
			}
		} else if (MINTIME_EDUCATION.equals(key)) {
			if (Double.parseDouble(value) < 1) {
				log.warn("Parameter MINTIME_EDUCATION has been set to "+value+" seconds but must be equal to or greater than 1 second. Default value of 3600 seconds will be used instead.");
			}
			else {
				setMintimeHome(value);
			}
		} else throw new IllegalArgumentException(key);
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, NEIGHBOURHOOD_SIZE);
		this.addParameterToMap(map, MAX_ITERATIONS);
		this.addParameterToMap(map, STOP_CRITERION);
		this.addParameterToMap(map, OFFSET);
		this.addParameterToMap(map, MAXIMUM_WALKING_DISTANCE);
		this.addParameterToMap(map, MODE_CHOICE);
		this.addParameterToMap(map, MINTIME_HOME);
		this.addParameterToMap(map, MINTIME_WORK);
		this.addParameterToMap(map, MINTIME_SHOPPING);
		this.addParameterToMap(map, MINTIME_LEISURE);
		this.addParameterToMap(map, MINTIME_EDUCATION);
		return map;
	}

	// TODO all "static" to be removed later, only bypassing solution
	private static String[] cachedPossibleModes = null;

	public static String[] getPossibleModes() {
		if (cachedPossibleModes == null) {
			if (possible_modes == null) cachedPossibleModes = new String [0];
			else {
				String[] possibleModesStringArray = possible_modes.split(",");
				cachedPossibleModes = new String[possibleModesStringArray.length];
				for (int i=0; i < possibleModesStringArray.length; i++) {
					cachedPossibleModes[i] = possibleModesStringArray[i].intern();
				}
			}
		}
		return cachedPossibleModes;
	}
	public void setPossibleModes(final String modes){
		this.possible_modes = modes;
	}
	public static String getNeighbourhoodSize() {
		return neighbourhood_size;
	}
	public void setNeighbourhoodSize(final String size) {
		this.neighbourhood_size = size;
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
	public static String getMaximumWalkingDistance() {
		return maximum_walking_distance;
	}
	public void setMaximumWalkingDistance(final String timer) {
		this.maximum_walking_distance = timer;
	}
	public static String getModeChoice() {
		return mode_choice;
	}
	public void setModeChoice(final String finalTimer) {
		this.mode_choice = finalTimer;
	}
	public static String getMintimeHome() {
		return mintime_home;
	}
	public void setMintimeHome(final String time) {
		this.mintime_home = time;
	}
	public static String getMintimeWork() {
		return mintime_work;
	}
	public void setMintimeWork(final String time) {
		this.mode_choice = time;
	}
	public static String getMintimeShopping() {
		return mintime_shopping;
	}
	public void setMintimeShopping(final String time) {
		this.mode_choice = time;
	}
	public static String getMintimeLeisure() {
		return mintime_leisure;
	}
	public void setMintimeLeisure(final String time) {
		this.mode_choice = time;
	}
	public static String getMintimeEducation() {
		return mintime_education;
	}
	public void setMintimeEducation(final String time) {
		this.mode_choice = time;
	}
}
