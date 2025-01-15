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

import java.util.Map;

public final class EvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ev";

	//ALl previously existing parameters written in the preferred format

	// no need to simulate with 1-second time step
	public static final String CHARGE_TIME_STEP = "chargeTimeStep";
	public static final String CHARGE_TIME_STEP_COMMENT = "charging will be simulated every 'chargeTimeStep'-th time step";
	private Integer chargeTimeStep = 15; // 15 s ==> 0.417% SOC when charging at 1C (i.e. full recharge in 1 hour)

	// only used if SeparateAuxDischargingHandler is used, otherwise ignored
	public static final String AUX_DISCHARGE_TIME_STEP = "auxDischargeTimeStep";
	public static final String AUX_DISCHARGE_TIME_STEP_COMMENT = "AUX discharging will be simulated every 'auxDischargeTimeStep'-th time step";
	private Integer auxDischargeTimeStep = 60; // 1 min

	public static final String MINIMUM_CHARGE_TIME = "minimumChargeTime";
	public static final String MINIMUM_CHARGE_TIME_COMMENT = "Minimum activity duration for charging. Used in EvNetwork Routing.";
	private Integer minimumChargeTime = 1200;

	public static final String ENFORCE_CHARGING_INTERACTION_DURATION = "enforceChargingInteractionDuration";
	public static final String ENFORCE_CHARGING_INTERACTION_DURATION_COMMENT = "If true, prolongs the charging interaction for the amount of time waiting in the charger queue (plus 1 second), i.e." +
		"enforces that charging interactions are undertaken as long as initially planned (by EVNetworkRoutingModule). Default is false.";
	private Boolean enforceChargingInteractionDuration = false;

	public static final String CHARGERS_FILE = "chargersFile";
	public static final String CHARGERS_FILE_COMMENT = "Location of the chargers file";
	private String chargersFile = null;

	public static final String TIME_PROFILES = "timeProfiles";
	public static final String TIME_PROFILES_COMMENT = "If true, charge/SoC time profile plots will be created";
	private Boolean timeProfiles = false;

	public static final String NUMBER_OF_INDIVIDUAL_TIME_PROFILES = "numberOfIndividualTimeProfiles";
	public static final String NUMBER_OF_INDIVIDUAL_TIME_PROFILES_COMMENT = "Number of individual time profiles to be created";
	private Integer numberOfIndividualTimeProfiles = 50;

	public static final String TRANSFER_FINAL_SOC_TO_NEXT_ITERATION = "transferFinalSoCToNextIteration";
	public static final String TRANSFER_FINAL_SOC_TO_NEXT_ITERATION_COMMENT = "determines whether the resulting SoC at the end of the iteration X is set to be the initial SoC"
		+ "in iteration X+1 for each EV."
		+ " If set to true, bear in mind that EV might start with 0% battery charge.";
	private Boolean transferFinalSoCToNextIteration = false;


	public EvConfigGroup() { super(GROUP_NAME); }

	@Override
	public Map<String,String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CHARGE_TIME_STEP, CHARGE_TIME_STEP_COMMENT);
		comments.put(AUX_DISCHARGE_TIME_STEP, AUX_DISCHARGE_TIME_STEP_COMMENT);
		comments.put(MINIMUM_CHARGE_TIME, MINIMUM_CHARGE_TIME_COMMENT);
		comments.put(ENFORCE_CHARGING_INTERACTION_DURATION, ENFORCE_CHARGING_INTERACTION_DURATION_COMMENT);
		comments.put(CHARGERS_FILE, CHARGERS_FILE_COMMENT);
		comments.put(TIME_PROFILES, TIME_PROFILES_COMMENT);
		comments.put(NUMBER_OF_INDIVIDUAL_TIME_PROFILES, NUMBER_OF_INDIVIDUAL_TIME_PROFILES_COMMENT);
		comments.put(TRANSFER_FINAL_SOC_TO_NEXT_ITERATION, TRANSFER_FINAL_SOC_TO_NEXT_ITERATION_COMMENT);

		return comments;
	}

	//Getters and Setters of all previously existing parameters
	@StringGetter( CHARGE_TIME_STEP )
	public Integer getChargeTimeStep() { return chargeTimeStep; }

	@StringSetter( CHARGE_TIME_STEP )
	public void setChargeTimeStep(Integer chargeTimeStep) {this.chargeTimeStep = chargeTimeStep; }


	@StringGetter( AUX_DISCHARGE_TIME_STEP )
	public Integer getAuxDischargeTimeStep() { return auxDischargeTimeStep; }

	@StringSetter( AUX_DISCHARGE_TIME_STEP )
	public void setAuxDischargeTimeStep(Integer auxDischargeTimeStep) { this.auxDischargeTimeStep = auxDischargeTimeStep; }


	@StringGetter( MINIMUM_CHARGE_TIME )
	public Integer getMinimumChargeTime() { return minimumChargeTime; }

	@StringSetter( MINIMUM_CHARGE_TIME )
	public void setMinimumChargeTime(Integer minimumChargeTime) { this.minimumChargeTime = minimumChargeTime; }


	@StringGetter( ENFORCE_CHARGING_INTERACTION_DURATION )
	public Boolean getEnforceChargingInteractionDuration() { return enforceChargingInteractionDuration; }

	@StringSetter( ENFORCE_CHARGING_INTERACTION_DURATION )
	public void setEnforceChargingInteractionDuration(Boolean enforceChargingInteractionDuration) { this.enforceChargingInteractionDuration = enforceChargingInteractionDuration; }


	@StringGetter( CHARGERS_FILE )
	public String getChargersFile() { return chargersFile; }

	@StringSetter( CHARGERS_FILE )
	public void setChargersFile(String chargersFile) { this.chargersFile = chargersFile; }


	@StringGetter( TIME_PROFILES )
	public Boolean getTimeProfiles() { return timeProfiles; }

	@StringSetter( TIME_PROFILES )
	public void setTimeProfiles(Boolean timeProfiles) { this.timeProfiles = timeProfiles; }


	@StringGetter( NUMBER_OF_INDIVIDUAL_TIME_PROFILES )
	public Integer getNumberOfIndividualTimeProfiles() { return numberOfIndividualTimeProfiles; }

	@StringSetter( NUMBER_OF_INDIVIDUAL_TIME_PROFILES )
	public void setNumberOfIndividualTimeProfiles(Integer numberOfIndividualTimeProfiles) { this.numberOfIndividualTimeProfiles = numberOfIndividualTimeProfiles; }


	@StringGetter( TRANSFER_FINAL_SOC_TO_NEXT_ITERATION )
	public Boolean getTransferFinalSoCToNextIteration() { return transferFinalSoCToNextIteration; }

	@StringSetter( TRANSFER_FINAL_SOC_TO_NEXT_ITERATION )
	public void setTransferFinalSocToNextIteration(Boolean transferFinalSoCToNextIteration) { this.transferFinalSoCToNextIteration = transferFinalSoCToNextIteration; }
}


