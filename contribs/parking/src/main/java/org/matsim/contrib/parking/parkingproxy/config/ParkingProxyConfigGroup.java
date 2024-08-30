/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

public class ParkingProxyConfigGroup extends ReflectiveConfigGroup {
	
	public static enum Iter0Method {noPenalty, hourPenalty, takeFromAttributes, estimateFromPlans}
	
	public static final String GROUP_NAME = "parkingProxy";
	public static final String ITER0 = "iter0";
	public static final String OBSERVE_ONLY = "observeOnly";
	public static final String DELAY_PER_CAR = "delayPerCar";
	public static final String MAX_DELAY = "maxDelay";
	public static final String SCALE_FACTOR = "scenarioScaleFactor";
	public static final String TIME_BIN_SIZE = "timeBinSize";
	public static final String CARS_PER_1000_PERSONS = "carsPer1000Persons";
	
	private Iter0Method iter0Method = Iter0Method.hourPenalty;
	private boolean observeOnly = false;
	private double delayPerCar = 2.5;
	private double maxDelay = 900;
	private int scenarioScaleFactor = 100;
	private int timeBinSize = 900;
	private int carsPer1000Persons = 500;

	public ParkingProxyConfigGroup() {
		super(GROUP_NAME);
	}
	
	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(SCALE_FACTOR, "The inverse of the scenario perentage, i.e. the number with which to multiply the" 
				+ " number of agents to get the real life population, e.g. 4 in a 25% scenario. Needs to be an Intger,"
				+ " so in case of weird percentages (e.g. 1/3) please round.");
		comments.put(DELAY_PER_CAR, "in seconds");
		comments.put(MAX_DELAY, "in seconds");
		comments.put(TIME_BIN_SIZE, "in seconds");
		return comments;
	}
	
	@StringGetter(ITER0)
	public Iter0Method getIter0Method() {
		return this.iter0Method;
	}
	@StringSetter(ITER0)
	public void setIter0Method(Iter0Method iter0Method) {
		this.iter0Method = iter0Method;
	}
	
	@StringGetter(OBSERVE_ONLY)
	public boolean getObserveOnly() {
		return this.observeOnly;
	}
	@StringSetter(OBSERVE_ONLY)
	public void setObserveOnly(boolean observeOnly) {
		this.observeOnly = observeOnly;
	}

	@StringGetter(DELAY_PER_CAR)
	public double getDelayPerCar() {
		return this.delayPerCar;
	}
	@StringSetter(DELAY_PER_CAR)
	public void setDelayPerCar(double delayPerCar) {
		this.delayPerCar = delayPerCar;
	}
	
	@StringGetter(MAX_DELAY)
	public double getMaxDelay() {
		return this.maxDelay;
	}
	@StringSetter(MAX_DELAY)
	public void setMaxDelay(double maxDelay) {
		this.maxDelay = maxDelay;
	}

	@StringGetter(TIME_BIN_SIZE)
	public int getTimeBinSize() {
		return timeBinSize;
	}
	@StringSetter(TIME_BIN_SIZE)
	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	@StringGetter(SCALE_FACTOR)
	public int getScenarioScaleFactor() {
		return scenarioScaleFactor;
	}
	@StringSetter(SCALE_FACTOR)
	public void setScenarioScaleFactor(int scenarioScaleFactor) {
		this.scenarioScaleFactor = scenarioScaleFactor;
	}
	
	@StringGetter(CARS_PER_1000_PERSONS)
	public int getCarsPer1000Persons() {
		return carsPer1000Persons;
	}
	@StringSetter(CARS_PER_1000_PERSONS)
	public void setCarsPer1000Persons(int carsPer1000Persons) {
		this.carsPer1000Persons = carsPer1000Persons;
	}
}
