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

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.ev.discharging.AttributeBasedAuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.AttributeBasedDriveEnergyConsumption;
import org.matsim.core.config.Config;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;

public final class EvConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "ev";

    public EvConfigGroup() {
        super(GROUP_NAME);
        initSingletonParameterSets();
    }

    public static EvConfigGroup get(Config config, boolean create) {
        if (!config.getModules().containsKey(GROUP_NAME)) {
            config.addModule(new EvConfigGroup());
        }

        return (EvConfigGroup) config.getModules().get(GROUP_NAME);
    }

    public static EvConfigGroup get(Config config) {
        return get(config, false);
    }

    private void initSingletonParameterSets() {
        ZoneSystemUtils.registerDefaultZoneSystems(this::addDefinition, //
                (ZoneSystemParams params) -> analysisZoneSystemParams = params, //
                () -> analysisZoneSystemParams);
    }

    @Nullable
    private ZoneSystemParams analysisZoneSystemParams;

    public ZoneSystemParams addOrGetAnalysisZoneSystemParams() {
        if (analysisZoneSystemParams == null) {
            ZoneSystemParams params = new SquareGridZoneSystemParams();
            this.addParameterSet(params);
        }

        return analysisZoneSystemParams;
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
    @Comment("If true, prolongs the charging interaction for the amount of time waiting in the charger queue (plus 1 second), i.e."
            +
            "enforces that charging interactions are undertaken as long as initially planned (by EVNetworkRoutingModule). Default is false.")
    private boolean enforceChargingInteractionDuration = false;

    @Parameter
    @Comment("Location of the chargers file")
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

    @Parameter
    @Comment("Interval at which detailed vehicle trajectories are written")
    private int writeVehicleTrajectoriesInterval = 0;

    @Parameter
    @Comment("Interval (iterations) at which detailed vehicle socs are written")
    private int writeVehicleSocInterval = 0;

    @Parameter
    @Comment("Interval (in seconds) at which detailed vehicle socs are sampled")
    private int writeVehicleSocFrequency = 600;

    @Parameter
    @Comment("Interval at which detailed charging activities are written")
    private int writeChargingActivitiesInterval = 0;

    @Parameter
    @Comment("Interval at which zonal energy demand information is written")
    private int writeZonalEnergyDemandInterval = 0;

    @Parameter
    @Comment("Interval at which chargers should be written out")
    private int writeChargersInterval = 100;

    public enum InitialSocBehavior {
        Keep, UpdateAfterIteration
    }

    @Parameter
    @Comment("determines whether the resulting SoC at the end of the iteration X is set to be the initial SoC"
            + "in iteration X+1 for each EV.")
    public InitialSocBehavior initialSocBehavior = InitialSocBehavior.Keep;

    public enum DriveEnergyConsumption {
        OhdeSlaski, AttributeBased, None
    }

    @Parameter
    @Comment("Determines how to obtain the drive energy consumption of the vehicles. OhdeSlaski is a generic model, AttributedBased requires your vehicles or vehicle types to have the " + AttributeBasedDriveEnergyConsumption.ATTRIBUTE + " attribute defined")
    private DriveEnergyConsumption driveEnergyConsumption = DriveEnergyConsumption.OhdeSlaski;

    public enum AuxEnergyConsumption {
        OhdeSlaski, AttributeBased, None
    }

    @Parameter
    @Comment("Determines how to obtain the aux energy consumption of the vehicles. OhdeSlaski is a generic model, AttributedBased requires your vehicles or vehicle types to have the " + AttributeBasedAuxEnergyConsumption.ATTRIBUTE + " attribute defined")
    private AuxEnergyConsumption auxEnergyConsumption = AuxEnergyConsumption.OhdeSlaski;

    

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

    public int getWriteVehicleTrajectoriesInterval() {
        return writeVehicleTrajectoriesInterval;
    }

    public void setWriteVehicleTrajectoriesInterval(int value) {
        this.writeVehicleTrajectoriesInterval = value;
    }

    public int getWriteChargingActivitiesInterval() {
        return writeChargingActivitiesInterval;
    }

    public void setWriteChargingActivitiesInterval(int value) {
        this.writeChargingActivitiesInterval = value;
    }

    public int getWriteVehicleSocInterval() {
        return writeVehicleSocInterval;
    }

    public void setWriteVehicleSocInterval(int value) {
        this.writeVehicleSocInterval = value;
    }

    public int getWriteVehicleSocFrequency() {
        return writeVehicleSocFrequency;
    }

    public void setWriteVehicleSocFrequency(int value) {
        this.writeVehicleSocFrequency = value;
    }

    public int getWriteZonalEnergyDemandInterval() {
        return writeZonalEnergyDemandInterval;
    }

    public void setWriteZonalEnergyDemandInterval(int value) {
        this.writeZonalEnergyDemandInterval = value;
    }

    public int getWriteChargersInterval() {
        return writeChargersInterval;
    }

    public void setWriteChargersInterval(int value) {
        this.writeChargersInterval = value;
    }

    public DriveEnergyConsumption getDriveEnergyConsumption() {
        return driveEnergyConsumption;
    }

    public void setDriveEnergyConsumption(DriveEnergyConsumption value) {
        this.driveEnergyConsumption = value;
    }

    public AuxEnergyConsumption getAuxEnergyConsumption() {
        return auxEnergyConsumption;
    }

    public void setAuxEnergyConsumption(AuxEnergyConsumption value) {
        this.auxEnergyConsumption = value;
    }
}
