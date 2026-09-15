package org.matsim.pt.transitSchedule;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.ChainedDeparture;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.Objects;

public class ChainedDepartureImpl implements ChainedDeparture {

	private final Id<TransitLine> chainedTransitLineId;
	private final Id<TransitRoute> chainedRouteId;
	private final Id<Departure> chainedDepartureId;

	public ChainedDepartureImpl(final Id<TransitLine> chainedTransitLineId, final Id<TransitRoute> chainedRouteId, final Id<Departure> chainedDepartureId) {
		this.chainedTransitLineId = Objects.requireNonNull(chainedTransitLineId);
		this.chainedRouteId = Objects.requireNonNull(chainedRouteId);
		this.chainedDepartureId = Objects.requireNonNull(chainedDepartureId);
	}

	@Override
	public Id<TransitLine> getChainedTransitLineId() {
		return this.chainedTransitLineId;
	}

	@Override
	public Id<TransitRoute> getChainedRouteId() {
		return this.chainedRouteId;
	}

	@Override
	public Id<Departure> getChainedDepartureId() {
		return this.chainedDepartureId;
	}

	@Override
	public String toString() {
		return "[ChainedDeparture: " +
			"chainedTransitLineId=" + chainedTransitLineId +
			", chainedRouteId=" + chainedRouteId +
			", chainedDepartureId=" + chainedDepartureId +
			']';
	}
}
