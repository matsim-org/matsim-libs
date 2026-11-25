package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author nkuehnel / MOIA
 */
public class NearestOperationFacilityWithCapacityFinder implements OperationFacilityFinder {

    private final OperationFacilities operationFacilities;
    private final OperationFacilityReservationManager reservationManager;
    private final LeastCostPathCalculator router;
    private final Network network;
    private final MobsimTimer timer;
    private final TravelTime travelTime;

    /*
     * Highest ratio of beeline distance to free speed travel time over all links of the network. Since a link is not
     * traversed faster than at free speed, no path from a to b takes less than beeline(a, b) / maxBeelineSpeed. This
     * lower bound lets the finder route only to the facilities that can still be the nearest one. It does not assume
     * that link lengths are at least the beeline distance.
     */
    private final double maxBeelineSpeed;

    public NearestOperationFacilityWithCapacityFinder(OperationFacilities operationFacilities,
                                                      OperationFacilityReservationManager reservationManager,
                                                      Network network,
                                                      TravelTime travelTime,
                                                      TravelDisutility travelDisutility, MobsimTimer timer) {
        this.operationFacilities = operationFacilities;
        this.reservationManager = reservationManager;
        this.network = network;
        this.timer = timer;
        this.travelTime = travelTime;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
        this.maxBeelineSpeed = calculateMaxBeelineSpeed(network);
    }

    static double calculateMaxBeelineSpeed(Network network) {
        double maxSpeed = 0.0;
        for (Link link : network.getLinks().values()) {
            double beeline = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
            double freeSpeedTime = link.getLength() / link.getFreespeed();
            if (beeline > 0.0) {
                maxSpeed = Math.max(maxSpeed, freeSpeedTime > 0.0 ? beeline / freeSpeedTime : Double.POSITIVE_INFINITY);
            }
        }
        return maxSpeed;
    }

    private static Predicate<OperationFacility> typefilter(Set<OperationFacilityType> types) {
        if (types == null || types.isEmpty()) {
            return f -> true;
        }
        return f -> types.contains(f.getType());
    }

    @Override
    public Optional<FacilityWithPath> findFacility(Link fromLink, DvrpVehicle dvrpVehicle, Set<OperationFacilityType> types) {
        return findNearest(fromLink, dvrpVehicle, timer.getTimeOfDay(), Double.POSITIVE_INFINITY,
                dvrpVehicle.getServiceEndTime(), types);
    }

    @Override
    public Optional<FacilityWithPath> findFacilityForTime(
            Link fromLink,
            DvrpVehicle dvrpVehicle,
            double departureTime,
            double latestArrival,
            double reservationEndTime,
            Set<OperationFacilityType> types) {
        return findNearest(fromLink, dvrpVehicle, departureTime, latestArrival, reservationEndTime, types);
    }

    private record Candidate(OperationFacility facility, Link toLink, int index, double minTravelTime) {
    }

    /**
     * Returns the facility with the shortest travel time among those that are reached by latestArrival and available
     * until reservationEndTime; among equal travel times, the first one in the order of the facilities. This is the
     * same result as routing to every facility. The facilities are visited in the order of their lower travel time
     * bound, and routing stops once the bound exceeds the best travel time found.
     */
    private Optional<FacilityWithPath> findNearest(Link fromLink, DvrpVehicle dvrpVehicle, double departureTime,
                                                   double latestArrival, double reservationEndTime,
                                                   Set<OperationFacilityType> types) {
        Predicate<OperationFacility> filter = typefilter(types);
        List<Candidate> candidates = new ArrayList<>();
        int index = 0;
        for (OperationFacility facility : operationFacilities.getFacilities().values()) {
            if (filter.test(facility)) {
                Link toLink = network.getLinks().get(facility.getLinkId());
                double beeline = CoordUtils.calcEuclideanDistance(fromLink.getToNode().getCoord(),
                        toLink.getToNode().getCoord());
                double minTravelTime = maxBeelineSpeed > 0.0 ? beeline / maxBeelineSpeed : 0.0;
                candidates.add(new Candidate(facility, toLink, index, minTravelTime));
            }
            index++;
        }
        candidates.sort(Comparator.comparingDouble(Candidate::minTravelTime).thenComparingInt(Candidate::index));

        FacilityWithPath best = null;
        int bestIndex = Integer.MAX_VALUE;
        for (Candidate candidate : candidates) {
            if (best != null && candidate.minTravelTime() > best.path().getTravelTime()) {
                break; // no remaining facility can be reached faster
            }
            if (departureTime + candidate.minTravelTime() > latestArrival) {
                continue; // cannot be reached in time
            }

            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, candidate.toLink(), departureTime, router,
                    travelTime);
            if (path.getArrivalTime() > latestArrival) {
                continue;
            }
            if (best != null && (path.getTravelTime() > best.path().getTravelTime()
                    || (path.getTravelTime() == best.path().getTravelTime() && candidate.index() > bestIndex))) {
                continue;
            }
            if (!reservationManager.isAvailable(candidate.facility(), dvrpVehicle, path.getArrivalTime(),
                    reservationEndTime)) {
                continue;
            }
            best = new FacilityWithPath(candidate.facility(), path);
            bestIndex = candidate.index();
        }
        return Optional.ofNullable(best);
    }
}
