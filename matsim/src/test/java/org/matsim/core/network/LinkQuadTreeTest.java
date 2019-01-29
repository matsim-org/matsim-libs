package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / senozon
 */
public class LinkQuadTreeTest {

	@Test
	public void testGetNearest() {
		
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

		Assert.assertEquals(foo, qt.getNearest(200, 200));
		Assert.assertEquals(foo, qt.getNearest(300, 300));
		Assert.assertEquals(bar, qt.getNearest(390, 300));
		Assert.assertEquals(fbr, qt.getNearest(1000, 1100));
		Assert.assertEquals(foo, qt.getNearest(-50, -50));
		Assert.assertEquals(a, qt.getNearest(1105, 1104));
		Assert.assertEquals(a, qt.getNearest(1105, 1103));
		Assert.assertEquals(b, qt.getNearest(1105, 1102));
		Assert.assertEquals(b, qt.getNearest(1105, 1101));
		Assert.assertEquals(c, qt.getNearest(1205, 1101));
	}

	@Test
	public void testGetNearest_longNear_smallFarAway() {
		
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
		
		Assert.assertEquals(b, qt.getNearest(600, 0));
		Assert.assertEquals(a, qt.getNearest(600, 210));
		Assert.assertEquals(b, qt.getNearest(300, 210)); // outside of segment (1)-(2), thus (3)-(4) is closer 
		Assert.assertEquals(a, qt.getNearest(400, 210)); // distance to (1) is smaller than to (3)-(4) 
	}
	
	@Test
	public void testPut_zeroLengthLink() {
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
		Assert.assertEquals(l13, qt.getNearest(100, 800));
	}

	@Test
	public void testRemove() {
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

		Assert.assertEquals(l13, qt.getNearest(100, 800));

		qt.remove(l13);
		Assert.assertEquals(l23, qt.getNearest(100, 800));
	}

	/**
	 * Test for MATSIM-687: links not stored in top-node are not removed
	 */
	@Test
	public void testRemove_inSubNode() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		LinkQuadTree qt = new LinkQuadTree(0, 0, 1000, 1000);
		Link lInTop1 = createLink(s, 400, 400, 600, 600);
		Link lInChildSW1 = createLink(s, 0, 0, 400, 400);
		qt.put(lInTop1);
		qt.put(lInChildSW1);

		Assert.assertEquals(lInChildSW1, qt.getNearest(100, 80));

		qt.remove(lInChildSW1);
		Assert.assertEquals(lInTop1, qt.getNearest(100, 80));
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
