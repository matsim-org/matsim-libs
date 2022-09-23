/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.pt.stop2stop;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.IOException;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Processes events to create a csv file with passenger volumes, delays and similar information for each stop to stop
 * segment of a pt {@link org.matsim.pt.transitSchedule.api.Departure}.
 *
 * This class is entirely based on events. In case simulated pt should deviate from the
 * {@link org.matsim.pt.transitSchedule.api.TransitSchedule} this class provides data on what the simulation really gave
 * ignoring the TransitSchedule.
 *
 * Currently there are no integrity checks, if the sequence of events is wrong or an event is missing, this class might
 * silently produce invalid output.
 * This class was intended to be run after the last iteration and the simulation has finished and no preconditions were
 * taken for TransitVehicle fleets which might change over the course of iterations (e.g. minibus). So running this
 * class as an EventsListener during the simulation might give wrong results and might need work to get the transit
 * vehicles updated before mobsim in each iteration.
 *
 * @author vsp-gleich
 */
public class PtStop2StopAnalysis implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler,
        VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        VehicleEntersTrafficEventHandler, LinkEnterEventHandler {

    private final Vehicles transitVehicles; // for vehicle capacity
    private final Map<Id<Vehicle>, PtVehicleData> transitVehicle2temporaryVehicleData = new HashMap<>();
    private final List<Stop2StopEntry> stop2StopEntriesForEachDeparture; // the output
    private static final Logger log = LogManager.getLogger(PtStop2StopAnalysis.class);

    public PtStop2StopAnalysis(Vehicles transitVehicles) {
        this.transitVehicles = transitVehicles;
        // set initial capacity to rough estimate of 30 entries by vehicle (should be sufficient)
        stop2StopEntriesForEachDeparture = new ArrayList<>(transitVehicles.getVehicles().size() * 30);
    }

    /**
     * Start of a pt service {@link org.matsim.pt.transitSchedule.api.Departure}
     */
    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        transitVehicle2temporaryVehicleData.put(event.getVehicleId(), new PtVehicleData(event.getTransitLineId(),
                event.getTransitRouteId(), event.getDepartureId(), event.getDriverId(),
                transitVehicles.getVehicles().get(event.getVehicleId()).getType()));
    }

    /**
     * Vehicle arrives at a stop. This event class is only used by transit vehicles and those should have had a TransitDriverStartsEvent before.
     */
    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        if (ptVehicleData == null) {
            log.error("Encountered a VehicleArrivesAtFacilityEvent without a previous TransitDriverStartsEvent for vehicle " + event.getVehicleId() + " at facility " + event.getFacilityId() + " at time " + event.getTime() + ". This should not happen, this analysis might fail subsequently.");
        } else {
            ptVehicleData.lastVehicleArrivesAtFacilityEvent = event;
        }
    }

    /**
     * Vehicle departs from a stop. This event class is only used by transit vehicles and those should have had a TransitDriverStartsEvent before.
     */
    @Override
    public void handleEvent(VehicleDepartsAtFacilityEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        if (ptVehicleData == null) {
            log.error("Encountered a VehicleDepartsAtFacilityEvent without a previous TransitDriverStartsEvent for vehicle " + event.getVehicleId() + " at facility " + event.getFacilityId() + " at time " + event.getTime() + ". This should not happen, this analysis might fail subsequently.");
        } else {
            // produce output entry
            stop2StopEntriesForEachDeparture.add(new Stop2StopEntry(ptVehicleData.transitLineId,
                    ptVehicleData.transitRouteId, ptVehicleData.departureId, event.getFacilityId(),
                    ptVehicleData.stopSequenceCounter, ptVehicleData.lastStopId,
                    ptVehicleData.lastVehicleArrivesAtFacilityEvent.getTime() - ptVehicleData.lastVehicleArrivesAtFacilityEvent.getDelay(),
                    ptVehicleData.lastVehicleArrivesAtFacilityEvent.getDelay(), event.getTime() - event.getDelay(),
                    event.getDelay(), ptVehicleData.currentPax, ptVehicleData.totalVehicleCapacity,
                    ptVehicleData.alightings, ptVehicleData.boardings, ptVehicleData.linksTravelledOnSincePreviousStop));
            // calculate number of passengers at departure
            // (the Stop2StopEntry before needed the number of passengers at arrival so do not move this up!)
            ptVehicleData.currentPax = ptVehicleData.currentPax - ptVehicleData.alightings + ptVehicleData.boardings;
            // reset counters
            ptVehicleData.alightings = 0;
            ptVehicleData.boardings = 0;
            ptVehicleData.lastVehicleArrivesAtFacilityEvent = null;
            ptVehicleData.linksTravelledOnSincePreviousStop.clear();
            // move forward stop
            ptVehicleData.lastStopId = event.getFacilityId();
            ptVehicleData.stopSequenceCounter++;
        }
    }

    /**
     * Count boarding passengers
     */
    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        // If this is a pt vehicle, we should have a non-null ptVehicleData object
        // && do not count transitVehicle driver
        if (ptVehicleData != null && event.getPersonId() != ptVehicleData.driverId) {
            ptVehicleData.boardings++;
        }
    }

    /**
     * Count alighting passengers and handle end of pt service
     */
    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        // If this is a pt vehicle, we should have a non-null ptVehicleData object
        // && do not count transitVehicle driver
        if (ptVehicleData != null) {
            if (event.getPersonId() == ptVehicleData.driverId) {
                // end of service, remove from vehicle list
                // this should happen after the terminus stop was left, so no need to save any data
                transitVehicle2temporaryVehicleData.remove(event.getVehicleId());
            } else {
                // a real passenger alighting
                ptVehicleData.alightings++;
            }
        }
    }

    @Override
    public void reset(int iteration) {
        if(transitVehicle2temporaryVehicleData.size() > 0) {
            log.warn(transitVehicle2temporaryVehicleData.size() + " transit vehicles did not finish service in the last iteration.");
        }
        transitVehicle2temporaryVehicleData.clear();
        stop2StopEntriesForEachDeparture.clear();
    }

    /**
     * Note down first link of the pt service (Departure) for which no LinkEnterEvent exists.
     */
    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        // If this is a pt vehicle, we should have a non-null ptVehicleData object
        if (ptVehicleData != null) {
            ptVehicleData.linksTravelledOnSincePreviousStop.add(event.getLinkId());
        }
    }

    /**
     * Note all links the vehicle passes on
     */
    @Override
    public void handleEvent(LinkEnterEvent event) {
        PtVehicleData ptVehicleData = transitVehicle2temporaryVehicleData.get(event.getVehicleId());
        // If this is a pt vehicle, we should have a non-null ptVehicleData object
        if (ptVehicleData != null) {
            ptVehicleData.linksTravelledOnSincePreviousStop.add(event.getLinkId());
        }
    }

    /**
     * temporary data structure used during events processing
     */
    private static class PtVehicleData {
        private final Id<TransitLine> transitLineId;
        private final Id<TransitRoute> transitRouteId;
        private final Id<Departure> departureId;
        private final Id<Person> driverId;
        private final double totalVehicleCapacity; // use double for possible future capacity scaling issues
        private int stopSequenceCounter = 0;
        private Id<TransitStopFacility> lastStopId;
        private VehicleArrivesAtFacilityEvent lastVehicleArrivesAtFacilityEvent;
        private int currentPax = 0;
        private int alightings = 0;
        private int boardings = 0;
        private final List<Id<Link>> linksTravelledOnSincePreviousStop = new ArrayList<>(8);

        private PtVehicleData(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId,
                              Id<Departure> departureId, Id<Person> driverId, VehicleType vehicleType) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
            this.departureId = departureId;
            this.driverId = driverId;
            this.totalVehicleCapacity = vehicleType.getCapacity().getSeats() + vehicleType.getCapacity().getStandingRoom();
        }
    }

    /**
     * output data structure
     */
    static final class Stop2StopEntry {
        final Id<TransitLine> transitLineId;// for aggregation -> set null or leave out and use Map lineId -> Stop2StopEntry?
        final Id<TransitRoute> transitRouteId;
        final Id<Departure> departureId;
        final Id<TransitStopFacility> stopId;
        final int stopSequence;
        final Id<TransitStopFacility> stopPreviousId;
        final double arrivalTimeScheduled;
        final double arrivalDelay;
        final double departureTimeScheduled;
        final double departureDelay;
        final int passengersAtArrival;
        final double totalVehicleCapacity;
        final int passengersAlighting;
        final int passengersBoarding;
        final List<Id<Link>> linkIdsSincePreviousStop;

        Stop2StopEntry(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId,
                       Id<Departure> departureId, Id<TransitStopFacility> stopId, int stopSequence,
                       Id<TransitStopFacility> stopPreviousId, double arrivalTimeScheduled, double arrivalDelay,
                       double departureTimeScheduled, double departureDelay, int passengersAtArrival,
                       double totalVehicleCapacity, int passengersAlighting, int passengersBoarding,
                       List<Id<Link>> linkIdsSincePreviousStop) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
            this.departureId = departureId;
            this.stopId = stopId;
            this.stopSequence = stopSequence;
            this.stopPreviousId = stopPreviousId;
            this.arrivalTimeScheduled = arrivalTimeScheduled;
            this.arrivalDelay = arrivalDelay;
            this.departureTimeScheduled = departureTimeScheduled;
            this.departureDelay = departureDelay;
            this.passengersAtArrival = passengersAtArrival;
            this.totalVehicleCapacity = totalVehicleCapacity;
            this.passengersAlighting = passengersAlighting;
            this.passengersBoarding = passengersBoarding;
            this.linkIdsSincePreviousStop = List.copyOf(linkIdsSincePreviousStop);
        }

        // getter for usage of standard Comparator implementations below and more
        public Id<TransitLine> getTransitLineId() {
            return transitLineId;
        }

        public Id<TransitRoute> getTransitRouteId() {
            return transitRouteId;
        }

        public Id<Departure> getDepartureId() {
            return departureId;
        }

        public Id<TransitStopFacility> getStopId() {
            return stopId;
        }

        public int getStopSequence() {
            return stopSequence;
        }

        public Id<TransitStopFacility> getStopPreviousId() {
            return stopPreviousId;
        }

        public double getArrivalTimeScheduled() {
            return arrivalTimeScheduled;
        }

        public double getArrivalDelay() {
            return arrivalDelay;
        }

        public double getDepartureTimeScheduled() {
            return departureTimeScheduled;
        }

        public double getDepartureDelay() {
            return departureDelay;
        }

        public int getPassengersAtArrival() {
            return passengersAtArrival;
        }

        public double getTotalVehicleCapacity() {
            return totalVehicleCapacity;
        }

        public int getPassengersAlighting() {
            return passengersAlighting;
        }

        public int getPassengersBoarding() {
            return passengersBoarding;
        }

        public List<Id<Link>> getLinkIdsSincePreviousStop() {
            return linkIdsSincePreviousStop;
        }
    }

    static final String[] HEADER = {"transitLine", "transitRoute", "departure", "stop", "stopSequence",
            "stopPrevious", "arrivalTimeScheduled", "arrivalDelay", "departureTimeScheduled", "departureDelay",
            "passengersAtArrival", "totalVehicleCapacity", "passengersAlighting", "passengersBoarding",
            "linkIdsSincePreviousStop"};

    public static Comparator<Stop2StopEntry> stop2StopEntryByTransitLineComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::getTransitLineId));
    public static Comparator<Stop2StopEntry> stop2StopEntryByTransitRouteComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::getTransitRouteId));
    public static Comparator<Stop2StopEntry> stop2StopEntryByDepartureComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::getDepartureId));
    public static Comparator<Stop2StopEntry> stop2StopEntryByStopSequenceComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::getStopSequence));

    public void writeStop2StopEntriesByDepartureCsv(String fileName, String columnSeparator, String listSeparatorInsideColumn) {
        stop2StopEntriesForEachDeparture.sort(stop2StopEntryByTransitLineComparator.
                thenComparing(stop2StopEntryByTransitRouteComparator).
                thenComparing(stop2StopEntryByDepartureComparator).
                thenComparing(stop2StopEntryByStopSequenceComparator));
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(HEADER))
        ) {
            for (Stop2StopEntry entry : stop2StopEntriesForEachDeparture) {
                printer.print(entry.transitLineId);
                printer.print(entry.transitRouteId);
                printer.print(entry.departureId);
                printer.print(entry.stopId);
                printer.print(entry.stopSequence);
                printer.print(entry.stopPreviousId);
                printer.print(entry.arrivalTimeScheduled);
                printer.print(entry.arrivalDelay);
                printer.print(entry.departureTimeScheduled);
                printer.print(entry.departureDelay);
                printer.print(entry.passengersAtArrival);
                printer.print(entry.totalVehicleCapacity);
                printer.print(entry.passengersAlighting);
                printer.print(entry.passengersBoarding);
                printer.print(entry.linkIdsSincePreviousStop.stream().map(id -> id.toString()).collect(Collectors.joining(listSeparatorInsideColumn)));
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Stop2StopEntry> getStop2StopEntriesByDeparture () {
        return List.copyOf(stop2StopEntriesForEachDeparture);
    }

    static final class Stop2StopAggregation {

        private final int departures;
        private final long passengers;
        private final double totalVehicleCapacity;

        Stop2StopAggregation (int departures, long passengers, double totalVehicleCapacity) {
            this.departures = departures;
            this.passengers = passengers;
            this.totalVehicleCapacity = totalVehicleCapacity;
        }

        public int getDepartures() {
            return departures;
        }

        public long getPassengers() {
            return passengers;
        }

        public double getTotalVehicleCapacity() {
            return totalVehicleCapacity;
        }
    }

    public static BinaryOperator<Stop2StopAggregation> aggregateStop2StopAggregations() {
        return (entry1, entry2) -> new PtStop2StopAnalysis.Stop2StopAggregation(entry1.getDepartures() + entry2.getDepartures(), entry1.getPassengers() + entry2.getPassengers(),
                entry1.getTotalVehicleCapacity() + entry2.getTotalVehicleCapacity());
    }
}
