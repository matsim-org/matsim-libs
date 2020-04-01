package org.matsim.contrib.socnetsim.utils;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;

public class QuadTreeRebuilderTest {
	@Test
	public void testGrowingQuadTree() {
		final QuadTreeRebuilder<Object> rebuilder = new QuadTreeRebuilder<>();

		Assert.assertEquals(
				"unexpected quadtree size",
				0,
				rebuilder.getQuadTree().size());

		rebuilder.put(new Coord(0, 0), new Object());

		Assert.assertEquals(
				"unexpected quadtree size",
				1,
				rebuilder.getQuadTree().size());

		rebuilder.put(new Coord(100, 100), new Object());

		Assert.assertEquals(
				"unexpected quadtree size",
				2,
				rebuilder.getQuadTree().size());

		Assert.assertEquals(
				"unexpected number of elements around origin",
				1,
				rebuilder.getQuadTree().getDisk(0, 0, 1).size());

		Assert.assertEquals(
				"unexpected number of elements far from origin",
				2,
				rebuilder.getQuadTree().getDisk(0, 0, 1000).size());
	}
}
