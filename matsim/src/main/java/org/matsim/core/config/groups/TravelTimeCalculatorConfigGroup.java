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
package org.matsim.core.config.groups;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;


/**
 * @author dgrether
 *
 */
public class TravelTimeCalculatorConfigGroup extends ConfigGroup {

	public static final String GROUPNAME = "travelTimeCalculator";
	
	public static enum TravelTimeCalculatorType {TravelTimeCalculatorArray,TravelTimeCalculatorHashMap} ;
	
	private static final String TRAVEL_TIME_CALCULATOR = "travelTimeCalculator";
	private static final String TRAVEL_TIME_BIN_SIZE = "travelTimeBinSize";
	private static final String TRAVEL_TIME_AGGREGATOR = "travelTimeAggregator";
	private static final String TRAVEL_TIME_GETTER = "travelTimeGetter";

	private static final String CALCULATE_LINK_TRAVELTIMES = "calculateLinkTravelTimes";
	private static final String CALCULATE_LINKTOLINK_TRAVELTIMES = "calculateLinkToLinkTravelTimes";

	private static final String ANALYZEDMODES = "analyzedModes";
	private static final String FILTERMODES = "filterModes";

	private TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorArray;
	private String travelTimeAggregator = "optimistic";
	private String travelTimeGetter = "average";
	private int traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times

	private boolean calculateLinkTravelTimes = true;
	private boolean calculateLinkToLinkTravelTimes = false;

	private String analyzedModes = TransportMode.car;
	private boolean filterModes = false;
	
	public TravelTimeCalculatorConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	public String getValue(final String key){
		if (TRAVEL_TIME_CALCULATOR.equals(key)) {
			return getTravelTimeCalculatorType().toString();
		} else if (TRAVEL_TIME_AGGREGATOR.equals(key)) {
			return getTravelTimeAggregatorType();
		} else if (TRAVEL_TIME_GETTER.equals(key)) {
			return getTravelTimeGetterType();
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			return Integer.toString(getTraveltimeBinSize());
		} else if (CALCULATE_LINK_TRAVELTIMES.equals(key)){
			return Boolean.toString(isCalculateLinkTravelTimes());
		} else if (CALCULATE_LINKTOLINK_TRAVELTIMES.equals(key)){
			return Boolean.toString(isCalculateLinkToLinkTravelTimes());
		} else if (ANALYZEDMODES.equals(key)){
			return getAnalyzedModes();
		} else if (FILTERMODES.equals(key)){
			return Boolean.toString(isFilterModes());
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
		} else if (TRAVEL_TIME_GETTER.equals(key)) {
			setTravelTimeGetterType(value);
		} else if (TRAVEL_TIME_BIN_SIZE.equals(key)) {
			setTraveltimeBinSize(Integer.parseInt(value));
		} else if (CALCULATE_LINK_TRAVELTIMES.equals(key)){
			this.setCalculateLinkTravelTimes(Boolean.parseBoolean(value));
		} else if (CALCULATE_LINKTOLINK_TRAVELTIMES.equals(key)){
			this.setCalculateLinkToLinkTravelTimes(Boolean.parseBoolean(value));
		} else if (ANALYZEDMODES.equals(key)){
			this.setAnalyzedModes(value);
		} else if (FILTERMODES.equals(key)){
			this.setFilterModes(Boolean.parseBoolean(value));
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(TRAVEL_TIME_CALCULATOR, getValue(TRAVEL_TIME_CALCULATOR));
		map.put(TRAVEL_TIME_AGGREGATOR, getValue(TRAVEL_TIME_AGGREGATOR));
		map.put(TRAVEL_TIME_GETTER, getValue(TRAVEL_TIME_GETTER));
		map.put(TRAVEL_TIME_BIN_SIZE, getValue(TRAVEL_TIME_BIN_SIZE));
		map.put(CALCULATE_LINK_TRAVELTIMES, getValue(CALCULATE_LINK_TRAVELTIMES));
		map.put(CALCULATE_LINKTOLINK_TRAVELTIMES, getValue(CALCULATE_LINKTOLINK_TRAVELTIMES));
		map.put(ANALYZEDMODES, getValue(ANALYZEDMODES));
		map.put(FILTERMODES, getValue(FILTERMODES));
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(TRAVEL_TIME_BIN_SIZE, "The size of the time bin (in sec) into which the link travel times are aggregated for " +
				"the router") ;
		map.put(TRAVEL_TIME_GETTER, "How to deal with link entry times at different positions during the time bin. Currently " +
				"supported: average, linearinterpolation");
		map.put(TRAVEL_TIME_AGGREGATOR, "How to deal with congested time bins that have no link entry events. `optimistic' " +
				"assumes free speed (too optimistic); 'experimental_LastMile' is experimental and probably too pessimistic.") ;
		map.put(ANALYZEDMODES, "Transport modes that will be respected by the travel time collector. 'car' is default, which " +
				"includes also bussed from the pt simulation module. Use this parameter in combination with 'filterModes' = true!");
		map.put(FILTERMODES, "If true, link travel times from legs performed on modes not included in the 'analyzedModes' parameter are ignored.");
		// === 
		String str = null ;
		for ( TravelTimeCalculatorType type : TravelTimeCalculatorType.values() ) {
			str += type.toString() + " " ;
		}
		map.put( TRAVEL_TIME_CALCULATOR, "possible values: " + str ) ;
		return map;
	}

	public void setTravelTimeCalculatorType(final String travelTimeCalculator){
		this.travelTimeCalculator = TravelTimeCalculatorType.valueOf( travelTimeCalculator ) ;
	}

	public TravelTimeCalculatorType getTravelTimeCalculatorType(){
		return this.travelTimeCalculator;
	}

	public void setTravelTimeAggregatorType(final String travelTimeAggregator){
		this.travelTimeAggregator = travelTimeAggregator;
	}

	public void setTravelTimeGetterType(final String travelTimeGetter){
		this.travelTimeGetter = travelTimeGetter;
	}
	 
	public String getTravelTimeAggregatorType(){
		return this.travelTimeAggregator;
	}

	public String getTravelTimeGetterType(){
		return this.travelTimeGetter;
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
		return this.calculateLinkTravelTimes;
	}

	public void setCalculateLinkTravelTimes(final boolean calculateLinkTravelTimes) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
	}

	public boolean isCalculateLinkToLinkTravelTimes() {
		return this.calculateLinkToLinkTravelTimes;
	}

	public void setCalculateLinkToLinkTravelTimes(
			final boolean calculateLinkToLinkTravelTimes) {
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
	}
	
	public boolean isFilterModes() {
		return this.filterModes;
	}
	
	public void setFilterModes(final boolean filterModes) {
		this.filterModes = filterModes;
	}
	
	public String getAnalyzedModes() {
		return this.analyzedModes;
	}
	
	public void setAnalyzedModes(final String analyzedModes) {
		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
	}
	
}