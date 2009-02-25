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

package org.matsim.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;

public class ControlerConfigGroup extends Module {

	public enum RoutingAlgorithmType {Dijkstra, AStarLandmarks};
	
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(ControlerConfigGroup.class);
	
	public static final String GROUP_NAME = "controler";

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private static final String FIRST_ITERATION = "firstIteration";
	private static final String LAST_ITERATION = "lastIteration";
	private static final String TRAVEL_TIME_CALCULATOR = "travelTimeCalculator";
	private static final String TRAVEL_TIME_BIN_SIZE = "travelTimeBinSize";
	private static final String TRAVEL_TIME_AGGREGATOR = "travelTimeAggregator";
	private static final String ROUTINGALGORITHMTYPE = "routingAlgorithmType";
	
	private String outputDirectory = "./output";
	private int firstIteration = 0;
	private int lastIteration = 1000;
	private String travelTimeCalculator = "TravelTimeCalculatorArray";
	private String travelTimeAggregator = "optimistic";
	private int traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times
	private RoutingAlgorithmType routingAlgorithmType = RoutingAlgorithmType.AStarLandmarks;
	
	public ControlerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			return getOutputDirectory();
		} else if (FIRST_ITERATION.equals(key)) {
			return Integer.toString(getFirstIteration());
		} else if (LAST_ITERATION.equals(key)) {
			return Integer.toString(getLastIteration());
		} else if (TRAVEL_TIME_CALCULATOR.equals(key)) {
			return getTravelTimeCalculatorType();
		} else if (TRAVEL_TIME_AGGREGATOR.equals(key)) {
			return getTravelTimeAggregatorType();
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			return Integer.toString(getTraveltimeBinSize());
		} else if (ROUTINGALGORITHMTYPE.equals(key)){
			return this.getRoutingAlgorithmType().toString();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			setOutputDirectory(value.replace('\\', '/'));
		} else if (FIRST_ITERATION.equals(key)) {
			setFirstIteration(Integer.parseInt(value));
		} else if (LAST_ITERATION.equals(key)) {
			setLastIteration(Integer.parseInt(value));
		} else if (TRAVEL_TIME_CALCULATOR.equals(key)) {
			setTravelTimeCalculatorType(value);
		} else if (TRAVEL_TIME_AGGREGATOR.equals(key)) {
			setTravelTimeAggregatorType(value);
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			setTraveltimeBinSize(Integer.parseInt(value));
		} else if (ROUTINGALGORITHMTYPE.equals(key)){
			if (RoutingAlgorithmType.Dijkstra.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
			}
			else if (RoutingAlgorithmType.AStarLandmarks.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
			}
			else {
				throw new IllegalArgumentException(value + " is not a valid parameter value for key: "+ key + " of config group " + this.GROUP_NAME);
			}
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(OUTPUT_DIRECTORY, getValue(OUTPUT_DIRECTORY));
		map.put(FIRST_ITERATION, getValue(FIRST_ITERATION));
		map.put(LAST_ITERATION, getValue(LAST_ITERATION));
		map.put(TRAVEL_TIME_CALCULATOR, getValue(TRAVEL_TIME_CALCULATOR));
		map.put(TRAVEL_TIME_AGGREGATOR, getValue(TRAVEL_TIME_AGGREGATOR));
		map.put(TRAVEL_TIME_BIN_SIZE, getValue(TRAVEL_TIME_BIN_SIZE));		
		map.put(ROUTINGALGORITHMTYPE, getValue(ROUTINGALGORITHMTYPE));
		return map;
	}
	
	@Override
	protected final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(TRAVEL_TIME_BIN_SIZE, "The size of the time bin (in sec) into which the link travel times are aggregated for the router") ;
		map.put(ROUTINGALGORITHMTYPE, "The type of routing (least cost path) algorithm used, may have the values: " + RoutingAlgorithmType.Dijkstra + " or " + RoutingAlgorithmType.AStarLandmarks);
		return map;
	}

	/* direct access */

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setFirstIteration(final int firstIteration) {
		this.firstIteration = firstIteration;
	}

	public int getFirstIteration() {
		return this.firstIteration;
	}

	public void setLastIteration(final int lastIteration) {
		this.lastIteration = lastIteration;
	}

	public int getLastIteration() {
		return this.lastIteration;
	}
	
	public void setTravelTimeCalculatorType(final String travelTimeCalculator){
		this.travelTimeCalculator = travelTimeCalculator;
	}

	public String getTravelTimeCalculatorType(){
		return this.travelTimeCalculator;
	}

	public void setTravelTimeAggregatorType(final String travelTimeAggregator){
		this.travelTimeAggregator = travelTimeAggregator;
	}

	public String getTravelTimeAggregatorType(){
		return this.travelTimeAggregator;
	}	
	
	/**
	 * Sets the size of the time-window over which the travel times are accumulated and averaged.<br>
	 * Note that smaller values for the binSize increase memory consumption to store the travel times.
	 *
	 * @param binSize The size of the time-window in seconds.
	 */
	public final void setTraveltimeBinSize(final int binSize) {
		this.traveltimeBinSize = binSize;
	}

	/**
	 * Returns the size of the time-window used to accumulate and average travel times.
	 *
	 * @return The size of the time-window in seconds.
	 */
	public final int getTraveltimeBinSize() {
		return this.traveltimeBinSize;
	}
	
	public RoutingAlgorithmType getRoutingAlgorithmType(){
		return this.routingAlgorithmType;
	}
	
	public void setRoutingAlgorithmType(RoutingAlgorithmType type){
		this.routingAlgorithmType = type;
	}
	
}
