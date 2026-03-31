package org.matsim.contrib.ev.strategic.replanning.innovator.chargers;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.replanning.innovator.InnovationHelper;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class EnergyHelper {
    private final static AuxEnergyConsumption.Factory emptyAuxFactory = ev -> (b, t, l) -> 0.0;

    InnovationHelper innovationHelper;

    private final Battery battery;
    private final BatteryCharging batteryCharging;

    private final List<Double> energy; // before the activity

    private EnergyHelper(InnovationHelper innovationHelper, Battery battery, BatteryCharging batteryCharging,
            List<Double> energy) {
        this.battery = battery;
        this.batteryCharging = batteryCharging;
        this.energy = energy;
        this.innovationHelper = innovationHelper;
    }

    public double charge(ActivityBasedCandidate slot, ChargerSpecification charger) {
        // calculate duration of the slot
        double duration = innovationHelper.duration(slot);

        // set energy to expected soc at that point
        double expectedCharge = energy.get(innovationHelper.startActivityIndex(slot));
        expectedCharge = Math.max(0.0, expectedCharge);
        battery.setCharge(expectedCharge);

        // calculate amount of energy that can be charged
        double chargedEnergy = batteryCharging.calcEnergyCharged(charger, duration);

        // update soc for following activities
        for (int k = innovationHelper.endActivityIndex(slot); k < energy.size(); k++) {
            energy.set(k, energy.get(k) + chargedEnergy);
        }

        return chargedEnergy;
    }

    public double charge(LegBasedCandidate slot, double duration, ChargerSpecification charger) {
        int followingActivityIndex = innovationHelper.followingActivityIndex(slot);

        // set energy to expected soc at the midpoint along the leg
        double startCharge = energy.get(followingActivityIndex - 1);
        double endCharge = energy.get(followingActivityIndex);
        double expectedCharge = 0.5 * (startCharge + endCharge);

        expectedCharge = Math.max(0.0, expectedCharge);
        battery.setCharge(expectedCharge);

        // calculate amount of energy that can be charged
        double chargedEnergy = batteryCharging.calcEnergyCharged(charger, duration);

        // update soc for following activities
        for (int k = followingActivityIndex; k < energy.size(); k++) {
            energy.set(k, energy.get(k) + chargedEnergy);
        }

        return chargedEnergy;
    }

    static public class Factory {
        private final TimeInterpretation timeInterpretation;
        private final TravelTime travelTime;
        private final Network network;

        private final DriveEnergyConsumption.Factory driveFactory;
        private final ChargingPower.Factory chargingFactory;

        private final Vehicles vehicles;
        private final ElectricFleetSpecification fleet;

        private final String chargingMode;

        public Factory(TimeInterpretation timeInterpretation, TravelTime travelTime, Network network,
                DriveEnergyConsumption.Factory driveFactory,
                ChargingPower.Factory chargingFactory,
                Vehicles vehicles, ElectricFleetSpecification fleet, String chargingMode) {
            this.timeInterpretation = timeInterpretation;
            this.travelTime = travelTime;
            this.network = network;
            this.driveFactory = driveFactory;
            this.chargingFactory = chargingFactory;
            this.vehicles = vehicles;
            this.fleet = fleet;
            this.chargingMode = chargingMode;
        }

        public EnergyHelper build(Plan plan, InnovationHelper innovationHelper) {
            Vehicle vehicle = vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), chargingMode));
            ElectricVehicleSpecification specification = fleet.getVehicleSpecifications().get(vehicle.getId());

            // TODO: This is not very clean, but would need a lot of refactoring.
            ElectricVehicle electricVehicle = ElectricFleetUtils.create(specification, driveFactory, emptyAuxFactory,
                    chargingFactory);

            DriveEnergyConsumption consumption = electricVehicle.getDriveEnergyConsumption();

            // track energy
            TimeTracker timeTracker = new TimeTracker(timeInterpretation);

            List<Double> energy = new LinkedList<>(); // at the beginning of each activity

            // initialize energy
            double remainingEnergy = electricVehicle.getVehicleSpecification().getInitialSoc()
                    * electricVehicle.getBattery().getCapacity();

            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Activity activity) {
                    if (!TripStructureUtils.isStageActivityType(activity.getType())) {
                        energy.add(remainingEnergy);
                    }
                } else if (element instanceof Leg leg) {
                    if (leg.getMode().equals(chargingMode)) {
                        // reduce energy according to consumption model
                        NetworkRoute route = (NetworkRoute) leg.getRoute();

                        List<Id<Link>> linkIds = new LinkedList<>();
                        linkIds.add(route.getStartLinkId());
                        linkIds.addAll(route.getLinkIds());
                        linkIds.add(route.getEndLinkId());

                        double enterTime = timeTracker.getTime().seconds();

                        for (Id<Link> linkId : linkIds) {
                            Link link = network.getLinks().get(linkId);
                            double traversalTime = travelTime.getLinkTravelTime(link, enterTime, plan.getPerson(),
                                    vehicle);

                            remainingEnergy -= consumption.calcEnergyConsumption(link, traversalTime, enterTime);
                            enterTime += traversalTime;
                        }
                    }
                }

                timeTracker.addElement(element);
            }

            BatteryCharging batteryCharging = (BatteryCharging) electricVehicle.getChargingPower();
            Battery battery = electricVehicle.getBattery();

            return new EnergyHelper(innovationHelper, battery, batteryCharging, energy);
        }
    }
}
