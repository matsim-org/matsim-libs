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

package org.matsim.contrib.drt.optimizer.insertion;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DetourTimeEstimatorTest {

	@Test
	public void freeSpeedZonalTimeEstimator_fromLinkToLinkSame() {
		var link = new FakeLink(null);
		var estimator = DetourTimeEstimator.createMatrixBasedEstimator(1, null, null);
		Assertions.assertThat(estimator.estimateTime(link, link, 345)).isZero();
	}

	@Test
	public void freeSpeedZonalTimeEstimator_fromLinkToLinkDifferent() {
		var linkA = new FakeLink(null, null, new FakeNode(null));
		var linkB = new FakeLink(null, new FakeNode(null), null);

		TravelTimeMatrix ttMatrix = mock(TravelTimeMatrix.class);
		when(ttMatrix.getTravelTime(eq(linkA.getToNode()), eq(linkB.getFromNode()), eq(345. + 2.))).thenReturn(1234);

		var estimator = DetourTimeEstimator.createMatrixBasedEstimator(1.5, ttMatrix, new QSimFreeSpeedTravelTime(1));
		double expectedTT = 2 //first link TT
				+ 1234 // TT between nodes
				+ linkB.getLength() / linkB.getFreespeed();// last link TT
		double adjustedTT = expectedTT / 1.5;// using speed factor
		Assertions.assertThat(estimator.estimateTime(linkA, linkB, 345)).isEqualTo(adjustedTT);
	}
}
