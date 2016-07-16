/* *********************************************************************** *
 * project: org.matsim.*
 * KtiLinkNetworkRouteImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup.SimLegInterpretation;

import java.util.List;

/**
 * Temporary solution to calculate the route distance as it is simulated in the JEDQSim.
 *
 * TODO Generalize in MATSim that routes are handled consistently with their interpretation in the traffic simulation.
 *
 * @author meisterk
 */
public class KtiLinkNetworkRouteImpl implements NetworkRoute, Cloneable {
	/*package*/ final static String ROUTE_TYPE = "links";
	
	LinkNetworkRouteImpl delegate ;
	
	final private PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;
	final private Network network;

	public KtiLinkNetworkRouteImpl(Id fromLinkId, Id toLinkId, Network network, SimLegInterpretation simLegInterpretation) {
//		super(fromLinkId, toLinkId);
		delegate = new LinkNetworkRouteImpl( fromLinkId, toLinkId ) ;
		this.network = network;
		this.simLegInterpretation = simLegInterpretation;
	}

	@Override
	public double getDistance() {

		double distance = RouteUtils.calcDistanceExcludingStartEndLink(this, this.network);

		if (!this.getStartLinkId().equals(this.getEndLinkId())) {
			switch (this.simLegInterpretation) {
			case CetinCompatible:
				distance += this.network.getLinks().get(this.getEndLinkId()).getLength();
				break;
			case CharyparEtAlCompatible:
				distance += this.network.getLinks().get(this.getStartLinkId()).getLength();
				break;
			}
		}
		return distance;

	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public final void setDistance(double dist) {
		this.delegate.setDistance(dist);
	}

	@Override
	public final double getTravelTime() {
		return this.delegate.getTravelTime();
	}

	@Override
	public final void setTravelTime(double travTime) {
		this.delegate.setTravelTime(travTime);
	}

	@Override
	public void setEndLinkId(Id<Link> linkId) {
		this.delegate.setEndLinkId(linkId);
	}

	@Override
	public void setStartLinkId(Id<Link> linkId) {
		this.delegate.setStartLinkId(linkId);
	}

	@Override
	public Id<Link> getStartLinkId() {
		return this.delegate.getStartLinkId();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return this.delegate.getEndLinkId();
	}

	@Override
	public LinkNetworkRouteImpl clone() {
		return this.delegate.clone();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return this.delegate.getLinkIds();
	}

	@Override
	public NetworkRoute getSubRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
		return this.delegate.getSubRoute(fromLinkId, toLinkId);
	}

	@Override
	public boolean equals(Object obj) {
		return this.delegate.equals(obj);
	}

	@Override
	public double getTravelCost() {
		return this.delegate.getTravelCost();
	}

	@Override
	public void setTravelCost(double travelCost) {
		this.delegate.setTravelCost(travelCost);
	}

	@Override
	public void setLinkIds(Id<Link> startLinkId, List<Id<Link>> srcRoute, Id<Link> endLinkId) {
		this.delegate.setLinkIds(startLinkId, srcRoute, endLinkId);
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.delegate.getVehicleId();
	}

	@Override
	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.delegate.setVehicleId(vehicleId);
	}

	@Override
	public String getRouteDescription() {
		return this.delegate.getRouteDescription();
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		this.delegate.setRouteDescription(routeDescription);
	}

	@Override
	public String getRouteType() {
		return this.delegate.getRouteType();
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

}
