package org.matsim.pt.routes;

import java.io.IOException;
import java.util.Objects;

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
	protected final static int NULL_ID = -1;
	
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
		routeDescription.transitLineIndex = Objects.isNull(transitLineId) ? NULL_ID : transitLineId.index();
		routeDescription.transitRouteIndex = Objects.isNull(transitRouteId) ? NULL_ID : transitRouteId.index();
		routeDescription.accessFacilityIndex = Objects.isNull(accessFacilityId) ? NULL_ID : accessFacilityId.index();
		routeDescription.egressFacilityIndex = Objects.isNull(egressFacilityId) ? NULL_ID : egressFacilityId.index();
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
		return OptionalTime.fromSeconds(routeDescription.boardingTime);
	}

	public void setBoardingTime(double boardingTime) {
		routeDescription.boardingTime = boardingTime;
	}

	@Override
	public Id<TransitLine> getLineId() {
		return routeDescription.transitLineIndex >= 0 ? Id.get(routeDescription.transitLineIndex, TransitLine.class) : null;
	}

	@Override
	public Id<TransitRoute> getRouteId() {
		return routeDescription.transitRouteIndex >= 0 ? Id.get(routeDescription.transitRouteIndex, TransitRoute.class) : null;
	}

	@Override
	public Id<TransitStopFacility> getAccessStopId() {
		return routeDescription.accessFacilityIndex >= 0 ? Id.get(routeDescription.accessFacilityIndex, TransitStopFacility.class) : null;
	}

	@Override
	public Id<TransitStopFacility> getEgressStopId() {
		return routeDescription.egressFacilityIndex >= 0 ? Id.get(routeDescription.egressFacilityIndex, TransitStopFacility.class) : null;
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
		public double boardingTime = OptionalTime.toSeconds(OptionalTime.undefined());

		public int transitLineIndex;
		public int transitRouteIndex;

		public int accessFacilityIndex;
		public int egressFacilityIndex;

		@JsonProperty("boardingTime")
		public String getBoardingTime() {
			return Time.writeTime(boardingTime);
		}

		@JsonProperty("accessFacilityId")
		public String getAccessFacilityId() {
			return accessFacilityIndex == NULL_ID ? null : Id.get(accessFacilityIndex, TransitStopFacility.class).toString();
		}

		@JsonProperty("egressFacilityId")
		public String getEgressFacilityId() {
			return egressFacilityIndex == NULL_ID ? null : Id.get(egressFacilityIndex, TransitStopFacility.class).toString();
		}

		@JsonProperty("transitLineId")
		public String getTransitLineId() {
			return transitLineIndex == NULL_ID ? null : Id.get(transitLineIndex, TransitLine.class).toString();
		}

		@JsonProperty("transitRouteId")
		public String getRouteLineId() {
			return transitRouteIndex == NULL_ID ? null : Id.get(transitRouteIndex, TransitRoute.class).toString();
		}

		@JsonProperty("boardingTime")
		public void setBoardingTime(String boardingTime) {
			this.boardingTime = OptionalTime.toSeconds(Time.parseOptionalTime(boardingTime));
		}

		@JsonProperty("transitLineId")
		public void setTransitLineId(String transitLineId) {
			this.transitLineIndex = transitLineId == null ? NULL_ID : Id.create(transitLineId, TransitLine.class).index();
		}

		@JsonProperty("transitRouteId")
		public void setRouteLineId(String transitRouteId) {
			this.transitRouteIndex = transitRouteId == null ? NULL_ID : Id.create(transitRouteId, TransitRoute.class).index();
		}

		@JsonProperty("accessFacilityId")
		public void setAccessFacilityId(String accessFacilityId) {
			this.accessFacilityIndex = accessFacilityId == null ? NULL_ID : Id.create(accessFacilityId, TransitStopFacility.class).index();
		}

		@JsonProperty("egressFacilityId")
		public void setEgressFacilityId(String egressFacilityId) {
			this.egressFacilityIndex = egressFacilityId == null ? NULL_ID : Id.create(egressFacilityId, TransitStopFacility.class).index();
		}

	}
}
