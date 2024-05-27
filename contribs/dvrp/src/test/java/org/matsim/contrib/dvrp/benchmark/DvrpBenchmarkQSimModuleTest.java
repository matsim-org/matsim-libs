/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpBenchmarkQSimModuleTest {
	@Test
	void calcLinkSpeed() {
		var link = NetworkUtils.createLink(Id.createLinkId("id"), null, null, null, 150, 15, 10, 1);
		var vehicle = mock(Vehicle.class);

		var travelTime = mock(TravelTime.class);

		when(travelTime.getLinkTravelTime(eq(link), eq(0.), isNull(), eq(vehicle))).thenReturn(10.);
		assertThat(DvrpBenchmarkQSimModule.calcLinkSpeed(travelTime, vehicle, link, 0)).isEqualTo(15);

		when(travelTime.getLinkTravelTime(eq(link), eq(100.), isNull(), eq(vehicle))).thenReturn(15.);
		assertThat(DvrpBenchmarkQSimModule.calcLinkSpeed(travelTime, vehicle, link, 100)).isEqualTo(10);
	}
}
