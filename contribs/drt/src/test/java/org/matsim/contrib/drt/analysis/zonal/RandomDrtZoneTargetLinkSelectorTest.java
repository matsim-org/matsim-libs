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

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystemImpl;
import org.matsim.core.network.NetworkUtils;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RandomDrtZoneTargetLinkSelectorTest {

	private static final Id<Link> LINK_ID_0 = Id.createLinkId("0");
	private static final Id<Link> LINK_ID_1 = Id.createLinkId("1");
	private static final Id<Link> LINK_ID_2 = Id.createLinkId("2");
	private static final Id<Link> LINK_ID_3 = Id.createLinkId("3");

	@Test
	void testSelectTargetLink_fourLinks() {
		Zone zone = ZoneImpl.createDummyZone(Id.create("zone", Zone.class), null);

		Network network = createNetwork();

		//fake random sequence
		IntUnaryOperator random = mock(IntUnaryOperator.class);
		ArgumentCaptor<Integer> boundCaptor = ArgumentCaptor.forClass(int.class);
		when(random.applyAsInt(boundCaptor.capture())).thenReturn(0, 3, 1, 2);

		//test selected target links
		RandomDrtZoneTargetLinkSelector selector = new RandomDrtZoneTargetLinkSelector(new ZoneSystemImpl(List.of(zone), coord -> Optional.of(zone), network), random);
		assertThat(selector.selectTargetLink(zone)).isEqualTo(network.getLinks().get(LINK_ID_0));
		assertThat(selector.selectTargetLink(zone)).isEqualTo(network.getLinks().get(LINK_ID_3));
		assertThat(selector.selectTargetLink(zone)).isEqualTo(network.getLinks().get(LINK_ID_1));
		assertThat(selector.selectTargetLink(zone)).isEqualTo(network.getLinks().get(LINK_ID_2));

		//check if correct values were passed to Random as the nextInt() bounds (== link count)
		assertThat(boundCaptor.getAllValues()).containsExactly(4, 4, 4, 4);
	}

	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(0, 1000));
		Node c = network.getFactory().createNode(Id.createNodeId("c"), new Coord(1000, 1000));
		Node d = network.getFactory().createNode(Id.createNodeId("d"), new Coord(1000, 0));
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);

		Link ab = network.getFactory().createLink(LINK_ID_0, a, b);
		Link bc = network.getFactory().createLink(LINK_ID_1, b, c);
		Link cd = network.getFactory().createLink(LINK_ID_2, c, d);
		Link da = network.getFactory().createLink(LINK_ID_3, d, a);
		network.addLink(ab);
		network.addLink(bc);
		network.addLink(cd);
		network.addLink(da);
		return network;
	}
}
