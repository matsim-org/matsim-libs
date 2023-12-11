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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * @author mrieser
 */
public class LanesBasedWidthCalculatorTest {

	@Test
	void testGetWidth_laneWidthNaN() {
		Network net = NetworkUtils.createNetwork();
        Node n1 = net.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node n2 = net.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		net.addNode(n1);
		net.addNode(n2);
		Link l1 = net.getFactory().createLink(Id.create("1", Link.class), n1, n2);
		l1.setNumberOfLanes(2.0);


		Assertions.assertEquals(3.75, net.getEffectiveLaneWidth(), 1e-10, "The default in the Network is set to a value that is possibly not conform to the default in network_v1.dtd");
		((Network)net).setEffectiveLaneWidth(1.0);
		double w = new LanesBasedWidthCalculator((Network) net, 1.0).getWidth(l1);
		Assertions.assertFalse(Double.isNaN(w));
		Assertions.assertEquals(2.0, w, 1e-10);
	}

}
