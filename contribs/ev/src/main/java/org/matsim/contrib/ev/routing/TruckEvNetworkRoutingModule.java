/**
 * TruckEvNetworkRoutingModule
 *
 * Extends MATSim routing functionality specifically for electric trucks. Dynamically assesses routes based on energy consumption,
 * battery SOC, and driving constraints, adding necessary charging stops using radius-based charger searches.
 *
 * Key Features:
 * - Dynamic energy consumption estimation per link
 * - SOC and driving-time-based thresholds for charging
 * - Radius-based charger selection with randomness to simulate real-world variation
 *
 * Author: Mattias Ingelström
 */

package org.matsim.contrib.ev.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates the route for EV agents, potentially inserting charging stops based on battery constraints
 * and truck-specific driving rules. A key improvement over previous implementations is the dynamic
 * recalculation and update of energy consumption from each newly placed charging station to the final
 * destination. Previous implementations did not update the energy estimates downstream from inserted
 * charging stops, potentially causing inaccuracies in route planning and charging decisions.
 */

final class TruckEvNetworkRoutingModule implements RoutingModule {

    // Constants for driving logic and charger search
    private static final double TRUCK_MAX_DRIVING_TIME = 4.5 * 3600; // Max driving time in seconds before needing a break
    private static final double DEFAULT_RADIUS = 1.3 * 60 * 1000;    // Max radius (in meters) to search for chargers
    private static final double MIN_SOC_THRESHOLD = 0.15;            // Lower bound SOC threshold to trigger charging
    private static final double MAX_SOC_THRESHOLD = 0.4;             // Upper bound SOC threshold to trigger charging

	private final String mode;
    private final Network network;
    private final RoutingModule delegate;
    private final ElectricFleetSpecification electricFleet;
    private final ChargingInfrastructureSpecification chargingInfrastructureSpecification;
    private final TravelTime travelTime;
    private final DriveEnergyConsumption.Factory driveConsumptionFactory;
    private final AuxEnergyConsumption.Factory auxConsumptionFactory;
    private final EvConfigGroup evConfigGroup;
    private final String vehicleSuffix;
    private final Random random = MatsimRandom.getLocalInstance();
    private final String stageActivityModePrefix;


    TruckEvNetworkRoutingModule(final String mode, final Network network, RoutingModule delegate, ElectricFleetSpecification electricFleet, ChargingInfrastructureSpecification chargingInfrastructureSpecification, TravelTime travelTime, DriveEnergyConsumption.Factory driveConsumptionFactory, AuxEnergyConsumption.Factory auxConsumptionFactory, EvConfigGroup evConfigGroup) {
        this.travelTime = travelTime;
        Gbl.assertNotNull(network);
        this.delegate = delegate;
        this.network = network;
        this.electricFleet = electricFleet;
        this.chargingInfrastructureSpecification = chargingInfrastructureSpecification;
        this.driveConsumptionFactory = driveConsumptionFactory;
        this.auxConsumptionFactory = auxConsumptionFactory;
        this.stageActivityModePrefix = mode + " charging";
        this.evConfigGroup = evConfigGroup;
		this.mode = mode;
        this.vehicleSuffix = "_" + mode;

    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        // Get basic route (no EV logic)
        List<? extends PlanElement> basicRoute = delegate.calcRoute(request);
        Id<Vehicle> evId = Id.create(request.getPerson().getId() + vehicleSuffix, Vehicle.class);
		// Skip if the main leg is not truck mode (e.g. access/egress walk)
		List<Leg> nonWalkLegs = TripStructureUtils.getLegs(basicRoute).stream()
			.filter(leg -> !"walk".equals(leg.getMode()))
			.toList();

		Leg basicLeg = (Leg) nonWalkLegs.get(nonWalkLegs.size() - 1);

        // If person has no EV, return default route
		if (!this.mode.equals(basicLeg.getMode()) || !electricFleet.getVehicleSpecifications().containsKey(evId)) {
            return basicRoute;
        }

        ElectricVehicleSpecification evSpec = electricFleet.getVehicleSpecifications().get(evId);
        double initialSOC = evSpec.getInitialCharge();
        double capacity = evSpec.getBatteryCapacity();

        // Estimate energy and time use for original leg
        Map<Link, Double> consumptionMap = estimateConsumption(evSpec, basicLeg);

        // If trip is easily doable, skip EV-specific logic
        if ((0.8 * initialSOC) > consumptionMap.values().stream().mapToDouble(Double::doubleValue).sum()) {
            return basicRoute;
        }

        // Build charging-aware route
        return createChargingPlan(request, basicRoute, evSpec, initialSOC, capacity);
    }

    /**
     * Builds a route that includes charging stops if needed.
     */
    private List<PlanElement> createChargingPlan(RoutingRequest request,
                                                        List<? extends PlanElement> basicRoute,
                                                        ElectricVehicleSpecification evSpec,
                                                        double initialSOC, double capacity) {

        List<ChargerSpecification> selectedChargers = selectChargingStops(
                request.getFromFacility(),
                request.getToFacility(),
                evSpec,
                initialSOC,
                capacity,
                request.getPerson(),
                request.getDepartureTime(),
                request);

        if (selectedChargers.isEmpty()) {
            return new ArrayList<>(basicRoute);
        }
        return assembleRouteWithCharging(request, selectedChargers, evSpec);
    }

