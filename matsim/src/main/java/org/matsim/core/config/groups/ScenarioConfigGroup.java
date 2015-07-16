/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioConfigGroup
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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author dgrether
 *
 */
public final class ScenarioConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "scenario";

	private static final String USE_LANES = "useLanes";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String USE_TRANSIT = "useTransit";
	private static final String USE_VEHICLES = "useVehicles";
	
	private static final String message = "This switch is no longer operational.  The  file is loaded if the file name"
			+ " is different from null.  If you needed this for the creation of the container, use the ScenarioBuilder in "
			+ "ScenarioUtils.  Note that loading the file does not mean that it is used anywhere; such functionality needs to be "
			+ "switched on elsewhere (e.g. in qsim, in transit, ...).  If this does not work for you, please let us know. kai, jun'15";


	private boolean useLanes = false;
	private boolean useHouseholds = false;


	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USE_HOUSEHOLDS, "Set this parameter to true if households should be used, false if not.");
//		map.put(USE_TRANSIT, "Set this parameter to true if transit should be simulated, false if not.");
		return map;
	}

	@StringGetter( USE_LANES )
	public boolean isUseLanes() {
		return this.useLanes;
	}

	@StringSetter( USE_LANES )
	public void setUseLanes(final boolean useLanes) {
		this.useLanes = useLanes;
	}

	@StringGetter( USE_HOUSEHOLDS )
	public boolean isUseHouseholds() {
		return this.useHouseholds;
	}

	@StringSetter( USE_HOUSEHOLDS )
	public void setUseHouseholds(final boolean b) {
		this.useHouseholds = b;
	}

	@SuppressWarnings("static-method")
	@Deprecated // since jul'15
	@StringSetter( USE_VEHICLES )
	public void setUseVehicles(@SuppressWarnings("unused") final boolean b) {
		throw new RuntimeException( message ) ;
	}
	@Deprecated // since jul'15
	@StringGetter( USE_VEHICLES )
	public static boolean getUseVehicles() {
		throw new RuntimeException( message ) ;
	}
	
	@SuppressWarnings("static-method")
	@StringSetter( USE_TRANSIT )
	@Deprecated // since jul'15
	public void setUseTransit(final boolean b) {
		throw new RuntimeException("The " + USE_TRANSIT + " switch has moved to the transit section of the config file." ) ;
	}
	@SuppressWarnings("static-method")
	@StringGetter( USE_TRANSIT )
	@Deprecated // since jul'15
	public boolean getUseTransit(final boolean b) {
		throw new RuntimeException("The " + USE_TRANSIT + " switch has moved to the transit section of the config file." ) ;
	}
	

}
