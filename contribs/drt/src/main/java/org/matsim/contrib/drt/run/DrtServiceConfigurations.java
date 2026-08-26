/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Runtime view on the time-dependent service configurations of one DRT service: which regime is active at a given time
 * and which stops it serves. This is the single source of truth for both the routing side
 * ({@code TimeDependentAccessEgressFacilityFinder}) and the request validation side
 * ({@code DrtServiceTimeRequestValidator}).
 *
 * @author nkuehnel / MOIA
 */
public class DrtServiceConfigurations {

	/**
	 * One service configuration with its stops resolved. The time window is half-open, i.e. {@code [startTime,
	 * endTime)}.
	 *
	 * @param stops   the stops served in this regime, empty if the service has no stops at all (door2door)
	 * @param linkIds the links of {@code stops}
	 */
	public record Regime(String name, OptionalTime startTime, OptionalTime endTime, Collection<DrtStopFacility> stops,
						 Set<Id<Link>> linkIds) {
		/**
		 * @return true if the given time lies in the half-open interval {@code [startTime, endTime)}
		 */
		public boolean covers(double time) {
			return startTime.orElse(Double.NEGATIVE_INFINITY) <= time && time < endTime.orElse(
					Double.POSITIVE_INFINITY);
		}
	}

	private final List<Regime> regimes;

	public DrtServiceConfigurations(DrtServiceConfigurationsParams params, DrtStopNetwork stopNetwork) {
		this.regimes = params.getServiceConfigurations()
				.stream()
				.map(serviceConfiguration -> createRegime(serviceConfiguration, stopNetwork))
				.toList();
	}

	private static Regime createRegime(DrtServiceConfigurationParams serviceConfiguration, DrtStopNetwork stopNetwork) {
		Collection<DrtStopFacility> stops = stopNetwork.getDrtStops()
				.values()
				.stream()
				.filter(stop -> serviceConfiguration.getStopNetwork() == null
						|| AttributeBasedStopFinder.parseStopNetworks(stop)
						.contains(serviceConfiguration.getStopNetwork()))
				.collect(ImmutableList.toImmutableList());
		if (serviceConfiguration.getStopNetwork() != null) {
			// otherwise the service configuration would silently serve nobody
			Verify.verify(!stops.isEmpty(),
					"No stop belongs to the stop network '%s' of %s '%s'. Check the '%s' attribute of the stops or the"
							+ " service areas they are derived from.", serviceConfiguration.getStopNetwork(),
					DrtServiceConfigurationParams.SET_NAME, serviceConfiguration.getServiceConfigurationName(),
					AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE);
		}
		Set<Id<Link>> linkIds = stops.stream().map(DrtStopFacility::getLinkId).collect(ImmutableSet.toImmutableSet());
		return new Regime(serviceConfiguration.getServiceConfigurationName(), serviceConfiguration.getStartTime(),
				serviceConfiguration.getEndTime(), stops, linkIds);
	}

	/**
	 * @return the service configuration whose half-open time window covers the given time, or an empty
	 * {@link Optional} if the service is not available then. The windows are pairwise disjoint (checked in
	 * {@link DrtServiceConfigurationsParams#checkConsistency}), so there is at most one match.
	 */
	public Optional<Regime> getActiveRegime(double time) {
		return regimes.stream().filter(regime -> regime.covers(time)).findFirst();
	}

	public List<Regime> getRegimes() {
		return regimes;
	}
}
