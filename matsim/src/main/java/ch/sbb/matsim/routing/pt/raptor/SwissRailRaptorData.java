/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorData {

    private static final Logger log = Logger.getLogger(SwissRailRaptorData.class);

    final RaptorStaticConfig config;
    final int countStops;
    final int countRouteStops;
    final RRoute[] routes;
    final double[] departures; // in the RAPTOR paper, this is usually called "trips", but I stick with the MATSim nomenclature
    final RRouteStop[] routeStops; // list of all route stops
    final RTransfer[] transfers;
    final Map<TransitStopFacility, Integer> stopFacilityIndices;
    final Map<TransitStopFacility, int[]> routeStopsPerStopFacility;
    final QuadTree<TransitStopFacility> stopsQT;
    final Map<String, Map<String, QuadTree<TransitStopFacility>>> stopFilterAttribute2Value2StopsQT;

    private SwissRailRaptorData(RaptorStaticConfig config, int countStops,
                                RRoute[] routes, double[] departures, RRouteStop[] routeStops,
                                RTransfer[] transfers, Map<TransitStopFacility, Integer> stopFacilityIndices,
                                Map<TransitStopFacility, int[]> routeStopsPerStopFacility, QuadTree<TransitStopFacility> stopsQT) {
        this.config = config;
        this.countStops = countStops;
        this.countRouteStops = routeStops.length;
        this.routes = routes;
        this.departures = departures;
        this.routeStops = routeStops;
        this.transfers = transfers;
        this.stopFacilityIndices = stopFacilityIndices;
        this.routeStopsPerStopFacility = routeStopsPerStopFacility;
        this.stopsQT = stopsQT;
        this.stopFilterAttribute2Value2StopsQT = new HashMap<String, Map<String, QuadTree<TransitStopFacility>>>();
    }

    public static SwissRailRaptorData create(TransitSchedule schedule, RaptorStaticConfig staticConfig, Network network) {
        log.info("Preparing data for SwissRailRaptor...");
        long startMillis = System.currentTimeMillis();

        int countRoutes = 0;
        long countRouteStops = 0;
        long countDepartures = 0;

        for (TransitLine line : schedule.getTransitLines().values()) {
            countRoutes += line.getRoutes().size();
            for (TransitRoute route : line.getRoutes().values()) {
                countRouteStops += route.getStops().size();
                countDepartures += route.getDepartures().size();
            }
        }

        if (countRouteStops > Integer.MAX_VALUE) {
            throw new RuntimeException("TransitSchedule has too many TransitRouteStops: " + countRouteStops);
        }
        if (countDepartures > Integer.MAX_VALUE) {
            throw new RuntimeException("TransitSchedule has too many Departures: " + countDepartures);
        }

        double[] departures = new double[(int) countDepartures];
        RRoute[] routes = new RRoute[countRoutes];
        RRouteStop[] routeStops = new RRouteStop[(int) countRouteStops];

        int indexRoutes = 0;
        int indexRouteStops = 0;
        int indexDeparture = 0;

        // enumerate TransitStopFacilities along their usage in transit routes to (hopefully) achieve a better memory locality
        // well, I'm not even sure how often we'll need the transit stop facilities, likely we'll use RouteStops more often
        Map<TransitStopFacility, Integer> stopFacilityIndices = new HashMap<>((int) (schedule.getFacilities().size() * 1.5));
        Map<TransitStopFacility, int[]> routeStopsPerStopFacility = new HashMap<>();

        boolean useModeMapping = staticConfig.isUseModeMappingForPassengers();
        for (TransitLine line : schedule.getTransitLines().values()) {
            List<TransitRoute> transitRoutes = new ArrayList<>(line.getRoutes().values());
            transitRoutes.sort((tr1, tr2) -> Double.compare(getEarliestDeparture(tr1).getDepartureTime(), getEarliestDeparture(tr2).getDepartureTime())); // sort routes by earliest departure for additional performance gains
            for (TransitRoute route : transitRoutes) {
                int indexFirstDeparture = indexDeparture;
                String mode = TransportMode.pt;
                if (useModeMapping) {
                    mode = staticConfig.getPassengerMode(route.getTransportMode());
                }
                RRoute rroute = new RRoute(indexRouteStops, route.getStops().size(), indexFirstDeparture, route.getDepartures().size());
                routes[indexRoutes] = rroute;
                NetworkRoute networkRoute = route.getRoute();
                List<Id<Link>> allLinkIds = new ArrayList<>();
                allLinkIds.add(networkRoute.getStartLinkId());
                allLinkIds.addAll(networkRoute.getLinkIds());
                if (allLinkIds.size() > 1 || networkRoute.getStartLinkId() != networkRoute.getEndLinkId()) {
                    allLinkIds.add(networkRoute.getEndLinkId());
                }
                Iterator<Id<Link>> linkIdIterator = allLinkIds.iterator();
                Id<Link> currentLinkId = linkIdIterator.next();
                double distanceAlongRoute = 0.0;
                for (TransitRouteStop routeStop : route.getStops()) {
                    while (!routeStop.getStopFacility().getLinkId().equals(currentLinkId)) {
                        if (linkIdIterator.hasNext()) {
                            currentLinkId = linkIdIterator.next();
                            Link link = network.getLinks().get(currentLinkId);
                            distanceAlongRoute += link.getLength();
                        } else {
                            distanceAlongRoute = Double.NaN;
                            break;
                        }
                    }
                    int stopFacilityIndex = stopFacilityIndices.computeIfAbsent(routeStop.getStopFacility(), stop -> stopFacilityIndices.size());
                    RRouteStop rRouteStop = new RRouteStop(routeStop, line, route, mode, indexRoutes, stopFacilityIndex, distanceAlongRoute);
                    final int thisRouteStopIndex = indexRouteStops;
                    routeStops[thisRouteStopIndex] = rRouteStop;
                    routeStopsPerStopFacility.compute(routeStop.getStopFacility(), (stop, currentRouteStops) -> {
                        if (currentRouteStops == null) {
                            return new int[] { thisRouteStopIndex };
                        }
                        int[] tmp = new int[currentRouteStops.length + 1];
                        System.arraycopy(currentRouteStops, 0, tmp, 0, currentRouteStops.length);
                        tmp[currentRouteStops.length] = thisRouteStopIndex;
                        return tmp;
                    });
                    indexRouteStops++;
                }
                for (Departure dep : route.getDepartures().values()) {
                    departures[indexDeparture] = dep.getDepartureTime();
                    indexDeparture++;
                }
                Arrays.sort(departures, indexFirstDeparture, indexDeparture);
                indexRoutes++;
            }
        }

        // only put used transit stops into the quad tree
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        Set<TransitStopFacility> stops = routeStopsPerStopFacility.keySet();
        for (TransitStopFacility stopFacility : stops) {
            double x = stopFacility.getCoord().getX();
            double y = stopFacility.getCoord().getY();

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
        QuadTree<TransitStopFacility> stopsQT = new QuadTree<>(minX, minY, maxX, maxY);
        for (TransitStopFacility stopFacility : stops) {
            double x = stopFacility.getCoord().getX();
            double y = stopFacility.getCoord().getY();
            stopsQT.put(x, y, stopFacility);
        }
        int countStopFacilities = stops.size();

        Map<Integer, RTransfer[]> allTransfers = calculateRouteStopTransfers(schedule, stopsQT, routeStopsPerStopFacility, routeStops, staticConfig);
        long countTransfers = 0;
        for (RTransfer[] transfers : allTransfers.values()) {
            countTransfers += transfers.length;
        }
        if (countTransfers > Integer.MAX_VALUE) {
            throw new RuntimeException("TransitSchedule has too many Transfers: " + countTransfers);
        }
        RTransfer[] transfers = new RTransfer[(int) countTransfers];
        int indexTransfer = 0;
        for (int routeStopIndex = 0; routeStopIndex < routeStops.length; routeStopIndex++) {
            RTransfer[] stopTransfers = allTransfers.get(routeStopIndex);
            int transferCount = stopTransfers == null ? 0 : stopTransfers.length;
            if (transferCount > 0) {
                RRouteStop routeStop = routeStops[routeStopIndex];
                routeStop.indexFirstTransfer = indexTransfer;
                routeStop.countTransfers = transferCount;
                System.arraycopy(stopTransfers, 0, transfers, indexTransfer, transferCount);
                indexTransfer += transferCount;
            }
        }

        SwissRailRaptorData data = new SwissRailRaptorData(staticConfig, countStopFacilities, routes, departures, routeStops, transfers, stopFacilityIndices, routeStopsPerStopFacility, stopsQT);

        long endMillis = System.currentTimeMillis();
        log.info("SwissRailRaptor data preparation done. Took " + (endMillis - startMillis) / 1000 + " seconds.");
        log.info("SwissRailRaptor statistics:  #routes = " + routes.length);
        log.info("SwissRailRaptor statistics:  #departures = " + departures.length);
        log.info("SwissRailRaptor statistics:  #routeStops = " + routeStops.length);
        log.info("SwissRailRaptor statistics:  #stopFacilities = " + countStopFacilities);
        log.info("SwissRailRaptor statistics:  #transfers (between routeStops) = " + transfers.length);
        return data;
    }

    // calculate possible transfers between TransitRouteStops
    private static Map<Integer, RTransfer[]> calculateRouteStopTransfers(TransitSchedule schedule, QuadTree<TransitStopFacility> stopsQT, Map<TransitStopFacility, int[]> routeStopsPerStopFacility, RRouteStop[] routeStops, RaptorStaticConfig config) {
        Map<Integer, RTransfer[]> transfers = new HashMap<>(stopsQT.size() * 5);
        double maxBeelineWalkConnectionDistance = config.getBeelineWalkConnectionDistance();
        double beelineWalkSpeed = config.getBeelineWalkSpeed();
        double beelineDistanceFactor = config.getBeelineWalkDistanceFactor();
        double minimalTransferTime = config.getMinimalTransferTime();

        Map<TransitStopFacility, List<TransitStopFacility>> stopToStopsTransfers = new HashMap<>();

        // first, add transfers based on distance
        for (TransitStopFacility fromStop : routeStopsPerStopFacility.keySet()) {
            Coord fromCoord = fromStop.getCoord();
            Collection<TransitStopFacility> nearbyStops = stopsQT.getDisk(fromCoord.getX(), fromCoord.getY(), maxBeelineWalkConnectionDistance);
            stopToStopsTransfers.computeIfAbsent(fromStop, stop -> new ArrayList<>(5)).addAll(nearbyStops);
        }

        // take the transfers from the schedule into account
        MinimalTransferTimes.MinimalTransferTimesIterator iter = schedule.getMinimalTransferTimes().iterator();
        while (iter.hasNext()) {
            iter.next();
            Id<TransitStopFacility> fromStopId = iter.getFromStopId();
            TransitStopFacility fromStop = schedule.getFacilities().get(fromStopId);
            Id<TransitStopFacility> toStopId = iter.getToStopId();
            TransitStopFacility toStop = schedule.getFacilities().get(toStopId);
            List<TransitStopFacility> destinationStops = stopToStopsTransfers.computeIfAbsent(fromStop, stop -> new ArrayList<>(5));
            if (!destinationStops.contains(toStop)) {
                destinationStops.add(toStop);
            }
        }

        // now calculate the transfers between the route stops
        MinimalTransferTimes mtt = schedule.getMinimalTransferTimes();
        ArrayList<RTransfer> stopTransfers = new ArrayList<>();
        for (Map.Entry<TransitStopFacility, List<TransitStopFacility>> e : stopToStopsTransfers.entrySet()) {
            TransitStopFacility fromStop = e.getKey();
            Coord fromCoord = fromStop.getCoord();
            int[] fromRouteStopIndices = routeStopsPerStopFacility.get(fromStop);
            Collection<TransitStopFacility> nearbyStops = e.getValue();
            for (TransitStopFacility toStop : nearbyStops) {
                int[] toRouteStopIndices = routeStopsPerStopFacility.get(toStop);
                double beelineDistance = CoordUtils.calcEuclideanDistance(fromCoord, toStop.getCoord());
                double transferTime = beelineDistance / beelineWalkSpeed;
                if (transferTime < minimalTransferTime) {
                    transferTime = minimalTransferTime;
                }

                transferTime = mtt.get(fromStop.getId(), toStop.getId(), transferTime);

                final double fixedTransferTime = transferTime; // variables must be effective final to be used in lambdas (below)

                for (int fromRouteStopIndex : fromRouteStopIndices) {
                    RRouteStop fromRouteStop = routeStops[fromRouteStopIndex];
                    stopTransfers.clear();
                    for (int toRouteStopIndex : toRouteStopIndices) {
                        RRouteStop toRouteStop = routeStops[toRouteStopIndex];
                        if (isUsefulTransfer(fromRouteStop, toRouteStop, maxBeelineWalkConnectionDistance, config.getOptimization())) {
                            RTransfer newTransfer = new RTransfer(fromRouteStopIndex, toRouteStopIndex, fixedTransferTime, beelineDistance * beelineDistanceFactor);
                            stopTransfers.add(newTransfer);
                        }
                    }
                    RTransfer[] newTransfers = stopTransfers.toArray(new RTransfer[0]);
                    transfers.compute(fromRouteStopIndex, (routeStopIndex, currentTransfers) -> {
                        if (currentTransfers == null) {
                            return newTransfers;
                        }
                        RTransfer[] tmp = new RTransfer[currentTransfers.length + newTransfers.length];
                        System.arraycopy(currentTransfers, 0, tmp, 0, currentTransfers.length);
                        System.arraycopy(newTransfers, 0, tmp, currentTransfers.length, newTransfers.length);
                        return tmp;
                    });
                }
            }
        }
        return transfers;
    }

    private static boolean isUsefulTransfer(RRouteStop fromRouteStop, RRouteStop toRouteStop, double maxBeelineWalkConnectionDistance, RaptorStaticConfig.RaptorOptimization optimization) {
        if (fromRouteStop == toRouteStop) {
            return false;
        }
        // there is no use to transfer away from the first stop in a route
        if (isFirstStopInRoute(fromRouteStop)) {
            return false;
        }
        // there is no use to transfer to the last stop in a route, we can't go anywhere from there
        if (isLastStopInRoute(toRouteStop)) {
            return false;
        }
        // if the first departure at fromRouteStop arrives after the last departure at toRouteStop,
        // we'll never get any connection here
        if (hasNoPossibleDeparture(fromRouteStop, toRouteStop)) {
            return false;
        }
        // if the stop facilities are different, and the destination stop is part
        // of the current route, it does not make sense to transfer here
        if (toStopIsPartOfRouteButNotSame(fromRouteStop, toRouteStop)) {
            return false;
        }
        // assuming vehicles serving the exact same stop sequence do not overtake each other,
        // it does not make sense to transfer to another route that serves the exact same upcoming stops
        if (cannotReachAdditionalStops(fromRouteStop, toRouteStop)) {
            return false;
        }
        if (optimization == RaptorStaticConfig.RaptorOptimization.OneToOneRouting) {
            // If one could have transferred to the same route one stop before, it does not make sense
            // to transfer here.
            // This optimization may lead to unexpected results in the case of OneToAllRouting ("tree"),
            // e.g. when starting at a single stop, users would expect that the stop facility
            // in the opposite direction could be reached within a minute or so by walk. But the algorithm
            // would find this if the transfers are missing.
            if (couldHaveTransferredOneStopEarlierInOppositeDirection(fromRouteStop, toRouteStop, maxBeelineWalkConnectionDistance)) {
                return false;
            }
        }
        // if we failed all other checks, it looks like this transfer is useful
        return true;
    }

    private static boolean isFirstStopInRoute(RRouteStop routeStop) {
        TransitRouteStop firstRouteStop = routeStop.route.getStops().get(0);
        return routeStop.routeStop == firstRouteStop;
    }

    private static boolean isLastStopInRoute(RRouteStop routeStop) {
        List<TransitRouteStop> routeStops = routeStop.route.getStops();
        TransitRouteStop lastRouteStop = routeStops.get(routeStops.size() - 1);
        return routeStop.routeStop == lastRouteStop;
    }

    private static boolean hasNoPossibleDeparture(RRouteStop fromRouteStop, RRouteStop toRouteStop) {
        Departure earliestDep = getEarliestDeparture(fromRouteStop.route);
        Departure latestDep = getLatestDeparture(toRouteStop.route);
        if (earliestDep == null || latestDep == null) {
            return true;
        }
        double earliestArrival = earliestDep.getDepartureTime() + fromRouteStop.arrivalOffset;
        double latestDeparture = latestDep.getDepartureTime() + toRouteStop.departureOffset;
        return earliestArrival > latestDeparture;
    }

    private static Departure getEarliestDeparture(TransitRoute route) {
        Departure earliest = null;
        for (Departure dep : route.getDepartures().values()) {
            if (earliest == null || dep.getDepartureTime() < earliest.getDepartureTime()) {
                earliest = dep;
            }
        }
        return earliest;
    }

    private static Departure getLatestDeparture(TransitRoute route) {
        Departure latest = null;
        for (Departure dep : route.getDepartures().values()) {
            if (latest == null || dep.getDepartureTime() > latest.getDepartureTime()) {
                latest = dep;
            }
        }
        return latest;
    }

    private static boolean toStopIsPartOfRouteButNotSame(RRouteStop fromRouteStop, RRouteStop toRouteStop) {
        TransitStopFacility fromStopFacility = fromRouteStop.routeStop.getStopFacility();
        TransitStopFacility toStopFacility = toRouteStop.routeStop.getStopFacility();
        if (fromStopFacility == toStopFacility) {
            return false;
        }
        for (TransitRouteStop routeStop : fromRouteStop.route.getStops()) {
            fromStopFacility = routeStop.getStopFacility();
            if (fromStopFacility == toStopFacility) {
                return true;
            }
        }
        return false;
    }

    private static boolean cannotReachAdditionalStops(RRouteStop fromRouteStop, RRouteStop toRouteStop) {
        Iterator<TransitRouteStop> fromIter = fromRouteStop.route.getStops().iterator();
        while (fromIter.hasNext()) {
            TransitRouteStop routeStop = fromIter.next();
            if (fromRouteStop.routeStop == routeStop) {
                break;
            }
        }
        Iterator<TransitRouteStop> toIter = toRouteStop.route.getStops().iterator();
        while (toIter.hasNext()) {
            TransitRouteStop routeStop = toIter.next();
            if (toRouteStop.routeStop == routeStop) {
                break;
            }
        }
        // both iterators now point to the route stops where the potential transfer happens
        while (true) {
            boolean fromRouteHasNext = fromIter.hasNext();
            boolean toRouteHasNext = toIter.hasNext();
            if (!toRouteHasNext) {
                // there are no more stops in the toRoute
                return true;
            }
            if (!fromRouteHasNext) {
                // there are no more stops in the fromRoute, but there are in the toRoute
                return false;
            }
            TransitRouteStop fromStop = fromIter.next();
            TransitRouteStop toStop = toIter.next();
            if (fromStop.getStopFacility() != toStop.getStopFacility()) {
                // the toRoute goes to a different stop
                return false;
            }
        }
    }

    private static boolean couldHaveTransferredOneStopEarlierInOppositeDirection(RRouteStop fromRouteStop, RRouteStop toRouteStop, double maxBeelineWalkConnectionDistance) {
        TransitRouteStop previousRouteStop = null;
        for (TransitRouteStop routeStop : fromRouteStop.route.getStops()) {
            if (fromRouteStop.routeStop == routeStop) {
                break;
            }
            previousRouteStop = routeStop;
        }
        if (previousRouteStop == null) {
            return false;
        }

        Iterator<TransitRouteStop> toIter = toRouteStop.route.getStops().iterator();
        while (toIter.hasNext()) {
            TransitRouteStop routeStop = toIter.next();
            if (toRouteStop.routeStop == routeStop) {
                break;
            }
        }
        boolean toRouteHasNext = toIter.hasNext();
        if (!toRouteHasNext) {
            return false;
        }

        TransitRouteStop toStop = toIter.next();
        if (previousRouteStop.getStopFacility() == toStop.getStopFacility()) {
            return true;
        }

        double distance = CoordUtils.calcEuclideanDistance(previousRouteStop.getStopFacility().getCoord(), toStop.getStopFacility().getCoord());
        return distance < maxBeelineWalkConnectionDistance;
    }

    public Collection<TransitStopFacility> findNearbyStops(double x, double y, double distance) {
        return this.stopsQT.getDisk(x, y, distance);
    }

    public TransitStopFacility findNearestStop(double x, double y) {
        return this.stopsQT.getClosest(x, y);
    }

    static final class RRoute {
        final int indexFirstRouteStop;
        final int countRouteStops;
        final int indexFirstDeparture;
        final int countDepartures;

        RRoute(int indexFirstRouteStop, int countRouteStops, int indexFirstDeparture, int countDepartures) {
            this.indexFirstRouteStop = indexFirstRouteStop;
            this.countRouteStops = countRouteStops;
            this.indexFirstDeparture = indexFirstDeparture;
            this.countDepartures = countDepartures;
        }
    }

    static final class RRouteStop {
        final TransitRouteStop routeStop;
        final TransitLine line;
        final TransitRoute route;
        final String mode;
        final int transitRouteIndex;
        final int stopFacilityIndex;
        final double arrivalOffset;
        final double departureOffset;
        final double distanceAlongRoute;
        int indexFirstTransfer = -1;
        int countTransfers = 0;

        RRouteStop(TransitRouteStop routeStop, TransitLine line, TransitRoute route, String mode, int transitRouteIndex, int stopFacilityIndex, double distanceAlongRoute) {
            this.routeStop = routeStop;
            this.line = line;
            this.route = route;
            this.mode = mode;
            this.transitRouteIndex = transitRouteIndex;
            this.stopFacilityIndex = stopFacilityIndex;
            this.distanceAlongRoute = distanceAlongRoute;
            // "normalize" the arrival and departure offsets, make sure they are always well defined.
            this.arrivalOffset = isUndefinedTime(routeStop.getArrivalOffset()) ? routeStop.getDepartureOffset() : routeStop.getArrivalOffset();
            this.departureOffset = isUndefinedTime(routeStop.getDepartureOffset()) ? routeStop.getArrivalOffset() : routeStop.getDepartureOffset();
        }

        private static boolean isUndefinedTime(double time) {
            return Time.isUndefinedTime(time) || Double.isNaN(time);
        }
    }

    static final class RTransfer {
        final int fromRouteStop;
        final int toRouteStop;
        final double transferTime;
        final double transferDistance;

        RTransfer(int fromRouteStop, int toRouteStop, double transferTime, double transferDistance) {
            this.fromRouteStop = fromRouteStop;
            this.toRouteStop = toRouteStop;
            this.transferTime = transferTime;
            this.transferDistance = transferDistance;
        }
    }
    
    /*
     * synchronized in order to avoid that multiple quad trees for the very same stop filter attribute/value combination are prepared at the same time 
     */
	public synchronized void prepareStopFilterQuadTreeIfNotExistent(String stopFilterAttribute, String stopFilterValue) {
		if (!stopFilterAttribute2Value2StopsQT.containsKey(stopFilterAttribute)) {
			stopFilterAttribute2Value2StopsQT.put(stopFilterAttribute, new HashMap<>());
		}
	    Set<TransitStopFacility> stops = routeStopsPerStopFacility.keySet();
        QuadTree<TransitStopFacility> stopsQTFiltered = new QuadTree<>(stopsQT.getMinEasting(), stopsQT.getMinNorthing(), stopsQT.getMaxEasting(), stopsQT.getMaxNorthing());
        for (TransitStopFacility stopFacility : stops) {
			Object attr = stopFacility.getAttributes().getAttribute(stopFilterAttribute);
			String attrValue = attr == null ? null : attr.toString();
			if (stopFilterValue.equals(attrValue)) {
	            double x = stopFacility.getCoord().getX();
	            double y = stopFacility.getCoord().getY();
	            stopsQTFiltered.put(x, y, stopFacility);
			}
        }
        stopFilterAttribute2Value2StopsQT.get(stopFilterAttribute).put(stopFilterValue, stopsQTFiltered);
	}
}