    /**
     * Dynamically selects charging stops along the route by recalculating energy consumption
     * from each potential charging stop onward to ensure accurate SOC and driving time estimates.
     */
    /**
     * Dynamically selects charging stops along the route by recalculating energy consumption
     * from each charging stop onward, ensuring no duplicate chargers are selected.
     */
    private List<ChargerSpecification> selectChargingStops(
            Facility fromFacility,
            Facility toFacility,
            ElectricVehicleSpecification evSpec,
            double initialSOC,
            double capacity,
            Person person,
            double departureTime,
            RoutingRequest request) {

        List<ChargerSpecification> selectedChargers = new ArrayList<>();
        Set<Id<Link>> usedChargingLinks = new HashSet<>();  // Track chargers already visited

        double currentSOC = initialSOC;
        double drivingTimeAccumulator = 0.0;
        Facility lastFacility = fromFacility;

        int maxIterations = 20;  // safety limit to avoid infinite loops, SHOULD not be needed
                                 // due to the check ensuring same stations cannot be selected
        int iterationCount = 0;

        while (iterationCount < maxIterations) {
            iterationCount++;

            // Calculate route from current position (facility) to destination
            List<? extends PlanElement> routeSegment = delegate.calcRoute(
                    DefaultRoutingRequest.of(lastFacility, toFacility, departureTime, person, request.getAttributes()));

			List<Leg> nonWalkLegs = TripStructureUtils.getLegs(routeSegment).stream()
				.filter(leg -> !"walk".equals(leg.getMode()))
				.toList();

			Leg nonWalkLeg = (Leg) nonWalkLegs.get(nonWalkLegs.size() - 1);
            Map<Link, Double> consumptionMap = estimateConsumption(evSpec, (Leg) nonWalkLeg);
            Map<Link, Double> timeMap = estimateTime((Leg) nonWalkLeg);

            double socThreshold = random.nextDouble(MIN_SOC_THRESHOLD, MAX_SOC_THRESHOLD);
            boolean chargeNeeded = false;

            for (Link link : consumptionMap.keySet()) {
                currentSOC -= consumptionMap.get(link);
                drivingTimeAccumulator += timeMap.get(link);

                if ((currentSOC / capacity) < socThreshold ||
                        (isTruck(evSpec) && drivingTimeAccumulator > TRUCK_MAX_DRIVING_TIME)) {

                    ChargerSpecification charger = findNearestCompatibleCharger(link, evSpec, usedChargingLinks);
                    if (charger == null) {
                        // No new charger found: break to avoid infinite loop
                        return selectedChargers;
                    }

                    Id<Link> chargerLinkId = charger.getLinkId();
                    usedChargingLinks.add(chargerLinkId);
                    selectedChargers.add(charger);

                    currentSOC = capacity; // TODO: Replace with realistic charging amount
                    drivingTimeAccumulator = 0.0;

                    lastFacility = new LinkWrapperFacility(network.getLinks().get(chargerLinkId));
                    departureTime += timeMap.get(link); // Update for next leg
                    chargeNeeded = true;
                    break; // Restart loop from new charging facility
                }
            }

            if (!chargeNeeded) {
                // Destination reachable without further stops
                break;
            }
        }

        if (iterationCount >= maxIterations) {
            throw new RuntimeException("Reached maximum iterations selecting charging stops; check charger placement or vehicle specs.");
        }

        return selectedChargers;
    }


    /**
     * Inserts charging stops and builds the final plan elements based on previously selected stops.
     */
    private List<PlanElement> assembleRouteWithCharging(RoutingRequest request, List<ChargerSpecification> selectedChargers, ElectricVehicleSpecification evSpec) {
        List<PlanElement> stagedRoute = new ArrayList<>();
        Facility lastFacility = request.getFromFacility();
        double departureTime = request.getDepartureTime();
        Set<Id<Link>> excludeLinks = new HashSet<>(); // The excluded links is now empty as the planned stops should be used
        for (ChargerSpecification charger : selectedChargers) {
            // At each planned stop, directly find the nearest compatible charger.

            Facility chargerFacility = new LinkWrapperFacility(network.getLinks().get(charger.getLinkId()));

            // Route to charger
            stagedRoute.addAll(delegate.calcRoute(DefaultRoutingRequest.of(
                    lastFacility, chargerFacility, departureTime, request.getPerson(), request.getAttributes())));
            departureTime = updateDepartureTime(stagedRoute);

            // Add charging activity
            Activity chargingActivity = generateChargingActivity(charger, evSpec);
            stagedRoute.add(chargingActivity);
            departureTime += chargingActivity.getMaximumDuration().seconds();

            lastFacility = chargerFacility;
        }

        // Route from last charger to destination
        stagedRoute.addAll(delegate.calcRoute(DefaultRoutingRequest.of(
                lastFacility, request.getToFacility(), departureTime, request.getPerson(), request.getAttributes())));

        return stagedRoute;
    }

