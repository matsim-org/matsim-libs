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

import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Builders for the service regime parameter sets, shared by the tests of the config, of the runtime object, of the
 * request validator and of the integration tests.
 *
 * @author nkuehnel / MOIA
 */
public final class DrtServiceRegimesFixtures {

	private DrtServiceRegimesFixtures() {
	}

	public static DrtServiceRegimeParams serviceRegime(String name, OptionalTime startTime, OptionalTime endTime,
			String stopNetwork) {
		DrtServiceRegimeParams serviceRegime = new DrtServiceRegimeParams(name);
		serviceRegime.setStartTime(startTime);
		serviceRegime.setEndTime(endTime);
		serviceRegime.setStopNetwork(stopNetwork);
		return serviceRegime;
	}

	public static DrtServiceRegimeParams serviceRegime(String name, double startTime, double endTime,
			String stopNetwork) {
		return serviceRegime(name, OptionalTime.defined(startTime), OptionalTime.defined(endTime), stopNetwork);
	}

	public static DrtServiceRegimesParams serviceRegimesParams(DrtServiceRegimeParams... serviceRegimes) {
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		for (DrtServiceRegimeParams serviceRegime : serviceRegimes) {
			params.addParameterSet(serviceRegime);
		}
		return params;
	}

	public static DrtServiceRegimes serviceRegimes(DrtStopNetwork stopNetwork,
			DrtServiceRegimeParams... serviceRegimes) {
		return new DrtServiceRegimes(serviceRegimesParams(serviceRegimes), stopNetwork);
	}
}
