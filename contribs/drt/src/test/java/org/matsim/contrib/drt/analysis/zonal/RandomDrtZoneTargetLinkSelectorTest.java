/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.fakes.FakeLink;
import org.mockito.ArgumentCaptor;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RandomDrtZoneTargetLinkSelectorTest {

	private final Link link0 = new FakeLink(Id.createLinkId("0"));
	private final Link link1 = new FakeLink(Id.createLinkId("1"));
	private final Link link2 = new FakeLink(Id.createLinkId("2"));
	private final Link link3 = new FakeLink(Id.createLinkId("3"));

	@Test
	void testSelectTargetLink_fourLinks() {
		DrtZone zone = DrtZone.createDummyZone("zone", List.of(link0, link1, link2, link3), null);

		//fake random sequence
		IntUnaryOperator random = mock(IntUnaryOperator.class);
		ArgumentCaptor<Integer> boundCaptor = ArgumentCaptor.forClass(int.class);
		when(random.applyAsInt(boundCaptor.capture())).thenReturn(0, 3, 1, 2);

		//test selected target links
		RandomDrtZoneTargetLinkSelector selector = new RandomDrtZoneTargetLinkSelector(random);
		assertThat(selector.selectTargetLink(zone)).isEqualTo(link0);
		assertThat(selector.selectTargetLink(zone)).isEqualTo(link3);
		assertThat(selector.selectTargetLink(zone)).isEqualTo(link1);
		assertThat(selector.selectTargetLink(zone)).isEqualTo(link2);

		//check if correct values were passed to Random as the nextInt() bounds (== link count)
		assertThat(boundCaptor.getAllValues()).containsExactly(4, 4, 4, 4);
	}
}
