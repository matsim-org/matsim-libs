/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.tripAnalyzer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ExperiencedTripsWriter {
	private String path;
	private Map<Id<Person>, List<ExperiencedTrip>> agent2trips;
	private Set<String> monitoredModes;
	// first level separator
	private String sep = ";";
	// second level separator
	private String sep2 = ",";
	private BufferedWriter bw;
	private Network network;

	private Map<String, Geometry> zoneMap;
	private Geometry boundary;
	private List<Geometry> districtGeometryList;
	private GeometryFactory geomfactory;
	private GeometryCollection geometryCollection;
	private Map<String, List<ExperiencedTrip>> tourId2trips;
	private Map<String, List<ParkingEvent>> zoneId2ParkingEvents;
	private List<ParkingEvent> parkingEvents;
	private Map<Id<Link>, String> linkToZoneMap;
	private Map<String, MutableInt> zoneToStreetMeters;
	private Map<String, Map<Double, Set<Id<Vehicle>>>> Zone2BinActiveVehicleMap;
	private Map<String, MutableDouble> Mode2MileageMap;
	private Set<ModalSplitSegment> ModalSplitSegments;

	public ExperiencedTripsWriter(String path, Map<Id<Person>, List<ExperiencedTrip>> agent2trips,
			Map<String, Map<Double, Set<Id<Vehicle>>>> Zone2BinActiveVehicleMap,
			Map<String, MutableDouble> Mode2MileageMap, Set<String> monitoredModes, Network network,
			Map<String, Geometry> zoneMap) {
		this.path = path;
		this.agent2trips = agent2trips;
		this.network = network;
		this.monitoredModes = monitoredModes;
		this.zoneMap = zoneMap;
		this.tourId2trips = new HashMap<String, List<ExperiencedTrip>>();
		this.parkingEvents = new ArrayList<ParkingEvent>();
		this.linkToZoneMap = new HashMap<Id<Link>, String>();
		this.zoneToStreetMeters = new HashMap<String, MutableInt>();
		this.Zone2BinActiveVehicleMap = Zone2BinActiveVehicleMap;
		this.Mode2MileageMap = Mode2MileageMap;
		this.ModalSplitSegments = new HashSet<ModalSplitSegment>();

		districtGeometryList = new ArrayList<Geometry>();
		zoneId2ParkingEvents = new HashMap<String, List<ParkingEvent>>();
		geomfactory = JTSFactoryFinder.getGeometryFactory(null);
		geometryCollection = geomfactory.createGeometryCollection(null);

		try {
			initialize();
			calucalteCarLinkLengthPerZone();
			getResearchAreaBoundary();
			tourIdentifier();
			tourClassifier();
			getParkingTimes();
			String folder = new File(path).getParentFile().getName();
			analyseCarParking(new File(path).getParent() + "\\" + folder + ".parking", parkingEvents, 120);
			analyseActiveVehicles(new File(path).getParent() + "\\" + folder + ".activeVehicles");
			writeMileagePerMode(new File(path).getParent() + "\\" + folder + ".mileage");
			writeModalSplits(new File(path).getParent() + "\\" + folder + ".modalsplit");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not initialize writer");
		}
	}

	private void initialize() throws IOException {
		bw = IOUtils.getBufferedWriter(path);
		// write header
		bw.write("tripClass" + sep + "tripId" + sep + "agent" + sep + "tripNumber" + sep + "activityBefore" + sep
				+ "activityAfter" + sep + "fromLinkId" + sep + "fromX" + sep + "fromY" + sep + "toLinkId" + sep + "toX"
				+ sep + "toY" + sep + "startTime" + sep + "endTime" + sep + "totalTravelTime" + sep + "numberOfLegs"
				+ sep + "transitStopsVisited");
		for (String mode : monitoredModes) {
			bw.write(sep + mode + ".InVehicleTime");
			bw.write(sep + mode + ".Distance");
			bw.write(sep + mode + ".WaitTime");
			bw.write(sep + mode + ".maxPerLegWaitTime");
			bw.write(sep + mode + ".NumberOfLegs");
		}
		bw.write(sep + "Other" + ".InVehicleTime");
		bw.write(sep + "Other" + ".Distance");
		bw.write(sep + "Other" + ".WaitTime");
		bw.write(sep + "Other" + ".maxPerLegWaitTime");
		bw.write(sep + "Other" + ".NumberOfLegs");

	}

	private void tourClassifier() {
		Set<String> acceptedMainModes = new HashSet<>(Arrays.asList("car", "pt", "drt", "walk", "ride", "bike"));

		// Modal Split Classes:
		// Class 1: tours (no spatial filtering, complete demand model)
		// Class 2: tours of city inhabitants (home activity is within city area)
		// Class 3: tours within the city (all tour activities are within city area)
		// Class 4: trips within the city (all trips with O/D within city area )
		// Class 5: in-bound commuter tours (all tours with work within city area, home
		// outside)
		// Class 6: out-bound commuter tours (all tours with works outside city area,
		// home inside)
		// Class 7: tours of home office candidates -->
		// (all tours with work within city area, home outside) or
		// (all tours with work outside city area, lives inside) or
		// (all tours with home within city area and work within city area)
		// Class 8: tours with no activities in city area

		ModalSplitSegment Class1 = new ModalSplitSegment("1", acceptedMainModes);
		ModalSplitSegment Class2 = new ModalSplitSegment("2", acceptedMainModes);
		ModalSplitSegment Class3 = new ModalSplitSegment("3", acceptedMainModes);
		ModalSplitSegment Class4 = new ModalSplitSegment("4", acceptedMainModes);
		ModalSplitSegment Class5 = new ModalSplitSegment("5", acceptedMainModes);
		ModalSplitSegment Class6 = new ModalSplitSegment("6", acceptedMainModes);

		ModalSplitSegments.add(Class1);
		ModalSplitSegments.add(Class2);
		ModalSplitSegments.add(Class3);
		ModalSplitSegments.add(Class4);
		ModalSplitSegments.add(Class5);
		ModalSplitSegments.add(Class6);

		// 1 Tour contains N Trips
		for (Entry<String, List<ExperiencedTrip>> TourEntrySet : tourId2trips.entrySet()) {
			// String tourId = TourEntrySet.getKey();
			List<ExperiencedTrip> triplist = TourEntrySet.getValue();

			// Handle Class 1: All trips of all tours are stored
			{

				for (ExperiencedTrip trip : triplist) {
					String mainMode = trip.getMainMode();
					if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
						double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

						if (Class1.mode2TripDistance.containsKey(mainMode)) {
							Class1.mode2TripDistance.get(mainMode).add(distance);

						} else {
							Class1.mode2TripDistance.put(mainMode, new ArrayList<Double>());
							Class1.mode2TripDistance.get(mainMode).add(distance);

						}

					}
					// ToDo
					// Else mode is unknown and not analyzed
					// E.g. pure transit_walks are not counted anyway

				}
			}

			// Class 2: All tours of city inhabitants (home activity is within city area) /
			// MID modal split
			{

				if (livesInside(triplist)) {
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class2.mode2TripDistance.containsKey(mainMode)) {
								Class2.mode2TripDistance.get(mainMode).add(distance);

							} else {
								Class2.mode2TripDistance.put(mainMode, new ArrayList<Double>());
								Class2.mode2TripDistance.get(mainMode).add(distance);

							}

						}
						// ToDo
						// Else mode is unknown and not analyzed
						// E.g. pure transit_walks are not counted anyway

					}

				}

			}

			// Class 3
			{
				if (allActivitiesWithinCity(triplist)) {
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class3.mode2TripDistance.containsKey(mainMode)) {
								Class3.mode2TripDistance.get(mainMode).add(distance);

							} else {
								Class3.mode2TripDistance.put(mainMode, new ArrayList<Double>());
								Class3.mode2TripDistance.get(mainMode).add(distance);

							}

						}
						// ToDo
						// Else mode is unknown and not analyzed
						// E.g. pure transit_walks are not counted anyway

					}

				}

			}

			// Class 4: trips within the city (all trips with O/D within city area )
			{

				for (ExperiencedTrip trip : triplist) {
					if (tripWithinCity(trip)) {
						String mainMode = trip.getMainMode();
						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class4.mode2TripDistance.containsKey(mainMode)) {
								Class4.mode2TripDistance.get(mainMode).add(distance);

							} else {
								Class4.mode2TripDistance.put(mainMode, new ArrayList<Double>());
								Class4.mode2TripDistance.get(mainMode).add(distance);

							}

						}

					}

					// ToDo
					// Else mode is unknown and not analyzed
					// E.g. pure transit_walks are not counted anyway

				}

			}

			// Class 5: in-bound commuter tours (all tours with work within city area, home
			// outside)
			{
				if (isInboundCommuterTour(triplist)) {
					// Id<Person> person = triplist.get(0).getAgent();
					// System.out.println(person.toString());
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						// System.out.println(trip.getSubTourNr() + "||" + mainMode);

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class5.mode2TripDistance.containsKey(mainMode)) {
								Class5.mode2TripDistance.get(mainMode).add(distance);

							} else {
								Class5.mode2TripDistance.put(mainMode, new ArrayList<Double>());
								Class5.mode2TripDistance.get(mainMode).add(distance);

							}

						}
						// ToDo
						// Else mode is unknown and not analyzed
						// E.g. pure transit_walks are not counted anyway

					}

				}

			}

			// Class 6:
			{
				if (isOutboundCommuterTour(triplist)) {
					// Id<Person> person = triplist.get(0).getAgent();
					// System.out.println(person.toString());
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						// System.out.println(trip.getSubTourNr() + "||" + mainMode);

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class6.mode2TripDistance.containsKey(mainMode)) {
								Class6.mode2TripDistance.get(mainMode).add(distance);

							} else {
								Class6.mode2TripDistance.put(mainMode, new ArrayList<Double>());
								Class6.mode2TripDistance.get(mainMode).add(distance);

							}

						}
						// ToDo
						// Else mode is unknown and not analyzed
						// E.g. pure transit_walks are not counted anyway

					}

				}

			}

		}

		// System.out.println("Class 1");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class1.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }
		//
		// System.out.println("Class 2");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class2.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }
		//
		// System.out.println("Class 3");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class3.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }
		//
		// System.out.println("Class 4");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class4.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }
		//
		// System.out.println("Class 5");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class5.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }
		//
		// System.out.println("Class 6");
		// for (String mode : acceptedMainModes) {
		// int tripCount = Class6.getNumberOfTripsPerMode(mode);
		// System.out.println("mode: " + mode + " || " + tripCount);
		//
		// }

	}

	private boolean isOutboundCommuterTour(List<ExperiencedTrip> triplist) {

		if (worksOutside(triplist) && livesInside(triplist)) {
			return true;
		}
		return false;

	}

	private boolean isInboundCommuterTour(List<ExperiencedTrip> triplist) {

		if (livesOutside(triplist) && worksInside(triplist)) {
			return true;
		}
		return false;

	}

	// private boolean isWithinCommuterTour(List<ExperiencedTrip> triplist) {
	//
	// if (worksInside(triplist) && livesInside(triplist)) {
	// return true;
	//
	// }
	// return false;
	//
	// }

	public boolean worksInside(List<ExperiencedTrip> triplist) {

		for (ExperiencedTrip trip : triplist) {
			String ActBefore = trip.getActivityBefore();

			if (ActBefore.contains("work")) {
				Link fromLink = network.getLinks().get(trip.getFromLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean worksOutside(List<ExperiencedTrip> triplist) {

		for (ExperiencedTrip trip : triplist) {
			String ActBefore = trip.getActivityBefore();

			if (ActBefore.contains("work")) {
				Link fromLink = network.getLinks().get(trip.getFromLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (!isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean livesInside(List<ExperiencedTrip> triplist) {

		for (ExperiencedTrip trip : triplist) {
			String ActBefore = trip.getActivityBefore();

			if (ActBefore.contains("home")) {
				Link fromLink = network.getLinks().get(trip.getFromLinkId());

				Coord coord = fromLink.getCoord();
				// If home is inside zoneMap return true
				if (isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean livesOutside(List<ExperiencedTrip> triplist) {

		for (ExperiencedTrip trip : triplist) {
			String ActBefore = trip.getActivityBefore();

			if (ActBefore.contains("home")) {
				Link fromLink = network.getLinks().get(trip.getFromLinkId());

				Coord coord = fromLink.getCoord();
				// If home is inside zoneMap return true
				if (!isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean allActivitiesWithinCity(List<ExperiencedTrip> triplist) {

		for (ExperiencedTrip trip : triplist) {

			Link fromLink = network.getLinks().get(trip.getFromLinkId());

			Coord coord = fromLink.getCoord();
			// return false if act is outside city area
			if (!isWithinZone(coord)) {
				return false;
			}

		}

		return true;

	}

	public boolean tripWithinCity(ExperiencedTrip trip) {

		Link fromLink = network.getLinks().get(trip.getFromLinkId());
		Link toLink = network.getLinks().get(trip.getToLinkId());

		Coord coord1 = fromLink.getCoord();
		Coord coord2 = toLink.getCoord();
		// return false if act is outside city area
		if (isWithinZone(coord1) && isWithinZone(coord2)) {
			return true;
		} else
			return false;

	}

	private void tourIdentifier() {
		String tourMatchKey = "home";
		// int checkedToursCounter = 0;

		for (Entry<Id<Person>, List<ExperiencedTrip>> tripsPerPersonEntry : agent2trips.entrySet()) {

			// Id<Person> PersonId = tripsPerPersonEntry.getKey();
			List<ExperiencedTrip> tripList = tripsPerPersonEntry.getValue();

			int subtourNr = 0;
			List<ExperiencedTrip> tripsWithinTour = new ArrayList<ExperiencedTrip>();
			String TourId = tripsPerPersonEntry.getKey().toString() + "__" + subtourNr;

			for (ExperiencedTrip trip : tripList) {

				String actAfter = trip.getActivityAfter();
				trip.setSubTourNr(subtourNr);
				tripsWithinTour.add(trip);

				if (actAfter.contains(tourMatchKey)) {
					// Create new List of ExperiencedTrip --> Tour
					// Save this tour
					// Clear the tripsWithinTour
					List<ExperiencedTrip> saveTripsWithinTour = new ArrayList<ExperiencedTrip>();
					saveTripsWithinTour.addAll(tripsWithinTour);
					tourId2trips.put(TourId, saveTripsWithinTour);
					subtourNr++;
					// checkedToursCounter++;
					tripsWithinTour.clear();

				}

			}

		}

		// System.out.print("Seen Tours #" + checkedToursCounter + "\n");

	}

	public boolean isWithinZone(Coord coord) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		for (String zone : zoneMap.keySet()) {
			Geometry geometry = zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return true;
			}
		}

		return false;
	}

	public String intersectShape(LineString beeline) {

		Point from = beeline.getStartPoint();
		Point to = beeline.getEndPoint();
		Geometry geom = boundary;

		if (geom.contains(to) && !geom.contains(from)) {
			return "inbound";
		}

		else if (geom.contains(from) && !geom.contains(to)) {
			return "outbound";
		}

		else if (!(geom.contains(from)) && !(geom.contains(to)) && (beeline.intersects(geom))) {
			return "through";
		}

		else if (!(geom.contains(from)) && !(geom.contains(to)) && !(beeline.intersects(geom))) {
			return "outside";
		}

		else if (geom.contains(from) && geom.contains(to)) {
			return "inside";
		} else
			return "undefined";

		//
		// for (Entry<String, Geometry> zoneGeom : zoneMap.entrySet()) {
		// if (zoneGeom.getValue().intersects(beeline)) {
		// return true;
		// }
		// }
		//
		// return false;

	}

	public void getResearchAreaBoundary() {
		// This class infers the geometric boundary of all network link

		for (Geometry zoneGeom : this.zoneMap.values()) {
			districtGeometryList.add(zoneGeom);
		}

		geometryCollection = (GeometryCollection) geomfactory.buildGeometry(districtGeometryList);
		this.boundary = geometryCollection.union();

	}

	private int mod(int x, int y) {
		int result = x % y;
		if (result < 0) {
			result += y;
		}
		return result;
	}

	private String identParkingZone(Coord coord) {

		for (String zone : zoneMap.keySet()) {
			Geometry geometry = zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return zone;
			}
		}

		return null;

	}

	public void getParkingTimes() {

		for (Entry<String, List<ExperiencedTrip>> TourEntrySet : tourId2trips.entrySet()) {
			// String tourId = TourEntrySet.getKey();
			List<ExperiencedTrip> triplist = TourEntrySet.getValue();

			int nrOfTrips = triplist.size();
			int i = 0;

			for (ExperiencedTrip trip : triplist) {
				String mainMode = trip.getMainMode();

				if (mainMode.equals(TransportMode.car)) {

					int pointer = mod((i - 1), nrOfTrips);
					double endOfParking = trip.getStartTime(); // trip start time == act end time
					Coord coord = network.getLinks().get(trip.getFromLinkId()).getCoord();
					double startOfParking = triplist.get(pointer).getEndTime(); // trip end time == act start time
					double activityDuration = Math.abs(endOfParking - startOfParking);
					String parkingZone = identParkingZone(coord);

					// trip.setParkingStart(startOfParking);
					// trip.setParkingEnd(endOfParking);
					Id<Person> personId = trip.getAgent();

					// If the activity was 0, there was no parking
					// Acutal Hanover model contains activities with 0 duration
					if (activityDuration > 0) {

						// If person parks over night (>24h) one needs to create two parking events
						// Split into two parking events till mid-night startOfParking---->mid-night +
						// mid-night ----> endOfParking
						if (endOfParking < startOfParking) {
							parkingEvents
									.add(new ParkingEvent(startOfParking, 24 * 3600.0, personId, coord, parkingZone));
							parkingEvents.add(new ParkingEvent(0.0, endOfParking, personId, coord, parkingZone));

						} else {

							parkingEvents
									.add(new ParkingEvent(startOfParking, endOfParking, personId, coord, parkingZone));
						}

					}
				}

				i++;
			}

		}

	}

	private void calucalteCarLinkLengthPerZone() {
		Map<Id<Link>, Geometry> linkIdGeometryMap = new HashMap<>();

		GeometryFactory f = new GeometryFactory();

		// int linkNumber = this.network.getLinks().values().size();
		// int linkCounter = 0;

		for (Link l : this.network.getLinks().values()) {
			if (l.getAllowedModes().contains("car")) {
				// Construct a LineSegment from link coordinates
				Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
				Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY());
				linkIdGeometryMap.put(l.getId(), new LineSegment(start, end).toGeometry(f));
			}
		}

		// Iterate over each link
		for (Entry<Id<Link>, Geometry> l : linkIdGeometryMap.entrySet()) {

			// Check if link intersects with zone
			for (String z : zoneMap.keySet()) {
				// System.out.println("Working on Zone: "+z);
				Geometry zone = this.zoneMap.get(z);
				if (zone.intersects(l.getValue())) {
					double linkLength = network.getLinks().get(l.getKey()).getLength();
					// System.out.println("Put link: " +l.getId() + " to zone: " +z);
					// this.linkToZoneMap.put(l.getKey(), z);

					if (zoneToStreetMeters.containsKey(z)) {
						zoneToStreetMeters.get(z).add(linkLength);
					} else {
						zoneToStreetMeters.put(z, new MutableInt(linkLength));
					}

					break;

				}
			}
			// linkCounter += 1;
			// System.out.println(linkCounter + " out of " +linkNumber );
		}

	}

	public void analyseCarParking(String fileName, List<ParkingEvent> parkingEvents, int binsize_s) {

		for (ParkingEvent event : parkingEvents) {
			String zoneId = event.getParkingZone();

			if (zoneId != null) {
				if (zoneId2ParkingEvents.containsKey(zoneId)) {
					zoneId2ParkingEvents.get(zoneId).add(event);

				} else {
					zoneId2ParkingEvents.put(zoneId, new ArrayList<ParkingEvent>());
					zoneId2ParkingEvents.get(zoneId).add(event);

				}
			}

		}

		if (parkingEvents.size() == 0)
			return;
		int startTime = 0;
		int endTime = 24 * 3600;
		Map<Double, List<ParkingEvent>> splitParkings = splitParkingsIntoBins(parkingEvents, startTime, endTime,
				binsize_s);

		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");
		 BufferedWriter bw_rel = IOUtils.getBufferedWriter(fileName + "_relational" +
		 ".csv");

		TimeSeriesCollection datasetrequ = new TimeSeriesCollection();
		TimeSeries parkCount = new TimeSeries("Active Park Events");

		try {
			// bw.write("timebin;parkings");
			bw.write("timebin");

			for (String zoneId : zoneMap.keySet()) {
				bw.write(";" + zoneId.toString());
			}
			bw.newLine();

			// ToDo write header for zones

			// Iterate over timebin
			for (Entry<Double, List<ParkingEvent>> e : splitParkings.entrySet()) {

				String row = Time.writeTime(e.getKey());

				for (String zoneId : zoneMap.keySet()) {

					long parkings = 0;

					if (!e.getValue().isEmpty()) {
						DescriptiveStatistics stats = new DescriptiveStatistics();
						for (ParkingEvent t : e.getValue()) {

							if (t.getParkingZone() != null) {
								if (t.getParkingZone().equals(zoneId)) {
									stats.addValue(1.0);
								}
							}

						}
						parkings = stats.getN();

						row = (row + ";" + parkings);

					}
					// Minute h = new Minute(sdf2.parse(Time.writeTime(e.getKey())));
					//
					// parkCount.addOrUpdate(h, parkings);
					// Time;ValuePerZone
					// bw.write(Time.writeTime(e.getKey()) + ";" + parkings);

				}
				bw.write(row);
				bw.newLine();

			}
			bw.flush();
			bw.close();
			// datasetrequ.addSeries(parkCount);
			// // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
			// // times", "Wait time (s)");
			// JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ, "Parked
			// Vehicles over Time",
			// "Total Parked Cars [-]");
			// // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			// ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);

		} catch (IOException e) {

			e.printStackTrace();
		}

		System.out.println("Start writing relational parking statistics");
		 // Write content in relational file format
		 try {
		 // Write header timebin,zone,parkingDemand
		 bw_rel.write("id;timebin;zone;parkingDemand;steet_m;density_km;geom");
		 bw_rel.newLine();
		
		 // ToDo write header for zones
		
		 int rowId = 0;
		 // Iterate over timebin
		 for (Entry<Double, List<ParkingEvent>> e : splitParkings.entrySet()) {
		
		 for (String zoneId : zoneMap.keySet()) {
		
		 long parkings = 0;
		
		 if (!e.getValue().isEmpty()) {
		 DescriptiveStatistics stats = new DescriptiveStatistics();
		 for (ParkingEvent t : e.getValue()) {
		
		 if (t.getParkingZone() != null) {
		 if (t.getParkingZone().equals(zoneId)) {
		 stats.addValue(1.0);
		 }
		 }
		
		 }
		 parkings = stats.getN();
		
		 WKTWriter wktwriter = new WKTWriter();
		
		 double densityPerKm = parkings /
		 (zoneToStreetMeters.get(zoneId).doubleValue() / 1000.0);
		
		 bw_rel.write(rowId + ";" + Time.writeTime(e.getKey()) + ";" + zoneId + ";" +
		 parkings + ";"
		 + zoneToStreetMeters.get(zoneId) + ";" + densityPerKm + ";"
		 + wktwriter.write(zoneMap.get(zoneId)));
		 bw_rel.newLine();
		 rowId++;
		
		 }
		 // Minute h = new Minute(sdf2.parse(Time.writeTime(e.getKey())));
		 //
		 // parkCount.addOrUpdate(h, parkings);
		 // Time;ValuePerZone
		 // bw.write(Time.writeTime(e.getKey()) + ";" + parkings);
		
		 }
		
		 }
		 bw_rel.flush();
		 bw_rel.close();
		 // datasetrequ.addSeries(parkCount);
		 // // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
		 // // times", "Wait time (s)");
		 // JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ,
		
		 //  "Parked Vehicles over Time",
		 // "Total Parked Cars [-]");
		 // // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
		 // ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);
		
		 } catch (IOException e) {
		
		 e.printStackTrace();
		 }

	}

	public void writeModalSplits(String fileName) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");
	
		
		try {
			//Write header
			ModalSplitSegment dummyModalSplitForHeader   = ModalSplitSegments.iterator().next();
			String header_modes = dummyModalSplitForHeader.mode2TripDistance.keySet().stream().map(Object::toString).collect(Collectors.joining(";"));
			bw.write("modalSplitType"+";"+header_modes.toString());
			
			bw.newLine();
			
			for (ModalSplitSegment segment : ModalSplitSegments)
			{
				
				String row= segment.SegmentClassNr;

				for (Entry<String, ArrayList<Double>> e : segment.mode2TripDistance.entrySet()) {

					
					row = row+";"+e.getValue().size();

				}
				bw.write(row);
				bw.newLine();
			}
			


			bw.flush();
			bw.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void writeMileagePerMode(String fileName) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");

		try {

			bw.write("mode;mileage_km");
			bw.newLine();
			for (Entry<String, MutableDouble> e : Mode2MileageMap.entrySet()) {

				String row = e.getKey() + ";" + e.getValue().getValue() / 1000.00;

				bw.write(row);
				bw.newLine();

			}

			bw.flush();
			bw.close();
			// datasetrequ.addSeries(parkCount);
			// // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
			// // times", "Wait time (s)");
			// JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ, "Parked
			// Vehicles over Time",
			// "Total Parked Cars [-]");
			// // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			// ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void analyseActiveVehicles(String fileName) {

		// for (ParkingEvent event : parkingEvents) {
		// String zoneId = event.getParkingZone();
		//
		// if (zoneId != null) {
		// if (zoneId2ParkingEvents.containsKey(zoneId)) {
		// zoneId2ParkingEvents.get(zoneId).add(event);
		//
		// } else {
		// zoneId2ParkingEvents.put(zoneId, new ArrayList<ParkingEvent>());
		// zoneId2ParkingEvents.get(zoneId).add(event);
		//
		// }
		// }
		//
		// }

		// if (parkingEvents.size() == 0)
		// return;
		int startTime = 0;
		int endTime = 24 * 3600;
		// Map<Double, List<ParkingEvent>> splitParkings =
		// splitParkingsIntoBins(parkingEvents, startTime, endTime,
		// binsize_s);

		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");
		// BufferedWriter bw_rel = IOUtils.getBufferedWriter(fileName + "_relational" +
		// ".csv");

		TimeSeriesCollection datasetrequ = new TimeSeriesCollection();
		TimeSeries parkCount = new TimeSeries("Active Vehicles Per Zone");

		try {
			bw.write("timebin");

			for (String zoneId : zoneMap.keySet()) {
				bw.write(";" + zoneId.toString());
			}
			bw.newLine();

			// ToDo write header for zones

			// Iterate over timebin

			SortedSet<Double> timeBins = new TreeSet<>(
					Zone2BinActiveVehicleMap.get(Zone2BinActiveVehicleMap.keySet().toArray()[0]).keySet());
			Set<String> zones = Zone2BinActiveVehicleMap.keySet();

			for (Double e : timeBins) {

				String row = Time.writeTime(e);

				for (String zoneId : zones) {

					long activeVehicles = 0;

					activeVehicles = Zone2BinActiveVehicleMap.get(zoneId).get(e).size();

					row = (row + ";" + activeVehicles);

				}
				bw.write(row);
				bw.newLine();

			}

			bw.flush();
			bw.close();
			// datasetrequ.addSeries(parkCount);
			// // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
			// // times", "Wait time (s)");
			// JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ, "Parked
			// Vehicles over Time",
			// "Total Parked Cars [-]");
			// // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			// ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private JFreeChart chartProfile(int length, TimeSeriesCollection dataset, String descriptor, String yax) {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(descriptor, "Time", yax, dataset);

		// ChartFactory.createXYLineChart("TimeProfile", "Time", "Wait Time
		// [s]", dataset,
		// PlotOrientation.VERTICAL, true, false, false);

		XYPlot plot = chart.getXYPlot();
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setBackgroundPaint(Color.white);

		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRange(true);

		XYItemRenderer renderer = plot.getRenderer();
		for (int s = 0; s < length; s++) {
			renderer.setSeriesStroke(s, new BasicStroke(2));
		}

		return chart;
	}

	public Map<Double, List<ParkingEvent>> splitParkingsIntoBins(Collection<ParkingEvent> parkEvents, int startTime,
			int endTime, int binSize_s) {

		LinkedList<ParkingEvent> allParkEvents = new LinkedList<>();
		allParkEvents.addAll(parkEvents);
		// Collections.sort(allParkEvents);

		// ParkingEvent currentParkEvent = allParkEvents.pollFirst();
		// if (currentParkEvent.getStartOfParking() > endTime) {
		// Logger.getLogger(ExperiencedTripsWriter.class).error("wrong end / start Times
		// for analysis");
		// }

		Map<Double, List<ParkingEvent>> splitParkEvents = new TreeMap<>();
		for (int time = startTime; time < endTime; time = time + binSize_s) {

			// time |-------| time + binSize_s //

			// Create an empty list of park events
			// We need to fill this list of this particular time bin
			List<ParkingEvent> currentList = new ArrayList<>();
			splitParkEvents.put(Double.valueOf(time), currentList); // time == start of interval

			int leftBinBorder = time;
			int righBintBorder = time + binSize_s;
			// A single park event can cover multiple bins
			// Thus, we need to check for each bin whether this parking event is
			// intersection the bin time interval
			for (ParkingEvent currentParkEvent : parkEvents) {
				double parkStart = currentParkEvent.getStartOfParking();
				double parkEnd = currentParkEvent.getEndOfParking();

				boolean skipBin = ((parkEnd < leftBinBorder) || (parkStart > righBintBorder));

				if (!skipBin) {

					currentList.add(currentParkEvent);
				}

			}
			// System.out.println(time + " || " + currentList.size());
			splitParkEvents.put(Double.valueOf(time), currentList);

		}

		return splitParkEvents;

	}

	public void writeExperiencedTrips() {
		try {
			bw.newLine();
			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				for (ExperiencedTrip trip : tripList) {

					Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
					Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();

					Coordinate start = new Coordinate(from.getX(), from.getY());
					Coordinate end = new Coordinate(to.getX(), to.getY());

					GeometryFactory f = new GeometryFactory();
					LineString beeline = new LineSegment(start, end).toGeometry(f);

					String tripClass = intersectShape(beeline);
//					String tripClass = "disabled";

					trip.setTripClass(tripClass);

					writeExperiencedTrip(trip);
					bw.newLine();

				}
			}
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	// public boolean intersectShape(Map<String, Geometry> zoneMap, LineString
	// beeline) {
	//
	// for (Entry<String, Geometry> zoneGeom : zoneMap.entrySet()) {
	// if (zoneGeom.getValue().intersects(beeline)) {
	// return true;
	// }
	// }
	//
	// return false;
	//
	// }

	// public void writeExperiencedTripsIntersectZoneMap() {
	// try {
	// GeometryFactory f = new GeometryFactory();
	// bw.newLine();
	// for (List<ExperiencedTrip> tripList : agent2trips.values()) {
	// for (ExperiencedTrip trip : tripList) {
	// if (relevantAgents.contains(trip.getAgent())) {
	//
	// Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
	// Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();
	//
	// Coordinate start = new Coordinate(from.getX(), from.getY());
	// Coordinate end = new Coordinate(to.getX(), to.getY());
	//
	// LineString beeline = new LineSegment(start, end).toGeometry(f);
	//
	// if (intersectShape(zoneMap, beeline)) {
	// writeExperiencedTrip(trip);
	// bw.newLine();
	// }
	//
	// }
	// }
	// }
	// bw.close();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// throw new RuntimeException("could not write");
	// }
	// }

	public void writeExperiencedLegs() {
		try {
			// add header for leg
			bw.write(sep + "legNr" + sep + "legFromLinkId" + sep + "legToLinkId" + sep + "legStartTime" + sep
					+ "legEndTime" + sep + "legMode" + sep + "legWaitTime" + sep + "legGrossWaitTime" + sep
					+ "legInVehicleTime" + sep + "legDistance" + sep + "legTransitRouteId" + sep + "legPtFromStop" + sep
					+ "legPtToStop");
			bw.newLine();
			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				{
					for (ExperiencedTrip trip : tripList) {
						// Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
						// Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();

						// if
						// (vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(from,
						// zoneMap)
						// && vwExamples.utils.modalSplitAnalyzer.modalSplitEvaluator.isWithinZone(to,
						// zoneMap))
						{

							for (int i = 0; i < trip.getLegs().size(); i++) {
								ExperiencedLeg leg = trip.getLegs().get(i);
								writeExperiencedTrip(trip);
								writeExperiencedLeg(leg, i);
								bw.newLine();
							}
						}
					}

				}
			}
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	// public void writeExperiencedLegsIntersectZoneMap() {
	//
	// GeometryFactory f = new GeometryFactory();
	//
	// try {
	// // add header for leg
	// bw.write(sep + "legNr" + sep + "legFromLinkId" + sep + "legToLinkId" + sep +
	// "legStartTime" + sep
	// + "legEndTime" + sep + "legMode" + sep + "legWaitTime" + sep +
	// "legGrossWaitTime" + sep
	// + "legInVehicleTime" + sep + "legDistance" + sep + "legTransitRouteId" + sep
	// + "legPtFromStop" + sep
	// + "legPtToStop");
	// bw.newLine();
	// for (List<ExperiencedTrip> tripList : agent2trips.values()) {
	// {
	// for (ExperiencedTrip trip : tripList) {
	// if (relevantAgents.contains(trip.getAgent())) {
	//
	// Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
	// Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();
	//
	// Coordinate start = new Coordinate(from.getX(), from.getY());
	// Coordinate end = new Coordinate(to.getX(), to.getY());
	//
	// LineString beeline = new LineSegment(start, end).toGeometry(f);
	//
	// if (intersectShape(zoneMap, beeline)) {
	//
	// for (int i = 0; i < trip.getLegs().size(); i++) {
	// ExperiencedLeg leg = trip.getLegs().get(i);
	// writeExperiencedTrip(trip);
	// writeExperiencedLeg(leg, i);
	// bw.newLine();
	// }
	// }
	// }
	// }
	//
	// }
	// }
	// bw.close();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// throw new RuntimeException("could not write");
	// }
	// }

	private void writeExperiencedTrip(ExperiencedTrip trip) {
		try {
			Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
			Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();
			bw.write(trip.getTripClass() + sep + trip.getId() + sep + trip.getAgent() + sep + trip.getTripNumber() + sep
					+ trip.getActivityBefore() + sep + trip.getActivityAfter() + sep + trip.getFromLinkId() + sep
					+ from.getX() + sep + from.getY() + sep + trip.getToLinkId() + sep + to.getX() + sep + to.getY()
					+ sep + convertSecondsToTimeString(trip.getStartTime()) + sep
					+ convertSecondsToTimeString(trip.getEndTime()) + sep + trip.getTotalTravelTime() + sep
					+ trip.getLegs().size());
			if (trip.getTransitStopsVisited().size() < 1) {
				bw.write(sep + "no pt");
			} else {
				Iterator<Id<TransitStopFacility>> stopIterator = trip.getTransitStopsVisited().iterator();
				bw.write(sep + stopIterator.next());
				while (stopIterator.hasNext()) {
					bw.write(sep2 + stopIterator.next());
				}
			}
			for (String mode : monitoredModes) {
				try {
					bw.write(sep + trip.getMode2inVehicleOrMoveTime().get(mode) + sep
							+ trip.getMode2inVehicleOrMoveDistance().get(mode) + sep + trip.getMode2waitTime().get(mode)
							+ sep + trip.getMode2maxPerLegWaitTime().get(mode) + sep
							+ trip.getMode2numberOfLegs().get(mode));
				} catch (NullPointerException e) {
					e.printStackTrace();
					throw new RuntimeException(
							"monitored mode " + mode + " not found in ExperiencedTrip " + trip.getId());
				}
			}
			bw.write(sep + trip.getMode2inVehicleOrMoveTime().get("Other") + sep
					+ trip.getMode2inVehicleOrMoveDistance().get("Other") + sep + trip.getMode2waitTime().get("Other")
					+ sep + trip.getMode2maxPerLegWaitTime().get("Other") + sep
					+ trip.getMode2numberOfLegs().get("Other"));
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	private void writeExperiencedLeg(ExperiencedLeg leg, int legNr) {
		try {
			bw.write(sep + Integer.toString(legNr + 1) + sep + leg.getFromLinkId() + sep + leg.getToLinkId() + sep
					+ convertSecondsToTimeString(leg.getStartTime()) + sep
					+ convertSecondsToTimeString(leg.getEndTime()) + sep + leg.getMode() + sep + leg.getWaitTime() + sep
					+ leg.getGrossWaitTime() + sep + leg.getInVehicleTime() + sep + leg.getDistance() + sep
					+ String.valueOf(leg.getTransitRouteId()));
			if (leg.getMode().equals(TransportMode.pt)) {
				bw.write(sep + String.valueOf(leg.getPtFromStop()) + sep + String.valueOf(leg.getPtToStop()));
			} else {
				bw.write(sep + "no pt" + sep + "no pt");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	public static String convertSecondsToTimeString(double seconds) {
		return Time.writeTime(seconds);
	}
}
