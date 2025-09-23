package org.matsim.drtExperiments.offlineStrategy;

import one.util.streamex.EntryStream;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.zone.skims.Matrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrices;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

/**
 * Link to link travel time to be used by the offline solver *
 */
public class LinkToLinkTravelTimeMatrix {
    private final TravelTimeMatrix nodeToNodeTravelTimeMatrix;
    private final TravelTime travelTime;
    private final Network network;

    LinkToLinkTravelTimeMatrix(Network network, TravelTime travelTime, Set<Id<Link>> relevantLinks, double time) {
        this.network = network;
        this.travelTime = travelTime;
        this.nodeToNodeTravelTimeMatrix = calculateTravelTimeMatrix(relevantLinks, time);
    }

    public static LinkToLinkTravelTimeMatrix prepareLinkToLinkTravelMatrix(Network network, TravelTime travelTime, FleetSchedules previousSchedules,
                                                                           Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap, List<GeneralRequest> newRequests,
                                                                           double time) {
        Set<Id<Link>> relevantLinks = new HashSet<>();

        // Vehicle locations
        for (OnlineVehicleInfo onlineVehicleInfo : onlineVehicleInfoMap.values()) {
            relevantLinks.add(onlineVehicleInfo.currentLink().getId());
        }

        // Requests locations
        // requests on the timetable
        for (List<TimetableEntry> timetable : previousSchedules.vehicleToTimetableMap().values()) {
            for (TimetableEntry timetableEntry : timetable) {
                if (timetableEntry.getStopType() == TimetableEntry.StopType.PICKUP) {
                    relevantLinks.add(timetableEntry.getRequest().getFromLinkId());
                } else {
                    relevantLinks.add(timetableEntry.getRequest().getToLinkId());
                }
            }
        }

        // new requests
        for (GeneralRequest request : newRequests) {
            relevantLinks.add(request.getFromLinkId());
            relevantLinks.add(request.getToLinkId());
        }

        // Pending rejected requests (i.e., not yet properly inserted and not yet formally rejected)
        for (GeneralRequest request : previousSchedules.pendingRequests().values()) {
            relevantLinks.add(request.getFromLinkId());
            relevantLinks.add(request.getToLinkId());
        }

        return new LinkToLinkTravelTimeMatrix(network, travelTime, relevantLinks, time);
    }

    @Deprecated
    public void updateFleetSchedule(FleetSchedules previousSchedules,
                                    Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap) {
        for (Id<DvrpVehicle> vehicleId : onlineVehicleInfoMap.keySet()) {
            previousSchedules.vehicleToTimetableMap().computeIfAbsent(vehicleId, t -> new ArrayList<>()); // When new vehicle enters service, create a new entry for it
            if (!onlineVehicleInfoMap.containsKey(vehicleId)) {
                previousSchedules.vehicleToTimetableMap().remove(vehicleId); // When a vehicle ends service, remove it from the schedule
            }
        }

        for (Id<DvrpVehicle> vehicleId : previousSchedules.vehicleToTimetableMap().keySet()) {
            List<TimetableEntry> timetable = previousSchedules.vehicleToTimetableMap().get(vehicleId);
            if (!timetable.isEmpty()) {
                Link currentLink = onlineVehicleInfoMap.get(vehicleId).currentLink();
                double currentTime = onlineVehicleInfoMap.get(vehicleId).divertableTime();
                for (TimetableEntry timetableEntry : timetable) {
                    Id<Link> stopLinkId = timetableEntry.getStopType() == TimetableEntry.StopType.PICKUP ?
                            timetableEntry.getRequest().getFromLinkId() : timetableEntry.getRequest().getToLinkId();
                    Link stopLink = network.getLinks().get(stopLinkId);
                    double newArrivalTime = currentTime + this.getTravelTime(currentLink, stopLink, currentTime);
                    timetableEntry.updateArrivalTime(newArrivalTime);
                    currentTime = timetableEntry.getDepartureTime();
                    currentLink = stopLink;
                }
            }
        }
    }

    public double getTravelTime(Link fromLink, Link toLink, double departureTime) {
        if (fromLink.getId().toString().equals(toLink.getId().toString())) {
            return 0;
        }
        double travelTimeFromNodeToNode = nodeToNodeTravelTimeMatrix.getTravelTime(fromLink.getToNode(), toLink.getFromNode(), departureTime);
        return FIRST_LINK_TT + travelTimeFromNodeToNode
                + VrpPaths.getLastLinkTT(travelTime, toLink, departureTime + travelTimeFromNodeToNode);
    }

    private TravelTimeMatrix calculateTravelTimeMatrix(Set<Id<Link>> relevantLinks, double time) {
        Map<Node, Zone> zoneByNode = relevantLinks
                .stream()
                .flatMap(linkId -> Stream.of(network.getLinks().get(linkId).getFromNode(), network.getLinks().get(linkId).getToNode()))
                .collect(toMap(n -> n, node -> new ZoneImpl(Id.create(node.getId(), Zone.class ), null, node.getCoord(), "node"),
                        (zone1, zone2) -> zone1));
        var nodeByZone = EntryStream.of(zoneByNode).invert().toMap();
        Matrix nodeToNodeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix( new TravelTimeMatrices.RoutingParams( network, travelTime,
                new TimeAsTravelDisutility(travelTime), Runtime.getRuntime().availableProcessors()) , nodeByZone, time );

        return (fromNode, toNode, departureTime) -> nodeToNodeMatrix.get(zoneByNode.get(fromNode), zoneByNode.get(toNode));
    }
}
