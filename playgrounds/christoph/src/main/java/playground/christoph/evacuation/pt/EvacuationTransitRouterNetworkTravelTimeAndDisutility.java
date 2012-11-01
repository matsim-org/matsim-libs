/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTransitRouterNetworkTravelTimeAndDisutility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.DepartureTimeCache;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

/**
 * TravelTime and TravelCost calculator to be used with the transit network used for transit routing
 * in evacuation scenarios.
 * 
 * In contrast to a TransitRouterNetworkTravelTimeAndDisutility, only travel time is respected.
 * Therefore, travel cost = travel time.
 * 
 * Moreover, this class should be thread-safe since all local variables are stored thread-local.
 *
 * @author cdobler
 */
public class EvacuationTransitRouterNetworkTravelTimeAndDisutility implements TravelTime, TransitTravelDisutility {

	final static double MIDNIGHT = 24.0*3600;

	protected final TransitRouterConfig config;
	
	private final ThreadLocal<Link> previousLinks;
	private final ThreadLocal<Double> previousTimes;
	private final ThreadLocal<Double> cachedTravelTimes;
	private final ThreadLocal<DepartureTimeCache> departureTimeCaches;

	public EvacuationTransitRouterNetworkTravelTimeAndDisutility(final TransitRouterConfig config) {
		this.config = config;
		
		this.previousLinks = new ThreadLocal<Link>();
		this.previousTimes = new ThreadLocal<Double>();
		this.cachedTravelTimes = new ThreadLocal<Double>();
		this.departureTimeCaches = new ThreadLocal<DepartureTimeCache>();
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {

		return getLinkTravelTime(link, time, person, vehicle);
	}
	
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		
		Link previousLink = this.previousLinks.get();
		Double previousTime = this.previousTimes.get();	// has to be Double since get() might return null!
		
		if ((link == previousLink) && (time == previousTime)) {
			return this.cachedTravelTimes.get();
		}
		this.previousLinks.set(link);
		this.previousTimes.set(time);

		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		TransitRouteStop fromStop = wrapped.fromNode.stop;
		TransitRouteStop toStop = wrapped.toNode.stop;
		if (wrapped.getRoute() != null) {
			// (agent stays on the same route, so use transit line travel time)
			
			// get the next departure time:
			DepartureTimeCache data = this.departureTimeCaches.get();
			if (data == null) {
				data = new DepartureTimeCache();
				departureTimeCaches.set(data);
			}
			double bestDepartureTime = data.getNextDepartureTime(wrapped.getRoute(), fromStop, time);

			// the travel time on the link is 
			//   the time until the departure (``dpTime - now'')
			//   + the travel time on the link (there.arrivalTime - here.departureTime)
			// But quite often, we only have the departure time at the next stop.  Then we use that:
			double arrivalOffset = (toStop.getArrivalOffset() != Time.UNDEFINED_TIME) ? toStop.getArrivalOffset() : toStop.getDepartureOffset();
			double time2 = (bestDepartureTime - time) + (arrivalOffset - fromStop.getDepartureOffset());
			if (time2 < 0) {
				// ( this can only happen, I think, when ``bestDepartureTime'' is after midnight but ``time'' was before )
				time2 += MIDNIGHT;
			}
			this.cachedTravelTimes.set(time2);
			return time2;
		}
		// different transit routes, so it must be a line switch
		double distance = wrapped.getLength();
		double time2 = distance / this.config.getBeelineWalkSpeed() + this.config.additionalTransferTime;
		this.cachedTravelTimes.set(time2);
				
		return time2;
	}
}