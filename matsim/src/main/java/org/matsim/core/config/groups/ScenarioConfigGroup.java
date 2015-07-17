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
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author dgrether
 *
 */
public final class ScenarioConfigGroup extends ConfigGroup {
	public static final String GROUP_NAME = "scenario";

	private static final String USE_LANES = "useLanes";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String USE_TRANSIT = "useTransit";
	private static final String USE_VEHICLES = "useVehicles";
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( ScenarioConfigGroup.class ) ;
	
	private boolean useLanes = false;

	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USE_HOUSEHOLDS, "Deprecated, do not use.  The file is loaded when the filename is given.  Functionality needs to be switched on elsewhere.");
		map.put(USE_TRANSIT, "Deprecated, do not use.  See transit section of config file.") ; // since jul'15
		return map;
	}

	@Override
	public final void addParam(final String paramName, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		if (USE_LANES.equals(paramName)) {
			this.setUseLanes( Boolean.parseBoolean(value) );
		} else if (USE_HOUSEHOLDS.equals(paramName)) {
			this.setUseHouseholds( Boolean.parseBoolean(value) );
		} else if (USE_VEHICLES.equals(paramName)) {
			this.setUseVehicles( Boolean.parseBoolean(value) );
		} else if (USE_TRANSIT.equals(paramName)) {
			this.setUseTransit( Boolean.parseBoolean(value) );
		} else {
			throw new IllegalArgumentException("Parameter '" + paramName + "' is not supported by config group '" + GROUP_NAME + "'.");
		}
	}
	@Override
	public final String getValue(final String param_name) {
		throw new UnsupportedOperationException("Use getters for accessing values!");
	}

	@Override
	public final Map<String, String> getParams() {
		Map<String, String> params = super.getParams();

		params.put(USE_LANES, Boolean.toString( this.isUseLanes() ) ) ;

		return params;
	}

	public boolean isUseLanes() {
		return this.useLanes;
	}

	public void setUseLanes(final boolean useLanes) {
		this.useLanes = useLanes;
	}

//	public boolean isUseHouseholds() {
//		return this.useHouseholds;
//	}

	@SuppressWarnings("static-method")
	@Deprecated // since jul'15
	public void setUseHouseholds(@SuppressWarnings("unused") final boolean b) {
		throw new RuntimeException( getMessage( USE_HOUSEHOLDS ) ) ;
	}

	// if they are not in getParams, they will not be included into the config file dump.
	
	// if they are, however, in addParam, then the methods will be called (which throw exceptions).
	
	// Once the methods below are removed throughout the code, those exceptions can be moved into the addParam method.
	
	// kai, jul'15

	
	
	@SuppressWarnings("static-method")
	@Deprecated // since jul'15
	public void setUseVehicles(@SuppressWarnings("unused") final Boolean b) {
		throw new RuntimeException( getMessage( USE_VEHICLES ) ) ;
	}
	
//	@SuppressWarnings("static-method")
//	@Deprecated // since jul'15
//	public Boolean getUseVehicles() {
//		throw new RuntimeException( getMessage( USE_VEHICLES ) ) ;
//	}
	
	@SuppressWarnings("static-method")
	@Deprecated // since jul'15
	public void setUseTransit(@SuppressWarnings("unused") final Boolean b) {
		throw new RuntimeException("The " + USE_TRANSIT + " switch has moved to the transit section of the config file." ) ;
	}

//	@SuppressWarnings("static-method")
//	@Deprecated // since jul'15
//	public Boolean getUseTransit() {
//		throw new RuntimeException("The " + USE_TRANSIT + " switch has moved to the transit section of the config file." ) ;
//	}
	
	private static String getMessage( String module ) {
		return "The " + module + " switch is no longer operational.  The file is loaded if the file name"
				+ " is different from null.  If you needed this for the creation of the container, use the ScenarioBuilder in "
				+ "ScenarioUtils.  Note that loading the file does not mean that it is used anywhere; such functionality needs to be "
				+ "switched on elsewhere (e.g. in qsim, in transit, ...).  If this does not work for you, please let us know. kai, jun'15";
	}
	
}
