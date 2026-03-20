package org.matsim.pt.routes;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
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

	public DefaultTransitPassengerRoute chainedRoute = null;

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

	public DefaultTransitPassengerRoute(TransitStopFacility accessFacility, TransitLine line, TransitRoute route,
										TransitStopFacility egressFacility, DefaultTransitPassengerRoute chainedRoute) {
		this( //
			accessFacility.getLinkId(), egressFacility.getLinkId(), //
			accessFacility.getId(), egressFacility.getId(), //
			line != null ? line.getId() : null, route != null ? route.getId() : null);
		this.chainedRoute = chainedRoute;
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

	private DefaultTransitPassengerRoute(RouteDescription r, DefaultTransitPassengerRoute chainedRoute) {
		super(null, null);
		this.boardingTime = r.boardingTime;
		this.accessFacility = r.accessFacilityId;
		this.egressFacility = r.egressFacilityId;
		this.transitLine = r.transitLineId;
		this.transitRoute = r.transitRouteId;
		this.chainedRoute = chainedRoute;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public String getRouteDescription() {

		try {
			RouteDescription routeDescription = new RouteDescription(this);
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
			this.chainedRoute = createChainedRoutes(parsed.chainedRoute);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new DefaultTransitPassengerRoute from the given RouteDescription recursively.
	 */
	private static DefaultTransitPassengerRoute createChainedRoutes(RouteDescription r) {

		if (r == null) {
			return null;
		}
		return new DefaultTransitPassengerRoute(r, createChainedRoutes(r.chainedRoute));
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
		// Egress is always the very last stop if the route is chained
		if (this.chainedRoute != null) {
			return this.chainedRoute.getEgressStopId();
		}

		return egressFacility;
	}

	@Override
	public DefaultTransitPassengerRoute getChainedRoute() {
		return chainedRoute;
	}

	@Override
	public DefaultTransitPassengerRoute clone() {
		DefaultTransitPassengerRoute copy = new DefaultTransitPassengerRoute( //
			getStartLinkId(), getEndLinkId(), //
			getAccessStopId(), getEgressStopId(), //
			getLineId(), getRouteId());

		// Perform deep copy of the chained route
		if (this.chainedRoute != null) {
			copy.chainedRoute = this.chainedRoute.clone();
		}

		copy.setDistance(getDistance());
		getTravelTime().ifDefined(copy::setTravelTime);
		getBoardingTime().ifDefined(copy::setBoardingTime);

		return copy;
	}

	private static final class RouteDescription {
		public double boardingTime = UNDEFINED_TIME;

		public Id<TransitLine> transitLineId;
		public Id<TransitRoute> transitRouteId;

		public Id<TransitStopFacility> accessFacilityId;
		public Id<TransitStopFacility> egressFacilityId;

		public RouteDescription chainedRoute;

		public RouteDescription() {
		}

		public RouteDescription(DefaultTransitPassengerRoute r) {
			boardingTime = r.boardingTime;
			accessFacilityId = r.accessFacility;
			egressFacilityId = r.egressFacility;
			transitLineId = r.transitLine;
			transitRouteId = r.transitRoute;

			if (r.chainedRoute != null) {
				this.chainedRoute = new RouteDescription(r.chainedRoute);
			}
		}

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

		@JsonProperty("chainedRoute")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public RouteDescription getChainedRoute() {
			return chainedRoute;
		}

		@JsonProperty("chainedRoute")
		public void setChainedRoute(RouteDescription chainedRoute) {
			this.chainedRoute = chainedRoute;
		}
	}
}
