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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * @author mrieser
 */
public class CalcBoundingBoxTest {

	@Test
	void testRun() {
		Network net = NetworkUtils.createNetwork();
        NetworkFactory nf = net.getFactory();

		Node n0 = nf.createNode(Id.create(0, Node.class), new Coord((double) 100, (double) 500));
		Node n1 = nf.createNode(Id.create(1, Node.class), new Coord((double) 300, (double) 400));
		Node n2 = nf.createNode(Id.create(2, Node.class), new Coord((double) 200, (double) 700));
		Node n3 = nf.createNode(Id.create(3, Node.class), new Coord((double) 600, (double) 200));
		Node n4 = nf.createNode(Id.create(4, Node.class), new Coord((double) 400, (double) 300));

		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);

		net.addLink(nf.createLink(Id.create(0, Link.class), n0, n1));
		net.addLink(nf.createLink(Id.create(1, Link.class), n0, n2));
		net.addLink(nf.createLink(Id.create(2, Link.class), n1, n0));
		net.addLink(nf.createLink(Id.create(3, Link.class), n1, n3));
		net.addLink(nf.createLink(Id.create(4, Link.class), n2, n1));
		net.addLink(nf.createLink(Id.create(5, Link.class), n2, n3));
		net.addLink(nf.createLink(Id.create(6, Link.class), n3, n2));
		net.addLink(nf.createLink(Id.create(7, Link.class), n3, n4));
		net.addLink(nf.createLink(Id.create(8, Link.class), n4, n0));
		net.addLink(nf.createLink(Id.create(9, Link.class), n4, n2));

		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(net);
		Assertions.assertEquals(100, bbox.getMinX(), 1e-9);
		Assertions.assertEquals(600, bbox.getMaxX(), 1e-9);
		Assertions.assertEquals(200, bbox.getMinY(), 1e-9);
		Assertions.assertEquals(700, bbox.getMaxY(), 1e-9);
	}

	@Test
	void testRun_allNegative() {
        Network net = NetworkUtils.createNetwork();
        NetworkFactory nf = net.getFactory();

		final double x4 = -100;
		final double y4 = -500;
		Node n0 = nf.createNode(Id.create(0, Node.class), new Coord(x4, y4));
		final double x3 = -300;
		final double y3 = -400;
		Node n1 = nf.createNode(Id.create(1, Node.class), new Coord(x3, y3));
		final double x2 = -200;
		final double y2 = -700;
		Node n2 = nf.createNode(Id.create(2, Node.class), new Coord(x2, y2));
		final double x1 = -600;
		final double y1 = -200;
		Node n3 = nf.createNode(Id.create(3, Node.class), new Coord(x1, y1));
		final double x = -400;
		final double y = -300;
		Node n4 = nf.createNode(Id.create(4, Node.class), new Coord(x, y));

		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);

		net.addLink(nf.createLink(Id.create(0, Link.class), n0, n1));
		net.addLink(nf.createLink(Id.create(1, Link.class), n0, n2));
		net.addLink(nf.createLink(Id.create(2, Link.class), n1, n0));
		net.addLink(nf.createLink(Id.create(3, Link.class), n1, n3));
		net.addLink(nf.createLink(Id.create(4, Link.class), n2, n1));
		net.addLink(nf.createLink(Id.create(5, Link.class), n2, n3));
		net.addLink(nf.createLink(Id.create(6, Link.class), n3, n2));
		net.addLink(nf.createLink(Id.create(7, Link.class), n3, n4));
		net.addLink(nf.createLink(Id.create(8, Link.class), n4, n0));
		net.addLink(nf.createLink(Id.create(9, Link.class), n4, n2));

		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(net);
		Assertions.assertEquals(-600, bbox.getMinX(), 1e-9);
		Assertions.assertEquals(-100, bbox.getMaxX(), 1e-9);
		Assertions.assertEquals(-700, bbox.getMinY(), 1e-9);
		Assertions.assertEquals(-200, bbox.getMaxY(), 1e-9);
	}
}
