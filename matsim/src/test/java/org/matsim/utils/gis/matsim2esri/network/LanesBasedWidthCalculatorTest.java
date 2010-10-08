/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.matsim2esri.network;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class LanesBasedWidthCalculatorTest {

	@Test
	public void testGetWidth_laneWidthNaN() {
		Network net = NetworkImpl.createNetwork();
		Node n1 = net.getFactory().createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node n2 = net.getFactory().createNode(new IdImpl("2"), new CoordImpl(1000, 0));
		net.addNode(n1);
		net.addNode(n2);
		Link l1 = net.getFactory().createLink(new IdImpl("1"), n1.getId(), n2.getId());
		l1.setNumberOfLanes(2.0);


		Assert.assertTrue(Double.isNaN(net.getEffectiveLaneWidth()));
		double w = new LanesBasedWidthCalculator((NetworkImpl) net, 1.0).getWidth(l1);
		Assert.assertFalse(Double.isNaN(w));
		Assert.assertEquals(2.0, w, 1e-10);
	}

}
