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
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes events to create a csv file with passenger volumes, delays and similar information for each stop to stop
 * segment of a pt {@link org.matsim.pt.transitSchedule.api.Departure}.
 * <p>
 * This class is entirely based on events. In case simulated pt should deviate from the
 * {@link org.matsim.pt.transitSchedule.api.TransitSchedule} this class provides data on what the simulation really gave
 * ignoring the TransitSchedule.
 * <p>
 * Currently, there are no integrity checks, if the sequence of events is wrong or an event is missing, this class might
 * silently produce invalid output.
 * This class was intended to be run after the last iteration and the simulation has finished and no preconditions were
 * taken for TransitVehicle fleets which might change over the course of iterations (e.g. minibus). So running this
 * class as an EventsListener during the simulation might give wrong results and might need work to get the transit
 * vehicles updated before mobsim in each iteration. Still it could be useful to run in each iteration in special cases,
 * such as the minibus contrib.
 *
 * @author vsp-gleich
 */
public class PtStop2StopAnalysis implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler,
        VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        VehicleEntersTrafficEventHandler, LinkEnterEventHandler {

    private final Vehicles transitVehicles; // for vehicle capacity
    private final Map<Id<Vehicle>, PtVehicleData> transitVehicle2temporaryVehicleData = new HashMap<>();
    private final List<Stop2StopEntry> stop2StopEntriesForEachDeparture; // the output
	private final double sampleUpscaleFactor;
    private static final Logger log = LogManager.getLogger(PtStop2StopAnalysis.class);

	/**
	 * @param transitVehicles needed to look up vehicle capacity
	 * @param sampleUpscaleFactor : factor to scale up output passenger volumes to 100%
	 */
    public PtStop2StopAnalysis(Vehicles transitVehicles, double sampleUpscaleFactor) {
        this.transitVehicles = transitVehicles;
		this.sampleUpscaleFactor = sampleUpscaleFactor;
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
			log.error("Encountered a VehicleArrivesAtFacilityEvent without a previous TransitDriverStartsEvent for vehicle {} at facility {} at time {}. This should not happen, this analysis might fail subsequently.", event.getVehicleId(), event.getFacilityId(), event.getTime());
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
			log.error("Encountered a VehicleDepartsAtFacilityEvent without a previous TransitDriverStartsEvent for vehicle {} at facility {} at time {}. This should not happen, this analysis might fail subsequently.", event.getVehicleId(), event.getFacilityId(), event.getTime());
        } else {
            // produce output entry
			stop2StopEntriesForEachDeparture.add(new Stop2StopEntry(ptVehicleData.transitLineId,
				ptVehicleData.transitRouteId, ptVehicleData.departureId, event.getFacilityId(),
				ptVehicleData.stopSequenceCounter, ptVehicleData.lastStopId,
				ptVehicleData.lastVehicleArrivesAtFacilityEvent.getTime() - ptVehicleData.lastVehicleArrivesAtFacilityEvent.getDelay(),
				ptVehicleData.lastVehicleArrivesAtFacilityEvent.getDelay(), event.getTime() - event.getDelay(),
				event.getDelay(), ptVehicleData.currentPax * sampleUpscaleFactor, ptVehicleData.totalVehicleCapacity,
				ptVehicleData.alightings * sampleUpscaleFactor, ptVehicleData.boardings * sampleUpscaleFactor,
				List.copyOf(ptVehicleData.linksTravelledOnSincePreviousStop)));
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
        if(!transitVehicle2temporaryVehicleData.isEmpty()) {
			log.warn("{} transit vehicles did not finish service in the last iteration.", transitVehicle2temporaryVehicleData.size());
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
	 *
	 * @param transitLineId for aggregation -> set null or leave out and use Map lineId -> Stop2StopEntry?
	 */
	record Stop2StopEntry(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Departure> departureId, Id<TransitStopFacility> stopId,
							  int stopSequence, Id<TransitStopFacility> stopPreviousId, double arrivalTimeScheduled, double arrivalDelay,
							  double departureTimeScheduled, double departureDelay, double passengersAtArrival, double totalVehicleCapacity,
							  double passengersAlighting, double passengersBoarding, List<Id<Link>> linkIdsSincePreviousStop) {}

    static final String[] HEADER = {"transitLine", "transitRoute", "departure", "stop", "stopSequence",
            "stopPrevious", "arrivalTimeScheduled", "arrivalDelay", "departureTimeScheduled", "departureDelay",
            "passengersAtArrival", "totalVehicleCapacity", "passengersAlighting", "passengersBoarding",
            "linkIdsSincePreviousStop"};

    static Comparator<Stop2StopEntry> stop2StopEntryByTransitLineComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::transitLineId));
    static Comparator<Stop2StopEntry> stop2StopEntryByTransitRouteComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::transitRouteId));
    static Comparator<Stop2StopEntry> stop2StopEntryByDepartureComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::departureId));
    static Comparator<Stop2StopEntry> stop2StopEntryByStopSequenceComparator =
            Comparator.nullsLast(Comparator.comparing(Stop2StopEntry::stopSequence));

	public void writeStop2StopEntriesByDepartureCsv(String fileName, String columnSeparator, String listSeparatorInsideColumn) {
		writeStop2StopEntriesByDepartureCsv(IOUtils.getFileUrl(fileName), columnSeparator, listSeparatorInsideColumn);
	}

	public void writeStop2StopEntriesByDepartureCsv(Path path, String columnSeparator, String listSeparatorInsideColumn) {
		writeStop2StopEntriesByDepartureCsv(IOUtils.getFileUrl(path.toString()), columnSeparator, listSeparatorInsideColumn);
	}

	public void writeStop2StopEntriesByDepartureCsv(URL url, String columnSeparator, String listSeparatorInsideColumn) {
        stop2StopEntriesForEachDeparture.sort(stop2StopEntryByTransitLineComparator.
                thenComparing(stop2StopEntryByTransitRouteComparator).
                thenComparing(stop2StopEntryByDepartureComparator).
                thenComparing(stop2StopEntryByStopSequenceComparator));
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(url),
			CSVFormat.Builder.create()
				.setDelimiter(columnSeparator)
				.setHeader(HEADER)
				.build())
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
                printer.print(entry.linkIdsSincePreviousStop.stream().map(Object::toString).collect(Collectors.joining(listSeparatorInsideColumn)));
                printer.println();
            }
        } catch (IOException e) {
			log.error(e);
        }
    }

    List<Stop2StopEntry> getStop2StopEntriesByDeparture () {
        return List.copyOf(stop2StopEntriesForEachDeparture);
    }
}
