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

import java.util.Comparator;
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
    }

    private static Predicate<OperationFacility> typefilter(Set<OperationFacilityType> types) {
        if (types == null || types.isEmpty()) {
            return f -> true;
        }
        return f -> types.contains(f.getType());
    }

    @Override
    public Optional<FacilityWithPath> findFacility(Link fromLink, DvrpVehicle dvrpVehicle, Set<OperationFacilityType> types) {
        Predicate<OperationFacility> filter = typefilter(types);
        Optional<FacilityWithPath> min = operationFacilities.getFacilities().values().stream()
                .filter(filter)
                .map(operationFacility -> {
                    Link toLink = network.getLinks().get(operationFacility.getLinkId());
                    VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, timer.getTimeOfDay(), router, travelTime);
                    return new FacilityWithPath(operationFacility, path);
                })
                .filter(facilityWithPath -> reservationManager.isAvailable(facilityWithPath.operationFacility(), dvrpVehicle,
                        facilityWithPath.path().getArrivalTime(), dvrpVehicle.getServiceEndTime()))
                .min(Comparator.comparing(f -> f.path().getTravelTime()));
        return min;
    }

    @Override
    public Optional<FacilityWithPath> findFacilityForTime(
            Link fromLink,
            DvrpVehicle dvrpVehicle,
            double departureTime,
            double latestArrival,
            double reservationEndTime,
            Set<OperationFacilityType> types) {
        Predicate<OperationFacility> filter = typefilter(types);
        Optional<FacilityWithPath> min = operationFacilities.getFacilities().values().stream()
                .filter(filter)
                .map(operationFacility -> {
                    Link toLink = network.getLinks().get(operationFacility.getLinkId());
                    VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime);
                    return new FacilityWithPath(operationFacility, path);
                })
                .filter(facilityWithPath -> facilityWithPath.path().getArrivalTime() <= latestArrival)
                .filter(facilityWithPath -> reservationManager.isAvailable(facilityWithPath.operationFacility(), dvrpVehicle,
                        facilityWithPath.path().getArrivalTime(), reservationEndTime))
                .min(Comparator.comparing(f -> f.path().getTravelTime()));
        return min;
    }
}
