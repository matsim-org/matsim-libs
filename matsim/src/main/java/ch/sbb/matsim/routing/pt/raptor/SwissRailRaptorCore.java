/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator.RouteSegmentIterator;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig.RaptorTransferCalculation;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.RaptorObserver;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.CachingTransferProvider;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RTransfer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * The actual RAPTOR implementation, based on Delling et al, Round-Based Public Transit Routing.
 *
 * This class is <b>NOT</b> thread-safe due to the use of internal state during the route calculation.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorCore {
    private final SwissRailRaptorData data;

    private final PathElement[] arrivalPathPerRouteStop;
    private final double[] egressCostsPerRouteStop;
    private final double[] leastArrivalCostAtRouteStop;
    private final double[] leastArrivalCostAtStop;
    private final BitSet improvedRouteStopIndices;
    private final BitSet reachedRouteStopIndices;
    private final BitSet improvedStops;
    private final BitSet destinationRouteStopIndices;
    private double bestArrivalCost = Double.POSITIVE_INFINITY;
    private final PathElement[] arrivalPathPerStop;
    private final PathElement[] tmpArrivalPathPerStop; // only used to ensure parallel update
    private final BitSet tmpImprovedStops; // only used to ensure parallel update
    private final boolean useCapacityConstraints;
    private final boolean useAdaptiveTransferCalculation;
    private final RaptorInVehicleCostCalculator inVehicleCostCalculator;
    private final RaptorTransferCostCalculator transferCostCalculator;
    private final RouteSegmentIteratorImpl routeSegmentIterator;

    private final static int TIME_UNDEFINED = Integer.MIN_VALUE;

    SwissRailRaptorCore(SwissRailRaptorData data, RaptorInVehicleCostCalculator inVehicleCostCalculator, RaptorTransferCostCalculator transferCostCalculator) {
        this.data = data;
        this.arrivalPathPerRouteStop = new PathElement[data.countRouteStops];
        this.egressCostsPerRouteStop = new double[data.countRouteStops];
        this.leastArrivalCostAtRouteStop = new double[data.countRouteStops];
        this.leastArrivalCostAtStop = new double[data.countStops];
        this.improvedRouteStopIndices = new BitSet(this.data.countRouteStops);
        this.reachedRouteStopIndices = new BitSet(this.data.countRouteStops);
        this.destinationRouteStopIndices = new BitSet(this.data.countRouteStops);
        this.improvedStops = new BitSet(this.data.countStops);
        this.arrivalPathPerStop = new PathElement[this.data.countStops];
        this.tmpArrivalPathPerStop = new PathElement[this.data.countStops];
        this.tmpImprovedStops = new BitSet(this.data.countStops);
        this.useCapacityConstraints = this.data.config.isUseCapacityConstraints();
        this.useAdaptiveTransferCalculation = this.data.config.getTransferCalculation().equals(RaptorTransferCalculation.Adaptive);
        this.inVehicleCostCalculator = inVehicleCostCalculator;
        this.transferCostCalculator = transferCostCalculator;
        this.routeSegmentIterator = new RouteSegmentIteratorImpl(this.data);
    }

    private void reset() {
        Arrays.fill(this.arrivalPathPerRouteStop, null);
        Arrays.fill(this.egressCostsPerRouteStop, Double.POSITIVE_INFINITY);
        Arrays.fill(this.arrivalPathPerStop, null);
        Arrays.fill(this.leastArrivalCostAtRouteStop, Double.POSITIVE_INFINITY);
        Arrays.fill(this.leastArrivalCostAtStop, Double.POSITIVE_INFINITY);
        this.improvedStops.clear();
        this.improvedRouteStopIndices.clear();
        this.reachedRouteStopIndices.clear();
        this.destinationRouteStopIndices.clear();
        this.bestArrivalCost = Double.POSITIVE_INFINITY;
    }

    public RaptorRoute calcLeastCostRoute(double depTime, Facility fromFacility, Facility toFacility, List<InitialStop> accessStops, List<InitialStop> egressStops, RaptorParameters parameters, Person person) {
        final int maxTransfers = 20; // sensible defaults, could be made configurable if there is a need for it.
        final int maxTransfersAfterFirstArrival = 2;

        reset();
        CachingTransferProvider transferProvider = this.data.new CachingTransferProvider();

		// Using a LinkedHashMap instead of a regular HashMap here is necessary to have a deterministic behaviour
        Map<TransitStopFacility, InitialStop> destinationStops = new LinkedHashMap<>();

        // go through all egressStops; check if already in destinationStops; if so, check if current cost is smaller; if so, then replace.  This can
        // presumably happen when the same stop can be reached at lower cost by a different egress mode. (*)
        for (InitialStop egressStop : egressStops) {
            InitialStop alternative = destinationStops.get(egressStop.stop);
            if (alternative == null || egressStop.accessCost < alternative.accessCost) {
                destinationStops.put(egressStop.stop, egressStop);
            }
        }
        // ??:
        for (InitialStop egressStop : destinationStops.values()) {
            int[] routeStopIndices = this.data.routeStopsPerStopFacility.get(egressStop.stop);
            if (routeStopIndices != null) {
                for (int routeStopIndex : routeStopIndices) {
                    this.destinationRouteStopIndices.set(routeStopIndex); // set bit at index position to true
                    this.egressCostsPerRouteStop[routeStopIndex] = egressStop.accessCost; // set egress costs from given stop
                    // presumably, the routeStops are the stops for the different routes that stop at the same stopFacility
                }
            }
        }

        // same as (*) for access stops:
		// Also, using a LinkedHashMap instead of a regular HashMap here is necessary to have a deterministic behaviour
        Map<TransitStopFacility, InitialStop> initialStops = new LinkedHashMap<>();
        for (InitialStop accessStop : accessStops) {
            InitialStop alternative = initialStops.get(accessStop.stop);
            if (alternative == null || accessStop.accessCost < alternative.accessCost) {
                initialStops.put(accessStop.stop, accessStop);
            }
        }

        boolean hasIntermodalAccess = false;
        // go through initial stops ...
        for (InitialStop stop : initialStops.values()) {
        	// ... retrieve all route stops ...
            int[] routeStopIndices = this.data.routeStopsPerStopFacility.get(stop.stop);
            // ... go through them ...
            for (int routeStopIndex : routeStopIndices) {
            	// ... set arrival time and arrival cost accordingly ...
                int arrivalTime = (int) (depTime + stop.accessTime);
                double arrivalCost = stop.accessCost;

                RRouteStop routeStop = this.data.routeStops[routeStopIndex];

                boolean isIntermodalAccess = stop.planElements != null;
                // (intermodal access is if there are planElements that describe the intermodal access, which depends, I think, on which constructor was
                // called (since also non-intermodal access has a leg). kai, jul'19

                if (!isIntermodalAccess && routeStop.routeStop == routeStop.route.getStops().get(routeStop.route.getStops().size() - 1)) {
                    // this is the last stop of a route, doesn't make sense to start here
                    // if it's intermodal, we still start here, as we might transfer to another close-by but non-intermodal stop.
                    continue;
                }
                if (!routeStop.routeStop.isAllowBoarding()) {
                    continue;
                }

                RRoute route = this.data.routes[routeStop.transitRouteIndex];
                int depOffset = routeStop.departureOffset;

                int departureIndex = findNextDepartureIndex(route, routeStop, arrivalTime);
                if (departureIndex >= 0) {
                    int nextDepartureTimeAtStop = this.data.departures[departureIndex] + depOffset;
                    int waitingTime = nextDepartureTimeAtStop - arrivalTime;
                    double waitingCost = waitingTime * -parameters.getMarginalUtilityOfWaitingPt_utl_s();

                    RRouteStop toRouteStop = this.data.routeStops[routeStopIndex];
                    PathElement pe = new PathElement(null, toRouteStop, TIME_UNDEFINED, nextDepartureTimeAtStop, nextDepartureTimeAtStop, arrivalTime, arrivalCost, 0,
						stop.distance, 0, true, false,null, stop);

                    /* okay, the following is not very nice...
                     * we want to find the least-cost access leg including the waiting time
                     * until the next departure. But that waiting time should not be included anywhere else,
                     * just to figure out which is the best access way.
                     * So we calculate that total cost, including the waiting time, and use that
                     * to compare the different access legs. We write these total costs to
                     * this.leastArrivalCostAtRouteStop[], although it should only contain the arrival
                     * cost. This works because updates to this value are calculated based on the
                     * time and cost in arrivalPathPerRouteStop, so the additional waitingCost at that
                     * first stop gets ignored/overwritten in the further run of the routing algorithm.
                     */
                    double xCost = arrivalCost + waitingCost;

                    if (xCost < this.leastArrivalCostAtRouteStop[routeStopIndex]) {
                        this.arrivalPathPerRouteStop[routeStopIndex] = pe;
                        this.leastArrivalCostAtRouteStop[routeStopIndex] = xCost;
                        this.improvedRouteStopIndices.set(routeStopIndex);
                        if (xCost < this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex]) {
                            this.improvedStops.set(toRouteStop.stopFacilityIndex);
                            this.arrivalPathPerStop[toRouteStop.stopFacilityIndex] = pe;
                            this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex] = xCost;
                        }
                    }
                } else if (isIntermodalAccess) {
                    // there is no more departure, but we start here by intermodal access, so still register to allow transfers to other (non-)intermodal stops.
                    RRouteStop toRouteStop = this.data.routeStops[routeStopIndex];
                    PathElement pe = new PathElement(null, toRouteStop, TIME_UNDEFINED, TIME_UNDEFINED, TIME_UNDEFINED,arrivalTime, arrivalCost, 0, stop.distance, 0, true, false, null, stop);

                    /* okay, the following is not very nice...
                     * ... see long comment above, it's the same
                     */
                    if (arrivalCost < this.leastArrivalCostAtRouteStop[routeStopIndex]) {
                        hasIntermodalAccess = true;
                        this.arrivalPathPerRouteStop[routeStopIndex] = pe;
                        this.leastArrivalCostAtRouteStop[routeStopIndex] = arrivalCost;
                        this.improvedRouteStopIndices.set(routeStopIndex);
                        if (arrivalCost < this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex]) {
                            this.improvedStops.set(toRouteStop.stopFacilityIndex);
                            this.arrivalPathPerStop[toRouteStop.stopFacilityIndex] = pe;
                            this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex] = arrivalCost;
                        }
                    }
                }
            }
        }

        if (hasIntermodalAccess) {
            // allow transfering from the initial stop to another one if we have intermodal access,
            // as not all stops might be intermodal

            // handleTransfers clears improvedRouteStopIndices, which is correct during rounds
            // but it loses the initial route stop indices directly after initialization.
            // so keep a copy and restore it
            BitSet initialRouteStopIndices = new BitSet();
            initialRouteStopIndices.or(this.improvedRouteStopIndices);

            handleTransfers(true, parameters, transferProvider);
            this.improvedRouteStopIndices.or(initialRouteStopIndices);
        }

        int allowedTransfersLeft = maxTransfersAfterFirstArrival;
        // the main loop
        for (int k = 0; k <= maxTransfers; k++) {
            // first stage (according to paper) is to set earliestArrivalTime_k(stop) = earliestArrivalTime_k-1(stop)
            // but because we re-use the earliestArrivalTime-array, we don't have to do anything.

            // second stage: process routes
            exploreRoutes(parameters, person, transferProvider);

            PathElement leastCostPath = findLeastCostArrival(destinationStops);

            if (leastCostPath != null) {
                if (allowedTransfersLeft == 0) {
                    break;
                }
                allowedTransfersLeft--;
            }

            if (this.improvedStops.isEmpty()) {
                break;
            }

            // third stage (according to paper): handle footpaths / transfers
            handleTransfers(true, parameters, transferProvider);

            // final stage: check stop criterion
            if (this.improvedRouteStopIndices.isEmpty()) {
                break;
            }
        }

        // create RaptorRoute based on PathElements
        PathElement leastCostPath = findLeastCostArrival(destinationStops);
        RaptorRoute raptorRoute = createRaptorRoute(fromFacility, toFacility, leastCostPath, depTime);
        return raptorRoute;
    }

    public List<RaptorRoute> calcRoutes(double earliestDepTime, double desiredDepTime, double latestDepTime, Facility fromFacility, Facility toFacility, List<InitialStop> accessStops, List<InitialStop> egressStops, RaptorParameters parameters, Person person) {
        List<RaptorRoute> foundRoutes = new ArrayList<>();
        int maxTransfers = 20; // sensible defaults, could be made configurable if there is a need for it.
        final int maxTransfersAfterFirstArrival = 2;
        Map<PathElement, InitialStop> initialStopsPerStartPath = new HashMap<>();

        reset();

        CachingTransferProvider transferProvider = this.data.new CachingTransferProvider();
        double marginalUtilityOfWaitingPt_utl_s = parameters.getMarginalUtilityOfWaitingPt_utl_s();

        PathElement lastFoundBestPath = null;

        /* the original algorithm works with time. Starting with the latest departure,
         * it's easy to go backwards in time and potentially improve already visited stops when
         * arriving there earlier. In our case, we operate with cost. The cost of two departures
         * along the same route at different times is the same, breaking the algorithm.
         * In order to fix it, we have to make the cost behave the same way as time does in
         * the original algorithm. Thus, for each handled departure, we add an additional cost,
         * named "costOffset", corresponding to the additional waiting time for this departure
         * compared to the earliest departure time. This way, cost should behave very similar to
         * time: an earlier  departure will not lead to smaller costs if the final arrival is at
         * the same time when the additional time is just spent waiting somewhere at a transfer.
         * The same connection at an earlier time, resulting in an earlier arrival, will indeed
         * be found as an improved solution, although when the costOffset is subtracted again, it will
         * have the same cost. This allows us to filter and score the different routes afterwards.
         */

        List<DepartureAtRouteStop> departures = new ArrayList<>();
        for (InitialStop accessStop : accessStops) {
            double earliestTimeAtStop = earliestDepTime + accessStop.accessTime;
            double latestTimeAtStop = latestDepTime + accessStop.accessTime;
            TransitStopFacility stop = accessStop.stop;
            int[] routeStopIndices = this.data.routeStopsPerStopFacility.get(stop);
            if (routeStopIndices != null) {
                for (int routeStopIndex : routeStopIndices) {
                    RRouteStop routeStop = this.data.routeStops[routeStopIndex];
                    if (routeStop.routeStop == routeStop.route.getStops().get(routeStop.route.getStops().size() - 1)) {
                        // this is the last stop of a route
                        continue;
                    }
                    if (!routeStop.routeStop.isAllowBoarding()) {
                        continue;
                    }
                    RRoute route = this.data.routes[routeStop.transitRouteIndex];
                    int depOffset = routeStop.departureOffset;
                    for (int depIndex = route.indexFirstDeparture; depIndex < route.indexFirstDeparture + route.countDepartures; depIndex++) {
                        int depTimeAtStart = this.data.departures[depIndex];
                        int depTimeAtStop = depTimeAtStart + depOffset;
                        if (depTimeAtStop >= earliestTimeAtStop && depTimeAtStop <= latestTimeAtStop) {
                            double costOffset = (depTimeAtStop - earliestTimeAtStop) * marginalUtilityOfWaitingPt_utl_s;
                            departures.add(new DepartureAtRouteStop(routeStop, routeStopIndex, depIndex, depTimeAtStop, costOffset, accessStop));
                        }
                    }
                }
            }
        }
        departures.sort((d1, d2) -> {
            // sort the departures by cost, not by time as in the original algorithm
            double c1 = d1.costOffset + d1.accessStop.accessCost;
            double c2 = d2.costOffset + d2.accessStop.accessCost;
            int cmp = Double.compare(c1, c2);
            if (cmp == 0) {
                cmp = Integer.compare(d1.departureIndex, d2.departureIndex);
            }
            return -cmp; // negate, we want to order from biggest to smallest
        });

        Map<TransitStopFacility, InitialStop> destinationStops = new HashMap<>();
        for (InitialStop egressStop : egressStops) {
            InitialStop alternative = destinationStops.get(egressStop.stop);
            if (alternative == null || egressStop.accessCost < alternative.accessCost) {
                destinationStops.put(egressStop.stop, egressStop);
            }
        }
        for (InitialStop egressStop : destinationStops.values()) {
            int[] routeStopIndices = this.data.routeStopsPerStopFacility.get(egressStop.stop);
            if (routeStopIndices != null) {
                for (int routeStopIndex : routeStopIndices) {
                    this.destinationRouteStopIndices.set(routeStopIndex);
                    this.egressCostsPerRouteStop[routeStopIndex] = egressStop.accessCost;
                }
            }
        }

        for (DepartureAtRouteStop depAtRouteStop : departures) {
            this.improvedStops.clear();
            this.improvedRouteStopIndices.clear();
            this.bestArrivalCost = Double.POSITIVE_INFINITY;
            { // initialization for this departure Time
                int arrivalTime = depAtRouteStop.depTime;
                double arrivalCost = depAtRouteStop.accessStop.accessCost + depAtRouteStop.costOffset;
                RRouteStop toRouteStop = depAtRouteStop.routeStop;
                int routeStopIndex = depAtRouteStop.routeStopIndex;
                PathElement pe = new PathElement(null, toRouteStop, depAtRouteStop.depTime, depAtRouteStop.depTime, depAtRouteStop.depTime, arrivalTime, arrivalCost, 0, depAtRouteStop.accessStop.distance, 0, true, false, null, depAtRouteStop.accessStop);
                this.arrivalPathPerRouteStop[routeStopIndex] = pe;
                this.leastArrivalCostAtRouteStop[routeStopIndex] = arrivalCost;
                this.arrivalPathPerStop[toRouteStop.stopFacilityIndex] = pe;
                this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex] = arrivalCost;
                this.improvedRouteStopIndices.set(routeStopIndex);
                initialStopsPerStartPath.put(pe, depAtRouteStop.accessStop);
            }

            // the main loop
            for (int k = 0; k <= maxTransfers; k++) {
                // first stage (according to paper) is to set earliestArrivalTime_k(stop) = earliestArrivalTime_k-1(stop)
                // but because we re-use the earliestArrivalTime-array, we don't have to do anything.

                // second stage: process routes
                exploreRoutes(parameters, person, transferProvider);

                PathElement leastCostPath = findLeastCostArrival(destinationStops);
                if (leastCostPath != null && (lastFoundBestPath == null || leastCostPath.comingFrom != lastFoundBestPath.comingFrom)) {
                    lastFoundBestPath = leastCostPath;

                    double depTime = calculateOptimalDepartureTime(leastCostPath, initialStopsPerStartPath);
                    leastCostPath.arrivalTravelCost -= depAtRouteStop.costOffset;
                    RaptorRoute raptorRoute = createRaptorRoute(fromFacility, toFacility, leastCostPath, depTime);
                    leastCostPath.arrivalTravelCost += depAtRouteStop.costOffset;
                    foundRoutes.add(raptorRoute);

                    int optimizedTransferLimit = leastCostPath.transferCount + maxTransfersAfterFirstArrival;
                    if (optimizedTransferLimit < maxTransfers) {
                        maxTransfers = optimizedTransferLimit;
                    }
                    if (k == maxTransfers) {
                        break; // no use to handle transfers
                    }
                }

                if (this.improvedStops.isEmpty()) {
                    break;
                }

                // third stage (according to paper): handle footpaths / transfers
                handleTransfers(false, parameters, transferProvider);

                // final stage: check stop criterion
                if (this.improvedRouteStopIndices.isEmpty()) {
                    break;
                }
            }
        }

        List<RaptorRoute> routes = filterRoutes(foundRoutes);
        return routes;
    }

    private double calculateOptimalDepartureTime(PathElement leastCostPath, Map<PathElement, InitialStop> initialStopsPerStartPath) {
        PathElement firstPE = leastCostPath;
        while (firstPE.comingFrom != null) {
            firstPE = firstPE.comingFrom;
        }
        double depTime = firstPE.arrivalTime;
        // currently, firstPE.arrivalTime is exactly the time of departure at that stop
        // let's add some time for safety reasons and to add some realism
        depTime -= this.data.config.getMinimalTransferTime();
        // for more realism, a (random) value from a distribution could be taken instead of a fixed value
        InitialStop accessStop = initialStopsPerStartPath.get(firstPE);
        depTime -= accessStop.accessTime; // take access time into account
        return Math.floor(depTime);
    }

    private List<RaptorRoute> filterRoutes(List<RaptorRoute> allRoutes) {
        // first, eliminate duplicates
        allRoutes.sort((r1, r2) -> {
            int cmp = Integer.compare(r1.getNumberOfTransfers(), r2.getNumberOfTransfers());
            if (cmp == 0) {
                cmp = Double.compare(r1.getDepartureTime(), r2.getDepartureTime());
            }
            if (cmp == 0) {
                cmp = Double.compare(r1.getTravelTime(), r2.getTravelTime());
            }
            return cmp;
        });
        List<RaptorRoute> uniqueRoutes = new ArrayList<>();
        int lastTransferCount = -1;
        double lastDepTime = Double.NaN;
        double lastTravelTime = Double.NaN;
        for (RaptorRoute route : allRoutes) {
            if (route.getNumberOfTransfers() != lastTransferCount
                || route.getDepartureTime() != lastDepTime
                || route.getTravelTime() != lastTravelTime) {
                uniqueRoutes.add(route);
                lastTransferCount = route.getNumberOfTransfers();
                lastDepTime = route.getDepartureTime();
                lastTravelTime = route.getTravelTime();
            }
        }

        // now search for non-dominant routes
        List<RaptorRoute> routesToKeep = new ArrayList<>();
        for (RaptorRoute route1 : uniqueRoutes) {
            boolean addRoute1 = true;
            for (RaptorRoute route2 : uniqueRoutes) {
                if (route1 != route2) {
                    // check if route2 dominates route1
                    double arrTime1 = route1.getDepartureTime() + route1.getTravelTime();
                    double arrTime2 = route2.getDepartureTime() + route2.getTravelTime();
                    if (route2.getNumberOfTransfers() <=route1.getNumberOfTransfers()
                        && route2.getDepartureTime() >= route1.getDepartureTime()
                        && arrTime2 <= arrTime1) {
                        addRoute1 = false;
                        break;
                    }
                }
            }
            if (addRoute1) {
                routesToKeep.add(route1);
            }
        }
        return routesToKeep;
    }

    public Map<Id<TransitStopFacility>, TravelInfo> calcLeastCostTree(double depTime, Collection<InitialStop> startStops, RaptorParameters parameters, Person person) {
      return this.calcLeastCostTree(depTime, startStops, parameters, person, null);
    }

    public Map<Id<TransitStopFacility>, TravelInfo> calcLeastCostTree(double depTime, Collection<InitialStop> startStops, RaptorParameters parameters, Person person, RaptorObserver observer) {
        reset();

        CachingTransferProvider transferProvider = this.data.new CachingTransferProvider();
        BitSet initialRouteStopIndices = new BitSet();
        BitSet initialStopIndices = new BitSet();
        for (InitialStop stop : startStops) {
            int[] routeStopIndices = this.data.routeStopsPerStopFacility.get(stop.stop);
            for (int routeStopIndex : routeStopIndices) {
							boolean useStop = true;
							RRouteStop routeStop = this.data.routeStops[routeStopIndex];
							if (!routeStop.routeStop.isAllowBoarding()) {
								useStop = false;
							}
							if (useStop && parameters.isExactDeparturesOnly()) {
								int routeIndex = routeStop.transitRouteIndex;
								RRoute route = this.data.routes[routeIndex];
								int currentDepartureIndex = findNextDepartureIndex(route, routeStop, (int) depTime);
								if (currentDepartureIndex >= 0) {
									int firstDepartureTime = this.data.departures[currentDepartureIndex];
									int stopDepartureTime = firstDepartureTime + routeStop.departureOffset;
									useStop = Math.abs(depTime - stopDepartureTime) < 1e-5;
								} else {
									useStop = false;
								}
							}
							if (useStop) {
								int arrivalTime = (int) (depTime + stop.accessTime);
								double arrivalCost = stop.accessCost;
								RRouteStop toRouteStop = this.data.routeStops[routeStopIndex];
								PathElement pe = new PathElement(null, toRouteStop, TIME_UNDEFINED, TIME_UNDEFINED, TIME_UNDEFINED, arrivalTime, arrivalCost, 0, stop.distance, 0, true, false, null, stop);
								this.arrivalPathPerRouteStop[routeStopIndex] = pe;
								this.arrivalPathPerStop[toRouteStop.stopFacilityIndex] = pe;
								this.leastArrivalCostAtRouteStop[routeStopIndex] = arrivalCost;
								this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex] = arrivalCost;
								this.improvedRouteStopIndices.set(routeStopIndex);
								// this is special: make sure we can transfer even at the start stop
								initialRouteStopIndices.set(routeStopIndex);
								initialStopIndices.set(toRouteStop.stopFacilityIndex);
							}
            }
        }

        // the main loop
        int maxTransfers = parameters.getMaxTransfers();
        int transfers = 0;
        while (true) {
            // first stage (according to paper) is to set earliestArrivalTime_k(stop) = earliestArrivalTime_k-1(stop)
            // but because we re-use the earliestArrivalTime-array, we don't have to do anything.

            // second stage: process routes
            exploreRoutes(parameters, person, transferProvider);

            if (this.improvedStops.isEmpty()) {
                break;
            }

            if (initialRouteStopIndices != null) {
                this.improvedRouteStopIndices.or(initialRouteStopIndices);
                this.improvedStops.or(initialStopIndices);
                initialRouteStopIndices = null;
                initialStopIndices = null;
            }

            if (transfers > maxTransfers) {
                break;
            }

					if (observer != null) {
						for (int stopIndex = this.improvedStops.nextSetBit(0); stopIndex >= 0; stopIndex = this.improvedStops.nextSetBit(stopIndex + 1)) {
							PathElement fromPE = this.arrivalPathPerStop[stopIndex];
							observeArrival(fromPE, observer);
						}
					}

					// third stage (according to paper): handle footpaths / transfers
            handleTransfers(true, parameters, transferProvider);
            transfers++;

            // final stage: check stop criterion
            if (this.improvedRouteStopIndices.isEmpty()) {
                break;
            }

					if (observer != null) {
						for (int stopIndex = this.tmpImprovedStops.nextSetBit(0); stopIndex >= 0; stopIndex = this.tmpImprovedStops.nextSetBit(stopIndex + 1)) {
							PathElement fromPE = this.arrivalPathPerStop[stopIndex];
							observeArrival(fromPE, observer);
						}
					}

				}

        // collect information for each stop
        Map<Id<TransitStopFacility>, TravelInfo> result = new HashMap<>();
        for (Map.Entry<TransitStopFacility, Integer> e : this.data.stopFacilityIndices.entrySet()) {
            TransitStopFacility stop = e.getKey();
            int index = e.getValue();
            PathElement destination = this.arrivalPathPerStop[index];
            if (destination != null) {
                TravelInfo ti = getTravelInfo(destination, parameters);
                result.put(stop.getId(), ti);
            }
        }
        return result;
    }

		private void observeArrival(PathElement pe, RaptorObserver observer) {
			PathElement backpointer = pe.comingFrom;
			if (backpointer != null) {
				while (backpointer.comingFrom != null) {
					backpointer = backpointer.comingFrom;
				}
				TransitStopFacility departureStopFacility = backpointer.toRouteStop.routeStop.getStopFacility();
				if (departureStopFacility != null) {
					TransitStopFacility arrivalStopFacility = pe.toRouteStop.routeStop.getStopFacility();
					observer.arrivedAtStop(pe.firstDepartureTime, arrivalStopFacility, pe.arrivalTime, pe.transferCount, () -> createRaptorRoute(departureStopFacility, arrivalStopFacility, pe, pe.firstDepartureTime));
				}
			}
		}

    private TravelInfo getTravelInfo(PathElement destination, RaptorParameters parameters) {
        PathElement firstStage = destination;
        PathElement secondStage = null;
        while (firstStage.comingFrom != null) {
            secondStage = firstStage;
            firstStage = firstStage.comingFrom;
        }
        int arrivalTimeAtLastStop = destination.arrivalTime;
        int departureTimeAtFirstStop = destination.firstDepartureTime;
        if (departureTimeAtFirstStop == TIME_UNDEFINED) {
            // a trip with no actual pt-leg, likely the start-location
            departureTimeAtFirstStop = arrivalTimeAtLastStop;
        }
        double accessTime = firstStage.initialStop.accessTime;
        double accessCost = firstStage.initialStop.accessCost;

        double waitingTime = departureTimeAtFirstStop - firstStage.arrivalTime;
        double waitingCost = waitingTime * -parameters.getMarginalUtilityOfWaitingPt_utl_s();

        double travelCost = destination.arrivalTravelCost - firstStage.arrivalTravelCost - waitingCost;
        int transferCount = destination.transferCount;
        if (destination.isTransfer && transferCount > 0) {
            transferCount--; // do not count this as transfer, as the router would merge it with the egress walk
        }
        if (secondStage != null && secondStage.isTransfer && transferCount > 0) {
            transferCount--; // the first "leg" is a transfer, do not count it as such as the router would merge it with the access walk
        }
        Id<TransitStopFacility> departureStopId = firstStage.toRouteStop.routeStop.getStopFacility().getId();
        return new TravelInfo(departureStopId, departureTimeAtFirstStop, arrivalTimeAtLastStop, travelCost, accessTime, accessCost, transferCount, waitingTime, waitingCost, destination);
    }

    private void exploreRoutes(RaptorParameters parameters, Person person, CachingTransferProvider transferProvider) {
        this.improvedStops.clear();
        this.reachedRouteStopIndices.clear();

        MutableInt routeIndex = new MutableInt(-1);
        for (int firstRouteStopIndex = this.improvedRouteStopIndices.nextSetBit(0); firstRouteStopIndex >= 0; firstRouteStopIndex = this.improvedRouteStopIndices.nextSetBit(firstRouteStopIndex+1)) {
            RRouteStop firstRouteStop = this.data.routeStops[firstRouteStopIndex];
            if (firstRouteStop.transitRouteIndex == routeIndex.getValue()) {
                continue; // we've handled this route already
            }
            int tmpRouteIndex = firstRouteStop.transitRouteIndex;

            // for each relevant route, step along route and look for new/improved connections
            RRoute route = this.data.routes[tmpRouteIndex];

            // firstRouteStop is the first RouteStop in the route we can board in this round
            // figure out which departure we can take
            PathElement boardingPE = this.arrivalPathPerRouteStop[firstRouteStopIndex];

            int agentFirstArrivalTime = boardingPE.arrivalTime;
            int currentBoardingRouteStopIndex = firstRouteStopIndex;
            int currentDepartureIndex = findNextDepartureIndex(route, firstRouteStop, agentFirstArrivalTime);

            if (currentDepartureIndex >= 0) {
				firstRouteStopIndex = exploreRoute(parameters, person, transferProvider,
					currentDepartureIndex, firstRouteStop, agentFirstArrivalTime,
					boardingPE, route, tmpRouteIndex,
					currentBoardingRouteStopIndex, firstRouteStopIndex, routeIndex);
            }
        }
    }

	private int exploreRoute(RaptorParameters parameters, Person person, CachingTransferProvider transferProvider,
							  int currentDepartureIndex, RRouteStop firstRouteStop, int agentFirstArrivalTime,
							  PathElement boardingPE, RRoute route, int tmpRouteIndex,
							  int currentBoardingRouteStopIndex, int firstRouteStopIndex, MutableInt routeIndex) {
		double marginalUtilityOfWaitingPt_utl_s = parameters.getMarginalUtilityOfWaitingPt_utl_s();
		boolean useTransportModeUtilities = parameters.isUseTransportModeUtilities();

		Vehicle currentVehicle = this.data.departureVehicles[currentDepartureIndex];
		int currentDepartureTime = this.data.departures[currentDepartureIndex];
		int currentAgentBoardingTime;
		double currentTravelCostWhenBoarding;
		double currentTransferCostWhenBoarding;
		{
			int vehicleArrivalTime = currentDepartureTime + firstRouteStop.arrivalOffset;
			currentAgentBoardingTime = Math.max(agentFirstArrivalTime, vehicleArrivalTime);
			int waitingTime = currentAgentBoardingTime - agentFirstArrivalTime;
			double waitingCost = -marginalUtilityOfWaitingPt_utl_s * waitingTime;
			currentTravelCostWhenBoarding = boardingPE.arrivalTravelCost + waitingCost;
			currentTransferCostWhenBoarding = boardingPE.arrivalTransferCost;
		}

		if ((currentTravelCostWhenBoarding + currentTransferCostWhenBoarding) > this.bestArrivalCost) {
			return firstRouteStopIndex;
		}

		routeIndex.setValue(tmpRouteIndex);

		int firstDepartureTime = (boardingPE.firstDepartureTime == TIME_UNDEFINED) ? currentAgentBoardingTime : boardingPE.firstDepartureTime;

		double marginalUtilityOfTravelTime_utl_s = parameters.getMarginalUtilityOfTravelTime_utl_s(
			!useTransportModeUtilities ? boardingPE.toRouteStop.mode : boardingPE.toRouteStop.route.getTransportMode());

		SwissRailRaptorData.RChained[] chains = this.data.chainedDepartures.get(currentDepartureIndex);
		PathElement lastPE = null;
		boolean hasChains = chains != null;

		for (int toRouteStopIndex = firstRouteStopIndex + 1; toRouteStopIndex < route.indexFirstRouteStop + route.countRouteStops; toRouteStopIndex++) {
			// In the original raptor algorithm, we step once through all route-stops in order,
			// which ensures that we don't handle any stop or route twice.
			// With chained departures, we actually jump out of this sequence,
			// resulting in the state that the same stop or route might be handled a second time in the same round.
			// To prevent this (as it could have unintended side-effects and even produce wrong results), we
			// proactively clear the handled route stop. So if this a chained route, it already gets handled now
			// and will not have to be handled (regularly) later on, thus we can just clear the corresponding bits.
			this.improvedRouteStopIndices.clear(toRouteStopIndex);
			RRouteStop toRouteStop = this.data.routeStops[toRouteStopIndex];
			if (!toRouteStop.routeStop.isAllowAlighting()) {
				continue;
			}
			this.routeSegmentIterator.reset(currentDepartureIndex, currentAgentBoardingTime, currentBoardingRouteStopIndex, toRouteStopIndex);
			int arrivalTime = currentDepartureTime + toRouteStop.arrivalOffset;
			int inVehicleTime = arrivalTime - currentAgentBoardingTime;
			double inVehicleCost = this.inVehicleCostCalculator.getInVehicleCost(inVehicleTime, marginalUtilityOfTravelTime_utl_s, person, currentVehicle, parameters, routeSegmentIterator);
			double arrivalTravelCost = currentTravelCostWhenBoarding + inVehicleCost;
			double arrivalTransferCost = (boardingPE.firstDepartureTime != TIME_UNDEFINED) ? (currentTransferCostWhenBoarding + this.transferCostCalculator.calcTransferCost(boardingPE, transferProvider, data.config, parameters, arrivalTime - firstDepartureTime, boardingPE.transferCount, boardingPE.arrivalTransferCost, boardingPE.arrivalTime)) : 0;
			double previousArrivalCost = this.leastArrivalCostAtRouteStop[toRouteStopIndex];
			double totalArrivalCost = arrivalTravelCost + arrivalTransferCost;

			double distance = toRouteStop.distanceAlongRoute - boardingPE.toRouteStop.distanceAlongRoute;

			// Create last path element if necessary
			lastPE = totalArrivalCost <= previousArrivalCost || hasChains ?
				new PathElement(boardingPE, toRouteStop, firstDepartureTime, currentAgentBoardingTime, currentDepartureTime + firstRouteStop.departureOffset, arrivalTime, arrivalTravelCost, arrivalTransferCost, distance, boardingPE.transferCount, false, false, null, null) :
				null;

			if (totalArrivalCost <= previousArrivalCost) {
				this.arrivalPathPerRouteStop[toRouteStopIndex] = lastPE;
				this.leastArrivalCostAtRouteStop[toRouteStopIndex] = totalArrivalCost;
				if (totalArrivalCost <= this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex]) {
					this.leastArrivalCostAtStop[toRouteStop.stopFacilityIndex] = totalArrivalCost;
					this.arrivalPathPerStop[toRouteStop.stopFacilityIndex] = lastPE;
					this.improvedStops.set(toRouteStop.stopFacilityIndex);
					checkForBestArrival(toRouteStopIndex, totalArrivalCost);
				}
			} else /*if (previousArrivalCost < arrivalCost)*/ {
				// looks like we could reach this stop with better cost from somewhere else
				// check if we can depart also with better cost, if yes, switch to this connection
				PathElement alternativeBoardingPE = this.arrivalPathPerRouteStop[toRouteStopIndex];
				int alternativeAgentFirstArrivalTime = alternativeBoardingPE.arrivalTime;
				int alternativeDepartureIndex = findNextDepartureIndex(route, toRouteStop, alternativeAgentFirstArrivalTime);
				if (alternativeDepartureIndex >= 0) {
					int alternativeDepartureTime = this.data.departures[alternativeDepartureIndex];
					int alternativeVehicleArrivalTime = alternativeDepartureTime + toRouteStop.arrivalOffset;
					int alternativeAgentBoardingTime = Math.max(alternativeAgentFirstArrivalTime, alternativeVehicleArrivalTime);
					int alternativeWaitingTime = alternativeAgentBoardingTime - alternativeAgentFirstArrivalTime;
					double alternativeWaitingCost = -marginalUtilityOfWaitingPt_utl_s * alternativeWaitingTime;
					double alternativeTravelCostWhenBoarding = alternativeBoardingPE.arrivalTravelCost + alternativeWaitingCost;
					double alternativeTotalCostWhenBoarding = alternativeTravelCostWhenBoarding + alternativeBoardingPE.arrivalTransferCost;
					if (alternativeTotalCostWhenBoarding < totalArrivalCost) {
						currentDepartureIndex = alternativeDepartureIndex;
						currentVehicle = this.data.departureVehicles[currentDepartureIndex];
						currentDepartureTime = alternativeDepartureTime;

						// Reset chain information when switching connection
						chains = null;
						lastPE = null;
						hasChains = false;

						if (alternativeBoardingPE.isTransfer) {
							firstRouteStop = this.data.routeStops[toRouteStopIndex];
						} else {
							// we improve to a line/route we entered at some earlier stop, do not create a new transfer for this,
							// but set the boarding info back to the original boarding of this route
							alternativeBoardingPE = alternativeBoardingPE.comingFrom;
							alternativeAgentFirstArrivalTime = alternativeBoardingPE.arrivalTime;
							alternativeVehicleArrivalTime = alternativeDepartureTime + alternativeBoardingPE.toRouteStop.arrivalOffset;
							alternativeAgentBoardingTime = Math.max(alternativeAgentFirstArrivalTime, alternativeVehicleArrivalTime);

							alternativeWaitingTime = alternativeAgentBoardingTime - alternativeAgentFirstArrivalTime;
							alternativeWaitingCost = -marginalUtilityOfWaitingPt_utl_s * alternativeWaitingTime;
							alternativeTravelCostWhenBoarding = alternativeBoardingPE.arrivalTravelCost + alternativeWaitingCost;

							firstRouteStop = alternativeBoardingPE.toRouteStop;
						}
						currentAgentBoardingTime = alternativeAgentBoardingTime;
						currentTravelCostWhenBoarding = alternativeTravelCostWhenBoarding;
						currentTransferCostWhenBoarding = alternativeBoardingPE.arrivalTransferCost;
						boardingPE = alternativeBoardingPE;
						firstDepartureTime = (boardingPE.firstDepartureTime == TIME_UNDEFINED) ? currentAgentBoardingTime : boardingPE.firstDepartureTime;
						currentBoardingRouteStopIndex = alternativeBoardingPE.toRouteStop.index;
					}
				}
			}

			firstRouteStopIndex = toRouteStopIndex; // we've handled this route stop, so we can skip it in the outer loop
		}

		if (hasChains) {

			MutableInt tmp = new MutableInt(-1);

			assert lastPE != null : "Path element should not be null if we have chains";

			for (SwissRailRaptorData.RChained chain : chains) {

				RRoute chainedRoute = this.data.routes[chain.toRoute];
				int chainedDepartureIndex = chain.toDeparture;

				int chainedDepartureTime = this.data.departures[chainedDepartureIndex];

				assert chainedDepartureTime >= lastPE.arrivalTime:
					String.format("Chained departure (%s -> %s) arrival time should be before departure time",
						this.data.departureIds[currentDepartureIndex], this.data.departureIds[chainedDepartureIndex]);

				RRouteStop firstStop = this.data.routeStops[chainedRoute.indexFirstRouteStop];

				PathElement chainPe = new PathElement(lastPE, firstStop, lastPE.firstDepartureTime, lastPE.arrivalTime, chainedDepartureTime,
					lastPE.arrivalTime, lastPE.arrivalTravelCost, lastPE.arrivalTransferCost, 0, lastPE.transferCount, false, true, null, null);

				// For arrival time the departure time of the first stop is used, so that no wait time is incurred
				exploreRoute(parameters, person, transferProvider,
					chainedDepartureIndex, firstStop, chainedDepartureTime, chainPe, chainedRoute, chain.toRoute,
					chainedRoute.indexFirstRouteStop, chainedRoute.indexFirstRouteStop, tmp);
			}
		}

		return firstRouteStopIndex;
	}

    private void checkForBestArrival(int routeStopIndex, double arrivalCost) {
        if (this.destinationRouteStopIndices.get(routeStopIndex)) {
            // this is a destination stop
            double totalCost = arrivalCost + this.egressCostsPerRouteStop[routeStopIndex];
            if (totalCost < this.bestArrivalCost) {
                this.bestArrivalCost = totalCost;
            }
        }
    }

    private int findNextDepartureIndex(RRoute route, RRouteStop routeStop, int time) {
        if (this.useCapacityConstraints) {
            return findNextDepartureIndexWithConstraints(route, routeStop, time);
        }
        int depTimeAtRouteStart = time - routeStop.departureOffset;
        int fromIndex = route.indexFirstDeparture;
        int toIndex = fromIndex + route.countDepartures;
        int pos = Arrays.binarySearch(this.data.departures, fromIndex, toIndex, depTimeAtRouteStart);
        if (pos < 0) {
            // binarySearch returns (-(insertion point) - 1) if the element was not found, which will happen most of the times.
            // insertion_point points to the next larger element, which is the next departure in our case
            // This can be transformed as follows:
            // retval = -(insertion point) - 1
            // ==> insertion point = -(retval+1) .
            pos = -(pos + 1);
        }
        if (pos >= toIndex) {
            // there is no later departure time
            return -1;
        }
        return pos;
    }

    private int findNextDepartureIndexWithConstraints(RRoute route, RRouteStop routeStop, int time) {
        return this.data.occupancyData.getNextAvailableDeparture(this.data, routeStop, time);
    }

    private void handleTransfers(boolean strict, RaptorParameters raptorParams, CachingTransferProvider transferProvider) {
        this.improvedRouteStopIndices.clear();
        this.tmpImprovedStops.clear();

        double margUtilityTransitWalk = raptorParams.getMarginalUtilityOfTravelTime_utl_s(TransportMode.walk); // replaced TransportMode.transit_walk with walk

        for (int stopIndex = this.improvedStops.nextSetBit(0); stopIndex >= 0; stopIndex = this.improvedStops.nextSetBit(stopIndex + 1)) {
            PathElement fromPE = this.arrivalPathPerStop[stopIndex];
            int arrivalTime = fromPE.arrivalTime;
            double arrivalTravelCost = fromPE.arrivalTravelCost;
            double arrivalTransferCost = fromPE.arrivalTransferCost;
            double totalArrivalCost = arrivalTravelCost + arrivalTransferCost;
            if (totalArrivalCost > this.bestArrivalCost) {
                continue;
            }
            RRouteStop fromRouteStop = fromPE.toRouteStop; // this is the route stop we arrive with least cost at stop

            final int firstTransferIndex;
            final int lastTransferIndex;
            final RTransfer[] transfers;

            if (!useAdaptiveTransferCalculation) {
            	// efficient lookup from the precomputed transfer candidates
            	transfers = this.data.transfers;
            	firstTransferIndex = fromRouteStop.indexFirstTransfer;
            	lastTransferIndex = firstTransferIndex + fromRouteStop.countTransfers;
            } else {
            	// more costly calculation and caching of transfer canddiates
            	transfers = this.data.calculateTransfers(fromRouteStop);
            	firstTransferIndex = 0;
            	lastTransferIndex = transfers.length;
            }

            for (int transferIndex = firstTransferIndex; transferIndex < lastTransferIndex; transferIndex++) {
                RTransfer transfer = transfers[transferIndex];

                int toRouteStopIndex = transfer.toRouteStop;
                transferProvider.reset(transfer);
                int newArrivalTime = arrivalTime + transfer.transferTime;
                double newArrivalTravelCost = arrivalTravelCost - transfer.transferTime * margUtilityTransitWalk;
                double newArrivalTransferCost = (fromPE.firstDepartureTime != TIME_UNDEFINED) ? (arrivalTransferCost + this.transferCostCalculator.calcTransferCost(fromPE, transferProvider, data.config, raptorParams, newArrivalTime - fromPE.firstDepartureTime, fromPE.transferCount + 1, arrivalTransferCost, arrivalTime)) : 0;
                double newTotalArrivalCost = newArrivalTravelCost + newArrivalTransferCost;
                double prevLeastArrivalCost = this.leastArrivalCostAtRouteStop[toRouteStopIndex];
                if (newTotalArrivalCost < prevLeastArrivalCost || (!strict && newTotalArrivalCost <= prevLeastArrivalCost)) {
                    RRouteStop toRouteStop = this.data.routeStops[toRouteStopIndex];
                    PathElement pe = new PathElement(fromPE, toRouteStop, fromPE.firstDepartureTime, TIME_UNDEFINED, TIME_UNDEFINED, newArrivalTime, newArrivalTravelCost, newArrivalTransferCost, transfer.transferDistance, fromPE.transferCount + 1, true, false, transfer, null);
                    this.arrivalPathPerRouteStop[toRouteStopIndex] = pe;
                    this.leastArrivalCostAtRouteStop[toRouteStopIndex] = newTotalArrivalCost;
                    this.improvedRouteStopIndices.set(toRouteStopIndex);
                    int toStopFacilityIndex = toRouteStop.stopFacilityIndex;
                    prevLeastArrivalCost = this.leastArrivalCostAtStop[toStopFacilityIndex];
                    if (newTotalArrivalCost < prevLeastArrivalCost || (!strict && newTotalArrivalCost <= prevLeastArrivalCost)) {
                        // store it in tmp only. We don't want that this PE is used by a stop processed later in the same round. ("parallel update")
                        this.leastArrivalCostAtStop[toStopFacilityIndex] = newTotalArrivalCost;
                        this.tmpArrivalPathPerStop[toStopFacilityIndex] = pe;
                        this.tmpImprovedStops.set(toStopFacilityIndex);
                    }
                }
            }
        }
        // "parallel update". now copy over the newly improved data after all transfers were handled
        for (int stopIndex = this.tmpImprovedStops.nextSetBit(0); stopIndex >= 0; stopIndex = this.tmpImprovedStops.nextSetBit(stopIndex + 1)) {
            PathElement pe = this.tmpArrivalPathPerStop[stopIndex];
            this.arrivalPathPerStop[stopIndex] = pe;
        }
    }

    private PathElement findLeastCostArrival(Map<TransitStopFacility, InitialStop> destinationStops) {
        double leastCost = Double.POSITIVE_INFINITY;
        double leastCostFeederOnly = Double.POSITIVE_INFINITY;
        PathElement leastCostPath = null;
        PathElement leastCostPathFeederOnly = null;

		SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling intermodalLegOnlyHandling = data.config.getIntermodalLegOnlyHandling();
		boolean checkBothPtAndPurelyIntermodalRoutes = intermodalLegOnlyHandling.equals(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.avoid) || intermodalLegOnlyHandling.equals(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.forbid);
		for (Map.Entry<TransitStopFacility, InitialStop> e : destinationStops.entrySet()) {
            TransitStopFacility stop = e.getKey();
            int stopIndex = this.data.stopFacilityIndices.get(stop);
            PathElement pe = this.arrivalPathPerStop[stopIndex];
			if (pe!=null) {
					InitialStop egressStop = e.getValue();
					int arrivalTime = (int) (pe.arrivalTime + egressStop.accessTime);
					double arrivalTravelCost = pe.arrivalTravelCost + egressStop.accessCost;
					double totalCost = arrivalTravelCost + pe.arrivalTransferCost;
					if ((totalCost < leastCost) || (totalCost == leastCost && pe.transferCount < leastCostPath.transferCount)) {
						PathElement egressLegCandidate = new PathElement(pe, null, pe.firstDepartureTime, TIME_UNDEFINED, TIME_UNDEFINED, arrivalTime, arrivalTravelCost, pe.arrivalTransferCost, egressStop.distance, pe.transferCount, true, false, null, egressStop);

						if (pe.comingFrom == null && checkBothPtAndPurelyIntermodalRoutes) {
							if (totalCost < leastCostFeederOnly) {
								leastCostFeederOnly = totalCost;
								leastCostPathFeederOnly = egressLegCandidate;
							}
						} else {
							// this is the egress leg
							leastCostPath = egressLegCandidate;
							leastCost = totalCost;
						}
					}

				}

        }

		if (checkBothPtAndPurelyIntermodalRoutes){
			if (Double.isInfinite(leastCost)){
				return leastCostPathFeederOnly;
			}
		}
        return leastCostPath;
    }

    private static RaptorRoute createRaptorRoute(Facility fromFacility, Facility toFacility, PathElement destinationPathElement, double departureTime) {
        ArrayList<PathElement> pes = new ArrayList<>();
        double arrivalCost = Double.POSITIVE_INFINITY;
        if (destinationPathElement != null) {
            arrivalCost = destinationPathElement.arrivalTravelCost + destinationPathElement.arrivalTransferCost;
            PathElement pe = destinationPathElement;
            while (pe.comingFrom != null) {
                pes.add(pe);
                pe = pe.comingFrom;
            }
            pes.add(pe);
        }
        Collections.reverse(pes);

        RaptorRoute raptorRoute = new RaptorRoute(fromFacility, toFacility, arrivalCost);
        double time = departureTime;
        TransitStopFacility fromStop = null;
        int peCount = pes.size();
		boolean chainNext = false;
        int i = -1;
        for (PathElement pe : pes) {
            i++;
            TransitStopFacility toStop = pe.toRouteStop == null ? null : pe.toRouteStop.routeStop.getStopFacility();
            double travelTime = pe.arrivalTime - time;
            if (pe.initialStop != null && pe.initialStop.planElements != null) {
				raptorRoute.addPlanElements(time, travelTime, pe.initialStop.planElements);
			}
			else if (pe.isChained) {
				// The next leg is chained to the previous one
				chainNext = true;
            } else if (pe.isTransfer) {
                // add (peCount > 2 || peCount == 2 && !pes.get(0).isTransfer) && to catch case of only access and egress
                // legs without a real leg in between which was previously caught above by
                // pes.size() == 2 && pes.get(0).isTransfer && pes.get(1).isTransfer
                //
                // in case of peCount < 2 there should be no effect, because peCount-2 < 0 and i will be 0, so i!=peCount - 2
				if ((peCount > 2 || peCount == 2 && !pes.get(0).isTransfer) && i == peCount - 2 && pes.get(i + 1).isTransfer && (!isIntermodal(pes.get(i + 1).initialStop))) {
					// the second last element and the last elements are transfers,
					// in this case the we skip the second last element so there is only one walk leg attached
					// This merge can only work if it is not intermodal...
					continue;
                }
                String mode = TransportMode.walk;
                raptorRoute.addNonPt(fromStop, toStop, time, travelTime, pe.distance, mode);
            } else {

                TransitLine line = pe.toRouteStop.line;
                TransitRoute route = pe.toRouteStop.route;

				if (chainNext) {
					// Chained part is appended to the last one
					raptorRoute.addChainedPart(fromStop, toStop, line, route, pe.toRouteStop.mode, time, pe.boardingTime, pe.vehDepartureTime, pe.arrivalTime, pe.distance);
					chainNext = false;
				} else
					raptorRoute.addPt(fromStop, toStop, line, route, pe.toRouteStop.mode, time, pe.boardingTime, pe.vehDepartureTime, pe.arrivalTime, pe.distance);

            }
            time = pe.arrivalTime;
            fromStop = toStop;
        }
        return raptorRoute;
    }

    private static boolean isIntermodal(InitialStop initialStop) {
        return initialStop != null && initialStop.planElements != null;
    }

    static class PathElement {
        final PathElement comingFrom;
        final RRouteStop toRouteStop;
        final int firstDepartureTime; // the departure time at the start stop
        final int boardingTime;
        final int vehDepartureTime;
        final int arrivalTime;
        double arrivalTravelCost;
        double arrivalTransferCost;
        final double distance;
        final int transferCount;
        final boolean isTransfer;
		final boolean isChained;
        final RTransfer transfer;
        final InitialStop initialStop;

        PathElement(PathElement comingFrom, RRouteStop toRouteStop, int firstDepartureTime, int boardingTime, int vehDeparturetime,
					int arrivalTime, double arrivalTravelCost, double arrivalTransferCost, double distance, int transferCount,
					boolean isTransfer, boolean isChained, RTransfer transfer, InitialStop initialStop) {
            this.comingFrom = comingFrom;
            this.toRouteStop = toRouteStop;
            this.firstDepartureTime = firstDepartureTime;
            this.boardingTime = boardingTime;
            this.vehDepartureTime = vehDeparturetime;
            this.arrivalTime = arrivalTime;
            this.arrivalTravelCost = arrivalTravelCost;
            this.arrivalTransferCost = arrivalTransferCost;
            this.distance = distance;
            this.transferCount = transferCount;
            this.isTransfer = isTransfer;
			this.isChained = isChained;
            this.transfer = transfer;
            this.initialStop = initialStop;
        }
    }

    private static class DepartureAtRouteStop {
        final RRouteStop routeStop;
        final InitialStop accessStop;
        final int departureIndex;
        final int routeStopIndex;
        final int depTime;
        final double costOffset;

        DepartureAtRouteStop(RRouteStop routeStop, int routeStopIndex, int departureIndex, int depTime, double costOffset, InitialStop accessStop) {
            this.routeStop = routeStop;
            this.routeStopIndex = routeStopIndex;
            this.departureIndex = departureIndex;
            this.depTime = depTime;
            this.costOffset = costOffset;
            this.accessStop = accessStop;
        }
    }

    public static final class TravelInfo {
        public final Id<TransitStopFacility> departureStop;
        public final int transferCount;

        /** The departure time at the first stop */
        public final double ptDepartureTime;
        /** The arrival time at the last stop */
        public final double ptArrivalTime;
        /** The travel time between the first stop (departure) and the last stop (arrival). */
        public final double ptTravelTime;
        /** The cost for travelling from the first stop to the last stop. Not included are accessCost or cost for waiting at the first stop. */
        public final double travelCost;

        /** The time required to travel from the origin to the first stop. Not included in {@link #ptTravelTime}. */
        public final double accessTime;
        public final double accessCost;

        /** the time an agent has to wait at the first stop until the first pt vehicle departs. */
        public final double waitingTime;
        /** the costs an agent accumulates due to waiting at the first stop until the first pt vehicle departs. */
        public final double waitingCost;

        private final PathElement destinationPath;

        TravelInfo(Id<TransitStopFacility> departureStop, double departureTime, double arrivalTime, double travelCost, double accessTime, double accessCost, int transferCount, double waitingTime, double waitingCost, PathElement destinationPath) {
            this.departureStop = departureStop;
            this.ptDepartureTime = departureTime;
            this.ptArrivalTime = arrivalTime;
            this.ptTravelTime = arrivalTime - departureTime;
            this.travelCost = travelCost;
            this.accessTime = accessTime;
            this.accessCost = accessCost;
            this.transferCount = transferCount;
            this.waitingTime = waitingTime;
            this.waitingCost = waitingCost;
            this.destinationPath = destinationPath;
        }

        public RaptorRoute getRaptorRoute() {
            PathElement firstPath = this.destinationPath;
            while (firstPath.comingFrom != null) {
                firstPath = firstPath.comingFrom;
            }

            Facility fromFacility = firstPath.toRouteStop.routeStop.getStopFacility();
            Facility toFacility = this.destinationPath.toRouteStop.routeStop.getStopFacility();
            return createRaptorRoute(fromFacility, toFacility, this.destinationPath, firstPath.arrivalTime);
        }

        public boolean isWalkOnly() {
            if (this.destinationPath.comingFrom == null) {
                return true;
            }
            PathElement pe = this.destinationPath;
            while (pe != null) {
                if (!pe.isTransfer) {
                    return false;
                }
                pe = pe.comingFrom;
            }
            return true;
        }
    }

    private static class RouteSegmentIteratorImpl implements RouteSegmentIterator {

        SwissRailRaptorData data;
        private int boardingTime;
        private int fromRouteStopIndex;
        private int toRouteStopIndex;

        private int routeDepartureTime;
        private int currentRouteStopIndex;

        private double currentInVehicleTime = -1;
        private double currentPassengerCount = -1;
        private double currentTimeOfDay = -1;
        private Id<Departure> currentDepartureId = null;

        public RouteSegmentIteratorImpl(SwissRailRaptorData data) {
            this.data = data;
        }

        void reset(int departureIndex, int boardingTime, int fromRouteStopIndex, int toRouteStopIndex) {
            this.boardingTime = boardingTime;
            this.fromRouteStopIndex = fromRouteStopIndex;
            this.toRouteStopIndex = toRouteStopIndex;

            this.routeDepartureTime = this.data.departures[departureIndex];
            this.currentRouteStopIndex = fromRouteStopIndex;
            this.currentInVehicleTime = -1;
            this.currentPassengerCount = -1;
            this.currentTimeOfDay = -1;
            this.currentDepartureId = this.data.departureIds[departureIndex];
        }

        @Override
        public boolean hasNext() {
            return this.currentRouteStopIndex < this.toRouteStopIndex;
        }

        @Override
        public void next() {
            if (this.currentRouteStopIndex >= this.toRouteStopIndex) {
                throw new NoSuchElementException();
            }

            int departureRouteStopIndex = this.currentRouteStopIndex;
            this.currentRouteStopIndex++;
            int nextRouteStopIndex = this.currentRouteStopIndex;
            RRouteStop depRouteStop = this.data.routeStops[departureRouteStopIndex];
            int startTime = this.routeDepartureTime + depRouteStop.departureOffset;
            if (departureRouteStopIndex == this.fromRouteStopIndex) {
                startTime = this.boardingTime;
            }
            RRouteStop nextRouteStop = this.data.routeStops[nextRouteStopIndex];
            int endTime = this.routeDepartureTime + nextRouteStop.departureOffset;
            if (nextRouteStopIndex == this.toRouteStopIndex) {
                endTime = this.routeDepartureTime + nextRouteStop.arrivalOffset;
            }
            this.currentInVehicleTime = endTime - startTime;
            this.currentTimeOfDay = startTime;

            DepartureData depData = this.data.occupancyData.getDepartureData(nextRouteStop.line.getId(), nextRouteStop.route.getId(), depRouteStop.routeStop.getStopFacility().getId(), this.currentDepartureId);
            this.currentPassengerCount = depData == null ? 0 : depData.paxCountAtDeparture;
        }

        @Override
        public double getInVehicleTime() {
            return this.currentInVehicleTime;
        }

        @Override
        public double getPassengerCount() {
            return this.currentPassengerCount;
        }

        @Override
        public double getTimeOfDay() {
            return this.currentTimeOfDay;
        }
    }
}
