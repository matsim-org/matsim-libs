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
import java.util.Collections;
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
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import analysis.drtOccupancy.DynModeTripsAnalyser;

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
	// private List<Geometry> districtGeometryList;
	// private GeometryFactory geomfactory;
	// private GeometryCollection geometryCollection;
	private Map<String, List<ExperiencedTrip>> tourId2trips;
	private Map<String, List<ParkingEvent>> zoneId2ParkingEvents;
	private List<ParkingEvent> parkingEvents;
	// private Map<Id<Link>, String> linkToZoneMap;
	private Map<String, MutableInt> zoneToStreetMeters;
	private Map<String, Map<Double, Set<Id<Vehicle>>>> Zone2BinActiveVehicleMap;
	private Map<String, MutableDouble> Mode2MileageMap;
	private Set<ModalSplitSegment> ModalSplitSegments;
	private Map<String, String> zone2WKTGeom;
	int totalNumberTripToBeStored = 0;
	int writeCounter = 0;
	GeometryFactory f;

	Set<String> acceptedMainModes;

	public ExperiencedTripsWriter(String path, Map<Id<Person>, List<ExperiencedTrip>> agent2trips,
			Map<String, Map<Double, Set<Id<Vehicle>>>> Zone2BinActiveVehicleMap,
			Map<String, MutableDouble> Mode2MileageMap, Set<String> monitoredModes, Network network,
			Map<String, Geometry> zoneMap, Geometry boundary) {
		this.path = path;
		this.agent2trips = agent2trips;
		this.network = network;
		this.monitoredModes = monitoredModes;
		this.zoneMap = zoneMap;
		this.tourId2trips = new HashMap<String, List<ExperiencedTrip>>();
		this.parkingEvents = new ArrayList<ParkingEvent>();
		// this.linkToZoneMap = new HashMap<Id<Link>, String>();
		this.zoneToStreetMeters = new HashMap<String, MutableInt>();
		this.Zone2BinActiveVehicleMap = Zone2BinActiveVehicleMap;
		this.Mode2MileageMap = Mode2MileageMap;
		this.ModalSplitSegments = new HashSet<ModalSplitSegment>();
		this.f = new GeometryFactory();
		this.acceptedMainModes = new HashSet<>(
				Arrays.asList("car", "pt", "drt", "walk", "ride", "bike", "stayHome", "uam"));
		this.boundary = boundary;

		zone2WKTGeom = new HashMap<String, String>();

		WKTWriter wktwriter = new WKTWriter();

		for (String zoneId : zoneMap.keySet()) {

			String zoneWKT = wktwriter.write(zoneMap.get(zoneId));
			zone2WKTGeom.put(zoneId, zoneWKT);

		}

		zoneId2ParkingEvents = new HashMap<String, List<ParkingEvent>>();

		try {
			initialize();
			calucalteCarLinkLengthPerZone();
			calcualteTrafficPerformance();
			tourIdentifier();
			tourClassifier();
			getParkingTimes();
			String folder = new File(path).getParentFile().getName();
			writeTripLengthDist(new File(path).getParent() + "\\" + folder + ".tripLength");
			analyseCarParking(new File(path).getParent() + "\\" + folder + ".parking", 120);
			analyseActiveVehicles(new File(path).getParent() + "\\" + folder + ".activeVehicles");
			writeMileagePerMode(new File(path).getParent() + "\\" + folder + ".mileage");
			writeModalSplits(new File(path).getParent() + "\\" + folder + ".modalsplit");
			writeAgentsTrajectories(new File(path).getParent() + "\\" + folder + ".trajectories");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not initialize writer");
		}
	}

	double getRouteDistance(List<Id<Link>> routeList) {
		double distance = 0.0;
		if (routeList != null) {

			for (Id<Link> linkeId : routeList) {
				distance = distance + network.getLinks().get(linkeId).getLength();
			}
		}
		return distance;

	}

	private void calcualteTrafficPerformance() {
		System.out.println("Working on Traffic Performance Calculation");
		// for (Entry<Id<Person>, List<ExperiencedTrip>> agentTrips :
		// agent2trips.entrySet()) {
		agent2trips.entrySet().parallelStream().forEach(agentTrips -> {

			for (ExperiencedTrip trip : agentTrips.getValue()) {

				Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
				Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();

				LineString beeline = new LineSegment(new Coordinate(from.getX(), from.getY()),
						new Coordinate(to.getX(), to.getY())).toGeometry(f);

				String tripClass = intersectShape(beeline);
				// String tripClass = "disabled";

				trip.setTripClass(tripClass);
				trip.setBeeline(beeline);

				// String mode = trip.getMainMode();
				// double beelineTripDist = DistanceUtils.calculateDistance(
				// network.getLinks().get(trip.getFromLinkId()).getCoord(),
				// network.getLinks().get(trip.getToLinkId()).getCoord());
				// double totalDrivenTripDist = 0.0;

				// for (ExperiencedLeg leg : trip.getLegs()) {

				// if(agentTrips.getKey().toString().equals("na_22_37047201"))
				// {

				// System.out.print(trip.getAgent() + ";" + leg.getInShapeMileage() + ";" +
				// leg.getMode() + "\n");
				// }

				// List<Id<Link>> routeList = leg.getRouteListe();
				// leg.getInShapreMileage();
				//
				// totalDrivenTripDist = totalDrivenTripDist + getRouteDistance(routeList);

				// System.out.print(routeList);

				// }

				// In case of beeline modes there will be no in vehicle mileage
				// Extract part pf the beeline that is within the research boundary
				// Geometry linePart = boundary.intersection(beeline);
				// if (totalDrivenTripDist == 0.0) {
				// totalDrivenTripDist = linePart.getLength();
				// }

				// trip.setTotalInVehicleMilage(totalDrivenTripDist);

				// if (totalDrivenTripDist < beelineTripDist) {
				// System.out.println(trip.getAgent() + " Mode: " + mode + " Beeline: " +
				// beelineTripDist / 1000.0
				// + " || " + "Driven: " + totalDrivenTripDist / 1000.0);
				// }

			}

		});

	}

	private void initialize() throws IOException {
		bw = IOUtils.getBufferedWriter(path);
		// write header
		bw.write("mainMode" + sep + "tripClass" + sep + "beeline" + sep + "tripId" + sep + "agent" + sep + "tripNumber"
				+ sep + "activityBefore" + sep + "activityAfter" + sep + "fromLinkId" + sep + "fromX" + sep + "fromY"
				+ sep + "toLinkId" + sep + "toX" + sep + "toY" + sep + "startTime" + sep + "endTime" + sep
				+ "totalTravelTime" + sep + "numberOfLegs" + sep + "transitStopsVisited");
		for (String mode : monitoredModes) {
			bw.write(sep + mode + ".InVehicleTime");
			bw.write(sep + mode + ".Distance");
			bw.write(sep + mode + ".WaitTime");
			bw.write(sep + mode + ".maxPerLegWaitTime");
			bw.write(sep + mode + ".NumberOfLegs");
			bw.write(sep + mode + ".inShapeVehMileage");
		}
		bw.write(sep + "Other" + ".InVehicleTime");
		bw.write(sep + "Other" + ".Distance");
		bw.write(sep + "Other" + ".WaitTime");
		bw.write(sep + "Other" + ".maxPerLegWaitTime");
		bw.write(sep + "Other" + ".NumberOfLegs");

	}

	private void tourClassifier() {
		System.out.println("Working Tour Classification");

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
		ModalSplitSegment Class7 = new ModalSplitSegment("7", acceptedMainModes);

		ModalSplitSegments.add(Class1);
		ModalSplitSegments.add(Class2);
		ModalSplitSegments.add(Class3);
		ModalSplitSegments.add(Class4);
		ModalSplitSegments.add(Class5);
		ModalSplitSegments.add(Class6);
		ModalSplitSegments.add(Class7);

		// int seenTrips = 0;
		// 1 Tour contains N Trips
		for (Entry<String, List<ExperiencedTrip>> TourEntrySet : tourId2trips.entrySet()) {
			// String tourId = TourEntrySet.getKey();

			List<ExperiencedTrip> triplist = TourEntrySet.getValue();

			// Handle Class 1: All trips of all tours are stored
			{

				for (ExperiencedTrip trip : triplist) {
					String mainMode = trip.getMainMode();
					String storeMode = null;
					if (mainMode.contains("walk")) {
						storeMode = "walk";
					} else {
						storeMode = mainMode;

					}

					if (mainMode.equals("unknown")) {
						System.out.println("Mode Missing! Check Code!");
					}

					if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
						double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

						if (Class1.mode2TripDistance.containsKey(storeMode)) {
							Class1.mode2TripDistance.get(storeMode).add(distance);

						} else {
							Class1.mode2TripDistance.put(storeMode, new ArrayList<Double>());
							Class1.mode2TripDistance.get(storeMode).add(distance);

						}

					}
					// ToDo
					// Else mode is unknown and not analyzed
					// E.g. pure transit_walks are not counted anyway

					// seenTrips++;
				}
			}
			// System.out.println("Seen trips: "+seenTrips);

			// Class 2: All tours of city inhabitants (home activity is within city area) /
			// MID modal split
			{

				if (livesInside(triplist)) {
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						String storeMode = null;
						if (mainMode.contains("walk")) {
							storeMode = "walk";
						} else {
							storeMode = mainMode;

						}

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class2.mode2TripDistance.containsKey(storeMode)) {
								Class2.mode2TripDistance.get(storeMode).add(distance);

							} else {
								Class2.mode2TripDistance.put(storeMode, new ArrayList<Double>());
								Class2.mode2TripDistance.get(storeMode).add(distance);

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
						String storeMode = null;
						if (mainMode.contains("walk")) {
							storeMode = "walk";
						} else {
							storeMode = mainMode;

						}

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class3.mode2TripDistance.containsKey(storeMode)) {
								Class3.mode2TripDistance.get(storeMode).add(distance);

							} else {
								Class3.mode2TripDistance.put(storeMode, new ArrayList<Double>());
								Class3.mode2TripDistance.get(storeMode).add(distance);

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

					// if(trip.getAgent().toString().contains("freight"))
					// {
					// System.out.println(trip.getAgent());
					// }

					if (tripWithinCity(trip)) {
						String mainMode = trip.getMainMode();
						String storeMode = null;
						if (mainMode.contains("walk")) {
							storeMode = "walk";
						} else {
							storeMode = mainMode;

						}

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class4.mode2TripDistance.containsKey(storeMode)) {
								Class4.mode2TripDistance.get(storeMode).add(distance);

							} else {
								Class4.mode2TripDistance.put(storeMode, new ArrayList<Double>());
								Class4.mode2TripDistance.get(storeMode).add(distance);

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
						String storeMode = null;
						if (mainMode.contains("walk")) {
							storeMode = "walk";
						} else {
							storeMode = mainMode;

						}

						// System.out.println(trip.getSubTourNr() + "||" + mainMode);

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class5.mode2TripDistance.containsKey(storeMode)) {
								Class5.mode2TripDistance.get(storeMode).add(distance);

							} else {
								Class5.mode2TripDistance.put(storeMode, new ArrayList<Double>());
								Class5.mode2TripDistance.get(storeMode).add(distance);

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
						String storeMode = null;
						if (mainMode.contains("walk")) {
							storeMode = "walk";
						} else {
							storeMode = mainMode;

						}

						// System.out.println(trip.getSubTourNr() + "||" + mainMode);

						if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
							double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

							if (Class6.mode2TripDistance.containsKey(storeMode)) {
								Class6.mode2TripDistance.get(storeMode).add(distance);

							} else {
								Class6.mode2TripDistance.put(storeMode, new ArrayList<Double>());
								Class6.mode2TripDistance.get(storeMode).add(distance);

							}

						}
						// ToDo
						// Else mode is unknown and not analyzed
						// E.g. pure transit_walks are not counted anyway

					}

				}

			}

			// Class 7: tours of home office candidates -->
			// (all tours with work within city area, home outside) or
			// (all tours with work outside city area, lives inside) or
			// (all tours with home within city area and work within city area)
			{
				String chain = getActivityChain(triplist);
				// if (chain.equals("home-work-home")) {
				//
				// potentialHomieAgents.add(triplist.get(0).getAgent());
				// }
				//
				// relevantAgents.add(triplist.get(0).getAgent());
				//
				// if(relevantAgents.size()>0)
				// {
				// double r = (double) potentialHomieAgents.size( )/ (double)
				// relevantAgents.size();
				// System.out.println(r);
				// }

				if (isOutboundCommuterTour(triplist) || isInboundCommuterTour(triplist)
						|| isWithinCommuterTour(triplist)) {
					// relevantAgents.add(triplist.get(0).getAgent());

					// String chain = getActivityChain(triplist);

					// If chain is a simple home-work-home chain
					// if (chain.equals("home-work-home")) {
					if (true) {

						// potentialHomieAgents.add(triplist.get(0).getAgent());

						// Id<Person> person = triplist.get(0).getAgent();
						// System.out.println(person.toString());
						for (ExperiencedTrip trip : triplist) {
							String mainMode = trip.getMainMode();
							String storeMode = null;
							if (mainMode.contains("walk")) {
								storeMode = "walk";
							} else {
								storeMode = mainMode;

							}
							// System.out.println(trip.getSubTourNr() + "||" + mainMode);

							if (trip.getMode2inVehicleOrMoveDistance().containsKey(mainMode)) {
								double distance = trip.getMode2inVehicleOrMoveDistance().get(mainMode);

								if (Class7.mode2TripDistance.containsKey(storeMode)) {
									Class7.mode2TripDistance.get(storeMode).add(distance);

								} else {
									Class7.mode2TripDistance.put(storeMode, new ArrayList<Double>());
									Class7.mode2TripDistance.get(storeMode).add(distance);

								}

							}
							// ToDo
							// Else mode is unknown and not analyzed
							// E.g. pure transit_walks are not counted anyway

						}

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

	private String getActivityChain(List<ExperiencedTrip> triplist) {
		StringJoiner joiner = new StringJoiner("-");

		for (ExperiencedTrip trip : triplist) {
			String act = trip.getActivityBefore();

			if (act.contains("home")) {
				act = "home";
			} else if (act.contains("work")) {
				act = "work";
			} else if (act.contains("shopping")) {
				act = "shopping";
			} else if (act.contains("other")) {
				act = "other";
			} else if (act.contains("leisure")) {
				act = "leisure";
			}

			joiner.add(act);
		}
		// Finalize with last act

		joiner.add("home");

		return joiner.toString();
	}

	private boolean isInboundCommuterTour(List<ExperiencedTrip> triplist) {

		if (livesOutside(triplist) && worksInside(triplist)) {
			return true;
		}
		return false;

	}

	private boolean isOutboundCommuterTour(List<ExperiencedTrip> triplist) {

		if (worksOutside(triplist) && livesInside(triplist)) {
			return true;
		}
		return false;

	}

	private boolean isWithinCommuterTour(List<ExperiencedTrip> triplist) {

		if (worksInside(triplist) && livesInside(triplist)) {
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
		System.out.println("Working Tour Identification");
		String tourMatchKey = "home";
		HashSet<String> commercialTrafficActs = new HashSet<String>();
		commercialTrafficActs.add("start");
		commercialTrafficActs.add("end");

		// int checkedToursCounter = 0;

		int realNumberTripStored = 0;
		MutableInt subtourIdent = new MutableInt(0);

		for (Entry<Id<Person>, List<ExperiencedTrip>> tripsPerPersonEntry : agent2trips.entrySet()) {

			Id<Person> PersonId = tripsPerPersonEntry.getKey();

			List<ExperiencedTrip> tripList = tripsPerPersonEntry.getValue();

			boolean isAgentMoving = proveAgentMoves(tripList);
			boolean isAgentTimeConsistent = proveTimeConsitency(tripList);

			if (isAgentTimeConsistent && isAgentMoving) {

				totalNumberTripToBeStored = totalNumberTripToBeStored + tripList.size();

				List<ExperiencedTrip> tripsWithinTour = new ArrayList<ExperiencedTrip>();

				for (ExperiencedTrip trip : tripList) {

					String actAfter = trip.getActivityAfter();

					if (commercialTrafficActs.contains(actAfter)) {
						Logger.getLogger(ExperiencedTripsWriter.class).warn("REWRITE FREIGHT ACTS");
						actAfter = tourMatchKey;
					}

					// trip.setSubTourNr(subtourIdent);
					tripsWithinTour.add(trip);

					if (actAfter.contains(tourMatchKey)) {
						// subtourIdent++;
						// Create new List of ExperiencedTrip --> Tour
						// Save this tour
						// Clear the tripsWithinTour
						List<ExperiencedTrip> saveTripsWithinTour = new ArrayList<ExperiencedTrip>();
						saveTripsWithinTour.addAll(tripsWithinTour);

						// if(♣.containsKey(TourId))
						// {
						// System.out.println("BUG Double tourId!");
						// }
						subtourIdent.increment();
						String TourId = tripsPerPersonEntry.getKey().toString() + "_" + subtourIdent;
						tourId2trips.put(TourId, saveTripsWithinTour);
						realNumberTripStored = realNumberTripStored + saveTripsWithinTour.size();

						// checkedToursCounter++;
						tripsWithinTour.clear();

					}
					// System.out.println("Trip sorted into tour: "+realNumberTripStored +" || all
					// trips: "+totalNumberTripToBeStored );
				}

				// if (tripsWithinTour.size()>0)
				// {
				// System.out.println("Agent did not reach home!");
				//
				// }

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
		// Geometry geom = boundary;

		if (boundary.contains(to) && !boundary.contains(from)) {
			return "inbound";
		}

		else if (boundary.contains(from) && !boundary.contains(to)) {
			return "outbound";
		}

		else if (!(boundary.contains(from)) && !(boundary.contains(to)) && (beeline.intersects(boundary))) {
			return "through";
		}

		else if (!(boundary.contains(from)) && !(boundary.contains(to)) && !(beeline.intersects(boundary))) {
			return "outside";
		}

		else if (boundary.contains(from) && boundary.contains(to)) {
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

	// public void getResearchAreaBoundary() {
	// // This class infers the geometric boundary of all network link
	//
	// Logger.getLogger(ExperiencedTripsWriter.class).warn("MERGED GEOMETRIES TO ONE
	// LARGE ZONE BOUNDARY");
	// for (Geometry zoneGeom : this.zoneMap.values()) {
	// districtGeometryList.add(zoneGeom);
	// }
	//
	// geometryCollection = (GeometryCollection)
	// geomfactory.buildGeometry(districtGeometryList);
	// this.boundary = geometryCollection.union();
	//
	// }

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

	ExperiencedTrip getPrevCarTrip(List<ExperiencedTrip> triplist, int actualTripIdx) {

		// j counts the number of backward steps
		for (int j = 1; j < triplist.size(); j++) {

			// Is the index in the list which we will check now
			int backwardPointer = (actualTripIdx - j);
			if (backwardPointer < 0) {
				backwardPointer = backwardPointer + triplist.size();
			}

			// System.out.println("ActualIdx: " + actualTripIdx + "Backward Pointer: " +
			// backwardPointer);
			ExperiencedTrip candidate = triplist.get(backwardPointer);

			if (candidate.getMainMode().equals(TransportMode.car)) {
				return candidate;
			}

		}

		System.out.print("No prev car trip found: " + triplist.get(0).getAgent());
		return null;

	}

	// TODO
	public boolean proveTimeConsitency(List<ExperiencedTrip> tripList) {
		// Logger.getLogger(ExperiencedTripsWriter.class).warn("TIME CONSISTENCY CHECK
		// ACTIVATED!");
		double actualTripStartTime = 0;
		for (ExperiencedTrip trip : tripList) {
			if (trip.getStartTime() >= actualTripStartTime) {
				actualTripStartTime = trip.getStartTime();
			} else {
				Logger.getLogger(ExperiencedTripsWriter.class)
						.warn("Agent with time consitency bug: " + tripList.get(0).getAgent().toString());
				return false;
			}

		}

		return true;

	}

	public boolean proveAgentMoves(List<ExperiencedTrip> tripList) {
		// Logger.getLogger(ExperiencedTripsWriter.class).warn("TIME CONSISTENCY CHECK
		// ACTIVATED!");
		HashSet<Id<Link>> linkSet = new HashSet<Id<Link>>();
		for (ExperiencedTrip trip : tripList) {
			linkSet.add(trip.getFromLinkId());

		}

		if (linkSet.size() > 1) {
			return true;
		} else {
			Logger.getLogger(ExperiencedTripsWriter.class)
					.warn("Agent is not moving, not considered in analysis:" + tripList.get(0).getAgent());
			return false;
		}

	}

	public void getParkingTimes() {
		System.out.println("Working on Parking Data");

		for (Entry<String, List<ExperiencedTrip>> TourEntrySet : tourId2trips.entrySet()) {
			// for (Entry<Id<Person>, List<ExperiencedTrip>> TourEntrySet :
			// agent2trips.entrySet()) {
			// String tourId = TourEntrySet.getKey();
			List<ExperiencedTrip> triplist = TourEntrySet.getValue();

			int nrOfTrips = triplist.size();
			int i = 0;

			for (ExperiencedTrip trip : triplist) {
				String mainMode = trip.getMainMode();

				if (mainMode.equals(TransportMode.car)) {

					if (trip.getFromLinkId() != trip.getToLinkId()) {

						// int pointer = mod((i - 1), nrOfTrips);

						ExperiencedTrip prevCarTrip = getPrevCarTrip(triplist, i);

						double startOfParking = prevCarTrip.getEndTime();

						double endOfParking = trip.getStartTime(); // trip start time == act end time
						Coord coord = network.getLinks().get(trip.getFromLinkId()).getCoord();
						// double startOfParking = triplist.get(pointer).getEndTime(); // trip end time
						// == act start time

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
								parkingEvents.add(
										new ParkingEvent(startOfParking, 24 * 3600.0, personId, coord, parkingZone));
								parkingEvents.add(new ParkingEvent(0.0, endOfParking, personId, coord, parkingZone));

							} else {

								parkingEvents.add(
										new ParkingEvent(startOfParking, endOfParking, personId, coord, parkingZone));
							}

						} else if (activityDuration < 0) {
							Logger.getLogger(ExperiencedTripsWriter.class).warn(
									"Person " + personId + " Act Start < Act End:" + " duration " + activityDuration);
						}

					} else {
						Logger.getLogger(ExperiencedTripsWriter.class)
								.warn("Car trip with no distance, skipped for parking:" + trip.getAgent());
					}

					i++;
				}

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

	public void analyseCarParking(String fileName, int binsize_s) {

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
		// BufferedWriter bw_rel = IOUtils.getBufferedWriter(fileName + "_relational" +
		// ".csv");

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

		// System.out.println("Start writing relational parking statistics");
		// // Write content in relational file format
		// try {
		//
		// // Write header timebin,zone,parkingDemand
		// bw_rel.write("id;timebin;zone;parkingDemand;steet_m;density_km;geom");
		// bw_rel.newLine();
		//
		// // ToDo write header for zones
		//
		// int rowId = 0;
		// // Iterate over timebin
		// for (Entry<Double, List<ParkingEvent>> e : splitParkings.entrySet()) {
		//
		// for (String zoneId : zoneMap.keySet()) {
		//
		// long parkings = 0;
		//
		// if (!e.getValue().isEmpty()) {
		// DescriptiveStatistics stats = new DescriptiveStatistics();
		// for (ParkingEvent t : e.getValue()) {
		//
		// if (t.getParkingZone() != null) {
		// if (t.getParkingZone().equals(zoneId)) {
		// stats.addValue(1.0);
		// }
		// }
		//
		// }
		// parkings = stats.getN();
		//
		// double densityPerKm = parkings /
		// (zoneToStreetMeters.get(zoneId).doubleValue() / 1000.0);
		//
		// bw_rel.write(rowId + ";" + Time.writeTime(e.getKey()) + ";" + zoneId + ";" +
		// parkings + ";"
		// + zoneToStreetMeters.get(zoneId) + ";" + densityPerKm + ";" +
		// zone2WKTGeom.get(zoneId));
		// bw_rel.newLine();
		// rowId++;
		//
		// }
		// // Minute h = new Minute(sdf2.parse(Time.writeTime(e.getKey())));
		// //
		// // parkCount.addOrUpdate(h, parkings);
		// // Time;ValuePerZone
		// // bw.write(Time.writeTime(e.getKey()) + ";" + parkings);
		//
		// }
		//
		// }
		// bw_rel.flush();
		// bw_rel.close();
		// // datasetrequ.addSeries(parkCount);
		// // // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
		// // // times", "Wait time (s)");
		// // JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ,
		//
		// // "Parked Vehicles over Time",
		// // "Total Parked Cars [-]");
		// // // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
		// // ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);
		//
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }

	}

	public void writeTripLengthDist(String fileName) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		double[] binBorders = { 0, 0.5, 1, 2, 5, 10, 20, 50, 100 };
		// double[] binBorders = { 0,2, 4, 6, 8, 10, 20, 50, 100 };

		for (ModalSplitSegment segment : ModalSplitSegments)

		{

			BufferedWriter bw = IOUtils.getBufferedWriter(fileName + "_" + segment.SegmentClassNr + ".csv");

			try {
				// Write header
				ModalSplitSegment dummyModalSplitForHeader = ModalSplitSegments.iterator().next();
				String header_modes = dummyModalSplitForHeader.mode2TripDistance.keySet().stream().map(Object::toString)
						.collect(Collectors.joining(";"));
				bw.write("modalSplitType" + ";" + header_modes.toString());

				bw.newLine();

				int binIdx = 0;

				double binRightBorder = Double.NaN;

				// Loop over all range bins
				for (double binLeftBorder : binBorders) {

					int j = binIdx + 1;

					String row;

					if (j < (binBorders.length)) {
						binRightBorder = binBorders[j];
						row = "[" + binLeftBorder + "<" + binRightBorder + "]";
					} else {
						binRightBorder = Double.MAX_VALUE;
						row = "[" + binLeftBorder + "<" + "inf" + "]";
					}

					// Create an entry to specify the bin
					for (Entry<String, ArrayList<Double>> e : segment.mode2TripDistance.entrySet()) {

						ArrayList<Double> values = e.getValue();

						int binCount = 0;

						for (Double value : values) {
							if ((value / 1000.0) >= binLeftBorder && (value / 1000.0) < binRightBorder) {
								binCount++;
							}
						}

						row = row + ";" + binCount;

					}
					bw.write(row);
					bw.newLine();
					binIdx++;
				}

				bw.flush();
				bw.close();

			} catch (IOException e) {

				e.printStackTrace();
			}
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
			// Write header
			ModalSplitSegment dummyModalSplitForHeader = ModalSplitSegments.iterator().next();
			String header_modes = dummyModalSplitForHeader.mode2TripDistance.keySet().stream().map(Object::toString)
					.collect(Collectors.joining(";"));
			bw.write("modalSplitType" + ";" + header_modes.toString());

			bw.newLine();

			for (ModalSplitSegment segment : ModalSplitSegments) {

				String row = segment.SegmentClassNr;

				for (Entry<String, ArrayList<Double>> e : segment.mode2TripDistance.entrySet()) {

					row = row + ";" + e.getValue().size();

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

	public void writeAgentsTrajectories(String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename + ".csv");
		try {
			// add header for leg
			bw.write("agent" + sep + "fromAct" + sep + "toAct" + sep + "legMode" + sep + "linkId" + sep
					+ "linkEnterTime" + sep + "linkLeaveTime" + sep + "fromNodeX" + sep + "fromNodeY" + sep + "toNodeX"
					+ sep + "toNode");
			bw.newLine();
			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				{
					for (ExperiencedTrip trip : tripList) {

						{

							for (ExperiencedLeg leg : trip.getLegs()) {
								String row;

								if (leg.getRouteListe() != null) {
									for (Triple<Id<Link>, Double, Double> trajectoryPart : leg.getRouteListe()) {

										if (trajectoryPart != null) {
											Link link = network.getLinks().get(trajectoryPart.getLeft());
											double fromNodeX = link.getFromNode().getCoord().getX();
											double fromNodeY = link.getFromNode().getCoord().getY();
											double toNodeX = link.getToNode().getCoord().getX();
											double toNodeY = link.getToNode().getCoord().getY();

											row = trip.getAgent().toString() + sep + trip.getActivityBefore() + sep
													+ trip.getActivityAfter() + sep + leg.getMode() + sep
													+ trajectoryPart.getLeft() + sep + trajectoryPart.getMiddle() + sep
													+ trajectoryPart.getRight() + sep + fromNodeX + sep + fromNodeY
													+ sep + toNodeX + sep + toNodeY;
										} else {
											row = trip.getAgent().toString() + sep + trip.getActivityBefore() + sep
													+ trip.getActivityAfter() + sep + leg.getMode() + sep + sep + ""
													+ sep + "" + sep + "" + sep + "" + sep + "" + sep + "";

										}
										bw.write(row);
										bw.newLine();
									}
								}
								// Route list my be null, if leg is a teleport mode, such non-network-work etc.
								// or if route is outside of the research area
								row = trip.getAgent().toString() + sep + trip.getActivityBefore() + sep
										+ trip.getActivityAfter() + sep + leg.getMode() + sep + sep + "" + sep + ""
										+ sep + "" + sep + "" + sep + "" + sep + "";
								bw.write(row);
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

			Set<Double> writtenPcts = new HashSet<Double>();
			int totalNumberOfTrips = totalNumberTripToBeStored;

			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				for (ExperiencedTrip trip : tripList) {

					double pct = Math.round(((double) writeCounter / (double) totalNumberOfTrips) * 100.0);

					if ((pct % 5) == 0 && !writtenPcts.contains(pct)) {
						System.out.println("Written trips: " + pct + " %");
						writtenPcts.add(pct);
						// System.out.println(writeCounter);
					}

					writeExperiencedTrip(trip);
					bw.newLine();
					writeCounter++;

				}
			}
			bw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	public void writeExperiencedLegs() {
		try {
			// add header for leg
			// bw.write(sep + "legNr" + sep + "legFromLinkId" + sep + "legToLinkId" + sep +
			// "legStartTime" + sep
			// + "legEndTime" + sep + "legMode" + sep + "legWaitTime" + sep +
			// "legGrossWaitTime" + sep
			// + "legInVehicleTime" + sep + "legDistance" + sep + "legTransitRouteId" + sep
			// + "legPtFromStop" + sep
			// + "legPtToStop" + sep + "legTraj");
			bw.write(sep + "legNr" + sep + "legFromLinkId" + sep + "legToLinkId" + sep + "legStartTime" + sep
					+ "legEndTime" + sep + "legMode" + sep + "legWaitTime" + sep + "legGrossWaitTime" + sep
					+ "legInVehicleTime" + sep + "legDistance" + sep + "legTransitRouteId" + sep + "legPtFromStop" + sep
					+ "legPtToStop");
			bw.newLine();
			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				{
					for (ExperiencedTrip trip : tripList) {

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

	private void writeExperiencedTrip(ExperiencedTrip trip) {
		try {
			Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
			Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();
			bw.write(trip.getMainMode() + sep + trip.getTripClass() + sep + trip.getBeeline() + sep + trip.getId() + sep
					+ trip.getAgent() + sep + trip.getTripNumber() + sep + trip.getActivityBefore() + sep
					+ trip.getActivityAfter() + sep + trip.getFromLinkId() + sep + from.getX() + sep + from.getY() + sep
					+ trip.getToLinkId() + sep + to.getX() + sep + to.getY() + sep
					+ convertSecondsToTimeString(trip.getStartTime()) + sep
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
							+ trip.getMode2numberOfLegs().get(mode) + sep
							+ trip.getMode2inShapeVehicleMilage().get(mode));
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
			// Add trajectoryInformation
			// if (leg.getRouteListe() != null) {
			// bw.write(sep + leg.getRouteListe().toString());
			// }
			// bw.write(sep + "[]");

		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	public static String convertSecondsToTimeString(double seconds) {
		return Time.writeTime(seconds);
	}
}
