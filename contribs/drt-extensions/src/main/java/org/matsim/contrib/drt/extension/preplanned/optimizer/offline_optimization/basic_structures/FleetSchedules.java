package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public record FleetSchedules(
        Map<Id<DvrpVehicle>, List<TimetableEntry>> vehicleToTimetableMap,
        Map<List<Id<Person>>, Id<DvrpVehicle>> requestIdToVehicleMap,
        Map<List<Id<Person>>, GeneralRequest> pendingRequests) {

    public static List<TimetableEntry> copyTimetable(List<TimetableEntry> timetable) {
        List<TimetableEntry> timetableCopy = new ArrayList<>();
        for (TimetableEntry timetableEntry : timetable) {
            timetableCopy.add(new TimetableEntry(timetableEntry));
        }
        return timetableCopy;
    }

    public static FleetSchedules initializeFleetSchedules(Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap) {
        Map<Id<DvrpVehicle>, List<TimetableEntry>> vehicleToTimetableMap = new LinkedHashMap<>();
        for (OnlineVehicleInfo vehicleInfo : onlineVehicleInfoMap.values()) {
            vehicleToTimetableMap.put(vehicleInfo.vehicle().getId(), new ArrayList<>());
        }
        Map<List<Id<Person>>, Id<DvrpVehicle>> requestIdToVehicleMap = new HashMap<>();
        Map<List<Id<Person>>, GeneralRequest> rejectedRequests = new LinkedHashMap<>();
        return new FleetSchedules(vehicleToTimetableMap, requestIdToVehicleMap, rejectedRequests);
    }

    public FleetSchedules copySchedule() {
        Map<Id<DvrpVehicle>, List<TimetableEntry>> vehicleToTimetableMapCopy = new LinkedHashMap<>();
        for (Id<DvrpVehicle> vehicleId : this.vehicleToTimetableMap().keySet()) {
            vehicleToTimetableMapCopy.put(vehicleId, copyTimetable(this.vehicleToTimetableMap.get(vehicleId)));
        }
        var requestIdToVehicleMapCopy = new HashMap<>(this.requestIdToVehicleMap);
        var rejectedRequestsCopy = new LinkedHashMap<>(this.pendingRequests);

        return new FleetSchedules(vehicleToTimetableMapCopy, requestIdToVehicleMapCopy, rejectedRequestsCopy);
    }

    public void updateFleetSchedule(Network network, LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix,
                                    Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap) {
        for (Id<DvrpVehicle> vehicleId : onlineVehicleInfoMap.keySet()) {
            // When new vehicle enters service, create a new entry for it
            this.vehicleToTimetableMap().computeIfAbsent(vehicleId, t -> new ArrayList<>());
            if (!onlineVehicleInfoMap.containsKey(vehicleId)) {
                // When a vehicle ends service, remove it from the schedule
                this.vehicleToTimetableMap().remove(vehicleId);
            }
        }

        for (Id<DvrpVehicle> vehicleId : this.vehicleToTimetableMap().keySet()) {
            List<TimetableEntry> timetable = this.vehicleToTimetableMap().get(vehicleId);
            if (!timetable.isEmpty()) {
                Link currentLink = onlineVehicleInfoMap.get(vehicleId).currentLink();
                double currentTime = onlineVehicleInfoMap.get(vehicleId).divertableTime();
                for (TimetableEntry timetableEntry : timetable) {
                    Id<Link> stopLinkId = timetableEntry.getStopType() == TimetableEntry.StopType.PICKUP ?
                            timetableEntry.getRequest().getFromLinkId() : timetableEntry.getRequest().getToLinkId();
                    Link stopLink = network.getLinks().get(stopLinkId);
                    double newArrivalTime = currentTime + linkToLinkTravelTimeMatrix.getTravelTime(currentLink, stopLink, currentTime);
                    timetableEntry.updateArrivalTime(newArrivalTime);

                    // Delay the latest arrival time of the stop when necessary (e.g., due to traffic uncertainty), in order to make sure assigned requests will remain feasible
                    if (timetableEntry.getStopType() == TimetableEntry.StopType.PICKUP) {
                        double originalLatestDepartureTime = timetableEntry.getRequest().getLatestDepartureTime();
                        timetableEntry.getRequest().setLatestDepartureTime(Math.max(originalLatestDepartureTime, newArrivalTime));
                    } else {
                        double originalLatestArrivalTime = timetableEntry.getRequest().getLatestArrivalTime();
                        timetableEntry.getRequest().setLatestArrivalTime(Math.max(originalLatestArrivalTime, newArrivalTime));
                    }

                    currentTime = timetableEntry.getDepartureTime();
                    currentLink = stopLink;
                }
            }
        }
    }
}
