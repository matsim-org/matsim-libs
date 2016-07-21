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

package contrib.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import contrib.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;
import contrib.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidateImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author polettif
 */
public class PseudoRouteStopImpl implements PseudoRouteStop {

	// dijkstra
	public final Map<PseudoRouteStop, Double> neighbours = new HashMap<>();
	private final LinkCandidate linkCandidate;
	public double travelCostToSource = Double.MAX_VALUE; // MAX_VALUE assumed to be infinity
	public PseudoRouteStop previous = null;

	private final double linkTravelCost;

	// schedule values
	public final Id<PseudoRouteStop> id;
	private final String name;

	private final Id<Link> linkId;

	private final double departureOffset;
	private final double arrivalOffset;
	private final boolean awaitDepartureTime;

	private final Coord coord;
	private final boolean isBlockingLane;
	private final String facilityName;
	private final String stopPostAreaId;
	private final Id<TransitStopFacility> parentStopFacilityId;
	private final double stopFacilityDistance;

	/**
	 * Constructor. All primitive attribute values of the transitRouteStop are stored
	 * to make access easier during stop facility replacement.
	 */
	/*package*/ PseudoRouteStopImpl(int order, TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		this.id = Id.create("[" + Integer.toString(order) + "]" + linkCandidate.getId(), PseudoRouteStop.class);
		this.name = routeStop.getStopFacility().getName() + " (" + linkCandidate.getLinkId() + ")";
		this.linkId = linkCandidate.getLinkId();
		this.stopFacilityDistance = linkCandidate.getStopFacilityDistance();

		// stop facility values
		this.coord = routeStop.getStopFacility().getCoord();
		this.parentStopFacilityId = routeStop.getStopFacility().getId();
		this.isBlockingLane = routeStop.getStopFacility().getIsBlockingLane();
		this.facilityName = routeStop.getStopFacility().getName();
		this.stopPostAreaId = routeStop.getStopFacility().getStopPostAreaId();

		// route stop values
		this.departureOffset = routeStop.getDepartureOffset();
		this.arrivalOffset = routeStop.getArrivalOffset();
		this.awaitDepartureTime = routeStop.isAwaitDepartureTime();

		// link value
		this.linkTravelCost = linkCandidate.getLinkTravelCost();
		this.linkCandidate = linkCandidate;
	}

	/**
	 * This constructor is only used to set dummy stops
	 */
	public PseudoRouteStopImpl(String id) {
		if(id.equals(PseudoGraphImpl.SOURCE)) {
			this.id = Id.create(PseudoGraphImpl.SOURCE, PseudoRouteStop.class);
			this.travelCostToSource = 0;
		} else {
			this.id = Id.create(PseudoGraphImpl.DESTINATION, PseudoRouteStop.class);
		}
		this.name = id;

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
		this.linkTravelCost = 0.0;
		this.linkCandidate = new LinkCandidateImpl();
	}


	@Override
	public double getTravelCostToSource() {
		return travelCostToSource;
	}
	@Override
	public void setTravelCostToSource(double cost) {
		this.travelCostToSource = cost;
	}
	@Override
	public PseudoRouteStop getClosestPrecedingRouteStop() {
		return previous;
	}
	@Override
	public void setClosestPrecedingRouteSTop(PseudoRouteStop stop) {
		this.previous = stop;
	}
	@Override
	public Map<PseudoRouteStop, Double> getNeighbours() {
		return neighbours;
	}


	@Override
	public Id<PseudoRouteStop> getId() {
		return id;
	}

	@Override
	public int compareTo(PseudoRouteStop other) {
		if(this.equals(other)) {
			return 0;
		}
		int dCompare = Double.compare(travelCostToSource, other.getTravelCostToSource());
		return (dCompare == 0 ? 1 : dCompare);
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public double getDepartureOffset() {
		return departureOffset;
	}

	@Override
	public double getArrivalOffset() {
		return arrivalOffset;
	}

	@Override
	public boolean awaitsDepartureTime() {
		return awaitDepartureTime;
	}

	@Override
	public Id<TransitStopFacility> getParentStopFacilityId() {
		return parentStopFacilityId;
	}

	@Override
	public LinkCandidate getLinkCandidate() {
		return linkCandidate;
	}

	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public boolean isBlockingLane() {
		return isBlockingLane;
	}

	@Override
	public String getFacilityName() {
		return facilityName;
	}

	@Override
	public String getStopPostAreaId() {
		return stopPostAreaId;
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

		PseudoRouteStopImpl other = (PseudoRouteStopImpl) obj;
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
