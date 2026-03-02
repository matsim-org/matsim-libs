package org.matsim.drtExperiments.offlineStrategy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.ArrayList;
import java.util.List;

public record InsertionCalculator(Network network, double stopDuration,
                                  LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix) {
    final static double NOT_FEASIBLE_COST = 1e6;

    /**
     * Compute the cost to insert the request in to the vehicle.
     */
    public InsertionData computeInsertionData(OnlineVehicleInfo vehicleInfo, GeneralRequest request,
                                              FleetSchedules previousSchedules) {
        Link fromLink = network.getLinks().get(request.getFromLinkId());
        Link toLink = network.getLinks().get(request.getToLinkId());
        Link currentLink = vehicleInfo.currentLink();
        double divertableTime = vehicleInfo.divertableTime();
        double serviceEndTime = vehicleInfo.vehicle().getServiceEndTime() - stopDuration; // the last stop must start on or before this time step
        List<TimetableEntry> originalTimetable = previousSchedules.vehicleToTimetableMap().get(vehicleInfo.vehicle().getId());

        // 1. if timetable is empty
        if (originalTimetable.isEmpty()) {
            double timeToPickup = linkToLinkTravelTimeMatrix.getTravelTime(currentLink, fromLink, divertableTime);
            double arrivalTimePickUp = divertableTime + timeToPickup;
            if (arrivalTimePickUp > request.getLatestDepartureTime() || arrivalTimePickUp > serviceEndTime) {
                return new InsertionData(null, NOT_FEASIBLE_COST, vehicleInfo);
            }

            double departureTimePickUp = Math.max(request.getEarliestDepartureTime(), arrivalTimePickUp) + stopDuration;
            double tripTravelTime = linkToLinkTravelTimeMatrix.getTravelTime(fromLink, toLink, departureTimePickUp);
            double arrivalTimeDropOff = departureTimePickUp + tripTravelTime;
            double totalInsertionCost = timeToPickup + tripTravelTime;

            List<TimetableEntry> updatedTimetable = new ArrayList<>();
            updatedTimetable.add(new TimetableEntry(request, TimetableEntry.StopType.PICKUP, arrivalTimePickUp, departureTimePickUp, 0, stopDuration, vehicleInfo.vehicle()));
            updatedTimetable.add(new TimetableEntry(request, TimetableEntry.StopType.DROP_OFF, arrivalTimeDropOff, arrivalTimeDropOff + stopDuration, 1, stopDuration, vehicleInfo.vehicle()));
            // Note: The departure time of the last stop is actually not meaningful, but this stop may become non-last stop later, therefore, we set the departure time of this stop as if it is a middle stop
            return new InsertionData(updatedTimetable, totalInsertionCost, vehicleInfo);
        }

        // 2. If original timetable is non-empty
        double insertionCost = NOT_FEASIBLE_COST;
        List<TimetableEntry> candidateTimetable = null;

        for (int i = 0; i < originalTimetable.size() + 1; i++) {
            double pickUpInsertionCost;
            List<TimetableEntry> temporaryTimetable;

            // Insert pickup
            if (i < originalTimetable.size()) {
                if (originalTimetable.get(i).isVehicleFullBeforeThisStop()) {
                    continue;
                }
                double detourA;
                double arrivalTimePickUpStop;
                Link linkOfStopBeforePickUpInsertion;
                double departureTimeOfStopBeforePickUpInsertion;
                if (i == 0) {
                    // insert pickup before the first stop
                    // no stop before pickup insertion --> use current location of the vehicle
                    linkOfStopBeforePickUpInsertion = currentLink;
                    // no stop before pickup insertion --> use divertable time of the vehicle
                    departureTimeOfStopBeforePickUpInsertion = divertableTime;
                    detourA = linkToLinkTravelTimeMatrix.getTravelTime(currentLink, fromLink, divertableTime);
                    arrivalTimePickUpStop = divertableTime + detourA;
                } else {
                    TimetableEntry stopBeforePickUpInsertion = originalTimetable.get(i - 1);
                    linkOfStopBeforePickUpInsertion = network.getLinks().get(stopBeforePickUpInsertion.getLinkId());
                    departureTimeOfStopBeforePickUpInsertion = stopBeforePickUpInsertion.getDepartureTime();
                    detourA = linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforePickUpInsertion, fromLink, stopBeforePickUpInsertion.getDepartureTime());
                    arrivalTimePickUpStop = departureTimeOfStopBeforePickUpInsertion + detourA;
                }
                if (arrivalTimePickUpStop > request.getLatestDepartureTime() || arrivalTimePickUpStop > serviceEndTime) {
                    break;
                    // Vehicle can no longer reach the pickup location in time. No need to continue with this vehicle
                }
                double departureTimePickUpStop = Math.max(arrivalTimePickUpStop, request.getEarliestDepartureTime()) + stopDuration;
                TimetableEntry stopAfterPickUpInsertion = originalTimetable.get(i);
                Link linkOfStopAfterPickUpInsertion = network.getLinks().get(stopAfterPickUpInsertion.getLinkId());
                double detourB = linkToLinkTravelTimeMatrix.getTravelTime(fromLink, linkOfStopAfterPickUpInsertion, departureTimePickUpStop);
                double newArrivalTimeOfNextStop = departureTimePickUpStop + detourB;
                double delayCausedByInsertingPickUp = newArrivalTimeOfNextStop - stopAfterPickUpInsertion.getArrivalTime();
                if (isInsertionNotFeasible(originalTimetable, i, delayCausedByInsertingPickUp, serviceEndTime)) {
                    continue;
                }
                pickUpInsertionCost = detourA + detourB - linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforePickUpInsertion, linkOfStopAfterPickUpInsertion, departureTimeOfStopBeforePickUpInsertion);
                TimetableEntry pickupStopToInsert = new TimetableEntry(request, TimetableEntry.StopType.PICKUP,
                        arrivalTimePickUpStop, departureTimePickUpStop, stopAfterPickUpInsertion.getOccupancyBeforeStop(), stopDuration, vehicleInfo.vehicle());
                temporaryTimetable = insertPickup(originalTimetable, i, pickupStopToInsert, delayCausedByInsertingPickUp);
            } else {
                // Append pickup at the end
                TimetableEntry stopBeforePickUpInsertion = originalTimetable.get(i - 1);
                Link linkOfStopBeforePickUpInsertion = network.getLinks().get(stopBeforePickUpInsertion.getLinkId());
                double departureTimeOfStopBeforePickUpInsertion = stopBeforePickUpInsertion.getDepartureTime();
                double travelTimeToPickUp = linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforePickUpInsertion, fromLink, departureTimeOfStopBeforePickUpInsertion);
                double arrivalTimePickUpStop = travelTimeToPickUp + departureTimeOfStopBeforePickUpInsertion;
                if (arrivalTimePickUpStop > request.getLatestDepartureTime() || arrivalTimePickUpStop > serviceEndTime) {
                    break;
                }
                double departureTimePickUpStop = Math.max(arrivalTimePickUpStop, request.getEarliestDepartureTime()) + stopDuration;
                pickUpInsertionCost = travelTimeToPickUp;
                TimetableEntry pickupStopToInsert = new TimetableEntry(request, TimetableEntry.StopType.PICKUP,
                        arrivalTimePickUpStop, departureTimePickUpStop, 0, stopDuration, vehicleInfo.vehicle());
                temporaryTimetable = insertPickup(originalTimetable, i, pickupStopToInsert, 0);
                //Appending pickup at the end will not cause any delay to the original timetable
            }

            // Insert drop off
            for (int j = i + 1; j < temporaryTimetable.size() + 1; j++) {
                // Check occupancy feasibility
                if (temporaryTimetable.get(j - 1).isVehicleOverloaded()) {
                    // If the stop before the drop-off insertion is overloaded, then it is not feasible to insert drop off at or after current location
                    break;
                }

                TimetableEntry stopBeforeDropOffInsertion = temporaryTimetable.get(j - 1);
                Link linkOfStopBeforeDropOffInsertion = network.getLinks().get(stopBeforeDropOffInsertion.getLinkId());
                double departureTimeOfStopBeforeDropOffInsertion = stopBeforeDropOffInsertion.getDepartureTime();
                if (j < temporaryTimetable.size()) { // Insert drop off between two stops
                    double detourC = linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforeDropOffInsertion, toLink, departureTimeOfStopBeforeDropOffInsertion);
                    double arrivalTimeDropOffStop = departureTimeOfStopBeforeDropOffInsertion + detourC;
                    if (arrivalTimeDropOffStop > request.getLatestArrivalTime() || arrivalTimeDropOffStop > serviceEndTime) {
                        break;
                    }
                    double departureTimeDropOffStop = arrivalTimeDropOffStop + stopDuration;
                    TimetableEntry stopAfterDropOffInsertion = temporaryTimetable.get(j);
                    Link linkOfStopAfterDropOffInsertion = network.getLinks().get(stopAfterDropOffInsertion.getLinkId());
                    double detourD = linkToLinkTravelTimeMatrix.getTravelTime(toLink, linkOfStopAfterDropOffInsertion, departureTimeDropOffStop);
                    double newArrivalTimeOfStopAfterDropOffInsertion = departureTimeDropOffStop + detourD;
                    double delayCausedByDropOffInsertion = newArrivalTimeOfStopAfterDropOffInsertion - stopAfterDropOffInsertion.getArrivalTime();
                    if (isInsertionNotFeasible(temporaryTimetable, j, delayCausedByDropOffInsertion, serviceEndTime)) {
                        continue;
                    }
                    double dropOffInsertionCost = detourC + detourD - linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforeDropOffInsertion, linkOfStopAfterDropOffInsertion, departureTimeOfStopBeforeDropOffInsertion);
                    double totalInsertionCost = dropOffInsertionCost + pickUpInsertionCost;
                    if (totalInsertionCost < insertionCost) {
                        insertionCost = totalInsertionCost;
                        TimetableEntry dropOffStopToInsert = new TimetableEntry(request, TimetableEntry.StopType.DROP_OFF,
                                arrivalTimeDropOffStop, departureTimeDropOffStop, stopAfterDropOffInsertion.getOccupancyBeforeStop(), stopDuration, vehicleInfo.vehicle()); //Attention: currently, the occupancy before next stop is already increased!
                        candidateTimetable = insertDropOff(temporaryTimetable, j, dropOffStopToInsert, delayCausedByDropOffInsertion);
                    }
                } else {
                    // Append drop off at the end
                    double travelTimeToDropOffStop = linkToLinkTravelTimeMatrix.getTravelTime(linkOfStopBeforeDropOffInsertion, toLink, departureTimeOfStopBeforeDropOffInsertion);
                    double arrivalTimeDropOffStop = departureTimeOfStopBeforeDropOffInsertion + travelTimeToDropOffStop;
                    if (arrivalTimeDropOffStop > request.getLatestArrivalTime() || arrivalTimeDropOffStop > serviceEndTime) {
                        continue;
                    }
                    double totalInsertionCost = pickUpInsertionCost + travelTimeToDropOffStop;
                    double departureTimeDropOffStop = arrivalTimeDropOffStop + stopDuration;
                    if (totalInsertionCost < insertionCost) {
                        insertionCost = totalInsertionCost;
                        TimetableEntry dropOffStopToInsert = new TimetableEntry(request, TimetableEntry.StopType.DROP_OFF,
                                arrivalTimeDropOffStop, departureTimeDropOffStop, 1, stopDuration, vehicleInfo.vehicle());
                        candidateTimetable = insertDropOff(temporaryTimetable, j, dropOffStopToInsert, 0);
                    }
                }
            }
        }
        return new InsertionData(candidateTimetable, insertionCost, vehicleInfo);
    }

    public void removeRequestFromSchedule(OnlineVehicleInfo vehicleInfo, GeneralRequest requestToRemove,
                                          FleetSchedules previousSchedule) {
        Id<DvrpVehicle> vehicleId = previousSchedule.requestIdToVehicleMap().get(requestToRemove.getPassengerId());
        List<TimetableEntry> timetable = previousSchedule.vehicleToTimetableMap().get(vehicleId);

        // remove the request from the timetable
        // First identify the pick-up and drop-off index of the request, and update the occupancy of those impacted stops
        int pickUpIdx = timetable.size();
        int dropOffIdx = timetable.size();
        for (int i = 0; i < timetable.size(); i++) {
            TimetableEntry stop = timetable.get(i);
            if (stop.getRequest().getPassengerId().toString().equals(requestToRemove.getPassengerId().toString())) {
                if (stop.getStopType() == TimetableEntry.StopType.PICKUP) {
                    pickUpIdx = i;
                } else {
                    dropOffIdx = i;
                }
            }

            if (i > pickUpIdx && i < dropOffIdx) {
                // Reduce the occupancy before the stop by 1
                stop.decreaseOccupancyByOne();
            }
        }

        // Remove the 2 stops
        // Hint: remove the drop-off stop first, as drop-off stop is after the pick-up stop (i.e., no interference on the idx)
        timetable.remove(dropOffIdx);
        timetable.remove(pickUpIdx);

        // Update the timetable
        for (int i = 0; i < timetable.size(); i++) {
            TimetableEntry stop = timetable.get(i);
            double departureTimeFromPreviousStop;
            double updatedArrivalTime;
            if (i == 0) {
                departureTimeFromPreviousStop = vehicleInfo.divertableTime();
                updatedArrivalTime = departureTimeFromPreviousStop + linkToLinkTravelTimeMatrix.
                        getTravelTime(vehicleInfo.currentLink(), network.getLinks().get(stop.getLinkId()), departureTimeFromPreviousStop);
            } else {
                TimetableEntry previousStop = timetable.get(i - 1);
                departureTimeFromPreviousStop = previousStop.getDepartureTime();
                updatedArrivalTime = departureTimeFromPreviousStop + linkToLinkTravelTimeMatrix.
                        getTravelTime(network.getLinks().get(previousStop.getLinkId()), network.getLinks().get(stop.getLinkId()), departureTimeFromPreviousStop);
            }
            stop.updateArrivalTime(updatedArrivalTime);
        }

        // put the request in the rejection list
        previousSchedule.requestIdToVehicleMap().remove(requestToRemove.getPassengerId());
        previousSchedule.pendingRequests().put(requestToRemove.getPassengerId(), requestToRemove);
    }

    // Nested classes / Records
    public record InsertionData(List<TimetableEntry> candidateTimetable, double cost, OnlineVehicleInfo vehicleInfo) {
    }

    // Private methods
    private boolean isInsertionNotFeasible(List<TimetableEntry> originalTimetable, int insertionIdx, double delay, double serviceEndTime) {
        for (int i = insertionIdx; i < originalTimetable.size(); i++) {
            TimetableEntry stop = originalTimetable.get(i);
            double newArrivalTime = stop.getArrivalTime() + delay;
            if (stop.isTimeConstraintViolated(delay) || newArrivalTime > serviceEndTime) {
                return true;
            }
            delay = stop.getEffectiveDelayIfStopIsDelayedBy(delay); // Update the delay after this stop (as stop time of some stops may be squeezed)
            if (delay <= 0) {
                return false; // The delay becomes 0, then there will be no impact on the following stops --> feasible (not feasible = false)
            }
        }
        return false; // If we reach here, then every stop is feasible (not feasible = false)
    }


    private List<TimetableEntry> insertPickup(List<TimetableEntry> originalTimetable, int pickUpIdx,
                                              TimetableEntry stopToInsert, double delay) {
        // Create a copy of the original timetable
        List<TimetableEntry> temporaryTimetable = FleetSchedules.copyTimetable(originalTimetable);
        if (pickUpIdx < temporaryTimetable.size()) {
            temporaryTimetable.add(pickUpIdx, stopToInsert);
            for (int i = pickUpIdx + 1; i < temporaryTimetable.size(); i++) {
                double effectiveDelay = temporaryTimetable.get(i).getEffectiveDelayIfStopIsDelayedBy(delay);
                temporaryTimetable.get(i).delayTheStopBy(delay);
                temporaryTimetable.get(i).increaseOccupancyByOne();
                delay = effectiveDelay;
                // Update the delay carry over to the next stop
            }
        } else {
            // insert at the end
            temporaryTimetable.add(stopToInsert);
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


}
