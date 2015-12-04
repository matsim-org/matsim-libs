/* *********************************************************************** *
 * project: org.matsim.*
 * TripReconstructor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.joinabletripsidentifier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EventListenner which reconstructs the trips from the events.
 * @author thibautd
 */
public class TripReconstructor implements 
		LinkLeaveEventHandler,
		LinkEnterEventHandler,
		PersonDepartureEventHandler,
		PersonArrivalEventHandler,
		ActivityEndEventHandler,
		ActivityStartEventHandler {
	private static final Logger log =
		Logger.getLogger(TripReconstructor.class);

	private final Network network;

	private final Map<Id, TripData> agentsData = new HashMap<Id, TripData>();

	private final QuadTree<LinkInformation> linkInformationsQuadTree;
	private final Map<Id, LinkInformation> linkInformationsMap =
		new HashMap<Id, LinkInformation>();
	private final List<Trip> trips = new ArrayList<Trip>();

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public TripReconstructor(
			final Network network) {
		this.network = network;

		double maxX = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;

		Coord fromNode, toNode;
		
		//construct quadTree
		log.info("   constructing link information QuadTree...");
		for (Link link : network.getLinks().values()) {
			fromNode = link.getFromNode().getCoord();
			toNode = link.getToNode().getCoord();

			maxX = Math.max(fromNode.getX(), maxX);
			minX = Math.min(fromNode.getX(), minX);
			maxX = Math.max(toNode.getX(), maxX);
			minX = Math.min(toNode.getX(), minX);

			maxY = Math.max(fromNode.getY(), maxY);
			minY = Math.min(fromNode.getY(), minY);
			maxY = Math.max(toNode.getY(), maxY);
			minY = Math.min(toNode.getY(), minY);
		}

		this.linkInformationsQuadTree = new QuadTree<LinkInformation>(minX, minY, maxX, maxY);
		log.info("   constructing link information QuadTree... DONE");
		log.info("   minX: "+minX+", minY: "+minY+", maxX: "+maxX+", maxY: "+maxY);

		//fill the quadTree
		log.info("   filling QuadTree...");
		LinkInformation info;
		for (Link link : network.getLinks().values()) {
			//fromNode = link.getFromNode().getCoord();
			//toNode = link.getToNode().getCoord();

			// add link in the quadtree at th location of both nodes
			info = new LinkInformation(link.getId(), link.getCoord());
			//this.linkInformationsQuadTree.put(fromNode.getX(), fromNode.getY(), info);
			//this.linkInformationsQuadTree.put(toNode.getX(), toNode.getY(), info);
			this.linkInformationsQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), info);

			// put the info in a map for easy access
			this.linkInformationsMap.put(link.getId(), info);
		}
		log.info("   filling QuadTree... DONE");
	}

	// /////////////////////////////////////////////////////////////////////////
	// getter for network: for use in trip identification
	// /////////////////////////////////////////////////////////////////////////
	public Network getNetwork() {
		return network;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters on the collected data
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @return an (unmutable) list of the trips observed in the events
	 */
	public List<Trip> getTrips() {
		return Collections.unmodifiableList(trips);
	}

	public QuadTree<LinkInformation> getLinkInformationQuadTree() {
		return this.linkInformationsQuadTree;
	}

	// /////////////////////////////////////////////////////////////////////////
	// event handling methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(final int iteration) {
		agentsData.clear();
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		if (isPtEvent(event.getPersonId())) return;
		this.agentsData.get( event.getPersonId() ).handleEvent( event );
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		if (isPtEvent(event.getPersonId())) return;
		this.agentsData.get( event.getPersonId() ).handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (isPtEvent(event.getDriverId())) return;
		//TODO: check if the entry exists
		this.agentsData.get( event.getDriverId() ).handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (isPtEvent(event.getDriverId())) return;
		//TODO: check if the entry exists
		this.agentsData.get( event.getDriverId() ).handleEvent(event);
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		TripData data = this.agentsData.remove( event.getPersonId() );
		data.handleEvent(event);
		Trip trip = data.toTrip();
		trips.add(trip);

		// place the trip geographically
		this.linkInformationsMap.get(event.getLinkId()).handleArrival(
				trip.getId(),
				trip.getDeparture().getTime(),
				trip.getArrival().getTime());
		this.linkInformationsMap.get(trip.getDeparture().getLinkId()).handleDeparture(
				trip.getId(),
				trip.getDeparture().getTime(),
				trip.getArrival().getTime());
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		//TODO: test wether a value was at this key
		this.agentsData.put(
				event.getPersonId(),
				new TripData(event) );
	}

	private boolean isPtEvent(final Id id) {
		return id.toString().matches("pt_.*");
	}
}

// /////////////////////////////////////////////////////////////////////////
// helper classes
// /////////////////////////////////////////////////////////////////////////
class TripData {
	private static final Map<Id, Integer> agentTripsCount = new HashMap<Id, Integer>();

	private final Id agentId;
	private final int tripCount;
	private final ActivityEndEvent origin;
	private PersonDepartureEvent departure = null;
	private PersonArrivalEvent arrival = null;
	private ActivityStartEvent destination = null;
	private final List<Event> routeEvents = new ArrayList<Event>();

	// //////////////////////////////////////////////////////////////////////
	// constructor
	// //////////////////////////////////////////////////////////////////////
	public TripData(final ActivityEndEvent origin) {
		this.origin = origin;
		this.agentId = origin.getPersonId();
		Integer count = agentTripsCount.get(agentId);
		
		if ( count == null ) {
			count = 0;
		}

		count++;
		this.tripCount = count;
		agentTripsCount.put(agentId, count);
	}

	// ////////////////////////////////////////////////////////////////////////
	// public methods
	// ////////////////////////////////////////////////////////////////////////
	public void handleEvent(final PersonDepartureEvent event) {
		this.departure = event;
	}

	public void handleEvent(final LinkEnterEvent event) {
		this.routeEvents.add(event);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		this.routeEvents.add(event);
	}
	
	public void handleEvent(final PersonArrivalEvent event) {
		this.arrival = event;
	}

	public void handleEvent(final ActivityStartEvent event) {
		this.destination = event;
	}

	public Trip toTrip() {
		if ( departure == null ||
				arrival == null ||
				destination == null ) {
			return null;
		}

		return new Trip(
				tripCount,
				origin,
				departure,
				arrival,
				destination,
				routeEvents);
	}
}

