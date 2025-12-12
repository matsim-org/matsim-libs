package org.matsim.contrib.ev.discharging;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.common.base.Preconditions;

public class AttributeBasedAuxEnergyConsumption implements AuxEnergyConsumption {
    static public final String ATTRIBUTE = "auxEnergyConsumption_kW";

    private final double consumption_kW;

    AttributeBasedAuxEnergyConsumption(double consumption_kW) {
        this.consumption_kW = consumption_kW;
    }

    @Override
    public double calcEnergyConsumption(double beginTime, double duration, Id<Link> linkId) {
        return EvUnits.kW_to_W(consumption_kW) * duration;
    }

    static public class Factory implements AuxEnergyConsumption.Factory {
        @Override
        public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
            // get from vehicle
            Double consumption_kW = get(electricVehicle.getVehicleSpecification().getMatsimVehicle());

            // get from vehicle type
            if (consumption_kW == null) {
                consumption_kW = get(electricVehicle.getVehicleSpecification().getMatsimVehicle().getType());
            }

            Preconditions.checkNotNull(consumption_kW, "The attribute " + ATTRIBUTE
                    + " could neither be found in the vehicle attributes nor in the vehicle type attributes for vehicle "
                    + electricVehicle.getId());

            return new AttributeBasedAuxEnergyConsumption(consumption_kW);
        }
    }

    static public Double get(Vehicle vehicle) {
        return (Double) vehicle.getAttributes().getAttribute(ATTRIBUTE);
    }

    static public void assign(Vehicle vehicle, double consumption_kW) {
        vehicle.getAttributes().putAttribute(ATTRIBUTE, consumption_kW);
    }

    static public Double get(VehicleType vehicleType) {
        return (Double) vehicleType.getAttributes().getAttribute(ATTRIBUTE);
    }

    static public void assign(VehicleType vehicleType, double consumption_kW) {
        vehicleType.getAttributes().putAttribute(ATTRIBUTE, consumption_kW);
    }
}
