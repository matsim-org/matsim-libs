/* *********************************************************************** *
 * project: org.matsim.*
 * TripUtilOffsetExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.counts.Counts;

import playground.yu.utils.ArrayTools;
import playground.yu.utils.math.SimpleStatistics;
import cadyts.utilities.misc.DynamicData;

/**
 * @author yu
 * 
 */
public class TripUtilOffsetExtractor implements ActivityEndEventHandler,
// AgentDepartureEventHandler,
		LinkEnterEventHandler, ActivityStartEventHandler {
	public static class TripEvents {
		private Id agentId;
		private ActivityEndEvent tripBegins = null;
		// private AgentDepartureEvent tripBegins = null;
		private List<LinkEnterEvent> enters = null;
		private ActivityStartEvent tripEnds = null;

		public TripEvents(Id agentId) {
			this.agentId = agentId;
		}

		// public AgentDepartureEvent getTripBegins() {
		// return tripBegins;
		// }
		public ActivityEndEvent getTripBegins() {
			return tripBegins;
		}

		public void setTripBegins(ActivityEndEvent depart) {
			tripBegins = depart;
		}

		public List<LinkEnterEvent> getEnters() {
			return enters;
		}

		public void addLinkEnterEvent(LinkEnterEvent linkEnterEvent) {
			if (tripBegins == null) {
				throw new RuntimeException(
						"There should have been a tripBegins in this TripEvents!!");
			}
			if (enters == null) {
				enters = new ArrayList<LinkEnterEvent>();
			}
			enters.add(linkEnterEvent);
		}

		public ActivityStartEvent getTripEnds() {
			return tripEnds;
		}

		public void setTripEnds(ActivityStartEvent actStart) {
			if (tripBegins == null) {
				throw new RuntimeException(
						"There should have been a tripBegins in this TripEvents!!");
			}
			tripEnds = actStart;
		}

	}

	/**
	 * GripTrip exists only for one timeBin
	 * 
	 * @author yu
	 * 
	 */
	public static class TripsWithUtilOffset {
		private final Tuple<Coord, Coord> odRelationship/* orig,dest [2] */;
		private final List<Double> utilOffsets;
		private int[] timeRange/* timeStepRange [2] */;

		public TripsWithUtilOffset(Tuple<Coord, Coord> odRelationship) {
			this.odRelationship = odRelationship;
			utilOffsets = new ArrayList<Double>();
		}

		public Coord getOrig() {
			return odRelationship.getFirst();
		}

		public Coord getDest() {
			return odRelationship.getSecond();
		}

		public double getAverageUtilOffset() {
			return SimpleStatistics.average(utilOffsets);
		}

		double getUtilOffsetsVariance() {
			return SimpleStatistics.variance(utilOffsets);
		}

		public double getStandardDeviation() {
			return Math.sqrt(getUtilOffsetsVariance());
		}

		public int getVolume() {
			return utilOffsets.size();
		}

		public void addTrip(double utilOffset) {
			utilOffsets.add(utilOffset);
		}

		public String getId() {
			return getOrig() + "->" + getDest();
		}

		public int[] getTimeRange() {
			return timeRange;
		}

		public void setTimeRange(int[] timeRange/* length=2 */) {
			this.timeRange = timeRange;
		}

	}

	private Map<Id/* agentId */, TripEvents> tripEvents = new HashMap<Id, TripEvents>();
	protected Map<Tuple<Coord, Coord>, TripsWithUtilOffset> tripsWithUtilOffsetMap = new HashMap<Tuple<Coord, Coord>, TripsWithUtilOffset>();

	private Counts counts;
	private Network net;
	private DynamicData<Link> linkUtilOffsets;
	private double gridLength;
	protected boolean involveZeroOffset = false;

	protected int caliStartTime;
	protected int caliEndTime;

	public TripUtilOffsetExtractor(Counts counts, Network net,
			DynamicData<Link> linkUtilOffsets, double gridLength,
			int calibrationStartTime, int calibrationEndTime) {
		super();
		this.counts = counts;
		this.net = net;
		this.linkUtilOffsets = linkUtilOffsets;
		this.gridLength = gridLength;
		caliStartTime = calibrationStartTime;
		caliEndTime = calibrationEndTime;
	}

	// public void handleEvent(AgentDepartureEvent event) {
	// Id agentId = event.getPersonId();
	// if (this.tripEvents.containsKey(agentId)) {
	// throw new RuntimeException(
	// "There should NOT be TripEvents with personId:\t" + agentId);
	// }
	// TripEvents tripEvents = new TripEvents(agentId);
	// this.tripEvents.put(agentId, tripEvents);
	// tripEvents.setTripBegins(event);
	// }
	public void handleEvent(ActivityEndEvent event) {
		Id agentId = event.getPersonId();
		if (tripEvents.containsKey(agentId)) {
			throw new RuntimeException(
					"There should NOT be TripEvents with personId:\t" + agentId);
		}
		TripEvents tripEvents = new TripEvents(agentId);
		this.tripEvents.put(agentId, tripEvents);
		tripEvents.setTripBegins(event);
	}

	public void reset(int iteration) {

	}

	public void handleEvent(LinkEnterEvent event) {
		Id agentId = event.getPersonId();
		TripEvents tripEvents = this.tripEvents.get(agentId);
		if (tripEvents == null) {
			throw new RuntimeException(
					"There should HAVE been TripEvents with personId\t"
							+ agentId);
		}
		tripEvents.addLinkEnterEvent(event);
	}

	public void handleEvent(ActivityStartEvent event) {
		Id agentId = event.getPersonId();
		TripEvents tripEvents = this.tripEvents.get(agentId);
		if (tripEvents == null) {
			throw new RuntimeException(
					"There should HAVE been TripEvents with personId\t"
							+ agentId);
		}
		tripEvents.setTripEnds(event);

		convert2Trip(tripEvents);

		this.tripEvents.remove(agentId);
	}

	protected double getLinkUtilOffset(Id linkId, int time) {
		if (counts.getCounts().containsKey(linkId)) {
			if (isInRange(linkId, net)) {
				return linkUtilOffsets.getSum(net.getLinks().get(linkId),
						(time - 1) * 3600, time * 3600 - 1);
			}
		}
		return 0d;
	}

	public Map<Tuple<Coord, Coord>, TripsWithUtilOffset> getTripsWithUtilOffsetMap() {
		return tripsWithUtilOffsetMap;
	}

	public Map<Tuple<Coord, Coord>, TripsWithUtilOffset> getChildTripsWithUtilOffsetMap(
			int timeStep/* timeStepArray length=2, start,end */) {
		Map<Tuple<Coord, Coord>, TripsWithUtilOffset> childMap = new HashMap<Tuple<Coord, Coord>, TripsWithUtilOffset>();
		for (Entry<Tuple<Coord, Coord>, TripsWithUtilOffset> coord_trips : tripsWithUtilOffsetMap
				.entrySet()) {
			Tuple<Coord, Coord> coords = coord_trips.getKey();
			TripsWithUtilOffset trips = coord_trips.getValue();

			int[] timeRange = trips.getTimeRange();
			if (ArrayTools.inRange(timeRange, timeStep)) {
				childMap.put(coords, trips);
			}
		}
		return childMap;

	}

	private static boolean isInRange(final Id linkId, final Network net) {
		Coord distanceFilterCenterNodeCoord = net.getNodes().get(
				new IdImpl("2531")).getCoord();
		double distanceFilter = 30000;
		Link l = net.getLinks().get(linkId);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkId.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	protected void convert2Trip(TripEvents tripEvents) {
		List<LinkEnterEvent> enters = tripEvents.getEnters();
		if (enters != null) {

			ActivityEndEvent tripBegins = tripEvents.getTripBegins();
			Coord orig = getGridCenterCoord(tripBegins.getLinkId());
			ActivityStartEvent tripEnds = tripEvents.getTripEnds();
			Coord dest = getGridCenterCoord(tripEnds.getLinkId());
			Tuple<Coord, Coord> odRelationship = new Tuple<Coord, Coord>(orig,
					dest);
			TripsWithUtilOffset trips = tripsWithUtilOffsetMap
					.get(odRelationship);
			if (trips == null) {
				trips = new TripsWithUtilOffset(odRelationship);
			}
			double utilOffset = 0;

			for (LinkEnterEvent enter : enters) {
				Id linkId = enter.getLinkId();
				int timeStep = getTimeStep(enter.getTime());
				if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
					utilOffset += getLinkUtilOffset(linkId, timeStep);
				}
			}

			if (utilOffset != 0d || involveZeroOffset) {
				trips.addTrip(utilOffset);
				trips.setTimeRange(new int[] {
						getTimeStep(tripBegins.getTime()),
						getTimeStep(tripEnds.getTime()) });
				tripsWithUtilOffsetMap.put(odRelationship, trips);
			}
		}
	}

	protected Coord getGridCenterCoord(Coord coord) {
		Coord center = new CoordImpl((int) coord.getX() / (int) gridLength
				* gridLength + gridLength / 2d, (int) coord.getY()
				/ (int) gridLength * gridLength + gridLength / 2d);
		return center;
	}

	protected Coord getGridCenterCoord(Id linkId) {
		return this.getGridCenterCoord(net.getLinks().get(linkId).getCoord());
	}

	protected static int getTimeStep(double time) {
		return (int) time / 3600 + 1;
	}

	public void setInvolveZeroOffset(boolean involveZeroOffset) {
		this.involveZeroOffset = involveZeroOffset;
	}
}
