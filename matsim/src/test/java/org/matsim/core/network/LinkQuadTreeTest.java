
/* *********************************************************************** *
 * project: org.matsim.*
 * LinkQuadTreeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

/**
 * @author mrieser / senozon
 */
public class LinkQuadTreeTest {

	@Test
	void testGetNearest() {

		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 2000, 2000);
		Link foo = createLink(s, 100, 200, 800, 500);
		Link bar = createLink(s, 400, 300, 500, 400);
		Link fbr = createLink(s, 800, 1400, 1400, 800);
		Link a = createLink(s, 1100, 1100, 1200, 1200);
		Link b = createLink(s, 1100, 1100, 1200, 1100);
		Link c = createLink(s, 1200, 1200, 1200, 1100);
		qt.put(foo);
		qt.put(bar);
		qt.put(fbr);
		qt.put(a);
		qt.put(b);
		qt.put(c);

		Assertions.assertEquals(foo, qt.getNearest(200, 200));
		Assertions.assertEquals(foo, qt.getNearest(300, 300));
		Assertions.assertEquals(bar, qt.getNearest(390, 300));
		Assertions.assertEquals(fbr, qt.getNearest(1000, 1100));
		Assertions.assertEquals(foo, qt.getNearest(-50, -50));
		Assertions.assertEquals(a, qt.getNearest(1105, 1104));
		Assertions.assertEquals(a, qt.getNearest(1105, 1103));
		Assertions.assertEquals(b, qt.getNearest(1105, 1102));
		Assertions.assertEquals(b, qt.getNearest(1105, 1101));
		Assertions.assertEquals(c, qt.getNearest(1205, 1101));
	}

	@Test
	void testGetDisk() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 2000, 2000);
		Link foo = createLink(s, 100, 200, 800, 500);
		Link bar = createLink(s, 400, 300, 500, 400);
		Link fbr = createLink(s, 800, 1400, 1400, 800);
		Link a = createLink(s, 1100, 1100, 1200, 1200);
		Link b = createLink(s, 1100, 1100, 1200, 1100);
		Link c = createLink(s, 1200, 1200, 1200, 1100);
		qt.put(foo);
		qt.put(bar);
		qt.put(fbr);
		qt.put(a);
		qt.put(b);
		qt.put(c);

		Assertions.assertEquals(List.of(foo, bar), qt.getDisk(400, 200, 250));
		Assertions.assertEquals(List.of(a, b, c), qt.getDisk(1200, 1200, 100));
		Assertions.assertEquals(List.of(fbr, a, b, c), qt.getDisk(1200, 1200, 150));
	}

	@Test
	void testGetDisk_onBorder() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		LinkQuadTree qt = new LinkQuadTree(0, 0, 2000, 2000);
		Link foo = createLink(s, 100, 100, 200, 100);
		qt.put(foo);

		Assertions.assertEquals(List.of(foo), qt.getDisk(100, 0, 100));
		Assertions.assertEquals(List.of(), qt.getDisk(100, 0, 99));
		Assertions.assertEquals(List.of(foo), qt.getDisk(100, 100, 0));

		Assertions.assertEquals(List.of(foo), qt.getDisk(200, 100, 0));
		Assertions.assertEquals(List.of(foo), qt.getDisk(200, 0, 100));
		Assertions.assertEquals(List.of(), qt.getDisk(200, 0, 99));

		Assertions.assertEquals(List.of(foo), qt.getDisk(150, 50, 50));
		Assertions.assertEquals(List.of(), qt.getDisk(150, 50, 49));
	}

	@Test
	void testGetNearest_longNear_smallFarAway() {

		/*
		 * Test the following constellation:
		 *
		 *                 (1)---(2)
		 *    (3)---------------------------------(4)
		 *
		 *                     X
		 *
		 *
		 *
		 * nodes 1 and 2 are closer to X, but link 3-4 is actually closer
		 *
		 */

		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 2000, 2000);
		Link a = createLink(s, 500, 200, 700, 200);
		Link b = createLink(s, 100, 100, 900, 100);
		qt.put(a);
		qt.put(b);

		Assertions.assertEquals(b, qt.getNearest(600, 0));
		Assertions.assertEquals(a, qt.getNearest(600, 210));
		Assertions.assertEquals(b, qt.getNearest(300, 210)); // outside of segment (1)-(2), thus (3)-(4) is closer
		Assertions.assertEquals(a, qt.getNearest(400, 210)); // distance to (1) is smaller than to (3)-(4)
	}

	@Test
	void testPut_zeroLengthLink() {
		/*
		 * Test the following constellation:
		 *
		 * (1)         (2)
		 *    \       /
		 *     \     /
		 *      \   /
		 *      <(3,4)>
		 *      /   \
		 *     /     \
		 *    /       \
		 * (5)         (6)
		 *
		 *
		 * node 1: 0/1000
		 * node 2: 1000/1000
		 * node 3, 4: 400, 400
		 * node 5: 0/0
		 * node 6: 1000/0
		 *
		 * with links in between nodes 3 and 4 as well
		 */

		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 1000, 1000);
		Link l13 = createLink(s, 0, 1000, 400, 400);
		Link l23 = createLink(s, 1000, 1000, 400, 400);
		Link l53 = createLink(s, 0, 0, 400, 400);
		Link l63 = createLink(s, 1000, 0, 400, 400);
		Link l43 = createLink(s, 400, 400, 400, 400);
		Link l34 = createLink(s, 400, 400, 400, 400);
		qt.put(l13);
		qt.put(l23);
		qt.put(l53);
		qt.put(l63);
		qt.put(l43);
		qt.put(l34);

		// mostly check that there is no exception like StackOverflowError
		Assertions.assertEquals(l13, qt.getNearest(100, 800));
	}

	@Test
	void testPut_zeroLengthLink_negativeCoords() {
		/* Same as test above, but with negative coords
		 */

		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, -1000, -1000);
		Link l13 = createLink(s, 0, -1000, -400, -400);
		Link l23 = createLink(s, -1000, -1000, -400, -400);
		Link l53 = createLink(s, 0, 0, -400, -400);
		Link l63 = createLink(s, -1000, 0, -400, -400);
		Link l43 = createLink(s, -400, -400, -400, -400);
		Link l34 = createLink(s, -400, -400, -400, -400);
		qt.put(l13);
		qt.put(l23);
		qt.put(l53);
		qt.put(l63);
		qt.put(l43);
		qt.put(l34);

		// mostly check that there is no exception like StackOverflowError
		Assertions.assertEquals(l13, qt.getNearest(-100, -800));
	}

	@Test
	void testRemove() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 1000, 1000);
		Link l13 = createLink(s, 0, 1000, 400, 400);
		Link l23 = createLink(s, 1000, 1000, 400, 400);
		Link l53 = createLink(s, 0, 0, 400, 400);
		Link l63 = createLink(s, 1000, 0, 400, 400);
		qt.put(l13);
		qt.put(l23);
		qt.put(l53);
		qt.put(l63);

		Assertions.assertEquals(l13, qt.getNearest(100, 800));

		qt.remove(l13);
		Assertions.assertEquals(l23, qt.getNearest(100, 800));
	}

	/**
	 * Test for MATSIM-687: links not stored in top-node are not removed
	 */
	@Test
	void testRemove_inSubNode() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 1000, 1000);
		Link lInTop1 = createLink(s, 400, 400, 600, 600);
		Link lInChildSW1 = createLink(s, 0, 0, 400, 400);
		qt.put(lInTop1);
		qt.put(lInChildSW1);

		Assertions.assertEquals(lInChildSW1, qt.getNearest(100, 80));

		qt.remove(lInChildSW1);
		Assertions.assertEquals(lInTop1, qt.getNearest(100, 80));
	}

	private Link createLink(Scenario s, double fromX, double fromY, double toX, double toY) {
		NetworkFactory nf = s.getNetwork().getFactory();
		Coord fc = new Coord(fromX, fromY);
		Coord tc = new Coord(toX, toY);
		return nf.createLink(
			Id.create(fc.toString() + "-" + tc.toString(), Link.class),
			nf.createNode(Id.create(fc.toString(), Node.class), fc),
			nf.createNode(Id.create(tc.toString(), Node.class), tc)
		);
	}

}
