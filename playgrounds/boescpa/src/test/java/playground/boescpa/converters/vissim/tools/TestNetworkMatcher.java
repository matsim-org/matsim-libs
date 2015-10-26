/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.vissim.tools;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;

import playground.boescpa.converters.vissim.ConvEvents;

/**
 * Tests the methods of the class DefaultNetworkMatcher.
 *
 * @author boescpa
 */
public class TestNetworkMatcher {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private ConvEvents.BaseGridCreator baseGridCreator;
	private ConvEvents.NetworkMapper defaultNetworkMapper;

	@Before
	public void prepare() {
		BaseGridCreator.setGridcellsize(100);

		this.baseGridCreator = new BaseGridCreator() {
			@Override
			protected final Long[] boundingBoxOfZones(String path2ZonesFile) {
				Long[] boundings = {0l, 201l, 0l, 200l};
				return boundings;
			}
		};

		this.defaultNetworkMapper = new AbstractNetworkMapper() {
			@Override
			protected Network providePreparedNetwork(String path2Network, String path2VissimZoneShp) {
				Network network = NetworkUtils.createNetwork();
				NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);
				network.addNode(networkFactory.createNode(Id.create(1, Node.class), new Coord((double) 10, (double) 30)));
				network.addNode(networkFactory.createNode(Id.create(2, Node.class), new Coord((double) 110, (double) 130)));
				network.addLink(networkFactory.createLink(Id.create(101, Link.class),
						network.getNodes().get(Id.create(1, Node.class)), network.getNodes().get(Id.create(2, Node.class))));
				network.addLink(networkFactory.createLink(Id.create(102, Link.class),
						network.getNodes().get(Id.create(2, Node.class)), network.getNodes().get(Id.create(1, Node.class))));
				return network;
			}

		};
	}

	@Test
	public void testMapMsNetwork() {
		Network mutualBase = baseGridCreator.createMutualBaseGrid("");
		HashMap<Id<Link>, Id<Node>[]> msKeys = defaultNetworkMapper.mapNetwork("", mutualBase, "");
		Id<Node>[] keysLink101 = msKeys.get(Id.create(101l, Link.class));
		Id<Node>[] keysLink102 = msKeys.get(Id.create(102l, Link.class));
		Assert.assertEquals(keysLink101.length, 3);
		Assert.assertEquals(keysLink101[0], Id.create(1l, Node.class));
		Assert.assertEquals(keysLink101[1], Id.create(5l, Node.class));
		Assert.assertEquals(keysLink101[2], Id.create(6l, Node.class));
		Assert.assertEquals(keysLink102.length, 3);
		Assert.assertEquals(keysLink102[0], Id.create(6l, Node.class));
		Assert.assertEquals(keysLink102[1], Id.create(5l, Node.class));
		Assert.assertEquals(keysLink102[2], Id.create(1l, Node.class));
	}
}
