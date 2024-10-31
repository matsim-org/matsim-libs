package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.messages.VehicleMsg;
import org.matsim.dsim.simulation.SimPerson;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.Objects;

public class SimVehicleProvider {

    private final double stuckThreshold;
    private final QSimConfigGroup.VehiclesSource vehiclesSource;
    private final double flowCapacityFactor;

    private final Vehicles vehicles;
    private final EventsManager em;

    SimVehicleProvider(Scenario scenario, EventsManager em) {
        stuckThreshold = scenario.getConfig().qsim().getStuckTime();
        vehicles = scenario.getVehicles();
        vehiclesSource = scenario.getConfig().qsim().getVehiclesSource();
        this.em = em;
        this.flowCapacityFactor = scenario.getConfig().qsim().getFlowCapFactor();
    }

    SimVehicle unparkVehicle(SimPerson driver, double now) {

        // simply generate a new vehicle. This could be more elaborate, but should do for now.
        Vehicle vehicle = Objects.requireNonNull(getVehicle(driver.getId(), driver.getCurrentLeg().getMode()),
                () -> "No vehicle found for person " + driver.getId() + " and mode " + driver.getCurrentLeg().getMode());

        double pce = vehicle.getType().getPcuEquivalents() / flowCapacityFactor;
        double maxV = vehicle.getType().getMaximumVelocity();
        em.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));
        return new BasicSimVehicle(vehicle.getId(), driver, pce, maxV, stuckThreshold);
    }

    SimPerson parkVehicle(SimVehicle vehicle, double now) {

        var driver = vehicle.getDriver();

        em.processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), vehicle.getId()));
        return driver;
    }

    SimVehicle vehicleFromMessage(VehicleMsg vehicleMessage) {
        var driver = new SimPerson(vehicleMessage.getDriver());
        return new BasicSimVehicle(
                vehicleMessage.getId(),
                driver,
                vehicleMessage.getPce(),
                vehicleMessage.getMaxV(),
                stuckThreshold
        );
    }

    private Vehicle getVehicle(Id<Person> personId, String mode) {

        return switch (vehiclesSource) {
            case defaultVehicle, modeVehicleTypesFromVehiclesData -> {
                Id<Vehicle> modeId = Id.createVehicleId(personId.toString() + "_" + mode);
                Vehicle modeVehicle = vehicles.getVehicles().get(modeId);
                yield modeVehicle != null ? modeVehicle : vehicles.getVehicles().get(Id.createVehicleId(personId.toString()));
            }
            case fromVehiclesData ->
                    throw new RuntimeException("Config:qsim.vehiclesSource=fromVehiclesData is not yet implemented. You can use 'defaultVehicle' or 'modeVehicleTypesFromVehiclesData'");
        };
    }
}
