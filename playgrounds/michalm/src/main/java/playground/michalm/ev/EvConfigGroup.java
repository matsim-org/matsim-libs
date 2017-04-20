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

package playground.michalm.ev;

import java.net.URL;

import org.matsim.core.config.*;

public class EvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ev";

	@SuppressWarnings("deprecation")
	public static EvConfigGroup get(Config config) {
		return (EvConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String CHARGE_TIME_STEP = "chargeTimeStep";
	public static final String AUX_DISCHARGE_TIME_STEP = "auxDischargeTimeStep";

	// input
	public static final String CHARGERS_FILE = "chargersFile";

	// output
	public static final String TIME_PROFILES = "timeProfiles";

	// no need to simulate with 1-second time step
	private int chargeTimeStep = 5; // 5 s ==> 0.35% SOC (fast charging, 50 kW)
	private int auxDischargeTimeStep = 60; // 1 min ==> 0.25% SOC (3 kW aux power)

	private String chargersFile = null;

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

	@StringGetter(CHARGERS_FILE)
	public String getChargersFile() {
		return chargersFile;
	}

	@StringSetter(CHARGERS_FILE)
	public void setChargersFile(String chargersFile) {
		this.chargersFile = chargersFile;
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
}
