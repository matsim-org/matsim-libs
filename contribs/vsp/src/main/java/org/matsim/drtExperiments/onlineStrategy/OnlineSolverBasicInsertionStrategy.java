package org.matsim.drtExperiments.onlineStrategy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegerLoad;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;
import org.matsim.drtExperiments.utils.DrtOperationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

public class OnlineSolverBasicInsertionStrategy implements OnlineSolver {
    private final Network network;
    private final double stopDuration;
    private final TravelTimeMatrix travelTimeMatrix;
    private final TravelTime travelTime;
    private final LeastCostPathCalculator router;

    public OnlineSolverBasicInsertionStrategy(Network network, DrtConfigGroup drtConfigGroup, TravelTimeMatrix travelTimeMatrix,
                                       TravelTime travelTime, TravelDisutility travelDisutility) {
        this.network = network;
        this.stopDuration = drtConfigGroup.getStopDuration();
        this.travelTimeMatrix = travelTimeMatrix;
        this.travelTime = travelTime;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }

    private static final DvrpLoad loadZero = IntegerLoad.fromValue( 0 );
    private static final DvrpLoad loadOne = IntegerLoad.fromValue( 0 );

    @Override
    public Id<DvrpVehicle> insert(DrtRequest request, Map<Id<DvrpVehicle>, List<TimetableEntry>> timetables,
                                  Map<Id<DvrpVehicle>, OnlineVehicleInfo> realTimeVehicleInfoMap) {
        // Request information
        Link fromLink = request.getFromLink();
        Link toLink = request.getToLink();
        double latestPickUpTime = request.getLatestStartTime();
        double latestArrivalTime = request.getLatestArrivalTime();
        GeneralRequest spontaneousRequest = DrtOperationUtils.createFromDrtRequest(request);

        // Try to find the best insertion
        double bestInsertionCost = Double.MAX_VALUE;
        DvrpVehicle selectedVehicle = null;
        List<TimetableEntry> updatedTimetable = null;

        for (Id<DvrpVehicle> vehicleId : timetables.keySet()) {
            OnlineVehicleInfo vehicleInfo = realTimeVehicleInfoMap.get(vehicleId);
            Link currentLink = vehicleInfo.currentLink();
            double divertableTime = vehicleInfo.divertableTime();
            double serviceEndTime = vehicleInfo.vehicle().getServiceEndTime() - stopDuration;

            List<TimetableEntry> originalTimetable = timetables.get(vehicleId);

            // 1 If original timetable is empty
            if (originalTimetable.isEmpty()) {
                double timeToPickup = calculateVrpTravelTimeFromMatrix(currentLink, fromLink, divertableTime);
                double arrivalTimePickUp = divertableTime + timeToPickup;
                double tripTravelTime = calculateVrpTravelTimeFromMatrix(fromLink, toLink, arrivalTimePickUp + stopDuration);
                double arrivalTimeDropOff = arrivalTimePickUp + stopDuration + tripTravelTime;
                double totalInsertionCost = timeToPickup + tripTravelTime;
                if (arrivalTimePickUp > latestPickUpTime || arrivalTimePickUp > serviceEndTime) {
                    continue;
                }

                if (totalInsertionCost < bestInsertionCost) {
                    bestInsertionCost = totalInsertionCost;
                    selectedVehicle = vehicleInfo.vehicle();
                    updatedTimetable = new ArrayList<>();
                    updatedTimetable.add(new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.PICKUP, arrivalTimePickUp, arrivalTimePickUp + stopDuration, loadZero, stopDuration, selectedVehicle));
                    updatedTimetable.add(new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.DROP_OFF, arrivalTimeDropOff, arrivalTimeDropOff + stopDuration, loadOne, stopDuration, selectedVehicle));
                    // Note: The departure time of the last stop is actually not meaningful, but this stop may become non-last stop later, therefore, we set the departure time of this stop as if it is a middle stop
                }
                continue;
            }

