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

 import java.util.Collections;
 import java.util.Set;
 
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ReflectiveConfigGroup;
 
 import jakarta.validation.constraints.NotNull;
 import jakarta.validation.constraints.Positive;
 
 public final class EvConfigGroup extends ReflectiveConfigGroup {
	 public static final String GROUP_NAME = "ev";
 
	 public EvConfigGroup() {
		 super(GROUP_NAME);
	 }
 
	 public static EvConfigGroup get(Config config) {
		 return (EvConfigGroup) config.getModules().get(GROUP_NAME);
	 }
 
	 @Parameter
	 @Comment("charging will be simulated every 'chargeTimeStep'-th time step")
	 // no need to simulate with 1-second time step
	 @Positive
	 private int chargeTimeStep = 15; // 15 s ==> 0.417% SOC when charging at 1C (i.e. full recharge in 1 hour)
 
	 @Parameter
	 @Comment("AUX discharging will be simulated every 'auxDischargeTimeStep'-th time step")
	 // only used if SeparateAuxDischargingHandler is used, otherwise ignored
	 @Positive
	 private int auxDischargeTimeStep = 60; // 1 min
 
	 @Parameter("minChargingTime")
	 @Comment("Minimum activity duration for charging. Used in EvNetwork Routing.")
	 private int minimumChargeTime = 1200;
 
	 @Parameter("enforceChargingInteractionDuration")
	 @Comment("If true, prolongs the charging interaction for the amount of time waiting in the charger queue (plus 1 second), i.e." +
		 "enforces that charging interactions are undertaken as long as initially planned (by EVNetworkRoutingModule). Default is false.")
	 private boolean enforceChargingInteractionDuration = false;
 
	 @Parameter
	 @Comment("Location of the chargers file")
	 @NotNull
	 private String chargersFile = null;
 
	 public enum EvAnalysisOutput {
		 TimeProfiles
	 }
 
	 @Parameter
	 @Comment("Choose which outputs should be generated")
	 private Set<EvAnalysisOutput> analysisOutputs = Collections.emptySet();
 
	 @Parameter
	 @Comment("Number of individual time profiles to be created")
	 @Positive
	 private int numberOfIndividualTimeProfiles = 50;
 
	 public enum InitialSocBehavior {
		 Keep, UpdateAfterIteration
	 }
 
	 @Parameter
	 @Comment("determines whether the resulting SoC at the end of the iteration X is set to be the initial SoC"
			 + "in iteration X+1 for each EV.")
	 public InitialSocBehavior initialSocBehavior = InitialSocBehavior.Keep;
 
	 public int getChargeTimeStep() {
		 return chargeTimeStep;
	 }
 
	 public void setChargeTimeStep(int chargeTimeStep) {
		 this.chargeTimeStep = chargeTimeStep;
	 }
 
	 public int getAuxDischargeTimeStep() {
		 return auxDischargeTimeStep;
	 }
 
	 public void setAuxDischargeTimeStep(int auxDischargeTimeStep) {
		 this.auxDischargeTimeStep = auxDischargeTimeStep;
	 }
 
	 public int getMinimumChargeTime() {
		 return minimumChargeTime;
	 }
 
	 public void setMinimumChargeTime(int minimumChargeTime) {
		 this.minimumChargeTime = minimumChargeTime;
	 }
 
	 public boolean isEnforceChargingInteractionDuration() {
		 return enforceChargingInteractionDuration;
	 }
 
	 public void setEnforceChargingInteractionDuration(boolean enforceChargingInteractionDuration) {
		 this.enforceChargingInteractionDuration = enforceChargingInteractionDuration;
	 }
 
	 public String getChargersFile() {
		 return chargersFile;
	 }
 
	 public void setChargersFile(String chargersFile) {
		 this.chargersFile = chargersFile;
	 }
 
	 public Set<EvAnalysisOutput> getAnalysisOutputs() {
		 return analysisOutputs;
	 }
 
	 public void setAnalysisOutputs(Set<EvAnalysisOutput> analysisOutputs) {
		 this.analysisOutputs = analysisOutputs;
	 }
 
	 public int getNumberOfIndividualTimeProfiles() {
		 return numberOfIndividualTimeProfiles;
	 }
 
	 public void setNumberOfIndividualTimeProfiles(int numberOfIndividualTimeProfiles) {
		 this.numberOfIndividualTimeProfiles = numberOfIndividualTimeProfiles;
	 }
 
	 public InitialSocBehavior getInitialSocBehavior() {
		 return initialSocBehavior;
	 }
 
	 public void setInitialSocBehavior(InitialSocBehavior initialSocBehavior) {
		 this.initialSocBehavior = initialSocBehavior;
	 }	
 }
 