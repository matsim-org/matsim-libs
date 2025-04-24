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
	protected final static int NULL_ID = -1;
	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	final public static String ROUTE_TYPE = "default_pt";

	public double boardingTime = UNDEFINED_TIME;

	public int transitLineIndex;
	public int transitRouteIndex;

	public int accessFacilityIndex;
	public int egressFacilityIndex;

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

		this.transitLineIndex = Objects.isNull(transitLineId) ? NULL_ID : transitLineId.index();
		this.transitRouteIndex = Objects.isNull(transitRouteId) ? NULL_ID : transitRouteId.index();
		this.accessFacilityIndex = Objects.isNull(accessFacilityId) ? NULL_ID : accessFacilityId.index();
		this.egressFacilityIndex = Objects.isNull(egressFacilityId) ? NULL_ID : egressFacilityId.index();
	}

	/**
	 * Construct for data read from JSON.
	 */
	private DefaultTransitPassengerRoute(double boardingTime, int accessFacilityIndex, int egressFacilityIndex,
										 int transitLineIndex, int transitRouteIndex, DefaultTransitPassengerRoute chainedRoute) {
		super(null, null);
		this.boardingTime = boardingTime;
		this.accessFacilityIndex = accessFacilityIndex;
		this.egressFacilityIndex = egressFacilityIndex;
		this.transitLineIndex = transitLineIndex;
		this.transitRouteIndex = transitRouteIndex;
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
			this.accessFacilityIndex = parsed.accessFacilityId == null ? NULL_ID : parsed.accessFacilityId.index();
			this.egressFacilityIndex = parsed.egressFacilityId == null ? NULL_ID : parsed.egressFacilityId.index();
			this.transitLineIndex = parsed.transitLineId == null ? NULL_ID : parsed.transitLineId.index();
			this.transitRouteIndex = parsed.transitRouteId == null ? NULL_ID : parsed.transitRouteId.index();
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

		double boardingTime = r.boardingTime;
		int accessFacilityIndex = r.accessFacilityId == null ? NULL_ID : r.accessFacilityId.index();
		int egressFacilityIndex = r.egressFacilityId == null ? NULL_ID : r.egressFacilityId.index();
		int transitLineIndex = r.transitLineId == null ? NULL_ID : r.transitLineId.index();
		int transitRouteIndex = r.transitRouteId == null ? NULL_ID : r.transitRouteId.index();

		return new DefaultTransitPassengerRoute(
			boardingTime, accessFacilityIndex, egressFacilityIndex, transitLineIndex, transitRouteIndex,
			createChainedRoutes(r.chainedRoute)
		);
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
		return this.transitLineIndex >= 0 ? Id.get(this.transitLineIndex, TransitLine.class) : null;
	}

	@Override
	public Id<TransitRoute> getRouteId() {
		return this.transitRouteIndex >= 0 ? Id.get(this.transitRouteIndex, TransitRoute.class) : null;
	}

	@Override
	public Id<TransitStopFacility> getAccessStopId() {
		return this.accessFacilityIndex >= 0 ? Id.get(this.accessFacilityIndex, TransitStopFacility.class) : null;
	}

	@Override
	public Id<TransitStopFacility> getEgressStopId() {
		return this.egressFacilityIndex >= 0 ? Id.get(this.egressFacilityIndex, TransitStopFacility.class) : null;
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
			accessFacilityId = r.accessFacilityIndex == NULL_ID ? null : Id.get(r.accessFacilityIndex, TransitStopFacility.class);
			egressFacilityId = r.egressFacilityIndex == NULL_ID ? null : Id.get(r.egressFacilityIndex, TransitStopFacility.class);
			transitLineId = r.transitLineIndex == NULL_ID ? null : Id.get(r.transitLineIndex, TransitLine.class);
			transitRouteId = r.transitRouteIndex == NULL_ID ? null : Id.get(r.transitRouteIndex, TransitRoute.class);

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
