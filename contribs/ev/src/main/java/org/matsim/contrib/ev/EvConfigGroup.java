/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Map;

public final class EvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ev";

	@SuppressWarnings("deprecation")
	public static EvConfigGroup get(Config config) {
		return (EvConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String CHARGE_TIME_STEP = "chargeTimeStep";
	static final String CHARGE_TIME_STEP_EXP = "charging will be simulated every 'chargeTimeStep'-th time step";

	public static final String AUX_DISCHARGE_TIME_STEP = "auxDischargeTimeStep";
	static final String AUX_DISCHARGE_TIME_STEP_EXP = "AUX discharging will be simulated every 'auxDischargeTimeStep'-th time step";

	public static final String MINCHARGETIME = "minChargingTime";
	static final String MINCHARGETIME_EXP = "Miinum activity duration for charging. Used in EvNetwork Routing.";

	// input
	public static final String CHARGERS_FILE = "chargersFile";
	static final String CHARGERS_FILE_EXP = "Location of the chargers file";

	public static final String VEHICLES_FILE = "vehiclesFile";
	static final String VEHICLES_FILE_EXP = "Location of the vehicles file";

	// output
	public static final String TIME_PROFILES = "timeProfiles";
	static final String TIME_PROFILES_EXP = "If true, SOC time profile plots will be created";


	// no need to simulate with 1-second time step
	@Positive
	private int chargeTimeStep = 5; // 5 s ==> 0.35% SOC (fast charging, 50 kW)

	private int minimumChargeTime = 1200;

	// only used if SeparateAuxDischargingHandler is used, otherwise ignored
	@Positive
	private int auxDischargeTimeStep = 60; // 1 min ==> 0.25% SOC (3 kW AUX power)

	@NotNull
	private String chargersFile = null;

	@NotNull
	private String vehiclesFile = null;

	private boolean timeProfiles = false;

	public EvConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(CHARGE_TIME_STEP, CHARGE_TIME_STEP_EXP);
		map.put(AUX_DISCHARGE_TIME_STEP, AUX_DISCHARGE_TIME_STEP_EXP);
		map.put(CHARGERS_FILE, CHARGERS_FILE_EXP);
		map.put(VEHICLES_FILE, VEHICLES_FILE_EXP);
		map.put(TIME_PROFILES, TIME_PROFILES_EXP);
		map.put(MINCHARGETIME, MINCHARGETIME_EXP);
		return map;
	}

	@StringGetter(CHARGE_TIME_STEP)
	public int getChargeTimeStep() {
		return chargeTimeStep;
	}

	@StringSetter(CHARGE_TIME_STEP)
	public void setChargeTimeStep(int chargeTimeStep) {
		this.chargeTimeStep = chargeTimeStep;
	}

	@StringGetter(AUX_DISCHARGE_TIME_STEP)
	public int getAuxDischargeTimeStep() {
		return auxDischargeTimeStep;
	}

	@StringSetter(AUX_DISCHARGE_TIME_STEP)
	public void setAuxDischargeTimeStep(int auxDischargeTimeStep) {
		this.auxDischargeTimeStep = auxDischargeTimeStep;
	}

	@StringGetter(CHARGERS_FILE)
	public String getChargersFile() {
		return chargersFile;
	}

	@StringSetter(CHARGERS_FILE)
	public void setChargersFile(String chargersFile) {
		this.chargersFile = chargersFile;
	}

	@StringGetter(VEHICLES_FILE)
	public String getVehiclesFile() {
		return vehiclesFile;
	}

	@StringSetter(VEHICLES_FILE)
	public void setVehiclesFile(String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
	}

	@StringGetter(TIME_PROFILES)
	public boolean getTimeProfiles() {
		return timeProfiles;
	}

	@StringSetter(TIME_PROFILES)
	public void setTimeProfiles(boolean timeProfiles) {
		this.timeProfiles = timeProfiles;
	}

	@StringSetter(MINCHARGETIME)
	public void setMinimumChargeTime(int minimumChargeTime) {
		this.minimumChargeTime = minimumChargeTime;
	}

	@StringGetter(MINCHARGETIME)
	public int getMinimumChargeTime() {
		return minimumChargeTime;
	}
}


