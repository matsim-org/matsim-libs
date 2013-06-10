/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.matsim4opus.config.modules;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author nagel
 *
 */
public class ImprovedPseudoPtConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME="improvedPseudoPt" ;

	public static final String USING_PT_STOPS = "usingPtStops";
	public static final String PT_STOPS = "ptStopsFile";
	public static final String PT_STOPS_SWITCH = "usePtStops";
	public static final String PT_TRAVEL_TIMES = "ptTravelTimesFile";
	public static final String PT_TRAVEL_DISTANCES = "ptTravelDistancesFile";
	public static final String PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH = "usingTravelTimesAndDistances";
	private String ptStopsInputFile;
	private String ptTravelTimesInputFile;
	private String ptTravelDistancesInputFile;
	private boolean usingTravelTimesAndDistances = false ;
	private boolean usingPtStops = false ;
	
	public ImprovedPseudoPtConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter(PT_STOPS)
    public void setPtStopsInputFile(String ptStops){
    	this.ptStopsInputFile = ptStops;
    }
	@StringGetter(PT_STOPS)
    public String getPtStopsInputFile(){
    	return this.ptStopsInputFile;
    }
    @StringSetter(PT_TRAVEL_TIMES)
    public void setPtTravelTimesInputFile(String ptTravelTimes){
    	this.ptTravelTimesInputFile = ptTravelTimes;
    }
    @StringGetter(PT_TRAVEL_TIMES)
    public String getPtTravelTimesInputFile(){
    	return this.ptTravelTimesInputFile;
    }
    @StringSetter(PT_TRAVEL_DISTANCES )
    public void setPtTravelDistancesInputFile(String ptTravelDistances){
    	this.ptTravelDistancesInputFile = ptTravelDistances;
    }
    @StringGetter(PT_TRAVEL_DISTANCES )
    public String getPtTravelDistancesInputFile(){
    	return this.ptTravelDistancesInputFile;
    }
    @StringSetter(PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH)
    public void setUsingTravelTimesAndDistances( boolean val ) {
    	this.usingTravelTimesAndDistances = val ;
    }
    @StringGetter(PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH)
    public boolean isUsingTravelTimesAndDistances() {
    	return this.usingTravelTimesAndDistances ;
    }
    @StringGetter(USING_PT_STOPS)
	public boolean isUsingPtStops() {
		return usingPtStops;
	}
    @StringSetter(USING_PT_STOPS)
	public void setUsingPtStops(boolean usingPtStops) {
		this.usingPtStops = usingPtStops;
	}

}
