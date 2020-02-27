
/* *********************************************************************** *
 * project: org.matsim.*
 * TripsCSVWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author jbischoff / SBB
 */
public class TripsCSVWriter {
    public static String[] HEADER = {"person", "tripNumber", "tripId",
            "departureTime", "travelTime", "totalWaitingTime", "traveledDistance", "euclideanDistance",
            "modeWithLongestShare", "modes", "originActivityType",
            "destinationActivityType", "originFacilityId", "originLinkId",
            "fromX", "fromY", "destinationFacilityId",
            "destinationLinkId", "toX", "toY"};
    private final String separator;
    private final CustomTripsWriterExtension extension;
    private final Scenario scenario;


    public TripsCSVWriter(Scenario scenario, CustomTripsWriterExtension extension) {
        this.scenario = scenario;
        this.separator = scenario.getConfig().global().getDefaultDelimiter();
        HEADER = ArrayUtils.addAll(HEADER, extension.getAdditionalHeader());
        this.extension = extension;

    }

    public void write(IdMap<Person, Plan> experiencedPlans, String filename) {
        try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(filename),
                CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)).withHeader(HEADER))) {
            for (Map.Entry<Id<Person>, Plan> entry : experiencedPlans.entrySet()) {
                csvPrinter.printRecords(getCSVRecord(entry.getValue(), entry.getKey()));
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private Iterable<?> getCSVRecord(Plan experiencedPlan, Id<Person> personId) {
        List<List<String>> records = new ArrayList<>();
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(experiencedPlan);

        for (int i = 0; i < trips.size(); i++) {
            TripStructureUtils.Trip trip = trips.get(i);
            List<String> record = new ArrayList<>();
            records.add(record);
            record.add(personId.toString());
            final String tripNo = Integer.toString(i + 1);
            record.add(tripNo); // trip number, numbered starting with 0
            String tripId = personId.toString() + "_" + tripNo;
            record.add(tripId);
            double distance = 0.0;
            double departureTime = trip.getOriginActivity().getEndTime();
            double travelTime = trip.getDestinationActivity().getStartTime() - departureTime;
            //experienced plans have a start time

            double totalWaitingTime = 0.0;
            double currentLongestShareDistance = Double.MIN_VALUE;
            String currentModeWithLongestShare = "";
            List<String> modes = new ArrayList<>();
            String lastActivityType = trip.getOriginActivity().getType();
            String nextActivityType = trip.getDestinationActivity().getType();
            Id<ActivityFacility> fromFacilityId = trip.getOriginActivity().getFacilityId();
            Id<ActivityFacility> toFacilityId = trip.getDestinationActivity().getFacilityId();
            Id<Link> fromLinkId = trip.getOriginActivity().getLinkId();
            Id<Link> toLinkId = trip.getDestinationActivity().getLinkId();
            Coord fromCoord = getCoordFromActivity(trip.getOriginActivity());
            Coord toCoord = getCoordFromActivity(trip.getDestinationActivity());
            int euclideanDistance = (int) CoordUtils.calcEuclideanDistance(fromCoord, toCoord);

            for (Leg leg : trip.getLegsOnly()) {
                modes.add(leg.getMode());
                final double legDist = leg.getRoute().getDistance();
                distance += legDist;
                Double boardingTime = (Double) leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
                if (boardingTime != null) {
                    double waitingTime = boardingTime - leg.getDepartureTime();
                    totalWaitingTime += waitingTime;
                }
                if (legDist > currentLongestShareDistance) {
                    currentLongestShareDistance = legDist;
                    currentModeWithLongestShare = leg.getMode();

                }

            }

            record.add(Time.writeTime(departureTime));
            record.add(Time.writeTime(travelTime));
            record.add(Time.writeTime(totalWaitingTime));
            record.add(Integer.toString((int) Math.round(distance)));
            record.add(Integer.toString(euclideanDistance));
            record.add(currentModeWithLongestShare);
            record.add(modes.stream().collect(Collectors.joining("-")));
            record.add(lastActivityType);
            record.add(nextActivityType);
            record.add(String.valueOf(fromFacilityId));
            record.add(String.valueOf(fromLinkId));
            record.add(Double.toString(fromCoord.getX()));
            record.add(Double.toString(fromCoord.getY()));

            record.add(String.valueOf(toFacilityId));
            record.add(String.valueOf(toLinkId));
            record.add(Double.toString(toCoord.getX()));
            record.add(Double.toString(toCoord.getY()));

            record.addAll(extension.getAdditionalColumns(trip));
            if (HEADER.length != record.size()) {
                    throw new RuntimeException("Custom CSV Writer Extension does not provide a sufficient number of additional columns. Must be " + HEADER.length + " , but is " + record.size());
            }
        }

        return records;
    }

    private Coord getCoordFromActivity(Activity activity) {
        if (activity.getCoord() != null) {
            return activity.getCoord();
        } else if (activity.getFacilityId() != null && scenario.getActivityFacilities().getFacilities().containsKey(activity.getFacilityId())) {
            Coord coord = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
            return coord != null ? coord : getCoordFromLink(activity.getLinkId());
        } else return getCoordFromLink(activity.getLinkId());
    }

    //this is the least desirable way
    private Coord getCoordFromLink(Id<Link> linkId) {
        return scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
    }


    public interface CustomTripsWriterExtension {
        String[] getAdditionalHeader();
        List<String> getAdditionalColumns(TripStructureUtils.Trip trip);
    }

    static class NoExtension implements CustomTripsWriterExtension {
        @Override
        public String[] getAdditionalHeader() {
            return new String[0];
        }

        @Override
        public List<String> getAdditionalColumns(TripStructureUtils.Trip trip) {
            return Collections.EMPTY_LIST;
        }
    }
}
