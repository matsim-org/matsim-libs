package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters.ErrorMode;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class ConstrainedChargingPlanInnovator implements ChargingPlanInnovator {
    private final static Logger logger = LogManager.getLogger(ConstrainedChargingPlanInnovator.class);

    private final ChargingPlanInnovator delegate;

    private final TimeInterpretation timeInterpretation;
    private final TravelTime travelTime;

    private final Network network;
    private final ChargingInfrastructureSpecification infrastructure;

    private final Vehicles vehicles;
    private final ElectricFleetSpecification electricFleet;

    private final String chargingMode;
    private final int iterations;
    private final ErrorMode errorMode;

    private final ChargingPower.Factory chargingPowerFactory;
    private final DriveEnergyConsumption.Factory driveEnergyConsumptionFactory;

    public ConstrainedChargingPlanInnovator(ChargingPlanInnovator delegate,
            TimeInterpretation timeInterpretation,
            TravelTime travelTime, Network network, Vehicles vehicles, ElectricFleetSpecification electricFleet,
            ChargingInfrastructureSpecification infrastructure, String chargingMode,
            int iterations, ErrorMode errorMode, ChargingPower.Factory chargingPowerFactory,
            DriveEnergyConsumption.Factory driveEnergyConsumptionFactory) {
        this.delegate = delegate;
        this.timeInterpretation = timeInterpretation;
        this.travelTime = travelTime;
        this.network = network;
        this.vehicles = vehicles;
        this.electricFleet = electricFleet;
        this.infrastructure = infrastructure;
        this.chargingMode = chargingMode;
        this.iterations = iterations;
        this.errorMode = errorMode;
        this.chargingPowerFactory = chargingPowerFactory;
        this.driveEnergyConsumptionFactory = driveEnergyConsumptionFactory;
    }

    @Override
    public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
        // obtain vehicle information
        Vehicle vehicle = vehicles.getVehicles().get(VehicleUtils.getVehicleId(person, chargingMode));
        ElectricVehicleSpecification electricVehicleSpecification = electricFleet.getVehicleSpecifications()
                .get(vehicle.getId());

        // TODO: This is not very clean, but making this proper would require quite a
        // bit of refactoring
        ElectricVehicle electricVehicle = ElectricFleetUtils.create(electricVehicleSpecification,
                driveEnergyConsumptionFactory, ev -> (a, b, c) -> 0.0, chargingPowerFactory);

        // obtain for calculation
        DriveEnergyConsumption consumption = electricVehicle.getDriveEnergyConsumption();
        BatteryCharging batteryCharging = (BatteryCharging) electricVehicle.getChargingPower();

        // reset so we don't obstruct calculation
        double batteryCapacity = electricVehicle.getBattery().getCapacity();
        electricVehicle.getBattery().setCharge(0.0);

        // obtain person preferences
        Double minimumSoc = StrategicChargingUtils.getMinimumSoc(person);
        double minimumEnergy = minimumSoc == null ? 0.0 : minimumSoc * batteryCapacity;

        Double minimumEndSoc = StrategicChargingUtils.getMinimumEndSoc(person);
        double minimumEndEnergy = minimumEndSoc == null ? 0.0 : minimumEndSoc * batteryCapacity;

        // track timing
        TimeTracker timeTracker = new TimeTracker(timeInterpretation);

        List<Double> baselineEnergy = new LinkedList<>(); // at the beginning of each activity
        List<Double> startTimes = new LinkedList<>();
        List<Double> endTimes = new LinkedList<>();

        // initialize energy
        double remainingEnergy = electricVehicle.getVehicleSpecification().getInitialSoc() * batteryCapacity;

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity activity) {
                if (!TripStructureUtils.isStageActivityType(activity.getType())) {
                    baselineEnergy.add(remainingEnergy);
                    startTimes.add(timeTracker.getTime().seconds());
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
                        double traversalTime = travelTime.getLinkTravelTime(link, enterTime, person, vehicle);

                        remainingEnergy -= consumption.calcEnergyConsumption(link, traversalTime, enterTime);
                        enterTime += traversalTime;
                    }
                }
            }

            timeTracker.addElement(element);

            if (element instanceof Activity activity) {
                if (!TripStructureUtils.isStageActivityType(activity.getType())) {
                    endTimes.add(timeTracker.getTime().orElse(Double.POSITIVE_INFINITY));
                }
            }
        }

        double baselineEndEnergy = remainingEnergy;
        baselineEnergy = new ArrayList<>(baselineEnergy);

        // now, run multiple iterations to find a feasible configuration
        ChargingPlan proposal = null;

        for (int iteration = 0; iteration < iterations; iteration++) {
            proposal = delegate.createChargingPlan(person, plan, chargingPlans);

            // copy
            List<Double> energy = new ArrayList<>(baselineEnergy);
            double endEnergy = baselineEndEnergy;

            // sorting
            List<ChargingPlanActivity> chargingActivities = new LinkedList<>();
            chargingActivities.addAll(proposal.getChargingActivities());

            Collections.sort(chargingActivities, Comparator.comparing(a -> {
                return a.isEnroute() ? //
                        startTimes.get(a.getFollowingActivityIndex() - 1) : //
                        startTimes.get(a.getStartActivityIndex());
            }));

            for (ChargingPlanActivity chargingActivity : proposal.getChargingActivities()) {
                ChargerSpecification charger = infrastructure.getChargerSpecifications()
                        .get(chargingActivity.getChargerId());

                if (!chargingActivity.isEnroute()) {
                    int startActivityIndex = chargingActivity.getStartActivityIndex();
                    int endActivityIndex = chargingActivity.getEndActivityIndex();

                    double startTime = startTimes.get(startActivityIndex);
                    double endTime = endTimes.get(endActivityIndex);
                    double chargingDuration = endTime - startTime;

                    // maximum that can be charged at that point
                    double maximumEnergy = Math.min(batteryCapacity - energy.get(startActivityIndex), batteryCapacity);

                    // what can be charged in that duration
                    double chargedEnergy = Math.min(batteryCharging.calcEnergyCharged(charger, chargingDuration),
                            maximumEnergy);

                    // bookkeeping
                    for (int k = endActivityIndex + 1; k < energy.size(); k++) {
                        energy.set(k, energy.get(k) + chargedEnergy);
                        endEnergy += chargedEnergy;
                    }
                } else {
                    int followingActivityIndex = chargingActivity.getFollowingActivityIndex();
                    double chargingDuration = chargingActivity.getDuration();

                    // assuming that we charge in the middle of the trip
                    double tripStartEnergy = energy.get(followingActivityIndex - 1);
                    double tripEndEnergy = energy.get(followingActivityIndex);
                    double referenceEnergy = 0.5 * (tripStartEnergy + tripEndEnergy);

                    // maximum that can be charged at that point
                    double maximumEnergy = Math.min(batteryCapacity - referenceEnergy, batteryCapacity);

                    // what can be charged in that duration
                    double chargedEnergy = Math.min(batteryCharging.calcEnergyCharged(charger, chargingDuration),
                            maximumEnergy);

                    // bookkeeping
                    for (int k = followingActivityIndex; k < energy.size(); k++) {
                        energy.set(k, energy.get(k) + chargedEnergy);
                        endEnergy += chargedEnergy;
                    }
                }
            }

            // now check if we adhere to the constraints
            boolean isValid = true;

            for (double state : energy) {
                isValid &= state >= minimumEnergy;
            }

            isValid &= endEnergy >= minimumEndEnergy;

            if (isValid) {
                return proposal; // we found a feasible configuration!
            }
        }

        // notify that we didn't find a feasible solution
        String message = String.format("Could not find feasible charging plan for person %s after %d iterations",
                person.getId().toString(), iterations);

        if (errorMode.equals(ErrorMode.printWarning)) {
            logger.warn(message);
        }
        if (errorMode.equals(ErrorMode.throwException)) {
            throw new IllegalStateException(message);
        }

        return proposal;
    }
}
