/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * A RouteStop used in the pseudoGraph.
 * <p/>
 * Link Candidates are made for each stop facility. Since one
 * stop facility might be accessed twice in the same transitRoute,
 * unique Link Candidates for each TransitRouteStop are needed. This
 * is achieved via this class.
 *
 * @author polettif
 */
public class PseudoRouteStop implements Identifiable<PseudoRouteStop>, Comparable<PseudoRouteStop> {

	private static PublicTransitMappingConfigGroup config;

	// dijkstra
	public final Map<PseudoRouteStop, Double> neighbours = new HashMap<>();
	public double distToSource = Double.MAX_VALUE; // MAX_VALUE assumed to be infinity$
	public PseudoRouteStop previous = null;

	private final double linkWeight;

	// schedule values
	public final Id<PseudoRouteStop> id;
	private final String name;

	private final String linkId;

	private final double departureOffset;
	private final double arrivalOffset;
	private final boolean awaitDepartureTime;

	private final Coord coord;
	private final boolean isBlockingLane;
	private final String facilityName;
	private final String stopPostAreaId;
	private final String parentStopFacilityId;
	private final String linkCandidateId;
	private final double stopFacilityDistance;

	public static void setConfig(PublicTransitMappingConfigGroup configGroup) {
		config = configGroup;
	}

	/**
	 * Constructor. All values are stored here as well to make access easier during
	 * stop facility replacement.
	 *
	 * @param order
	 * @param routeStop
	 * @param linkCandidate
	 */
	public PseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		this.id = Id.create("[" + Integer.toString(order) + "]" + linkCandidate.getId(), PseudoRouteStop.class);
		this.linkCandidateId = linkCandidate.getId();
		this.name = routeStop.getStopFacility().getName() + " (" + linkCandidate.getLinkIdStr() + ")";
		this.linkId = linkCandidate.getLinkIdStr();
		this.stopFacilityDistance = linkCandidate.getStopFacilityDistance();

		// stop facility values
		this.coord = routeStop.getStopFacility().getCoord();
		this.parentStopFacilityId = routeStop.getStopFacility().getId().toString();
		this.isBlockingLane = routeStop.getStopFacility().getIsBlockingLane();
		this.facilityName = routeStop.getStopFacility().getName();
		this.stopPostAreaId = routeStop.getStopFacility().getStopPostAreaId();

		// route stop values
		this.departureOffset = routeStop.getDepartureOffset();
		this.arrivalOffset = routeStop.getArrivalOffset();
		this.awaitDepartureTime = routeStop.isAwaitDepartureTime();

		// link value
		this.linkWeight = (config.getPseudoRouteWeightType().equals(PublicTransitMappingConfigGroup.PseudoRouteWeightType.travelTime) ? linkCandidate.getLinkTravelTime() : linkCandidate.getLinkLength());
//		this.linkWeight = linkCandidate.getLinkLength();
	}

	/**
	 * This constructor is only used to set dummy stops
	 *
	 * @param id
	 */
	public PseudoRouteStop(String id) {
		if(id.equals(PseudoGraph.SOURCE)) {
			this.id = Id.create(PseudoGraph.SOURCE, PseudoRouteStop.class);
			this.distToSource = 0;
		} else {
			this.id = Id.create(PseudoGraph.DESTINATION, PseudoRouteStop.class);
		}
		this.name = id;
		this.linkCandidateId = null;

		 // MAX_VALUE assumed to be infinity$
		previous = null;

		this.linkId = null;
		this.stopFacilityDistance = 0.0;

		// stop facility values
		this.coord = null;
		this.parentStopFacilityId = null;
		this.isBlockingLane = false;
		this.facilityName = null;
		this.stopPostAreaId = null;

		// route stop values
		this.departureOffset = 0.0;
		this.arrivalOffset = 0.0;
		this.awaitDepartureTime = false;

		// link value
		this.linkWeight = 0.0;
	}

	@Override
	public Id<PseudoRouteStop> getId() {
		return id;
	}

	@Override
	public int compareTo(PseudoRouteStop other) {
		if(other.getId().equals(this.id)) {
			return 0;
		}
		return Double.compare(distToSource, other.distToSource);
	}

	public double getLinkWeight() {
		return linkWeight;
	}

	public double getDepartureOffset() {
		return departureOffset;
	}

	public double getArrivalOffset() {
		return arrivalOffset;
	}

	public boolean isAwaitDepartureTime() {
		return awaitDepartureTime;
	}

	@Deprecated
	public String getChildStopFacilityId() {
		return linkCandidateId;
	}

	public Coord getCoord() {
		return coord;
	}

	public boolean getIsBlockingLane() {
		return isBlockingLane;
	}

	public String getFacilityName() {
		return facilityName;
	}

	public String getStopPostAreaId() {
		return stopPostAreaId;
	}

	public String getLinkIdStr() {
		return linkId;
	}

	public String getParentStopFacilityId() {
		return parentStopFacilityId;
	}

	@Override
	public String toString() {
		return facilityName + " " + id;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		PseudoRouteStop other = (PseudoRouteStop) obj;
		if(id == null) {
			if(other.id != null)
				return false;
		} else if(!id.toString().equals(other.id.toString()))
			return false;
		return true;
	}

	public double getStopFacilityDistance() {
		return stopFacilityDistance;
	}

}
