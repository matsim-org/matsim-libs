/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorConfigGroup
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.trafficmonitoring;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;


/**
 * @author dgrether
 *
 */
public class TravelTimeCalculatorConfigGroup extends Module {
	
	private static final long serialVersionUID = 1L;

	public static final String GROUPNAME = "travelTimeCalculator";

	public enum TravelTimeCalculatorType {};
	
	
	private static final String TRAVEL_TIME_CALCULATOR = "travelTimeCalculator";
	private static final String TRAVEL_TIME_BIN_SIZE = "travelTimeBinSize";
	private static final String TRAVEL_TIME_AGGREGATOR = "travelTimeAggregator";
	
	private static final String CALCULATE_LINK_TRAVELTIMES = "calculateLinkTravelTimes";
	private static final String CALCULATE_LINKTOLINK_TRAVELTIMES = "calculateLinkToLinkTravelTimes";
	
	
	
	private String travelTimeCalculator = "TravelTimeCalculatorArray";
	private String travelTimeAggregator = "optimistic";
	private int traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times

	private boolean calculateLinkTravelTimes = true;
	private boolean calculateLinkToLinkTravelTimes = false;
	
	/**
	 * @param name
	 */
	public TravelTimeCalculatorConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	public String getValue(final String key){
		if (TRAVEL_TIME_CALCULATOR.equals(key)) {
			return getTravelTimeCalculatorType();
		} else if (TRAVEL_TIME_AGGREGATOR.equals(key)) {
			return getTravelTimeAggregatorType();
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			return Integer.toString(getTraveltimeBinSize());
		} else if (CALCULATE_LINK_TRAVELTIMES.equals(key)){
			return Boolean.toString(isCalculateLinkTravelTimes());
		} else if (CALCULATE_LINKTOLINK_TRAVELTIMES.equals(key)){
			return Boolean.toString(isCalculateLinkToLinkTravelTimes());
		}
		else {
			throw new IllegalArgumentException(key);
		}	
	}
	
	@Override
	public void addParam(final String key, final String value) {
		if (TRAVEL_TIME_CALCULATOR.equals(key)) {
			setTravelTimeCalculatorType(value);
		} else if (TRAVEL_TIME_AGGREGATOR.equals(key)) {
			setTravelTimeAggregatorType(value);
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			setTraveltimeBinSize(Integer.parseInt(value));
		} else if (CALCULATE_LINK_TRAVELTIMES.equals(key)){
			this.setCalculateLinkTravelTimes(Boolean.parseBoolean(value));
		} else if (CALCULATE_LINKTOLINK_TRAVELTIMES.equals(key)){
			this.setCalculateLinkToLinkTravelTimes(Boolean.parseBoolean(value));
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(TRAVEL_TIME_CALCULATOR, getValue(TRAVEL_TIME_CALCULATOR));
		map.put(TRAVEL_TIME_AGGREGATOR, getValue(TRAVEL_TIME_AGGREGATOR));
		map.put(TRAVEL_TIME_BIN_SIZE, getValue(TRAVEL_TIME_BIN_SIZE));	
		map.put(CALCULATE_LINK_TRAVELTIMES, getValue(CALCULATE_LINK_TRAVELTIMES));
		map.put(CALCULATE_LINKTOLINK_TRAVELTIMES, getValue(CALCULATE_LINKTOLINK_TRAVELTIMES));
		return map;
	}
	
	@Override
	protected final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(TRAVEL_TIME_BIN_SIZE, "The size of the time bin (in sec) into which the link travel times are aggregated for the router") ;
		return map;
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

	
	public boolean isCalculateLinkTravelTimes() {
		return calculateLinkTravelTimes;
	}

	
	public void setCalculateLinkTravelTimes(boolean calculateLinkTravelTimes) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
	}

	
	public boolean isCalculateLinkToLinkTravelTimes() {
		return calculateLinkToLinkTravelTimes;
	}

	
	public void setCalculateLinkToLinkTravelTimes(
			boolean calculateLinkToLinkTravelTimes) {
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
	}

}	
	
