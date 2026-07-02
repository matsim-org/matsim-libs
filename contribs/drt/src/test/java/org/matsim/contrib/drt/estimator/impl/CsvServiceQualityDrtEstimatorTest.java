/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.estimator.impl;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvServiceQualityDrtEstimatorTest {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void usesNearestTimeForExactStopPair() throws IOException {
		Path csv = Path.of(utils.getOutputDirectory()).resolve("service-quality.csv");
		Files.createDirectories(csv.getParent());
		Files.writeString(csv, String.join("\n",
			"time;originStop;destinationStop;waitTime;directRideTime;rideTimeWithDetour;detourFactor",
			"28800;o;d;120;600;780;1.3",
			"32400;o;d;180;600;840;1.4"));

		CsvServiceQualityDrtEstimator estimator = new CsvServiceQualityDrtEstimator(csv.toString(), stopNetwork());
		DrtRoute route = new DrtRoute(Id.createLinkId("from"), Id.createLinkId("to"));
		route.setDistance(1234);

		var estimate = estimator.estimate(route, OptionalTime.defined(30000));

		assertThat(estimate.waitingTime()).isEqualTo(120);
		assertThat(estimate.rideTime()).isEqualTo(780);
		assertThat(estimate.rideDistance()).isEqualTo(1234);
	}

	@Test
	void failsForMissingStopPair() throws IOException {
		Path csv = Path.of(utils.getOutputDirectory()).resolve("service-quality.csv");
		Files.createDirectories(csv.getParent());
		Files.writeString(csv, String.join("\n",
			"time;originStop;destinationStop;waitTime;directRideTime;rideTimeWithDetour;detourFactor",
			"28800;o;d;120;600;780;1.3"));

		CsvServiceQualityDrtEstimator estimator = new CsvServiceQualityDrtEstimator(csv.toString(), stopNetwork());
		DrtRoute route = new DrtRoute(Id.createLinkId("to"), Id.createLinkId("from"));

		assertThatThrownBy(() -> estimator.estimate(route, OptionalTime.defined(28800)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No DRT service quality estimate");
	}

	private static DrtStopNetwork stopNetwork() {
		DrtStopFacility origin = new DrtStopFacilityImpl(Id.create("o", DrtStopFacility.class), Id.createLinkId("from"),
			new Coord(0, 0), new AttributesImpl());
		DrtStopFacility destination = new DrtStopFacilityImpl(Id.create("d", DrtStopFacility.class), Id.createLinkId("to"),
			new Coord(1, 1), new AttributesImpl());
		return () -> ImmutableMap.of(origin.getId(), origin, destination.getId(), destination);
	}
}
