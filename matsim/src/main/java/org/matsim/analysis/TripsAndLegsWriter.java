
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

import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author jbischoff / SBB
 */
public class TripsAndLegsWriter {
    public static final String[] TRIPSHEADER_BASE = {"person", "trip_number", "trip_id",
            "dep_time", "trav_time", "wait_time", "traveled_distance", "euclidean_distance",
            "main_mode", "longest_distance_mode", "modes", "start_activity_type",
            "end_activity_type", "start_facility_id", "start_link",
            "start_x", "start_y", "end_facility_id",
            "end_link", "end_x", "end_y", "first_pt_boarding_stop", "last_pt_egress_stop"};

    public static final String[] LEGSHEADER_BASE = {"person", "trip_id",
            "dep_time", "trav_time", "wait_time", "distance", "mode", "start_link",
            "start_x", "start_y", "end_link", "end_x", "end_y", "access_stop_id", "egress_stop_id", "transit_line", "transit_route", "vehicle_id"};

    private final String[] TRIPSHEADER;
    private final String[] LEGSHEADER;
    private final CustomTripsWriterExtension tripsWriterExtension;
    private final Scenario scenario;
    private final CustomLegsWriterExtension legsWriterExtension;
    private final AnalysisMainModeIdentifier mainModeIdentifier;
    private final CustomTimeWriter customTimeWriter;

    private static final Logger log = LogManager.getLogger(TripsAndLegsWriter.class);

	@Inject
	public TripsAndLegsWriter(Scenario scenario, CustomTripsWriterExtension tripsWriterExtension, CustomLegsWriterExtension legWriterExtension,
					   AnalysisMainModeIdentifier mainModeIdentifier, CustomTimeWriter customTimeWriter) {
		this.scenario = scenario;
		this.tripsWriterExtension = tripsWriterExtension;
		this.legsWriterExtension = legWriterExtension;
		this.mainModeIdentifier = mainModeIdentifier;
		this.customTimeWriter = customTimeWriter;

		TRIPSHEADER = ArrayUtils.addAll(TRIPSHEADER_BASE, tripsWriterExtension.getAdditionalTripHeader());
		LEGSHEADER = ArrayUtils.addAll(LEGSHEADER_BASE, legWriterExtension.getAdditionalLegHeader());
	}

	private char getDefaultDelimiter(){
		return scenario.getConfig().global().getDefaultDelimiter().charAt(0);
	}

