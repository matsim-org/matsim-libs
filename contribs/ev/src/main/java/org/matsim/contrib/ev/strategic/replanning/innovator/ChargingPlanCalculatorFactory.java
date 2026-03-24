package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class ChargingPlanCalculatorFactory {
    private final double estimationFactor;
    private final double simulationEndTime;
    private final String chargingMode;

    private final TimeInterpretation timeInterpretation;
    private final Network network;
    private final TravelTime travelTime;

    private final ElectricFleetSpecification electricFleetSpecification;
    private final ChargingInfrastructureSpecification chargingInfrastructureSpecification;
    private final DriveEnergyConsumption.Factory driveEnergyConsumptionFactory;
    private final ChargingPower.Factory chargingPowerFactory;

    public ChargingPlanCalculatorFactory(TimeInterpretation timeInterpretation,
            Network network, TravelTime travelTime,
            ElectricFleetSpecification electricFleetSpecification,
            ChargingInfrastructureSpecification chargingInfrastructureSpecification,
            DriveEnergyConsumption.Factory driveEnergyConsumptionFactory, ChargingPower.Factory chargingPowerFactory,
            double estimationFactor, double simulationEndTime, String chargingMode) {
        this.timeInterpretation = timeInterpretation;
        this.network = network;
        this.travelTime = travelTime;

        this.electricFleetSpecification = electricFleetSpecification;
        this.chargingInfrastructureSpecification = chargingInfrastructureSpecification;
        this.driveEnergyConsumptionFactory = driveEnergyConsumptionFactory;
        this.chargingPowerFactory = chargingPowerFactory;

        this.estimationFactor = estimationFactor;
        this.simulationEndTime = simulationEndTime;
        this.chargingMode = chargingMode;
    }

    public ChargingPlanCalculator createCalculator(Plan plan) {
        Person person = plan.getPerson();

        // VEHICLE INFORMATION

        // obtain vehicle information
        ElectricVehicleSpecification electricVehicleSpecification = electricFleetSpecification
                .getVehicleSpecifications()
                .get(VehicleUtils.getVehicleId(person, chargingMode));
        Vehicle vehicle = electricVehicleSpecification.getMatsimVehicle();

        // TODO: This is not very clean, but making this proper would require quite a
        // bit of refactoring
        ElectricVehicle electricVehicle = ElectricFleetUtils.create(electricVehicleSpecification,
                driveEnergyConsumptionFactory, ev -> (a, b, c) -> 0.0, chargingPowerFactory);

        // CALCULATE TIMES AND ENERGY CONSUMPTION
        DriveEnergyConsumption driveConsumption = electricVehicle.getDriveEnergyConsumption();

        // after each actvity (index 2 describes activity consumed between activity 2
        // and 3)
        List<Double> consumedEnergy = new LinkedList<>();
        List<Double> startTimes = new LinkedList<>();
        List<Double> endTimes = new LinkedList<>();

        TimeTracker timeTracker = new TimeTracker(timeInterpretation);

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity activity) {
                if (!TripStructureUtils.isStageActivityType(activity.getType())) {
                    startTimes.add(timeTracker.getTime().seconds());
                    timeTracker.addActivity(activity);
                    consumedEnergy.add(0.0);
                    endTimes.add(timeTracker.getTime().orElse(Double.POSITIVE_INFINITY));
                } else {
                    timeTracker.addActivity(activity);
                }
            } else if (element instanceof Leg leg && leg.getMode().equals(chargingMode)) {
                // reduce energy according to consumption model
                NetworkRoute route = (NetworkRoute) leg.getRoute();

                List<Id<Link>> linkIds = new LinkedList<>();
                linkIds.add(route.getStartLinkId());
                linkIds.addAll(route.getLinkIds());
                linkIds.add(route.getEndLinkId());

                double enterTime = timeTracker.getTime().seconds();
                double energy = 0.0;

                for (Id<Link> linkId : linkIds) {
                    if (enterTime < simulationEndTime) {
                        Link link = network.getLinks().get(linkId);
                        double traversalTime = travelTime.getLinkTravelTime(link, enterTime, person, vehicle);

                        energy += driveConsumption.calcEnergyConsumption(link, traversalTime, enterTime)
                                * estimationFactor;

                        enterTime += traversalTime;
                    }
                }

                consumedEnergy.set(consumedEnergy.size() - 1, consumedEnergy.getLast() + energy);
                timeTracker.addLeg(leg);
            }
        }

        consumedEnergy = new ArrayList<>(consumedEnergy);
        startTimes = new ArrayList<>(startTimes);
        endTimes = new ArrayList<>(endTimes);

        BatteryCharging batteryCharging = (BatteryCharging) electricVehicle.getChargingPower();

        Double maximumSoc = StrategicChargingUtils.getMaximumSoc(vehicle);

        if (maximumSoc == null) {
            maximumSoc = 1.0;
        }

        double maximumEnergy = maximumSoc * electricVehicleSpecification.getBatteryCapacity();

        return new ChargingPlanCalculator(chargingInfrastructureSpecification,
                batteryCharging, simulationEndTime, maximumEnergy,
                consumedEnergy, startTimes, endTimes);
    }
}
