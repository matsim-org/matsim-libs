package org.matsim.contrib.socnetsim.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;

public class QuadTreeRebuilderTest {
	@Test
	void testGrowingQuadTree() {
		final QuadTreeRebuilder<Object> rebuilder = new QuadTreeRebuilder<>();

		Assertions.assertEquals(
				0,
				rebuilder.getQuadTree().size(),
				"unexpected quadtree size");

		rebuilder.put(new Coord(0, 0), new Object());

		Assertions.assertEquals(
				1,
				rebuilder.getQuadTree().size(),
				"unexpected quadtree size");

		rebuilder.put(new Coord(100, 100), new Object());

		Assertions.assertEquals(
				2,
				rebuilder.getQuadTree().size(),
				"unexpected quadtree size");

		Assertions.assertEquals(
				1,
				rebuilder.getQuadTree().getDisk(0, 0, 1).size(),
				"unexpected number of elements around origin");

		Assertions.assertEquals(
				2,
				rebuilder.getQuadTree().getDisk(0, 0, 1000).size(),
				"unexpected number of elements far from origin");
	}
}
