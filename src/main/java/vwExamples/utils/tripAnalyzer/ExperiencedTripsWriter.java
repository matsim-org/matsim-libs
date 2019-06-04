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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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
	private Set<Id<Person>> relevantAgents;
	private Map<String, Geometry> zoneMap;
	private Geometry boundary;
	private List<Geometry> districtGeometryList;
	private GeometryFactory geomfactory;
	private GeometryCollection geometryCollection;
	private Map<String, List<ExperiencedTrip>> tourId2trips;

	public ExperiencedTripsWriter(String path, Map<Id<Person>, List<ExperiencedTrip>> agent2trips,
			Set<String> monitoredModes, Network network, Set<Id<Person>> relevantAgents,
			Map<String, Geometry> zoneMap) {
		this.path = path;
		this.agent2trips = agent2trips;
		this.network = network;
		this.monitoredModes = monitoredModes;
		this.relevantAgents = relevantAgents;
		this.zoneMap = zoneMap;
		this.tourId2trips = new HashMap<String, List<ExperiencedTrip>>();

		districtGeometryList = new ArrayList<Geometry>();
		geomfactory = JTSFactoryFinder.getGeometryFactory(null);
		geometryCollection = geomfactory.createGeometryCollection(null);

		try {
			initialize();
			getResearchAreaBoundary();
			tourIdentifier();
			tourClassifier();
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
		// Class 5: in-bound commuter tours (all tours with work within city area, home outside)
		// Class 6: out-bound commuter tours (all tours with works outside city area, home inside)
		// Class 7: tours of home office candidates -->
		// (all tours with work within city area, home outside) or
		// (all tours with work outside city area, lives inside) or
		// (all tours with home within city area and work within city area)
		// Class 8: tours with no activities in city area

		ModalSplitSegment Class1 = new ModalSplitSegment(1, acceptedMainModes);
		ModalSplitSegment Class2 = new ModalSplitSegment(2, acceptedMainModes);
		ModalSplitSegment Class3 = new ModalSplitSegment(3, acceptedMainModes);
		ModalSplitSegment Class4 = new ModalSplitSegment(4, acceptedMainModes);
		ModalSplitSegment Class5 = new ModalSplitSegment(5, acceptedMainModes);
		ModalSplitSegment Class6 = new ModalSplitSegment(6, acceptedMainModes);

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
			
			// Class 5: in-bound commuter tours (all tours with work within city area, home outside)
			{
				if (isInboundCommuterTour(triplist)) {
					//Id<Person> person = triplist.get(0).getAgent();
					//System.out.println(person.toString());
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						//System.out.println(trip.getSubTourNr() + "||" +  mainMode);
						
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
					//Id<Person> person = triplist.get(0).getAgent();
					//System.out.println(person.toString());
					for (ExperiencedTrip trip : triplist) {
						String mainMode = trip.getMainMode();
						//System.out.println(trip.getSubTourNr() + "||" +  mainMode);
						
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

		System.out.println("Class 1");
		for (String mode : acceptedMainModes) {
			int tripCount = Class1.getNumberOfTripsPerMode(mode);
			System.out.println("mode: " + mode + " || " + tripCount);

		}
		

	}


	 private boolean isOutboundCommuterTour(List<ExperiencedTrip> triplist) {
			
	 if (worksOutside(triplist) && livesInside(triplist))
	 {
		 return true;
	 }
	 return false;
	
	 }
	
	 private boolean isInboundCommuterTour(List<ExperiencedTrip> triplist) {
	
	 if (livesOutside(triplist) && worksInside(triplist))
	 {
		 return true;
	 }
	 return false;
	
	 }

//	private boolean isWithinCommuterTour(List<ExperiencedTrip> triplist) {
//
//		if (worksInside(triplist) && livesInside(triplist)) {
//			return true;
//
//		}
//		return false;
//
//	}

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
//		int checkedToursCounter = 0;

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
					//Create new List of ExperiencedTrip --> Tour
					//Save this tour
					//Clear the tripsWithinTour
					List<ExperiencedTrip> saveTripsWithinTour = new ArrayList<ExperiencedTrip>();
					saveTripsWithinTour.addAll(tripsWithinTour);
					tourId2trips.put(TourId, saveTripsWithinTour);
					subtourNr++;
//					checkedToursCounter++;
					tripsWithinTour.clear();
					
				}

			}

		}

//		System.out.print("Seen Tours #" + checkedToursCounter + "\n");

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

	public void writeExperiencedTrips() {
		try {
			bw.newLine();
			for (List<ExperiencedTrip> tripList : agent2trips.values()) {
				for (ExperiencedTrip trip : tripList) {
					if (relevantAgents.contains(trip.getAgent())) {

						Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
						Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();

						Coordinate start = new Coordinate(from.getX(), from.getY());
						Coordinate end = new Coordinate(to.getX(), to.getY());

						GeometryFactory f = new GeometryFactory();
						LineString beeline = new LineSegment(start, end).toGeometry(f);

						String tripClass = intersectShape(beeline);
						trip.setTripClass(tripClass);

						writeExperiencedTrip(trip);
						bw.newLine();

					}
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
						if (relevantAgents.contains(trip.getAgent())) {

							Coord from = network.getLinks().get(trip.getFromLinkId()).getCoord();
							Coord to = network.getLinks().get(trip.getToLinkId()).getCoord();

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
