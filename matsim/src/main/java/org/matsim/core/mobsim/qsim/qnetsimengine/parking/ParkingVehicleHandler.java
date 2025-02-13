package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;

public class ParkingVehicleHandler implements VehicleHandler {
    @Override
    public void handleVehicleDeparture(QVehicle vehicle, Link link) {

    }

    @Override
    public VehicleArrival handleVehicleArrival(QVehicle vehicle, Link link) {
        return VehicleArrival.PARKING;
    }

    @Override
    public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {

    }
}
