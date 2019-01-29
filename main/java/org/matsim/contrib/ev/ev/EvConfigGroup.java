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

package org.matsim.contrib.ev.ev;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.net.URL;

public class EvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ev";

	@SuppressWarnings("deprecation")
	public static EvConfigGroup get(Config config) {
		return (EvConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String CHARGE_TIME_STEP = "chargeTimeStep";

	public static final String AUX_DISCHARGE_TIME_STEP = "auxDischargeTimeStep";
	public static final String AUX_DISCHARGING_SIMULATION = "auxDischargingSimulation";

	// input
	public static final String CHARGERS_FILE = "chargersFile";
	public static final String VEHICLES_FILE = "vehiclesFile";

	// output
	public static final String TIME_PROFILES = "timeProfiles";

	// no need to simulate with 1-second time step
	@Positive
	private int chargeTimeStep = 5; // 5 s ==> 0.35% SOC (fast charging, 50 kW)

	// only used if SeperateAuxDischargingHandler is used, otherwise ignored
	@Positive
	private int auxDischargeTimeStep = 60; // 1 min ==> 0.25% SOC (3 kW AUX power)

	public static enum AuxDischargingSimulation {
		// AuxDischargingHandler handles AUX consumption (every {@code auxDischargeTimeStep} seconds)
		seperateAuxDischargingHandler,
		// DriveDischargingHandler handles AUX consumption during drives
		// an external handler needs to be used to simulate AUX consumption when a vehicle is parked
		insideDriveDischargingHandler,
		// AUX consumption is not simulated directly
		// an external handler needs to be used to simulate AUX consumption
		none;
	}

	@NotNull
	private AuxDischargingSimulation auxDischargingSimulation;

	@NotNull
	private String chargersFile = null;
	@NotNull
	private String vehiclesFile = null;

	private boolean timeProfiles = false;

	public EvConfigGroup() {
		super(GROUP_NAME);
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

	@StringGetter(AUX_DISCHARGING_SIMULATION)
	public AuxDischargingSimulation getAuxDischargingSimulation() {
		return auxDischargingSimulation;
	}

	@StringSetter(AUX_DISCHARGING_SIMULATION)
	public void setAuxDischargingSimulation(AuxDischargingSimulation auxDischargingSimulation) {
		this.auxDischargingSimulation = auxDischargingSimulation;
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

	public URL getChargersFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.chargersFile);
	}

	public URL getVehiclesFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.vehiclesFile);
	}
}
