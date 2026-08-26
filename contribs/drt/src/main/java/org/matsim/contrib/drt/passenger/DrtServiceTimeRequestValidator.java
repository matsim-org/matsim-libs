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

package org.matsim.contrib.drt.passenger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.run.DrtServiceConfigurations;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;

/**
 * Enforces the time-dependent service configuration as a hard constraint: a request is only accepted if its desired
 * departure time is covered by one of the configured service time windows and if both its links are served in that
 * window.
 * <p>
 * The check is needed in addition to the routing-side restriction because requests may reach the optimizer without
 * having been routed in this run (prebooking, within-day replanning, input plans carrying finished DRT routes).
 * <p>
 * Only the earliest (i.e. desired) start time is checked, so the end of the service time is soft: a vehicle which
 * arrives after the service has ended still serves the request.
 *
 * @author nkuehnel / MOIA
 */
public class DrtServiceTimeRequestValidator implements PassengerRequestValidator {

	public static final String OUTSIDE_SERVICE_TIME_CAUSE = "outside_service_time";
	public static final String OUTSIDE_SERVICE_AREA_ACCESS_CAUSE = "outside_service_area_access";
	public static final String OUTSIDE_SERVICE_AREA_EGRESS_CAUSE = "outside_service_area_egress";

	private final DrtServiceConfigurations serviceConfigurations;

	public DrtServiceTimeRequestValidator(DrtServiceConfigurations serviceConfigurations) {
		this.serviceConfigurations = serviceConfigurations;
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		Optional<DrtServiceConfigurations.Regime> regime = serviceConfigurations.getActiveRegime(
				request.getEarliestStartTime());
		if (regime.isEmpty()) {
			return Set.of(OUTSIDE_SERVICE_TIME_CAUSE);
		}

		Set<Id<Link>> linkIds = regime.get().linkIds();
		if (linkIds.isEmpty()) {
			// door2door: there are no stops, so only the time is restricted
			return Set.of();
		}

		Set<String> causes = new HashSet<>();
		if (!linkIds.contains(request.getFromLink().getId())) {
			causes.add(OUTSIDE_SERVICE_AREA_ACCESS_CAUSE);
		}
		if (!linkIds.contains(request.getToLink().getId())) {
			causes.add(OUTSIDE_SERVICE_AREA_EGRESS_CAUSE);
		}
		return causes;
	}
}
