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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Provides tests for the SimpleAnmParser-class and the parseAndTransformAmNetwork-method of DefaultNetworkMatcher.
 *
 * @author boescpa
 */
public class TestSimpleAnmParser extends AmNetworkMapper {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Network network = null;

	@Before
	public void prepare() {
		String anmFile = utils.getClassInputDirectory() + "anmDummy.anm";
		this.network = providePreparedNetwork(anmFile,"");
	}

	@Test
	public void testParser() {
		// Test nodes:
		Assert.assertEquals(network.getNodes().size(), 2);
		Assert.assertEquals(network.getNodes().get(Id.create(1l, Node.class)).getCoord().getX(), 1.1);
		Assert.assertEquals(network.getNodes().get(Id.create(2l, Node.class)).getCoord().getY(), 2.2);

		// Test link:
		Assert.assertEquals(network.getLinks().size(), 1);
		Assert.assertEquals(network.getLinks().get(Id.create("1A", Link.class)).getFromNode().getId().toString(),"1");
		Assert.assertEquals(network.getLinks().get(Id.create("1A", Link.class)).getToNode().getId().toString(),"2");
		Assert.assertNull(network.getLinks().get(Id.create("2A", Link.class)));
	}
}
