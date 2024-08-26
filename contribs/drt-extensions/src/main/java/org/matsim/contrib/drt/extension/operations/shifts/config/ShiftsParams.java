/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.shifts.config;

import com.google.common.base.Verify;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftsParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "drtShifts";

	@Parameter
	@Comment("path to shift xml")
	public String shiftInputFile;

	@Parameter
	@Comment("changeover duration in [seconds]")
	public double changeoverDuration = 900;

	@Parameter
	@Comment("maximum delay of shift assignment after start time has passed in [seconds]. If a shift can not be assigned to a vehicle until the planned start of the shift plus the defined max delay, the shift is discarded. Defaults to 0")
	public double maxUnscheduledShiftDelay = 0;

	@Parameter
	@Comment("Time of shift assignment (i.e. which vehicle carries out a specific shift) before start of shift in [seconds]")
	public double shiftScheduleLookAhead = 1800;

	@Parameter
	@Comment("Time of shift end scheduling (i.e. plan shift end location) before end of shift in [seconds]")
	public double shiftEndLookAhead = 3600;

	@Parameter
	@Comment("Time of shift end rescheduling  (i.e. check whether shift should end" +
			" at a different facility) before end of shift in [seconds]")
	public double shiftEndRescheduleLookAhead = 1800;

	@Parameter
	@Comment("Time interval for periodic check of shift end re-scheduling for shifts ending within the " +
			"'shiftEndRescheduleLookAhead' in [seconds]")
	public double updateShiftEndInterval = 60 * 3;

	@Parameter
	@Comment("set to true if shifts can start and end at in field operational facilities," +
			" false if changeover is only allowed at hubs")
	public boolean allowInFieldChangeover = true;

	//electric shifts
	@Parameter
	@Comment("defines the battery state of charge threshold at which vehicles will start charging" +
			" at hubs when not in an active shift. values between [0,1)")
	public double chargeAtHubThreshold = 0.6;

	@Parameter
	@Comment("defines the battery state of charge threshold at which vehicles will start charging" +
			" during breaks and shift changeovers. values between [0,1)")
	public double chargeDuringBreakThreshold = 0.6;

	@Parameter
	@Comment("defines the interval at which idle vehicles at operation facilities are checked for whether" +
			"they can and should start charging. In [seconds]")
	public double chargeAtHubInterval = 3 * 60;

	@Parameter
	@Comment("defines the minimum battery state of charge threshold at which vehicles are available " +
			" for shift assignment. values between [0,1)")
	public double shiftAssignmentBatteryThreshold = 0.6;

	@Parameter
	@Comment("defines the charger type that should be chosen when charging during shift break or changeover. " +
			"Defaults to '" + ChargerSpecification.DEFAULT_CHARGER_TYPE + "'")
	public String breakChargerType = ChargerSpecification.DEFAULT_CHARGER_TYPE;

	@Parameter
	@Comment("defines the charger type that should be chosen when charging inactive vehicles outside of shifts. " +
			"Defaults to '" + ChargerSpecification.DEFAULT_CHARGER_TYPE + "'")
	public String outOfShiftChargerType = ChargerSpecification.DEFAULT_CHARGER_TYPE;

	@Parameter
	@Comment("defines the logging interval in [seconds]")
	public double loggingInterval = 600;

	@Parameter
	@Comment("Defines whether vehicles should be eligible for insertion when they have a shift assigned which has not yet started. " +
			"Defaults to false. Should be set to true if used together with prebookings that are inserted before shift starts. " +
			"In this case, make sure that 'shiftScheduleLookAhead' is larger than the prebboking slack.")
	public boolean considerUpcomingShiftsForInsertion = false;

	public ShiftsParams() {
		super(SET_NAME);
	}

	public URL getShiftInputUrl(URL context) {
		return shiftInputFile == null ? null : ConfigGroup.getInputFileURL(context, shiftInputFile);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(chargeAtHubThreshold >= shiftAssignmentBatteryThreshold,
				"chargeAtHubThreshold must be higher than shiftAssignmentBatteryThreshold to " +
						"avoid deadlocks with undercharged vehicles in hubs.");
	}
}
