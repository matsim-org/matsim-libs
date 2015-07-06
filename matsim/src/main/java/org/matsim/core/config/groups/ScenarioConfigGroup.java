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
	private static final String USE_SIGNALSYSTEMS = "useSignalsystems";
	private static final String USE_HOUSEHOLDS = "useHouseholds";
	private static final String USE_TRANSIT = "useTransit";

	private boolean useLanes = false;
	private boolean useSignalSystems = false;
	private boolean useHouseholds = false;
	private boolean useTransit = false;


	public ScenarioConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		map.put(USE_SIGNALSYSTEMS, "Set this parameter to true if signal systems should be used, false if not.");
		map.put(USE_HOUSEHOLDS, "Set this parameter to true if households should be used, false if not.");
		map.put(USE_TRANSIT, "Set this parameter to true if transit should be simulated, false if not.");
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

	@StringGetter( USE_SIGNALSYSTEMS )
	public boolean isUseSignalSystems() {
		return this.useSignalSystems;
	}

	@StringSetter( USE_SIGNALSYSTEMS )
	public void setUseSignalSystems(final boolean useSignalSystems) {
		this.useSignalSystems = useSignalSystems;
	}

	@StringGetter( USE_HOUSEHOLDS )
	public boolean isUseHouseholds() {
		return this.useHouseholds;
	}

	@StringGetter( USE_TRANSIT )
	public boolean isUseTransit() {
		return this.useTransit;
	}

	@StringSetter( USE_HOUSEHOLDS )
	public void setUseHouseholds(final boolean b) {
		this.useHouseholds = b;
	}

	@SuppressWarnings("static-method")
	@Deprecated
	public void setUseVehicles(@SuppressWarnings("unused") final boolean b) {
		throw new RuntimeException( "The setUseVehicles switch is no longer operational.  The vehicles file is loaded if the file name"
				+ " is different from null.  If you needed this for the creation of the vehicles container, use the ScenarioBuilder in "
				+ "ScenarioUtils.  If this does not work for you, please let us know. kai, jun'15" ) ;
	}

	@StringSetter( USE_TRANSIT )
    public void setUseTransit(final boolean b) {
		this.useTransit = b;
	}

}
