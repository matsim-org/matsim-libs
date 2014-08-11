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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

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
}
