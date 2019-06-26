/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.delays;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.tuple.Triple;
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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

public class TravelDelayCalculator implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private List<String> trips = new ArrayList<>();
	private Map<Id<Person>, ArrayList<MutableDouble>> travelTimes = new HashMap<>();
	private Map<Id<Person>, ArrayList<Coord>> FromTo = new HashMap<>();
	private Map<Id<Vehicle>, Double> linkEnterTimes = new HashMap<>();
	private Network network;
	private Map<String, Geometry> zoneMap;
	private List<Geometry> districtGeometryList = new ArrayList<Geometry>();
	private GeometryFactory geomfactory = JTSFactoryFinder.getGeometryFactory(null);
	private GeometryCollection geometryCollection = geomfactory.createGeometryCollection(null);
	private Geometry boundary;
	private Map<Id<Link>, MutableDouble> LinkFlowMap = new HashMap<>();
	private Map<Id<Link>, MutableDouble> LinkDelayMap = new HashMap<>();
	private Map<Id<Link>, List<Double>> CongestionIdxMap = new HashMap<>();


	public TravelDelayCalculator(Network network, Geometry boundary) {
		this.network = network;
		// this.relevantAgents = relevantAgents;
		this.boundary = boundary;
	}
	
	public Map<Id<Link>, MutableDouble> getLinkFlowMap() {
		return LinkFlowMap;
	}
	
	public Map<Id<Link>, MutableDouble> getLinkDelayMap() {
		return LinkDelayMap;
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

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (linkEnterTimes.containsKey(event.getVehicleId())) {
			double enterTime = linkEnterTimes.remove(event.getVehicleId());
			double travelTime = event.getTime() - enterTime;
			Link l = network.getLinks().get(event.getLinkId());
			double freeSpeedTravelTime = Math.min(l.getLength() / l.getFreespeed(), travelTime);
			// the last link
			Id<Person> pid = Id.createPersonId(event.getVehicleId());
			if (travelTimes.containsKey(pid)) {
				ArrayList<MutableDouble> t = travelTimes.get(pid);
				t.get(0).add(freeSpeedTravelTime);
				t.get(1).add(travelTime);
				t.get(2).add(l.getLength());
				
				double congestionIdx = travelTime / freeSpeedTravelTime;

				if (LinkFlowMap.containsKey(l.getId())) {
					LinkFlowMap.get(l.getId()).add(1);
					
					LinkDelayMap.get(l.getId()).add(travelTime-freeSpeedTravelTime);
					CongestionIdxMap.get(l.getId()).add(congestionIdx);
//					System.out.println(l.getId() + "||" +  LinkDelayMap.get(l.getId()).toString());

				}
				else {
					LinkFlowMap.put((l.getId()), new MutableDouble());
					LinkDelayMap.put((l.getId()), new MutableDouble());
					CongestionIdxMap.put(l.getId(), new ArrayList<Double>());
					
					LinkFlowMap.get(l.getId()).add(1);
					LinkDelayMap.get(l.getId()).add(travelTime-freeSpeedTravelTime);
					CongestionIdxMap.get(l.getId()).add(congestionIdx);
				}
			}

		}

	}
	
	

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEnterTimes.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		Coord fromCoord = null;
		Coord toCoord = null;
		LineString beeline = null;

		if (event.getLegMode().equals(TransportMode.car)) {

			if (this.FromTo.containsKey(event.getPersonId())) {
				fromCoord = this.FromTo.get(event.getPersonId()).get(0);
				toCoord = network.getLinks().get(event.getLinkId()).getCoord();
				FromTo.remove(event.getPersonId());

				Coordinate start = new Coordinate(fromCoord.getX(), fromCoord.getY());
				Coordinate end = new Coordinate(toCoord.getX(), toCoord.getY());

				GeometryFactory f = new GeometryFactory();
				beeline = new LineSegment(start, end).toGeometry(f);

			}

			if (travelTimes.containsKey(event.getPersonId())) {
				Id<Person> pid = event.getPersonId();
				Id<Vehicle> vid = Id.createVehicleId(pid);
				if (linkEnterTimes.containsKey(vid))
				// this will ignore all pt drivers, whose vehicleId != personId
				{
					double enterTime = linkEnterTimes.remove(vid);

					double travelTime = event.getTime() - enterTime;
					Link l = network.getLinks().get(event.getLinkId());
					double freeSpeedTravelTime = Math.min(l.getLength() / l.getFreespeed(), travelTime);

					if (travelTimes.containsKey(pid)) {
						ArrayList<MutableDouble> t = travelTimes.remove(pid);
						t.get(0).add(freeSpeedTravelTime);
						t.get(1).add(travelTime);
						t.get(2).add(l.getLength());

						String tripType = intersectShape(beeline);

						String result = event.getPersonId() + ";" + event.getTime() + ";" + t.get(0).intValue() + ";"
								+ t.get(1).intValue() + ";" + (t.get(1).intValue() - t.get(0).intValue() + ";"
										+ beeline.toString() + ";" + tripType + ";" + t.get(2));
						trips.add(result);

					}
				}
			}

		}

	}

	public List<String> getTrips() {
		return trips;
	}
	
	public double getMeanCongestionIdxPerLink(Id<Link> linkId)
	{
		List<Double> idxList = CongestionIdxMap.get(linkId);
		Double average = idxList.stream().mapToDouble(val -> val).average().orElse(0.0);
		return average;
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.travelTimes.put(event.getPersonId(), new ArrayList<MutableDouble>());
			// Add three empty values
			this.travelTimes.get(event.getPersonId()).add(new MutableDouble());
			this.travelTimes.get(event.getPersonId()).add(new MutableDouble());
			this.travelTimes.get(event.getPersonId()).add(new MutableDouble());
			Coord fromCoord = network.getLinks().get(event.getLinkId()).getCoord();
			this.FromTo.put(event.getPersonId(), new ArrayList<Coord>());
			this.FromTo.get(event.getPersonId()).add(fromCoord);
		}
		
		

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (travelTimes.containsKey(event.getPersonId())) {
			linkEnterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

}
