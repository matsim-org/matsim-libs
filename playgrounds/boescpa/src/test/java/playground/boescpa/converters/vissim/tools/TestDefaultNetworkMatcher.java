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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.HashMap;

/**
 * Tests the methods of the class DefaultNetworkMatcher.
 *
 * @author boescpa
 */
public class TestDefaultNetworkMatcher extends DefaultNetworkMatcher {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private ConvEvents2Anm.NetworkMatcher defaultNetworkMatcher1;
	private ConvEvents2Anm.NetworkMatcher defaultNetworkMatcher2;
	private ConvEvents2Anm.NetworkMatcher defaultNetworkMatcher3;

	@Before
	public void prepare() {
		DefaultNetworkMatcher.setGridcellsize(100);

		this.defaultNetworkMatcher1 = new DefaultNetworkMatcher() {
			@Override
			protected final Long[] boundingBoxOfZones(String path2ZonesFile) {
				Long[] boundings = {0l, 201l, 0l, 200l};
				return boundings;
			}
		};

		this.defaultNetworkMatcher2 = new DefaultNetworkMatcher();

		this.defaultNetworkMatcher3 = new DefaultNetworkMatcher() {
			@Override
			protected  final Network readAndCutMsNetwork(String path2MATSimNetwork, String path2VissimZoneShp) {
				Network network = NetworkUtils.createNetwork();
				NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);
				network.addNode(networkFactory.createNode(new IdImpl(1), new CoordImpl(10, 30)));
				network.addNode(networkFactory.createNode(new IdImpl(2), new CoordImpl(110, 130)));
				network.addLink(networkFactory.createLink(new IdImpl(101),
						network.getNodes().get(new IdImpl(1)), network.getNodes().get(new IdImpl(2))));
				network.addLink(networkFactory.createLink(new IdImpl(102),
						network.getNodes().get(new IdImpl(2)), network.getNodes().get(new IdImpl(1))));
				return network;
			}
		};


	}

	@Test
	public void testMatchNetworks() {
		Network network = defaultNetworkMatcher1.createMutualBaseGrid("");
		Assert.assertTrue(network.getNodes().size() == 12);
		Node minNode = network.getNodes().get(new IdImpl(1));
		Node maxNode = network.getNodes().get(new IdImpl(network.getNodes().size()));
		Assert.assertEquals(minNode.getCoord().getX(),0.0);
		Assert.assertEquals(minNode.getCoord().getY(),0.0);
		Assert.assertEquals(maxNode.getCoord().getX(),201.0, DefaultNetworkMatcher.getGridcellsize() - 1);
		Assert.assertEquals(maxNode.getCoord().getY(),200.0, DefaultNetworkMatcher.getGridcellsize() - 1);
	}

	@Test
	public void testBoundingBoxOfZones() {
		Network network = defaultNetworkMatcher2.createMutualBaseGrid(utils.getClassInputDirectory() + "TestDefaultNetworkMatcher_DummySHP.shp");
		Assert.assertTrue(network.getNodes().size() == 17094);
		Node minNode = network.getNodes().get(new IdImpl(1));
		Node maxNode = network.getNodes().get(new IdImpl(network.getNodes().size()));
		Assert.assertEquals(minNode.getCoord().getX(),675666.0);
		Assert.assertEquals(minNode.getCoord().getY(),242315.0);
		Assert.assertEquals(maxNode.getCoord().getX(),690908.0, DefaultNetworkMatcher.getGridcellsize() - 1);
		Assert.assertEquals(maxNode.getCoord().getY(),253296.0, DefaultNetworkMatcher.getGridcellsize() - 1);
	}

	@Test
	public void testMapMsNetwork() {
		Network mutualBase = defaultNetworkMatcher1.createMutualBaseGrid("");
		HashMap<Id, Id[]> msKeys = defaultNetworkMatcher3.mapMsNetwork("", mutualBase, "");
		Id[] keysLink101 = msKeys.get(new IdImpl(101l));
		Id[] keysLink102 = msKeys.get(new IdImpl(102l));
		Assert.assertEquals(keysLink101.length, 3);
		Assert.assertEquals(keysLink101[0], new IdImpl(1l));
		Assert.assertEquals(keysLink101[1], new IdImpl(5l));
		Assert.assertEquals(keysLink101[2], new IdImpl(6l));
		Assert.assertEquals(keysLink102.length, 3);
		Assert.assertEquals(keysLink102[0], new IdImpl(6l));
		Assert.assertEquals(keysLink102[1], new IdImpl(5l));
		Assert.assertEquals(keysLink102[2], new IdImpl(1l));
	}
}
