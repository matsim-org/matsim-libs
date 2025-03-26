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
	private String shiftInputFile;

	@Parameter
	@Comment("changeover duration in [seconds]")
	private double changeoverDuration = 900;

	@Parameter
	@Comment("maximum delay of shift assignment after start time has passed in [seconds]. If a shift can not be assigned to a vehicle until the planned start of the shift plus the defined max delay, the shift is discarded. Defaults to 0")
	private double maxUnscheduledShiftDelay = 0;

	@Parameter
	@Comment("Time of shift assignment (i.e. which vehicle carries out a specific shift) before start of shift in [seconds]")
	private double shiftScheduleLookAhead = 1800;

	@Parameter
	@Comment("Time of shift end scheduling (i.e. plan shift end location) before end of shift in [seconds]")
	private double shiftEndLookAhead = 3600;

	@Parameter
	@Comment("Time of shift end rescheduling  (i.e. check whether shift should end" +
			" at a different facility) before end of shift in [seconds]")
	private double shiftEndRescheduleLookAhead = 1800;

	@Parameter
	@Comment("Time interval for periodic check of shift end re-scheduling for shifts ending within the " +
			"'shiftEndRescheduleLookAhead' in [seconds]")
	private double updateShiftEndInterval = 60 * 3;

	@Parameter
	@Comment("set to true if shifts can start and end at in field operational facilities," +
			" false if changeover is only allowed at hubs")
	private boolean allowInFieldChangeover = true;

	//electric shifts
	@Parameter
	@Comment("defines the battery state of charge threshold at which vehicles will start charging" +
			" at hubs when not in an active shift. values between [0,1)")
	private double chargeAtHubThreshold = 0.6;

	@Parameter
	@Comment("defines the battery state of charge threshold at which vehicles will start charging" +
			" during breaks and shift changeovers. values between [0,1)")
	private double chargeDuringBreakThreshold = 0.6;

	@Parameter
	@Comment("defines the interval at which idle vehicles at operation facilities are checked for whether" +
			"they can and should start charging. In [seconds]")
	private double chargeAtHubInterval = 3 * 60;

	@Parameter
	@Comment("defines the minimum battery state of charge threshold at which vehicles are available " +
			" for shift assignment. values between [0,1)")
	private double shiftAssignmentBatteryThreshold = 0.6;

	@Parameter
	@Comment("defines the charger type that should be chosen when charging during shift break or changeover. " +
			"Defaults to '" + ChargerSpecification.DEFAULT_CHARGER_TYPE + "'")
	private String breakChargerType = ChargerSpecification.DEFAULT_CHARGER_TYPE;

	@Parameter
	@Comment("defines the charger type that should be chosen when charging inactive vehicles outside of shifts. " +
			"Defaults to '" + ChargerSpecification.DEFAULT_CHARGER_TYPE + "'")
	private String outOfShiftChargerType = ChargerSpecification.DEFAULT_CHARGER_TYPE;

	@Parameter
	@Comment("defines the logging interval in [seconds]")
	private double loggingInterval = 600;

	@Parameter
	@Comment("Defines whether vehicles should be eligible for insertion when they have a shift assigned which has not yet started. " +
			"Defaults to false. Should be set to true if used together with prebookings that are inserted before shift starts. " +
			"In this case, make sure that 'shiftScheduleLookAhead' is larger than the prebboking slack.")
	private boolean considerUpcomingShiftsForInsertion = false;

	@Parameter
	@Comment("Defines when the vehicle will head to the hub once the shift end is scheduled with the options [immediate] and [justInTime]. " +
			"immediate: the vehicle will directly head to the hub once the shift end is scheduled (and may continue service from there)." +
			"justInTime: the vehicle will stay at its current position and head to the hub just in time for the shift end."
	)
	private ShiftEndRelocationArrival shiftEndRelocationArrival = ShiftEndRelocationArrival.justInTime;

	public String getShiftInputFile() {
		return shiftInputFile;
	}

	public void setShiftInputFile(String shiftInputFile) {
		this.shiftInputFile = shiftInputFile;
	}

	public double getChangeoverDuration() {
		return changeoverDuration;
	}

	public void setChangeoverDuration(double changeoverDuration) {
		this.changeoverDuration = changeoverDuration;
	}

	public double getMaxUnscheduledShiftDelay() {
		return maxUnscheduledShiftDelay;
	}

	public void setMaxUnscheduledShiftDelay(double maxUnscheduledShiftDelay) {
		this.maxUnscheduledShiftDelay = maxUnscheduledShiftDelay;
	}

	public double getShiftScheduleLookAhead() {
		return shiftScheduleLookAhead;
	}

	public void setShiftScheduleLookAhead(double shiftScheduleLookAhead) {
		this.shiftScheduleLookAhead = shiftScheduleLookAhead;
	}

	public double getShiftEndLookAhead() {
		return shiftEndLookAhead;
	}

	public void setShiftEndLookAhead(double shiftEndLookAhead) {
		this.shiftEndLookAhead = shiftEndLookAhead;
	}

	public double getShiftEndRescheduleLookAhead() {
		return shiftEndRescheduleLookAhead;
	}

	public void setShiftEndRescheduleLookAhead(double shiftEndRescheduleLookAhead) {
		this.shiftEndRescheduleLookAhead = shiftEndRescheduleLookAhead;
	}

	public double getUpdateShiftEndInterval() {
		return updateShiftEndInterval;
	}

	public void setUpdateShiftEndInterval(double updateShiftEndInterval) {
		this.updateShiftEndInterval = updateShiftEndInterval;
	}

	public boolean isAllowInFieldChangeover() {
		return allowInFieldChangeover;
	}

	public void setAllowInFieldChangeover(boolean allowInFieldChangeover) {
		this.allowInFieldChangeover = allowInFieldChangeover;
	}

	public double getChargeAtHubThreshold() {
		return chargeAtHubThreshold;
	}

	public void setChargeAtHubThreshold(double chargeAtHubThreshold) {
		this.chargeAtHubThreshold = chargeAtHubThreshold;
	}

	public double getChargeDuringBreakThreshold() {
		return chargeDuringBreakThreshold;
	}

	public void setChargeDuringBreakThreshold(double chargeDuringBreakThreshold) {
		this.chargeDuringBreakThreshold = chargeDuringBreakThreshold;
	}

	public double getChargeAtHubInterval() {
		return chargeAtHubInterval;
	}

	public void setChargeAtHubInterval(double chargeAtHubInterval) {
		this.chargeAtHubInterval = chargeAtHubInterval;
	}

	public double getShiftAssignmentBatteryThreshold() {
		return shiftAssignmentBatteryThreshold;
	}

	public void setShiftAssignmentBatteryThreshold(double shiftAssignmentBatteryThreshold) {
		this.shiftAssignmentBatteryThreshold = shiftAssignmentBatteryThreshold;
	}

	public String getBreakChargerType() {
		return breakChargerType;
	}

	public void setBreakChargerType(String breakChargerType) {
		this.breakChargerType = breakChargerType;
	}

	public String getOutOfShiftChargerType() {
		return outOfShiftChargerType;
	}

	public void setOutOfShiftChargerType(String outOfShiftChargerType) {
		this.outOfShiftChargerType = outOfShiftChargerType;
	}

	public double getLoggingInterval() {
		return loggingInterval;
	}

	public void setLoggingInterval(double loggingInterval) {
		this.loggingInterval = loggingInterval;
	}

	public boolean isConsiderUpcomingShiftsForInsertion() {
		return considerUpcomingShiftsForInsertion;
	}

	public void setConsiderUpcomingShiftsForInsertion(boolean considerUpcomingShiftsForInsertion) {
		this.considerUpcomingShiftsForInsertion = considerUpcomingShiftsForInsertion;
	}

	public ShiftEndRelocationArrival getShiftEndRelocationArrival() {
		return shiftEndRelocationArrival;
	}

	public void setShiftEndRelocationArrival(ShiftEndRelocationArrival shiftEndRelocationArrival) {
		this.shiftEndRelocationArrival = shiftEndRelocationArrival;
	}

	public enum ShiftEndRelocationArrival {immediate, justInTime}

	public ShiftsParams() {
		super(SET_NAME);
	}

	public URL getShiftInputUrl(URL context) {
		return getShiftInputFile() == null ? null : ConfigGroup.getInputFileURL(context, getShiftInputFile());
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(getChargeAtHubThreshold() >= getShiftAssignmentBatteryThreshold(),
				"chargeAtHubThreshold must be higher than shiftAssignmentBatteryThreshold to " +
						"avoid deadlocks with undercharged vehicles in hubs.");
	}
}
