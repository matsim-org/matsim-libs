/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;


public class FreeFloatingConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "FreeFloating";
		
	private String travelingFreeFloating = null;
	
	private String constantFreeFloating = null;
	
	private String vehiclelocationsInputFile = null;
	
	private String timeFeeFreeFloating = null;
	
	private String timeParkingFeeFreeFloating = null;
	
	private String distanceFeeFreeFloating = null;
	
	private boolean useFeeFreeFloating = false;	
	
	private String specialTimeStart = null; //in seconds
	
	private String specialTimeEnd = null;  //in seconds
	
	private String specialTimeFee = null;

	
	public FreeFloatingConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "travelingFreeFloating" )
	public String getUtilityOfTravelling() {
		return this.travelingFreeFloating;
	}

	@StringSetter( "travelingFreeFloating" )
	public void setUtilityOfTravelling(final String travelingFreeFloating) {
		this.travelingFreeFloating = travelingFreeFloating;
	}

	@StringGetter( "constantFreeFloating" )
	public String constantFreeFloating() {
		return this.constantFreeFloating;
	}

	@StringSetter( "constantFreeFloating" )
	public void setConstantFreeFloating(final String constantFreeFloating) {
		this.constantFreeFloating = constantFreeFloating;
	}
	
	@StringGetter( "vehiclelocationsFreefloating" )
	public String getvehiclelocations() {
		return this.vehiclelocationsInputFile;
	}

	@StringSetter( "vehiclelocationsFreefloating" )
	public void setvehiclelocations(final String vehiclelocationsInputFile) {
		this.vehiclelocationsInputFile = vehiclelocationsInputFile;
	}
	
	@StringGetter( "timeFeeFreeFloating" )
	public String timeFeeFreeFloating() {
		return this.timeFeeFreeFloating;
	}

	@StringSetter( "timeFeeFreeFloating" )
	public void setTimeFeeFreeFloating(final String timeFeeFreeFloating) {
		this.timeFeeFreeFloating = timeFeeFreeFloating;
	}
	
	@StringGetter( "timeParkingFeeFreeFloating" )
	public String timeParkingFeeFreeFloating() {
		return this.timeParkingFeeFreeFloating;
	}

	@StringSetter( "timeParkingFeeFreeFloating" )
	public void setTimeParkingFeeFreeFloating(final String timeParkingFeeFreeFloating) {
		this.timeParkingFeeFreeFloating = timeParkingFeeFreeFloating;
	}
	
	@StringGetter( "distanceFeeFreeFloating" )
	public String distanceFeeFreeFloating() {
		return this.distanceFeeFreeFloating;
	}

	@StringSetter( "distanceFeeFreeFloating" )
	public void setDistanceFeeFreeFloating(final String distanceFeeFreeFloating) {
		this.distanceFeeFreeFloating = distanceFeeFreeFloating;
	}
	
	@StringGetter( "useFreeFloating" )
	public boolean useFeeFreeFloating() {
		return this.useFeeFreeFloating;
	}

	@StringSetter( "useFreeFloating" )
	public void setUseFeeFreeFloating(final boolean useFeeFreeFloating) {
		this.useFeeFreeFloating = useFeeFreeFloating;
	}
	
	@StringGetter( "specialTimeStart" )
	public String specialTimeStart() {
		return this.specialTimeStart;
	}

	@StringSetter( "specialTimeStart" )
	public void setSpecialTimeStart(final String specialTimeStart) {
		this.specialTimeStart = specialTimeStart;
	}
	
	@StringGetter( "specialTimeEnd" )
	public String specialTimeEnd() {
		return this.specialTimeEnd;
	}

	@StringSetter( "specialTimeEnd" )
	public void setSpecialTimeEnd(final String specialTimeEnd) {
		this.specialTimeEnd = specialTimeEnd;
	}
	
	@StringGetter( "specialTimeFee" )
	public String specialTimeFee() {
		return this.specialTimeFee;
	}

	@StringSetter( "specialTimeFee" )
	public void setSpecialTimeFee(final String specialTimeFee) {
		this.specialTimeFee = specialTimeFee;
	}
	
}