    /**
     * Finds the closest compatible charger within the radius, excluding already visited chargers.
     */
    private ChargerSpecification findNearestCompatibleCharger(Link link, ElectricVehicleSpecification evSpec, Set<Id<Link>> excludeLinks) {
        return chargingInfrastructureSpecification.getChargerSpecifications().values().stream()
                .filter(charger -> evSpec.getChargerTypes().contains(charger.getChargerType()))
                .filter(charger -> !excludeLinks.contains(charger.getLinkId()))  // Exclude previously used chargers
                .filter(charger -> NetworkUtils.getEuclideanDistance(
                        network.getLinks().get(charger.getLinkId()).getCoord(),
                        link.getCoord()) < DEFAULT_RADIUS)
                .min(Comparator.comparingDouble(charger -> NetworkUtils.getEuclideanDistance(
                        network.getLinks().get(charger.getLinkId()).getCoord(),
                        link.getCoord())))
                .orElse(null);
    }

    /**
     * Generates a charging activity with estimated duration.
     */
    private Activity generateChargingActivity(ChargerSpecification charger, ElectricVehicleSpecification evSpec) {
        Link chargerLink = network.getLinks().get(charger.getLinkId());

        // Create initial InteractionActivity
        Activity interactionActivity = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(
                chargerLink.getCoord(), chargerLink.getId(), this.stageActivityModePrefix);

        // Convert InteractionActivity to regular Activity to allow setting duration
        Activity chargeAct = PopulationUtils.createActivity(interactionActivity);

        double maxPower = Math.min(charger.getPlugPower(), evSpec.getBatteryCapacity() / 3600);
        double estimatedTime = evSpec.getBatteryCapacity() / maxPower;

        // Apply truck-specific bounds
        if (isTruck(evSpec)) {
            estimatedTime = Math.min(Math.max(evConfigGroup.getMinimumChargeTime(), estimatedTime), 1800);
        } else {
            estimatedTime = Math.max(evConfigGroup.getMinimumChargeTime(), estimatedTime);
        }

        chargeAct.setMaximumDuration(estimatedTime);
        return chargeAct;
    }

    /**
     * Estimates per-link energy consumption for the given leg.
     */
    private Map<Link, Double> estimateConsumption(ElectricVehicleSpecification evSpec, Leg leg) {
        Map<Link, Double> consumptionMap = new LinkedHashMap<>();
        NetworkRoute route = (NetworkRoute) leg.getRoute();

		    List<Link> links = NetworkUtils.getLinks(this.network, route.getLinkIds());

        double departureTime = leg.getDepartureTime().seconds();

        // Create a temporary electric vehicle instance to use its energy models
        ElectricVehicle pseudoVehicle;
        try {
            pseudoVehicle = ElectricFleetUtils.create(
                    evSpec,
                    this.driveConsumptionFactory,
                    this.auxConsumptionFactory,
                    v -> charger -> { throw new UnsupportedOperationException(); } // No charging needed here
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create pseudo electric vehicle for energy estimation", e);
        }

        DriveEnergyConsumption driveConsumption = pseudoVehicle.getDriveEnergyConsumption();
        AuxEnergyConsumption auxConsumption = pseudoVehicle.getAuxEnergyConsumption();

        double linkEnterTime = departureTime;

        for (Link link : links) {
            double travelTime = this.travelTime.getLinkTravelTime(link, linkEnterTime, null, null);
            double driveEnergy = driveConsumption.calcEnergyConsumption(link, travelTime, linkEnterTime);
            double auxEnergy = auxConsumption.calcEnergyConsumption(linkEnterTime, travelTime, link.getId());

            consumptionMap.put(link, driveEnergy + auxEnergy);
            linkEnterTime += travelTime;
        }

        return consumptionMap;
    }

    /**
     * Estimates per-link travel time for the given leg.
     */
    private Map<Link, Double> estimateTime(Leg leg) {
        Map<Link, Double> timeMap = new LinkedHashMap<>();
        NetworkRoute route = (NetworkRoute) leg.getRoute();
        List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());
        double linkEnterTime = leg.getDepartureTime().seconds();

        for (Link link : links) {
            double travelTime = this.travelTime.getLinkTravelTime(link, linkEnterTime, null, null);
            timeMap.put(link, travelTime);
            linkEnterTime += travelTime;
        }

        return timeMap;
    }
    /**
     * Determines whether the given vehicle is a truck based on its type ID.
     */
    private boolean isTruck(ElectricVehicleSpecification evSpec) {
        return evSpec.getMatsimVehicle().getType().getId().toString().toLowerCase().contains("truck");
    }

    /**
     * Utility to compute next departure time based on most recent leg.
     */
    private double updateDepartureTime(List<PlanElement> routeSegment) {
        Leg lastLeg = (Leg) routeSegment.get(routeSegment.size() - 1);
        return lastLeg.getDepartureTime().seconds() + lastLeg.getTravelTime().seconds();
    }
}