            // 2 If the timetable is not empty
            // Try to insert request in the timetable, BEFORE stop i (i.e., not including appending at the end)
            boolean noNeedToContinueWithThisVehicle = false;
            for (int i = 0; i < originalTimetable.size(); i++) {
                TimetableEntry stopAfterPickUpInsertion = originalTimetable.get(i);
                if (stopAfterPickUpInsertion.isVehicleFullBeforeThisStop()) {
                    continue; // Not possible to insert pickup at this location, try next location
                }
                Link linkOfStopAfterPickUpInsertion = network.getLinks().get(stopAfterPickUpInsertion.getLinkId());

                double detourA;
                double detourB;
                double pickupTime;
                double delayCausedByPickupDetour;
                if (i == 0) {
                    detourA = calculateVrpTravelTimeFromMatrix(currentLink, fromLink, divertableTime);
                    pickupTime = divertableTime + detourA;
                    if (pickupTime > latestPickUpTime || pickupTime > serviceEndTime) {
                        noNeedToContinueWithThisVehicle = true;
                        break; // Vehicle cannot reach the pickup location in time. No need to continue with this vehicle
                    }
                    detourB = calculateVrpTravelTimeFromMatrix(fromLink, linkOfStopAfterPickUpInsertion, pickupTime + stopDuration);
                    delayCausedByPickupDetour = detourA + detourB - calculateVrpTravelTimeFromMatrix(currentLink, linkOfStopAfterPickUpInsertion, divertableTime);
                } else {
                    TimetableEntry stopBeforePickUpInsertion = originalTimetable.get(i - 1);
                    Link linkOfStopBeforePickUpInsertion = network.getLinks().get(stopBeforePickUpInsertion.getLinkId());
                    detourA = calculateVrpTravelTimeFromMatrix(linkOfStopBeforePickUpInsertion, fromLink, stopBeforePickUpInsertion.getDepartureTime());
                    pickupTime = stopBeforePickUpInsertion.getDepartureTime() + detourA;
                    if (pickupTime > latestPickUpTime || pickupTime > serviceEndTime) {
                        noNeedToContinueWithThisVehicle = true;
                        break; // Vehicle cannot reach the pickup location in time from this point. No need to continue on the timetable.
                    }
                    detourB = calculateVrpTravelTimeFromMatrix(fromLink, linkOfStopAfterPickUpInsertion, pickupTime + stopDuration);
                    delayCausedByPickupDetour = detourA + detourB - calculateVrpTravelTimeFromMatrix(linkOfStopBeforePickUpInsertion, linkOfStopAfterPickUpInsertion, stopBeforePickUpInsertion.getDepartureTime());
                }

                delayCausedByPickupDetour = Math.max(0, delayCausedByPickupDetour); // Due to the inaccuracy of the TT matrix, the delay may be smaller than 0, which is not meaningful
                boolean isPickupFeasible = isInsertionFeasible(originalTimetable, i, delayCausedByPickupDetour + stopDuration, serviceEndTime);
                if (isPickupFeasible) {
                    TimetableEntry pickupStopToInsert = new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.PICKUP,
                            pickupTime, pickupTime + stopDuration, stopAfterPickUpInsertion.getOccupancyBeforeStop(), stopDuration, vehicleInfo.vehicle());
                    List<TimetableEntry> temporaryTimetable = insertPickup(originalTimetable, i, pickupStopToInsert, delayCausedByPickupDetour + stopDuration);

                    // Try to insert drop off from here (insert drop off AFTER the stop j)
                    for (int j = i; j < temporaryTimetable.size(); j++) {
                        if (temporaryTimetable.get(j).isVehicleOverloaded()) {
                            break; // Drop off must be inserted before this stop. No need to continue with the timetable
                        }
                        TimetableEntry stopBeforeDropOffInsertion = temporaryTimetable.get(j);
                        Link linkOfStopBeforeDropOffInsertion = network.getLinks().get(stopBeforeDropOffInsertion.getLinkId());
                        if (j + 1 < temporaryTimetable.size()) {
                            // Append drop off between j and j+1
                            TimetableEntry stopAfterDropOffInsertion = temporaryTimetable.get(j + 1);
                            Link linkOfStopAfterDropOffInsertion = network.getLinks().get(stopAfterDropOffInsertion.getLinkId());
                            double detourC = calculateVrpTravelTimeFromMatrix(linkOfStopBeforeDropOffInsertion, toLink, stopBeforeDropOffInsertion.getDepartureTime());
                            double dropOffTime = detourC + stopBeforeDropOffInsertion.getDepartureTime();
                            if (dropOffTime > latestArrivalTime || dropOffTime > serviceEndTime) {
                                break; // No more drop-off feasible after this stop. No need to continue in the timetable
                            }
                            double detourD = calculateVrpTravelTimeFromMatrix(toLink, linkOfStopAfterDropOffInsertion, dropOffTime + stopDuration);
                            double delayCausedByDropOffDetour = detourC + detourD - calculateVrpTravelTimeFromMatrix(linkOfStopBeforeDropOffInsertion, linkOfStopAfterDropOffInsertion, stopBeforeDropOffInsertion.getDepartureTime());
                            delayCausedByDropOffDetour = Math.max(0, delayCausedByDropOffDetour);
                            boolean isDropOffIsFeasible = isInsertionFeasible(temporaryTimetable, j + 1, delayCausedByDropOffDetour + stopDuration, serviceEndTime);
                            double totalInsertionCost = delayCausedByDropOffDetour + delayCausedByPickupDetour; // Currently, we assume cost = total extra drive time caused by the insertion

                            if (isDropOffIsFeasible && totalInsertionCost < bestInsertionCost) {
                                TimetableEntry dropOffStopToInsert = new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.DROP_OFF,
                                        dropOffTime, dropOffTime + stopDuration, stopAfterDropOffInsertion.getOccupancyBeforeStop(), stopDuration, vehicleInfo.vehicle());
                                updatedTimetable = insertDropOff(temporaryTimetable, j + 1, dropOffStopToInsert, delayCausedByDropOffDetour + stopDuration);
                                bestInsertionCost = totalInsertionCost;
                                selectedVehicle = vehicleInfo.vehicle();
                            }
                        } else {
                            // Append drop off at the end
                            double detourC = calculateVrpTravelTimeFromMatrix(linkOfStopBeforeDropOffInsertion, toLink, stopBeforeDropOffInsertion.getDepartureTime());
                            double dropOffTime = detourC + stopBeforeDropOffInsertion.getDepartureTime();
                            double totalInsertionCost = detourC + delayCausedByPickupDetour;
                            boolean isDropOffFeasible = dropOffTime <= latestArrivalTime && dropOffTime <= serviceEndTime;

                            if (isDropOffFeasible && totalInsertionCost < bestInsertionCost) {
                                TimetableEntry dropOffStopToInsert = new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.DROP_OFF,
                                        dropOffTime, dropOffTime + stopDuration, loadOne, stopDuration, vehicleInfo.vehicle());
                                updatedTimetable = insertDropOff(temporaryTimetable, j + 1, dropOffStopToInsert, detourC + stopDuration);
                                bestInsertionCost = totalInsertionCost;
                                selectedVehicle = vehicleInfo.vehicle();
                            }
                        }
                    }
                }
            }

            // Try to append the request at the end
            if (!noNeedToContinueWithThisVehicle) {
                TimetableEntry stopBeforePickUpInsertion = originalTimetable.get(originalTimetable.size() - 1);
                Link linkOfStopBeforePickUpInsertion = network.getLinks().get(stopBeforePickUpInsertion.getLinkId());
                double timeToPickUp = calculateVrpTravelTimeFromMatrix(linkOfStopBeforePickUpInsertion, fromLink, stopBeforePickUpInsertion.getDepartureTime());
                double pickupTime = stopBeforePickUpInsertion.getDepartureTime() + timeToPickUp;
                if (pickupTime <= latestPickUpTime) {
                    double tripTravelTime = calculateVrpTravelTimeFromMatrix(fromLink, toLink, pickupTime + stopDuration);
                    double dropOffTime = pickupTime + stopDuration + tripTravelTime;
                    double totalInsertionCost = timeToPickUp + tripTravelTime;
                    if (totalInsertionCost < bestInsertionCost) {
                        TimetableEntry pickupStopToInsert = new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.PICKUP,
                                pickupTime, pickupTime + stopDuration, loadZero, stopDuration, vehicleInfo.vehicle());
                        TimetableEntry dropOffStopToInsert = new TimetableEntry(spontaneousRequest, TimetableEntry.StopType.DROP_OFF,
                                dropOffTime, dropOffTime + stopDuration, loadOne, stopDuration, vehicleInfo.vehicle());
                        List<TimetableEntry> temporaryTimetable = insertPickup(originalTimetable, originalTimetable.size(), pickupStopToInsert, timeToPickUp + stopDuration);

                        updatedTimetable = insertDropOff(temporaryTimetable, temporaryTimetable.size(), dropOffStopToInsert, tripTravelTime + stopDuration);
                        bestInsertionCost = totalInsertionCost;
                        selectedVehicle = vehicleInfo.vehicle();
                    }
                }
            }
        }

        // Insert the request to the best vehicle
        if (selectedVehicle != null) {
            updateTimetableWithAccurateTravelTime(realTimeVehicleInfoMap.get(selectedVehicle.getId()), updatedTimetable);
            timetables.put(selectedVehicle.getId(), updatedTimetable);
            return selectedVehicle.getId();
        }
        return null;
    }

    /**
     * Re-calculate the timetable based on accurate travel time information
     */
    private void updateTimetableWithAccurateTravelTime(OnlineVehicleInfo onlineVehicleInfo, List<TimetableEntry> updatedTimetable) {
        double currentTime = onlineVehicleInfo.divertableTime();
        Link currentLink = onlineVehicleInfo.currentLink();
        for (TimetableEntry stop : updatedTimetable) {
            Link stopLink = network.getLinks().get(stop.getLinkId());
            double newArrivalTime = currentTime + calculateAccurateTravelTime(currentLink, stopLink, currentTime);
            stop.updateArrivalTime(newArrivalTime);
            currentLink = stopLink;
            currentTime = stop.getDepartureTime();
        }
    }

    private boolean isInsertionFeasible(List<TimetableEntry> originalTimetable, int insertionIdx, double delay, double serviceEndTime) {
        for (int i = insertionIdx; i < originalTimetable.size(); i++) {
            TimetableEntry stop = originalTimetable.get(i);
            double newArrivalTime = stop.getArrivalTime() + delay;
            if (stop.isTimeConstraintViolated(delay) || newArrivalTime > serviceEndTime) {
                return false;
            }
            delay = stop.getEffectiveDelayIfStopIsDelayedBy(delay); // Update the delay after this stop (as stop time of some stops may be squeezed)
            if (delay <= 0) {
                return true; // The delay becomes 0, then there will be no impact on the following stops
            }
        }
        return true; // If every stop is feasible, then return true
    }

    private List<TimetableEntry> insertPickup(List<TimetableEntry> originalTimetable, int pickUpIdx,
                                              TimetableEntry stopToInsert, double delay) {
        // Create a copy of the original timetable (and copy each object inside)
        // Note: Delay includes the pickup time
        List<TimetableEntry> temporaryTimetable = new ArrayList<>();
        for (TimetableEntry timetableEntry : originalTimetable) {
            temporaryTimetable.add(new TimetableEntry(timetableEntry));
        }

        if (pickUpIdx < temporaryTimetable.size()) {
            temporaryTimetable.add(pickUpIdx, stopToInsert);
            for (int i = pickUpIdx + 1; i < temporaryTimetable.size(); i++) {
                double effectiveDelay = temporaryTimetable.get(i).getEffectiveDelayIfStopIsDelayedBy(delay);
                temporaryTimetable.get(i).delayTheStopBy(delay);
                temporaryTimetable.get(i).increaseOccupancyByOne();
                delay = effectiveDelay; // Update the delay carry over to the next stop
            }
        } else {
            temporaryTimetable.add(stopToInsert); // insert at the end
        }
        return temporaryTimetable;
    }

    private List<TimetableEntry> insertDropOff(List<TimetableEntry> temporaryTimetable, int dropOffIdx,
                                               TimetableEntry stopToInsert, double delay) {
        // Note: Delay includes the Drop-off time
        List<TimetableEntry> candidateTimetable = new ArrayList<>();
        for (TimetableEntry timetableEntry : temporaryTimetable) {
            candidateTimetable.add(new TimetableEntry(timetableEntry));
        }

        if (dropOffIdx < candidateTimetable.size()) {
            candidateTimetable.add(dropOffIdx, stopToInsert);
            for (int i = dropOffIdx + 1; i < candidateTimetable.size(); i++) {
                double effectiveDelay = candidateTimetable.get(i).getEffectiveDelayIfStopIsDelayedBy(delay);
                candidateTimetable.get(i).delayTheStopBy(delay);
                candidateTimetable.get(i).decreaseOccupancyByOne();
                delay = effectiveDelay; // Update the delay carry over to the next stop
            }
        } else {
            candidateTimetable.add(stopToInsert); // insert at the end
        }
        return candidateTimetable;
    }

    private double calculateVrpTravelTimeFromMatrix(Link fromLink, Link toLink, double departureTime) {
        if (fromLink.getId().toString().equals(toLink.getId().toString())) {
            return 0;
        }
        return FIRST_LINK_TT + travelTimeMatrix.getTravelTime(fromLink.getToNode(), toLink.getFromNode(), departureTime)
                + VrpPaths.getLastLinkTT(travelTime, toLink, departureTime);
    }

    private double calculateAccurateTravelTime(Link fromLink, Link toLink, double departureTime) {
        if (fromLink.getId().toString().equals(toLink.getId().toString())) {
            return 0;
        }
        return VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime).getTravelTime();
    }

}
