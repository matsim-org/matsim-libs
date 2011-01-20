/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class CalcBoundingBoxTest {

	@Test
	public void testRun() {
		Network net = NetworkImpl.createNetwork();
		NetworkFactory nf = net.getFactory();

		Node n0 = nf.createNode(new IdImpl(0), new CoordImpl(100, 500));
		Node n1 = nf.createNode(new IdImpl(1), new CoordImpl(300, 400));
		Node n2 = nf.createNode(new IdImpl(2), new CoordImpl(200, 700));
		Node n3 = nf.createNode(new IdImpl(3), new CoordImpl(600, 200));
		Node n4 = nf.createNode(new IdImpl(4), new CoordImpl(400, 300));

		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);

		net.addLink(nf.createLink(new IdImpl(0), n0, n1));
		net.addLink(nf.createLink(new IdImpl(1), n0, n2));
		net.addLink(nf.createLink(new IdImpl(2), n1, n0));
		net.addLink(nf.createLink(new IdImpl(3), n1, n3));
		net.addLink(nf.createLink(new IdImpl(4), n2, n1));
		net.addLink(nf.createLink(new IdImpl(5), n2, n3));
		net.addLink(nf.createLink(new IdImpl(6), n3, n2));
		net.addLink(nf.createLink(new IdImpl(7), n3, n4));
		net.addLink(nf.createLink(new IdImpl(8), n4, n0));
		net.addLink(nf.createLink(new IdImpl(9), n4, n2));

		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(net);
		Assert.assertEquals(100, bbox.getMinX(), 1e-9);
		Assert.assertEquals(600, bbox.getMaxX(), 1e-9);
		Assert.assertEquals(200, bbox.getMinY(), 1e-9);
		Assert.assertEquals(700, bbox.getMaxY(), 1e-9);
	}

	@Test
	public void testRun_allNegative() {
		Network net = NetworkImpl.createNetwork();
		NetworkFactory nf = net.getFactory();

		Node n0 = nf.createNode(new IdImpl(0), new CoordImpl(-100, -500));
		Node n1 = nf.createNode(new IdImpl(1), new CoordImpl(-300, -400));
		Node n2 = nf.createNode(new IdImpl(2), new CoordImpl(-200, -700));
		Node n3 = nf.createNode(new IdImpl(3), new CoordImpl(-600, -200));
		Node n4 = nf.createNode(new IdImpl(4), new CoordImpl(-400, -300));

		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);

		net.addLink(nf.createLink(new IdImpl(0), n0, n1));
		net.addLink(nf.createLink(new IdImpl(1), n0, n2));
		net.addLink(nf.createLink(new IdImpl(2), n1, n0));
		net.addLink(nf.createLink(new IdImpl(3), n1, n3));
		net.addLink(nf.createLink(new IdImpl(4), n2, n1));
		net.addLink(nf.createLink(new IdImpl(5), n2, n3));
		net.addLink(nf.createLink(new IdImpl(6), n3, n2));
		net.addLink(nf.createLink(new IdImpl(7), n3, n4));
		net.addLink(nf.createLink(new IdImpl(8), n4, n0));
		net.addLink(nf.createLink(new IdImpl(9), n4, n2));

		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(net);
		Assert.assertEquals(-600, bbox.getMinX(), 1e-9);
		Assert.assertEquals(-100, bbox.getMaxX(), 1e-9);
		Assert.assertEquals(-700, bbox.getMinY(), 1e-9);
		Assert.assertEquals(-200, bbox.getMaxY(), 1e-9);
	}
}
