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

import java.util.Map;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;


/**
 * @author dgrether
 *
 */
public final class TravelTimeCalculatorConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUPNAME = "travelTimeCalculator";

	public enum TravelTimeCalculatorType {TravelTimeCalculatorArray,TravelTimeCalculatorHashMap}

	private static final String TRAVEL_TIME_CALCULATOR = "travelTimeCalculator";
	private static final String TRAVEL_TIME_BIN_SIZE = "travelTimeBinSize";
	private static final String TRAVEL_TIME_AGGREGATOR = "travelTimeAggregator";
	private static final String TRAVEL_TIME_GETTER = "travelTimeGetter";
	private static final String MAX_TIME = "maxTime";

	private static final String CALCULATE_LINK_TRAVELTIMES = "calculateLinkTravelTimes";
	private static final String CALCULATE_LINKTOLINK_TRAVELTIMES = "calculateLinkToLinkTravelTimes";

	private static final String ANALYZEDMODES = "analyzedModes";
	private static final String FILTERMODES = "filterModes";
	private static final String SEPARATEMODES = "separateModes";

	private TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorArray;
	private String travelTimeAggregator = "optimistic";
	private String travelTimeGetter = "average";
	private int traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times
	private int maxTime = 30 * 3600;

	private boolean calculateLinkTravelTimes = true;
	private boolean calculateLinkToLinkTravelTimes = false;

	private String analyzedModes = TransportMode.car;
	private boolean filterModes = false;
	private boolean separateModes = false;

	public TravelTimeCalculatorConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(TRAVEL_TIME_BIN_SIZE, "The size of the time bin (in sec) into which the link travel times are aggregated for " +
				"the router") ;
		map.put(MAX_TIME, "The lenght (in sec) of the time period that is splited into time bins; an additional time bin is created " +
				"to aggregate all travel times collected after maxTime") ;
		map.put(TRAVEL_TIME_GETTER, "How to deal with link entry times at different positions during the time bin. Currently " +
				"supported: average, linearinterpolation");
		map.put(TRAVEL_TIME_AGGREGATOR, "How to deal with congested time bins that have no link entry events. `optimistic' " +
				"assumes free speed (too optimistic); 'experimental_LastMile' is experimental and probably too pessimistic.") ;
		map.put(ANALYZEDMODES, "Transport modes that will be respected by the travel time collector. 'car' is default, which " +
				"includes also busses from the pt simulation module. Use this parameter in combination with 'filterModes' = true!");
		map.put(FILTERMODES, "If true, link travel times from legs performed on modes not included in the 'analyzedModes' parameter are ignored.");
		map.put(SEPARATEMODES, "If true, link travel times are measured and calculated separately for each mode in analyzedModes. Other modes are ignored. If true, filterModes has no effect.");
		// === 
		StringBuilder str = new StringBuilder();
		for ( TravelTimeCalculatorType type : TravelTimeCalculatorType.values() ) {
			str.append(type.toString());
			str.append(' ');
		}
		map.put( TRAVEL_TIME_CALCULATOR, "possible values: " + str.toString());
		return map;
	}

	@StringSetter( TRAVEL_TIME_CALCULATOR )
	public void setTravelTimeCalculatorType(final String travelTimeCalculator){
		this.travelTimeCalculator = TravelTimeCalculatorType.valueOf( travelTimeCalculator ) ;
	}

	@StringGetter( TRAVEL_TIME_CALCULATOR )
	public TravelTimeCalculatorType getTravelTimeCalculatorType(){
		return this.travelTimeCalculator;
	}

	@StringSetter( TRAVEL_TIME_AGGREGATOR )
	public void setTravelTimeAggregatorType(final String travelTimeAggregator){
		this.travelTimeAggregator = travelTimeAggregator;
	}

	@StringSetter( TRAVEL_TIME_GETTER )
	public void setTravelTimeGetterType(final String travelTimeGetter){
		this.travelTimeGetter = travelTimeGetter;
	}

	@StringGetter( TRAVEL_TIME_AGGREGATOR )
	public String getTravelTimeAggregatorType(){
		return this.travelTimeAggregator;
	}

	@StringGetter( TRAVEL_TIME_GETTER )
	public String getTravelTimeGetterType(){
		return this.travelTimeGetter;
	}

	/**
	 * Sets the size of the time-window over which the travel times are accumulated and averaged.<br>
	 * Note that smaller values for the binSize increase memory consumption to store the travel times.
	 *
	 * @param binSize The size of the time-window in seconds.
	 */
	@StringSetter( TRAVEL_TIME_BIN_SIZE )
	public final void setTraveltimeBinSize(final int binSize) {
		this.traveltimeBinSize = binSize;
	}


	@StringSetter( MAX_TIME )
	public void setMaxTime(int maxTime) {
		this.maxTime = maxTime;
	}

	/**
	 * Returns the size of the time-window used to accumulate and average travel times.
	 *
	 * @return The size of the time-window in seconds.
	 */
	@StringGetter( TRAVEL_TIME_BIN_SIZE )
	public final int getTraveltimeBinSize() {
		return this.traveltimeBinSize;
	}

	@StringGetter( MAX_TIME )
	public int getMaxTime() {
		return maxTime;
	}

	@StringGetter( CALCULATE_LINK_TRAVELTIMES )
	public boolean isCalculateLinkTravelTimes() {
		return this.calculateLinkTravelTimes;
	}

	@StringSetter( CALCULATE_LINK_TRAVELTIMES )
	public void setCalculateLinkTravelTimes(final boolean calculateLinkTravelTimes) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
	}

	@StringGetter( CALCULATE_LINKTOLINK_TRAVELTIMES )
	public boolean isCalculateLinkToLinkTravelTimes() {
		return this.calculateLinkToLinkTravelTimes;
	}

	@StringSetter( CALCULATE_LINKTOLINK_TRAVELTIMES )
	public void setCalculateLinkToLinkTravelTimes(
			final boolean calculateLinkToLinkTravelTimes) {
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
	}

	@StringGetter( FILTERMODES )
	public boolean isFilterModes() {
		return this.filterModes;
	}

	@StringSetter( FILTERMODES )
	public void setFilterModes(final boolean filterModes) {
		this.filterModes = filterModes;
	}

	@StringGetter( ANALYZEDMODES )
	public String getAnalyzedModes() {
		return this.analyzedModes;
	}

	@StringSetter( ANALYZEDMODES )
	public void setAnalyzedModes(final String analyzedModes) {
//		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
		// lower case is confusing here because at other places (qsimConfigGroup, planCalcRoute), it takes mode string as it is. Amit Aug'17
		this.analyzedModes = analyzedModes;
	}

	@StringGetter(SEPARATEMODES)
	public boolean getSeparateModes() {
		return this.separateModes;
	}

	@StringSetter(SEPARATEMODES)
	public void setSeparateModes(boolean separateModes) {
		this.separateModes = separateModes;
	}

}
