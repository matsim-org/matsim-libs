package org.matsim.pt.routes;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultTransitPassengerRoute extends AbstractRoute implements TransitPassengerRoute {
	final public static String ROUTE_TYPE = "default_pt";

	private RouteDescription routeDescription = null;

	DefaultTransitPassengerRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		this(startLinkId, endLinkId, null, null, null, null);
	}

	public DefaultTransitPassengerRoute(TransitStopFacility accessFacility, TransitLine line, TransitRoute route,
			TransitStopFacility egressFacility) {
		this( //
				accessFacility.getLinkId(), egressFacility.getLinkId(), //
				accessFacility.getId(), egressFacility.getId(), //
				line != null ? line.getId() : null, route != null ? route.getId() : null);
	}

	public DefaultTransitPassengerRoute( //
			final Id<Link> accessLinkId, final Id<Link> egressLinkId, //
			Id<TransitStopFacility> accessFacilityId, Id<TransitStopFacility> egressFacilityId, //
			Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
		super(accessLinkId, egressLinkId);

		this.routeDescription = new RouteDescription();
		routeDescription.transitLineId = transitLineId;
		routeDescription.transitRouteId = transitRouteId;
		routeDescription.accessFacilityId = accessFacilityId;
		routeDescription.egressFacilityId = egressFacilityId;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public String getRouteDescription() {
		try {
			return new ObjectMapper().writeValueAsString(routeDescription);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		try {
			this.routeDescription = new ObjectMapper().readValue(routeDescription, RouteDescription.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public OptionalTime getBoardingTime() {
		return routeDescription.boardingTime;
	}

	public void setBoardingTime(double boardingTime) {
		routeDescription.boardingTime = OptionalTime.defined(boardingTime);
	}

	@Override
	public Id<TransitLine> getLineId() {
		return routeDescription.transitLineId;
	}

	@Override
	public Id<TransitRoute> getRouteId() {
		return routeDescription.transitRouteId;
	}

	@Override
	public Id<TransitStopFacility> getAccessStopId() {
		return routeDescription.accessFacilityId;
	}

	@Override
	public Id<TransitStopFacility> getEgressStopId() {
		return routeDescription.egressFacilityId;
	}

	@Override
	public DefaultTransitPassengerRoute clone() {
		DefaultTransitPassengerRoute copy = new DefaultTransitPassengerRoute( //
				getStartLinkId(), getEndLinkId(), //
				getAccessStopId(), getEgressStopId(), //
				getLineId(), getRouteId());

		copy.setDistance(getDistance());
		getTravelTime().ifDefined(copy::setTravelTime);
		getBoardingTime().ifDefined(copy::setBoardingTime);

		return copy;
	}

	public static class RouteDescription {
		public OptionalTime boardingTime = OptionalTime.undefined();

		public Id<TransitLine> transitLineId;
		public Id<TransitRoute> transitRouteId;

		public Id<TransitStopFacility> accessFacilityId;
		public Id<TransitStopFacility> egressFacilityId;

		@JsonProperty("boardingTime")
		public String getBoardingTime() {
			return Time.writeTime(boardingTime);
		}

		@JsonProperty("accessFacilityId")
		public String getAccessFacilityId() {
			return accessFacilityId == null ? null : accessFacilityId.toString();
		}

		@JsonProperty("egressFacilityId")
		public String getEgressFacilityId() {
			return egressFacilityId == null ? null : egressFacilityId.toString();
		}

		@JsonProperty("transitLineId")
		public String getTransitLineId() {
			return transitLineId == null ? null : transitLineId.toString();
		}

		@JsonProperty("transitRouteId")
		public String getRouteLineId() {
			return transitRouteId == null ? null : transitRouteId.toString();
		}

		@JsonProperty("boardingTime")
		public void setBoardingTime(String boardingTime) {
			this.boardingTime = Time.parseOptionalTime(boardingTime);
		}

		@JsonProperty("transitLineId")
		public void setTransitLineId(String transitLineId) {
			this.transitLineId = transitLineId == null ? null : Id.create(transitLineId, TransitLine.class);
		}

		@JsonProperty("transitRouteId")
		public void setRouteLineId(String transitRouteId) {
			this.transitRouteId = transitRouteId == null ? null : Id.create(transitRouteId, TransitRoute.class);
		}

		@JsonProperty("accessFacilityId")
		public void setAccessFacilityId(String accessFacilityId) {
			this.accessFacilityId = accessFacilityId == null ? null
					: Id.create(accessFacilityId, TransitStopFacility.class);
		}

		@JsonProperty("egressFacilityId")
		public void setEgressFacilityId(String egressFacilityId) {
			this.egressFacilityId = egressFacilityId == null ? null
					: Id.create(egressFacilityId, TransitStopFacility.class);
		}

	}
}
