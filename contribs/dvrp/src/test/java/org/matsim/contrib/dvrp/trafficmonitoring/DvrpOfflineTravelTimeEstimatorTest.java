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

package org.matsim.contrib.dvrp.trafficmonitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpOfflineTravelTimeEstimatorTest {
	private final Network network = NetworkUtils.createNetwork();
    private final Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord());
	private final Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord());
	private final Link linkAB = NetworkUtils.createAndAddLink(network, Id.createLinkId("A_B"), nodeA, nodeB, 100, 10,
			10, 1);

	private final TravelTime initialTT = (link, time, person, vehicle) -> {
		Preconditions.checkArgument(link == linkAB);
		if (time < 100) {
			return 1; // bin 0
		} else if (time < 200) {
			return 2; // bin 1
		} else {
			return 3; // bin 2
		}
	};

	//bins: [-inf, 100), [100, 200), [200, +inf)
	private final TimeDiscretizer timeDiscretizer = new TimeDiscretizer(200, 100);

	@Test
	void getLinkTravelTime_timeAreCorrectlyBinned() {
		var estimator = new DvrpOfflineTravelTimeEstimator(initialTT, null, network, timeDiscretizer, 0.25, null, ";");

		//bin 0
		assertThat(linkTravelTime(estimator, linkAB, -1)).isEqualTo(1);
		assertThat(linkTravelTime(estimator, linkAB, 0)).isEqualTo(1);
		assertThat(linkTravelTime(estimator, linkAB, 99)).isEqualTo(1);

		//bin 1
		assertThat(linkTravelTime(estimator, linkAB, 100)).isEqualTo(2);
		assertThat(linkTravelTime(estimator, linkAB, 199)).isEqualTo(2);

		//bin 2
		assertThat(linkTravelTime(estimator, linkAB, 200)).isEqualTo(3);
		assertThat(linkTravelTime(estimator, linkAB, 99999)).isEqualTo(3);
	}

	@Test
	void getLinkTravelTime_exponentialAveragingOverIterations() {
		double alpha = 0.25;

		//observed TTs for each time bin
		int observedTT_0 = 10;
		int observedTT_1 = 20;
		int observedTT_2 = 30;

		TravelTime observedTT = (link, time, person, vehicle) -> {
			Preconditions.checkArgument(link == linkAB);
			if (time < 100) {
				return observedTT_0;
			} else if (time < 200) {
				return observedTT_1;
			} else {
				return observedTT_2;
			}
		};

		var estimator = new DvrpOfflineTravelTimeEstimator(initialTT, observedTT, network, timeDiscretizer, alpha,
				null,";");

		//expected TTs for each time bin
		double expectedTT_0 = 1;
		double expectedTT_1 = 2;
		double expectedTT_2 = 3;

		//run iterations 0..10
		for (int i = 0; i < 10; i++) {
			//assert TTs during simulation
			assertThat(linkTravelTime(estimator, linkAB, 0)).isEqualTo(expectedTT_0);
			assertThat(linkTravelTime(estimator, linkAB, 100)).isEqualTo(expectedTT_1);
			assertThat(linkTravelTime(estimator, linkAB, 200)).isEqualTo(expectedTT_2);

			//update TTs using observed TTs
			estimator.notifyMobsimBeforeCleanup(null);

			//update expected TTs (for next iteration)
			expectedTT_0 = (1 - alpha) * expectedTT_0 + alpha * observedTT_0;
			expectedTT_1 = (1 - alpha) * expectedTT_1 + alpha * observedTT_1;
			expectedTT_2 = (1 - alpha) * expectedTT_2 + alpha * observedTT_2;
		}
	}

	@Test
	void getLinkTravelTime_linkOutsideNetwork_fail() {
		var linkOutsideNetwork = new FakeLink(Id.createLinkId("some-link"));
		var estimator = new DvrpOfflineTravelTimeEstimator(initialTT, null, network, timeDiscretizer,
				0.25, null, ";");

		assertThatThrownBy(() -> linkTravelTime(estimator, linkOutsideNetwork, 0)).isExactlyInstanceOf(
				NullPointerException.class)
				.hasMessage("Link (%s) does not belong to network. No travel time data.", linkOutsideNetwork.getId());
	}

	private double linkTravelTime(DvrpTravelTimeEstimator estimator, Link link, double startTime) {
		return estimator.getLinkTravelTime(link, startTime, null, null);
	}
}
