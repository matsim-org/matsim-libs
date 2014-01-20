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

package playground.balac.freefloating.config;

import org.matsim.core.config.experimental.ReflectiveModule;


public class FreeFloatingConfigGroup extends ReflectiveModule {
	
	public static final String GROUP_NAME = "FreeFloating";
		
	private String travelingFreeFLoating = null;
	
	private String constantFreeFloating = null;
	
	private String vehiclelocationsInputFile = null;
	
	public FreeFloatingConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter( "travelingFreeFLoating" )
	public String getUtilityOfTravelling() {
		return this.travelingFreeFLoating;
	}

	@StringSetter( "travelingFreeFLoating" )
	public void setUtilityOfTravelling(final String travelingFreeFLoating) {
		this.travelingFreeFLoating = travelingFreeFLoating;
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
}
