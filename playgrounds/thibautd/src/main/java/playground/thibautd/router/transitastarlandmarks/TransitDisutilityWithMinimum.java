/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.router.transitastarlandmarks;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

/**
 * Wraps a {@link TransitRouterNetworkTravelTimeAndDisutility} and decorates it with {@link TravelDisutility}.
 * Concretely, this adds the "minimum disutility" functionality, which makes possible the use of A* landmarks
 * for speedup of the routing process.
 *
 * @author thibautd
 */
public class TransitDisutilityWithMinimum implements TravelTime, TransitTravelDisutility, TravelDisutility {
	private final TransitRouterNetworkTravelTimeAndDisutility delegate;
	private final PreparedTransitSchedule preparedTransitSchedule;
	protected final TransitRouterConfig config;

	public TransitDisutilityWithMinimum(
			final TransitRouterConfig config,
			final PreparedTransitSchedule preparedTransitSchedule ) {
		this.delegate = new TransitRouterNetworkTravelTimeAndDisutility( config , preparedTransitSchedule );
		this.config = config;
		this.preparedTransitSchedule = preparedTransitSchedule;
	}

	@Override
	public double getLinkTravelDisutility(
			final Link link,
			final double time,
			final Person person,
			final Vehicle vehicle,
			final CustomDataManager dataManager ) {
		return delegate.getLinkTravelDisutility( link, time, person, vehicle, dataManager );
	}

	public double getTravelTime( Person person,
			final Coord coord,
			final Coord toCoord ) {
		return delegate.getTravelTime( person, coord, toCoord );
	}

	public double getVehArrivalTime( Link link, double now ) {
		return delegate.getVehArrivalTime( link, now );
	}

	public double getTravelDisutility(
			final Person person,
			final Coord coord,
			final Coord toCoord ) {
		return delegate.getTravelDisutility( person, coord, toCoord );
	}

	@Override
	public double getLinkTravelTime( Link link,
			final double time,
			final Person person,
			final Vehicle vehicle ) {
		return delegate.getLinkTravelTime( link, time, person, vehicle );
	}

	@Override
	public double getLinkTravelDisutility(
			final Link link,
			final double time,
			final Person person,
			final Vehicle vehicle ) {
		return getLinkTravelDisutility( link , time , person , vehicle , null );
	}

	@Override
	public double getLinkMinimumTravelDisutility( final Link link ) {
		if (((TransitRouterNetwork.TransitRouterNetworkLink) link).getRoute() == null) {
			// "route" here means "pt route".  If no pt route is attached, it means that it is a transfer link.
			return defaultTransferCost( link );
		}

		final double inVehTime = getLinkMinimumTravelTime( link );
		return - inVehTime       * this.config.getMarginalUtilityOfTravelTimePt_utl_s()
			   -link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();

	}

	private final double defaultTransferCost( final Link link ) {
		final double transfertime = getLinkMinimumTravelTime( link );
		final double waittime = this.config.getAdditionalTransferTime();

		// say that the effective walk time is the transfer time minus some "buffer"
		double walktime = transfertime - waittime;

		double walkDistance = link.getLength() ;

		// weigh this "buffer" not with the walk time disutility, but with the wait time disutility:
		// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
		// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
		// be the same as the marginal opprotunity cost of time).  kai, nov'11
		return - walktime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
		       - walkDistance * this.config.getMarginalUtilityOfTravelDistancePt_utl_m()
		       - waittime * this.config.getMarginalUtilityOfWaitingPt_utl_s()
		       - this.config.getUtilityOfLineSwitch_utl();
	}

	public double getLinkMinimumTravelTime( final Link link ) {
		TransitRouterNetwork.TransitRouterNetworkLink wrapped = (TransitRouterNetwork.TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.getRoute() != null) {
			// (agent stays on the same route, so use transit line travel time)

			// the travel time on the link is
			//   the time until the departure (``dpTime - now'')
			//   + the travel time on the link (there.arrivalTime - here.departureTime)
			// But quite often, we only have the departure time at the next stop.  Then we use that:
			double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			return arrivalOffset - fromStop.getDepartureOffset();
		}

		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		return distance / this.config.getBeelineWalkSpeed() + this.config.getAdditionalTransferTime();
	}
}