	public void write(IdMap<Person, Plan> experiencedPlans, String tripsFilename, String legsFilename) {
        try (CSVPrinter tripsCSVprinter = new CSVPrinter(IOUtils.getBufferedWriter(tripsFilename),
                CSVFormat.Builder.create().setDelimiter(getDefaultDelimiter()).setHeader(TRIPSHEADER).build());
             CSVPrinter legsCSVprinter = new CSVPrinter(IOUtils.getBufferedWriter(legsFilename),
				 CSVFormat.Builder.create().setDelimiter(getDefaultDelimiter()).setHeader(LEGSHEADER).build())
        ) {
            for (Map.Entry<Id<Person>, Plan> entry : experiencedPlans.entrySet()) {
                Tuple<Iterable<?>, Iterable<?>> tripsAndLegRecords = getPlanCSVRecords(entry.getValue(), entry.getKey());
                tripsCSVprinter.printRecords(tripsAndLegRecords.getFirst());
                legsCSVprinter.printRecords(tripsAndLegRecords.getSecond());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Tuple<Iterable<?>, Iterable<?>> getPlanCSVRecords(Plan experiencedPlan, Id<Person> personId) {
        List<List<String>> tripRecords = new ArrayList<>();
        List<List<String>> legRecords = new ArrayList<>();
        Tuple<Iterable<?>, Iterable<?>> record = new Tuple<>(tripRecords, legRecords);
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(experiencedPlan);

        for (int i = 0; i < trips.size(); i++) {
            TripStructureUtils.Trip trip = trips.get(i);
            List<String> tripRecord = new ArrayList<>();
            tripRecords.add(tripRecord);
            tripRecord.add(personId.toString());
            final String tripNo = Integer.toString(i + 1);
            tripRecord.add(tripNo); // trip number, numbered starting with 0
            String tripId = personId + "_" + tripNo;
            tripRecord.add(tripId);
            double distance = 0.0;
            double departureTime = trip.getOriginActivity().getEndTime().orElse(0);
            double travelTime = trip.getDestinationActivity().getStartTime().orElse(0) - departureTime;
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
            String firstPtBoardingStop = null;
            String lastPtEgressStop = null;

            String mainMode = "";
            if (mainModeIdentifier != null) {
                try {
                    mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
					if (mainMode == null)
						mainMode = "";
                } catch (Exception e) {
                    // leave field empty
                }
            }

            for (Leg leg : trip.getLegsOnly()) {
                modes.add(leg.getMode());
                final double legDist = leg.getRoute().getDistance();
                distance += legDist;
                Double boardingTime = (Double) leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
                if (boardingTime != null) {
					double waitingTime = boardingTime - leg.getDepartureTime().seconds();
                    totalWaitingTime += waitingTime;
                }
                if (StringUtils.isBlank(currentModeWithLongestShare) || legDist > currentLongestShareDistance) {
                    currentLongestShareDistance = legDist;
                    currentModeWithLongestShare = leg.getMode();

                }
                if (leg.getRoute() instanceof TransitPassengerRoute) {
                    TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
                    firstPtBoardingStop = firstPtBoardingStop != null ? firstPtBoardingStop : route.getAccessStopId().toString();
                    lastPtEgressStop = route.getEgressStopId().toString();
                }

            }

            tripRecord.add(customTimeWriter.writeTime(departureTime));
            tripRecord.add(customTimeWriter.writeTime(travelTime));
            tripRecord.add(customTimeWriter.writeTime(totalWaitingTime));
            tripRecord.add(Integer.toString((int) Math.round(distance)));
            tripRecord.add(Integer.toString(euclideanDistance));
            tripRecord.add(mainMode);
            tripRecord.add(currentModeWithLongestShare);
            tripRecord.add(modes.stream().collect(Collectors.joining("-")));
            tripRecord.add(lastActivityType);
            tripRecord.add(nextActivityType);
            tripRecord.add(String.valueOf(fromFacilityId));
            tripRecord.add(String.valueOf(fromLinkId));
            tripRecord.add(Double.toString(fromCoord.getX()));
            tripRecord.add(Double.toString(fromCoord.getY()));

            tripRecord.add(String.valueOf(toFacilityId));
            tripRecord.add(String.valueOf(toLinkId));
            tripRecord.add(Double.toString(toCoord.getX()));
            tripRecord.add(Double.toString(toCoord.getY()));
            tripRecord.add(firstPtBoardingStop != null ? firstPtBoardingStop : "");
            tripRecord.add(lastPtEgressStop != null ? lastPtEgressStop : "");
            tripRecord.addAll(tripsWriterExtension.getAdditionalTripColumns(personId, trip));

            if (TRIPSHEADER.length != tripRecord.size()) {
                // put the whole error message also into the RuntimeException, so maven shows it on the command line output (log messages are shown incompletely)
				String errorMessage = getTripErrorMessage(tripRecord).toString();
				log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            Activity prevAct = null;
            Leg prevLeg = null;
            List<PlanElement> allElements = new ArrayList<>();
            allElements.add(trip.getOriginActivity());
            allElements.addAll(trip.getTripElements());
            allElements.add(trip.getDestinationActivity());
            for (PlanElement pe : allElements) {
                if (pe instanceof Activity currentAct) {
					if (prevLeg != null) {
                        List<String> legRecord = getLegRecord(prevLeg, personId.toString(), tripId, prevAct, currentAct, trip);
                        legRecords.add(legRecord);
                    }
                    prevAct = currentAct;

                } else if (pe instanceof Leg) {
                    prevLeg = (Leg) pe;
                }
            }
        }

        return record;
    }

	private StringBuilder getTripErrorMessage(List<String> tripRecord) {
		StringBuilder str = new StringBuilder();
		str.append("Custom CSV Trip Writer Extension does not provide an identical number of additional values and additional columns. Number of columns is ")
           .append(TRIPSHEADER.length).append(", and number of values is ").append(tripRecord.size())
           .append(".\n")
		   .append("TripsWriterExtension class was: ").append(tripsWriterExtension.getClass())
           .append(". Column name to value pairs supplied were:\n");
		for(int j = 0; j < Math.max(TRIPSHEADER.length, tripRecord.size()); j++) {
			String columnNameJ;
			try {
				columnNameJ = TRIPSHEADER[j];
			} catch (ArrayIndexOutOfBoundsException e) {
				columnNameJ = "!COLUMN MISSING!";
			}
			String tripRecordJ;
			try {
				tripRecordJ = tripRecord.get(j);
			} catch (IndexOutOfBoundsException e) {
				tripRecordJ = "!VALUE MISSING!";
			}
			str.append(j + ": " + columnNameJ + ": " + tripRecordJ + "\n");
		}
		return str;
	}

	private List<String> getLegRecord(Leg leg, String personId, String tripId, Activity previousAct, Activity nextAct, TripStructureUtils.Trip trip) {
        List<String> record = new ArrayList<>();
        record.add(personId);
        record.add(tripId);
        record.add(customTimeWriter.writeTime(leg.getDepartureTime().seconds()));
        record.add(customTimeWriter.writeTime(leg.getTravelTime().seconds()));
        Double boardingTime = (Double) leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
        Id<Vehicle> vehicleId = (Id<Vehicle>) leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME);
        double waitingTime = 0.;
        if (boardingTime != null) {
            waitingTime = boardingTime - leg.getDepartureTime().seconds();
        }
        record.add(customTimeWriter.writeTime(waitingTime));
        record.add(Integer.toString((int) leg.getRoute().getDistance()));
        record.add(leg.getMode());
        record.add(leg.getRoute().getStartLinkId().toString());
        Coord startCoord = getCoordFromActivity(previousAct);
        Coord endCoord = getCoordFromActivity(nextAct);
        record.add(Double.toString(startCoord.getX()));
        record.add(Double.toString(startCoord.getY()));
        record.add(leg.getRoute().getEndLinkId().toString());
        record.add(Double.toString(endCoord.getX()));
        record.add(Double.toString(endCoord.getY()));
        String transitLine = "";
        String transitRoute = "";
        String ptAccessStop = "";
        String ptEgressStop = "";
        if (leg.getRoute() instanceof TransitPassengerRoute) {
            TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
            transitLine = route.getLineId().toString();
            transitRoute = route.getRouteId().toString();
            ptAccessStop = route.getAccessStopId().toString();
            ptEgressStop = route.getEgressStopId().toString();
        }
        record.add(ptAccessStop);
        record.add(ptEgressStop);
        record.add(transitLine);
        record.add(transitRoute);
        record.add(vehicleId != null ? vehicleId.toString() : "");

        record.addAll(legsWriterExtension.getAdditionalLegColumns(trip, leg));

        if (LEGSHEADER.length != record.size()) {
            // put the whole error message also into the RuntimeException, so maven shows it on the command line output (log messages are shown incompletely)
			String errorMessage = getLegErrorMessage(record);
			log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return record;
    }

	private String getLegErrorMessage(List<String> record) {
		StringBuilder str = new StringBuilder();
		str.append("Custom CSV Leg Writer Extension does not provide an identical number of additional values and additional columns. Number of columns is ")
		   .append(LEGSHEADER.length)
		   .append(", and number of values is ")
		   .append(record.size())
		   .append(".\n")
		   .append("LegsWriterExtension class was: ")
		   .append(legsWriterExtension.getClass())
		   .append(". Column name to value pairs supplied were:\n");
		for(int j = 0; j < Math.max(LEGSHEADER.length, record.size()); j++) {
			String columnNameJ;
			try {
				columnNameJ = LEGSHEADER[j];
			} catch (ArrayIndexOutOfBoundsException e) {
				columnNameJ = "!COLUMN MISSING!";
			}
			String recordJ;
			try {
				recordJ = record.get(j);
			} catch (IndexOutOfBoundsException e) {
				recordJ = "!VALUE MISSING!";
			}
			str.append(j).append(": ").append(columnNameJ).append(": ").append(recordJ).append("\n");
		}
		return str.toString();
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

        String[] getAdditionalTripHeader();

        List<String> getAdditionalTripColumns(TripStructureUtils.Trip trip);

        default List<String> getAdditionalTripColumns(Id<Person> personId, TripStructureUtils.Trip trip) {
            return getAdditionalTripColumns(trip);
        }
    }

    public interface CustomLegsWriterExtension {
        String[] getAdditionalLegHeader();

        List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg);
    }


    public interface CustomTimeWriter {
        String writeTime(double time);
    }

    static class SecondsFromMidnightTimeWriter implements CustomTimeWriter {
        @Override
        public String writeTime(double time) {
            return Long.toString((long) time);
        }
    }

    static class DefaultTimeWriter implements CustomTimeWriter {
        @Override
        public String writeTime(double time) {
            return Time.writeTime(time);
        }
    }

    static class NoTripWriterExtension implements CustomTripsWriterExtension {
        @Override
        public String[] getAdditionalTripHeader() {
            return new String[0];
        }

        @Override
        public List<String> getAdditionalTripColumns(TripStructureUtils.Trip trip) {
            return Collections.EMPTY_LIST;
        }

    }

    static class NoLegsWriterExtension implements CustomLegsWriterExtension {
        @Override
        public String[] getAdditionalLegHeader() {
            return new String[0];
        }

        @Override
        public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
            return Collections.EMPTY_LIST;
        }
    }
}
