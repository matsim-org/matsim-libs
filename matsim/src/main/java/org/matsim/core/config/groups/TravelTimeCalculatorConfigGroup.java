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

	private static final String CALCULATE_LINK_TRAVELTIMES = "calculateLinkTravelTimes";
	private static final String CALCULATE_LINKTOLINK_TRAVELTIMES = "calculateLinkToLinkTravelTimes";

	private static final String ANALYZEDMODES = "analyzedModes";
	private static final String FILTERMODES = "filterModes";
	private static final String SEPARATEMODES = "separateModes";

	private TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorArray;
	private String travelTimeAggregator = "optimistic";
	private String travelTimeGetter = "average";
	private int traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times

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
		map.put(TRAVEL_TIME_GETTER, "How to deal with link entry times at different positions during the time bin. Currently " +
				"supported: average, linearinterpolation");
		map.put(TRAVEL_TIME_AGGREGATOR, "How to deal with congested time bins that have no link entry events. `optimistic' " +
				"assumes free speed (too optimistic); 'experimental_LastMile' is experimental and probably too pessimistic.") ;
		map.put(ANALYZEDMODES, "Transport modes that will be respected by the travel time collector. 'car' is default, which " +
				"includes also busses from the pt simulation module. Use this parameter in combination with 'filterModes' = true!");
		map.put(FILTERMODES, "If true, link travel times from legs performed on modes not included in the 'analyzedModes' parameter are ignored.");
		map.put(SEPARATEMODES, "If true, link travel times are measured and calculated separately for each mode in analyzedModes. Other modes are ignored. If true, filterModes has no effect.");
		// === 
		String str = null ;
		for ( TravelTimeCalculatorType type : TravelTimeCalculatorType.values() ) {
			str += type.toString() + " " ;
		}
		map.put( TRAVEL_TIME_CALCULATOR, "possible values: " + str ) ;
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

	/**
	 * Returns the size of the time-window used to accumulate and average travel times.
	 *
	 * @return The size of the time-window in seconds.
	 */
	@StringGetter( TRAVEL_TIME_BIN_SIZE )
	public final int getTraveltimeBinSize() {
		return this.traveltimeBinSize;
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
		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
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
