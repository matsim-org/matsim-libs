package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
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
	

	private Link createLink(Scenario s, double fromX, double fromY, double toX, double toY) {
		NetworkFactory nf = s.getNetwork().getFactory();
		Coord fc = s.createCoord(fromX, fromY);
		Coord tc = s.createCoord(toX, toY);
		return nf.createLink(
				s.createId(fc.toString() + "-" + tc.toString()), 
				nf.createNode(s.createId(fc.toString()), fc), 
				nf.createNode(s.createId(tc.toString()), tc) 
				);
	}
	

}
