package org.matsim.urbanEV;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;



public class UseSocOfPreviousIteration implements MobsimBeforeCleanupListener {

    ElectricFleet electricFleet;
    Scenario scenario;


    @Inject
    UseSocOfPreviousIteration (ElectricFleet electricFleet, Scenario scenario){

        this.electricFleet = electricFleet;
        this.scenario = scenario;
    }

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {

        for (Id<ElectricVehicle> electricVehicleId : electricFleet.getElectricVehicles().keySet()) {
            for (Id<VehicleType> vehicleTypeId : scenario.getVehicles().getVehicleTypes().keySet()) {
                if (electricVehicleId.toString().equals(vehicleTypeId.toString())){
                    EVUtils.setInitialEnergy(scenario.getVehicles().getVehicleTypes().get(vehicleTypeId).getEngineInformation(), electricFleet.getElectricVehicles().get(electricVehicleId).getBattery().getSoc());
                }
            }
        }
    }
}
