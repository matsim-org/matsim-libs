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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class EvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ev";

	@SuppressWarnings("deprecation")
	public static EvConfigGroup get(Config config) {
		return (EvConfigGroup)config.getModule(GROUP_NAME);
	}

	@Parameter
	@Comment("charging will be simulated every 'chargeTimeStep'-th time step")
	// no need to simulate with 1-second time step
	@Positive
	public int chargeTimeStep = 15; // 15 s ==> 0.417% SOC when charging at 1C (i.e. full recharge in 1 hour)

	@Parameter
	@Comment("AUX discharging will be simulated every 'auxDischargeTimeStep'-th time step")
	// only used if SeparateAuxDischargingHandler is used, otherwise ignored
	@Positive
	public int auxDischargeTimeStep = 60; // 1 min

	@Parameter("minChargingTime")
	@Comment("Minimum activity duration for charging. Used in EvNetwork Routing.")
	public int minimumChargeTime = 1200;

	@Parameter("enforceChargingInteractionDuration")
	@Comment("If true, prolongs the charging interaction for the amount of time waiting in the charger queue (plus 1 second), i.e." +
		"enforces that charging interactions are undertaken as long as initially planned (by EVNetworkRoutingModule). Default is false.")
	public boolean enforceChargingInteractionDuration = false;

	@Parameter
	@Comment("Location of the chargers file")
	@NotNull
	public String chargersFile = null;

	/**
	 * @deprecated -- please avoid booleans in config.  Change to enum.  kai, apr'23
	 */
	// output
	@Parameter
	@Comment("If true, charge/SoC time profile plots will be created")
	public boolean timeProfiles = false;

	@Parameter
	@Comment("Number of individual time profiles to be created")
	@Positive
	public int numberOfIndividualTimeProfiles = 50;

	/**
	 * @deprecated -- please avoid booleans in config.  Change to enum.  kai, apr'23
	 */
	@Parameter
	@Comment("determines whether the resulting SoC at the end of the iteration X is set to be the initial SoC"
			+ "in iteration X+1 for each EV."
			+ " If set to true, bear in mind that EV might start with 0% battery charge.")
	public boolean transferFinalSoCToNextIteration = false;

	public EvConfigGroup() {
		super(GROUP_NAME);
	}
}


