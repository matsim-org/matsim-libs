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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Provides tests for the SimpleAnmParser-class and the parseAndTransformAmNetwork-method of DefaultNetworkMatcher.
 *
 * @author boescpa
 */
public class TestSimpleAnmParser extends DefaultNetworkMatcher{

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private String anmFile = "";

	@Before
	public void prepare() {
		anmFile = utils.getClassInputDirectory() + "anmDummy.anm";
	}

	@Test
	public void testParser() {
		Network network = parseAndTransformAmNetwork(anmFile);

		// Test nodes:
		Assert.assertEquals(network.getNodes().size(), 2);
		Assert.assertEquals(network.getNodes().get(new IdImpl(1l)).getCoord().getX(), 1.1);
		Assert.assertEquals(network.getNodes().get(new IdImpl(2l)).getCoord().getY(), 2.2);

		// Test link:
		Assert.assertEquals(network.getLinks().size(), 1);
		Assert.assertEquals(network.getLinks().get(new IdImpl("1A")).getFromNode().getId().toString(),"1");
		Assert.assertEquals(network.getLinks().get(new IdImpl("1A")).getToNode().getId().toString(),"2");
	}
}
