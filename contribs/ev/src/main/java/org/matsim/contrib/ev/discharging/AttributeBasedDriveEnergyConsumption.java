package org.matsim.contrib.ev.discharging;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.common.base.Preconditions;

public class AttributeBasedDriveEnergyConsumption implements DriveEnergyConsumption {
    static public final String ATTRIBUTE = "driveEnergyConsumption_Wh_km";

    private final double consumption_Wh_km;

    AttributeBasedDriveEnergyConsumption(double consumption_Wh_km) {
        this.consumption_Wh_km = consumption_Wh_km;
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
        double length_km = link.getLength() * 1e-3;
        double energy_Wh = consumption_Wh_km * length_km;
        return EvUnits.kWh_to_J(energy_Wh * 1e-3);
    }

    static public class Factory implements DriveEnergyConsumption.Factory {
        @Override
        public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
            // get from vehicle
            Double consumption_Wh_km = get(electricVehicle.getVehicleSpecification().getMatsimVehicle());

            // get from vehicle type
            if (consumption_Wh_km == null) {
                consumption_Wh_km = get(electricVehicle.getVehicleSpecification().getMatsimVehicle().getType());
            }

            Preconditions.checkNotNull(consumption_Wh_km, "The attribute " + ATTRIBUTE
                    + " could neither be found in the vehicle attributes nor in the vehicle type attributes for vehicle "
                    + electricVehicle.getId());

            return new AttributeBasedDriveEnergyConsumption(consumption_Wh_km);
        }
    }

    static public Double get(Vehicle vehicle) {
        return (Double) vehicle.getAttributes().getAttribute(ATTRIBUTE);
    }

    static public void assign(Vehicle vehicle, double consumption_Wh_km) {
        vehicle.getAttributes().putAttribute(ATTRIBUTE, consumption_Wh_km);
    }

    static public Double get(VehicleType vehicleType) {
        return (Double) vehicleType.getAttributes().getAttribute(ATTRIBUTE);
    }

    static public void assign(VehicleType vehicleType, double consumption_Wh_km) {
        vehicleType.getAttributes().putAttribute(ATTRIBUTE, consumption_Wh_km);
    }
}
