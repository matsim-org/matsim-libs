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

	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	final public static String ROUTE_TYPE = "default_pt";

	public double boardingTime = UNDEFINED_TIME;

	public Id<TransitLine> transitLine;
	public Id<TransitRoute> transitRoute;

	public Id<TransitStopFacility> accessFacility;
	public Id<TransitStopFacility> egressFacility;

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

		this.transitLine = transitLineId;
		this.transitRoute = transitRouteId;
		this.accessFacility = accessFacilityId;
		this.egressFacility = egressFacilityId;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public String getRouteDescription() {

		try {
			RouteDescription routeDescription = new RouteDescription();
			routeDescription.boardingTime = this.boardingTime;
			routeDescription.accessFacilityId = this.accessFacility;
			routeDescription.egressFacilityId = this.egressFacility;
			routeDescription.transitLineId = this.transitLine;
			routeDescription.transitRouteId = this.transitRoute;
			return OBJECT_MAPPER.writeValueAsString(routeDescription);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		try {
			RouteDescription parsed = OBJECT_MAPPER.readValue(routeDescription, RouteDescription.class);
			this.boardingTime = parsed.boardingTime;
			this.accessFacility = parsed.accessFacilityId;
			this.egressFacility = parsed.egressFacilityId;
			this.transitLine = parsed.transitLineId;
			this.transitRoute = parsed.transitRouteId;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public OptionalTime getBoardingTime() {
		return asOptionalTime(this.boardingTime);
	}

	public void setBoardingTime(double boardingTime) {
		OptionalTime.assertDefined(boardingTime);
		this.boardingTime = boardingTime;
	}

	@Override
	public Id<TransitLine> getLineId() {
		return this.transitLine;
	}

	@Override
	public Id<TransitRoute> getRouteId() {
		return this.transitRoute;
	}

	@Override
	public Id<TransitStopFacility> getAccessStopId() {
		return this.accessFacility;
	}

	@Override
	public Id<TransitStopFacility> getEgressStopId() {
		return this.egressFacility;
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
		public double boardingTime = UNDEFINED_TIME;

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
			return this.accessFacilityId == null ? null : this.accessFacilityId.toString();
		}

		@JsonProperty("egressFacilityId")
		public String getEgressFacilityId() {
			return this.egressFacilityId == null ? null : this.egressFacilityId.toString();
		}

		@JsonProperty("transitLineId")
		public String getTransitLineId() {
			return this.transitLineId == null ? null : this.transitLineId.toString();
		}

		@JsonProperty("transitRouteId")
		public String getRouteLineId() {
			return this.transitRouteId == null ? null : this.transitRouteId.toString();
		}

		@JsonProperty("boardingTime")
		public void setBoardingTime(String boardingTime) {
			this.boardingTime = Time.parseOptionalTime(boardingTime).orElse(UNDEFINED_TIME);
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
			this.accessFacilityId = accessFacilityId == null ? null : Id.create(accessFacilityId, TransitStopFacility.class);
		}

		@JsonProperty("egressFacilityId")
		public void setEgressFacilityId(String egressFacilityId) {
			this.egressFacilityId = egressFacilityId == null ? null : Id.create(egressFacilityId, TransitStopFacility.class);
		}

	}
}
