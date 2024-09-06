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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;


/**
 * @author dgrether
 *
 */
public final class TravelTimeCalculatorConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = LogManager.getLogger( TravelTimeCalculatorConfigGroup.class ) ;

	public static final String GROUPNAME = "travelTimeCalculator";

	private static final String TRAVEL_TIME_BIN_SIZE = "travelTimeBinSize";
	private static final String TRAVEL_TIME_AGGREGATOR = "travelTimeAggregator";
	private static final String TRAVEL_TIME_GETTER = "travelTimeGetter";
	private static final String MAX_TIME = "maxTime";

	private static final String CALCULATE_LINK_TRAVELTIMES = "calculateLinkTravelTimes";
	private static final String CALCULATE_LINKTOLINK_TRAVELTIMES = "calculateLinkToLinkTravelTimes";

	private static final String ANALYZEDMODES = "analyzedModes";
	private static final String FILTERMODES = "filterModes";
	private static final String SEPARATEMODES = "separateModes";

	private String travelTimeAggregator = "optimistic";
	private String travelTimeGetter = "average";
	private double traveltimeBinSize = 15 * 60; // use a default of 15min time-bins for analyzing the travel times
	private int maxTime = 30 * 3600;

	private boolean calculateLinkTravelTimes = true;
	private boolean calculateLinkToLinkTravelTimes = false;

	private Set<String> analyzedModes = new LinkedHashSet<>(  ) ;
	private boolean filterModes = false;
	private boolean separateModes = true;

	public TravelTimeCalculatorConfigGroup() {
		super(GROUPNAME);
		analyzedModes.add( TransportMode.car ) ;
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
		map.put(ANALYZEDMODES, "(only for backwards compatibility; only used if " + SEPARATEMODES + "==false && + " + FILTERMODES + "==true)  Transport modes that will be " +
							 "respected by the travel time collector. 'car' is default which includes also buses from the pt simulation module.");
		map.put(FILTERMODES, "(only for backwards compatiblity; only used if " + SEPARATEMODES + "==false)  Only modes included in analyzedModes are included." ) ;
		map.put(SEPARATEMODES, "(only for backwards compatibility) If false, link travel times are measured and aggregated over all vehicles using the link." ) ;
		return map;
	}

	enum DifferentModesHandling { separateAccordingToAnalyzedModes, jointButRestrictedToAnalyzedModes, jointAndUsingAllModes }

	// ---
	@StringSetter( TRAVEL_TIME_AGGREGATOR )
	public void setTravelTimeAggregatorType(final String travelTimeAggregator){
		this.travelTimeAggregator = travelTimeAggregator;
	}

	@StringGetter( TRAVEL_TIME_AGGREGATOR )
	public String getTravelTimeAggregatorType(){
		return this.travelTimeAggregator;
	}
	// ---
	@StringSetter( TRAVEL_TIME_GETTER )
	public void setTravelTimeGetterType(final String travelTimeGetter){
		this.travelTimeGetter = travelTimeGetter;
	}
	@StringGetter( TRAVEL_TIME_GETTER )
	public String getTravelTimeGetterType(){
		return this.travelTimeGetter;
	}
	// ---
	/**
	 * Sets the size of the time-window over which the travel times are accumulated and averaged.<br>
	 * Note that smaller values for the binSize increase memory consumption to store the travel times.
	 *
	 * @param binSize The size of the time-window in seconds.
	 */
	@StringSetter( TRAVEL_TIME_BIN_SIZE )
	public final void setTraveltimeBinSize(final double binSize) {
		this.traveltimeBinSize = binSize;
	}
	/**
	 * Returns the size of the time-window used to accumulate and average travel times.
	 *
	 * @return The size of the time-window in seconds.
	 */
	@StringGetter( TRAVEL_TIME_BIN_SIZE )
	public final double getTraveltimeBinSize() {
		return this.traveltimeBinSize;
	}
	// ---
	@StringSetter( MAX_TIME )
	public void setMaxTime(int maxTime) {
		this.maxTime = maxTime;
	}
	@StringGetter( MAX_TIME )
	public int getMaxTime() {
		return maxTime;
	}
	// ---
	@StringGetter( CALCULATE_LINK_TRAVELTIMES )
	public boolean isCalculateLinkTravelTimes() {
		return this.calculateLinkTravelTimes;
	}

	@StringSetter( CALCULATE_LINK_TRAVELTIMES )
	public void setCalculateLinkTravelTimes(final boolean calculateLinkTravelTimes) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
	}
	// ---
	@StringGetter( CALCULATE_LINKTOLINK_TRAVELTIMES )
	public boolean isCalculateLinkToLinkTravelTimes() {
		return this.calculateLinkToLinkTravelTimes;
	}

	@StringSetter( CALCULATE_LINKTOLINK_TRAVELTIMES )
	public void setCalculateLinkToLinkTravelTimes( final boolean calculateLinkToLinkTravelTimes) {
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
	}
	// ---
	@StringGetter( FILTERMODES )
	public boolean isFilterModes() {
		return this.filterModes;
	}

	@StringSetter( FILTERMODES )
	public void setFilterModes(final boolean filterModes) {
		this.filterModes = filterModes;
	}
	// ---
	@StringGetter( ANALYZEDMODES )
	public String getAnalyzedModesAsString() {
		return CollectionUtils.setToString( this.analyzedModes ) ;
	}
	public Set<String> getAnalyzedModes(){
		return analyzedModes;
	}

	@StringSetter( ANALYZEDMODES )
	public void setAnalyzedModesAsString( final String analyzedModes ) {

//		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
		// lower case is confusing here because at other places (qsimConfigGroup, planCalcRoute), it takes mode string as it is. Amit Aug'17
		this.analyzedModes = CollectionUtils.stringToSet( analyzedModes ) ;
	}
	public void setAnalyzedModes( final Set<String> analyzedModes ) {
		this.analyzedModes = analyzedModes ;
	}
	// ---
	@StringGetter(SEPARATEMODES)
	public boolean getSeparateModes() {
		return this.separateModes;
	}

	@StringSetter(SEPARATEMODES)
	public void setSeparateModes(boolean separateModes) {
		this.separateModes = separateModes;
	}

}
