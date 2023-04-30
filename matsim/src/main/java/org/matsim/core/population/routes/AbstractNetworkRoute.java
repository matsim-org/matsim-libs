package org.matsim.core.population.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * @author mrieser / Simunto
 */
public abstract class AbstractNetworkRoute implements NetworkRoute, Cloneable {

	private double distance = Double.NaN;
	private double travelCost = Double.NaN;
	private double travelTime = Double.NaN; // NaN means undefined
	private int startLinkIndex = -1;
	private int endLinkIndex = -1;
	private Id<Vehicle> vehicleId;

	public AbstractNetworkRoute clone() {
		try {
			return (AbstractNetworkRoute) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getDistance() {
		return this.distance;
	}

	@Override
	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public double getTravelCost() {
		return this.travelCost;
	}

	@Override
	public void setTravelCost(double travelCost) {
		this.travelCost = travelCost;
	}

	@Override
	public OptionalTime getTravelTime() {
		return Double.isNaN(this.travelTime) ? OptionalTime.undefined() : OptionalTime.defined(this.travelTime);
	}

	@Override
	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public void setTravelTimeUndefined() {
		this.travelTime = Double.NaN;
	}

	@Override
	public Id<Link> getStartLinkId() {
		return this.startLinkIndex < 0 ? null : Id.get(this.startLinkIndex, Link.class);
	}

	@Override
	public void setStartLinkId(Id<Link> linkId) {
		this.startLinkIndex = linkId == null ? -1 : linkId.index();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return this.endLinkIndex < 0 ? null : Id.get(this.endLinkIndex, Link.class);
	}

	@Override
	public void setEndLinkId(Id<Link> linkId) {
		this.endLinkIndex = linkId == null ? -1 : linkId.index();
	}

	@Override
	public NetworkRoute getSubRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
		/*
		 * the index where the link after fromLinkId can be found in the route:
		 * fromIndex==0 --> fromLinkId == startLinkId,
		 * fromIndex==1 --> fromLinkId == first link in the route, etc.
		 */
		int fromIndex = -1;
		/*
		 * the index where toLinkId can be found in the route
		 */
		int toIndex = -1;

		List<Id<Link>> route = this.getLinkIds();

		if (fromLinkId.equals(this.getStartLinkId())) {
			fromIndex = 0;
		} else {
			for (int i = 0, n = route.size(); (i < n) && (fromIndex < 0); i++) {
				if (fromLinkId.equals(route.get(i))) {
					fromIndex = i+1;
				}
			}
			if (fromIndex < 0 && fromLinkId.equals(this.getEndLinkId())) {
				fromIndex = route.size();
			}
			if (fromIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because fromLinkId is not part of the route.");
			}
		}

		if (fromLinkId.equals(toLinkId)) {
			toIndex = fromIndex - 1;
		} else {
			for (int i = fromIndex, n = route.size(); (i < n) && (toIndex < 0); i++) {
				if (fromLinkId.equals(route.get(i))) {
					fromIndex = i+1; // in case of a loop, cut it short
				}
				if (toLinkId.equals(route.get(i))) {
					toIndex = i;
				}
			}
			if (toIndex < 0 && toLinkId.equals(this.getEndLinkId())) {
				toIndex = route.size();
			}
			if (toIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because toLinkId is not part of the route.");
			}
		}
		NetworkRoute ret = RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
		if (toIndex > fromIndex) {
			ret.setLinkIds(fromLinkId, route.subList(fromIndex, toIndex), toLinkId);
		} else {
			ret.setLinkIds(fromLinkId, null, toLinkId);
		}
		return ret;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public String getRouteDescription() {
		StringBuilder desc = new StringBuilder(100);
		desc.append(this.getStartLinkId().toString());
		for (Id<Link> linkId : this.getLinkIds()) {
			desc.append(" ");
			desc.append(linkId.toString());
		}
		// If the start links equals the end link additionally check if its is a round trip.
		if (!this.getEndLinkId().equals(this.getStartLinkId()) || this.getLinkIds().size() > 0) {
			desc.append(" ");
			desc.append(this.getEndLinkId().toString());
		}
		return desc.toString();
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		List<Id<Link>> linkIds = NetworkUtils.getLinkIds(routeDescription);
		Id<Link> startLinkId = getStartLinkId();
		Id<Link> endLinkId = getEndLinkId();
		if (linkIds.size() > 0) {
			startLinkId = linkIds.remove(0);
			setStartLinkId(startLinkId);
		}
		if (linkIds.size() > 0) {
			endLinkId = linkIds.remove(linkIds.size() - 1);
			setEndLinkId(endLinkId);
		}
		this.setLinkIds(startLinkId, linkIds, endLinkId);
	}

	@Override
	public String getRouteType() {
		return "links";
	}

}
