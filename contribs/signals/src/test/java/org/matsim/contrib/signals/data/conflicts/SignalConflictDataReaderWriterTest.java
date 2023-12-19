/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.data.conflicts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsReader;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsWriter;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 */
public class SignalConflictDataReaderWriterTest {

	private static final Logger LOG = LogManager.getLogger(SignalConflictDataReaderWriterTest.class);

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testReaderAndWriter() {
		LOG.info("create conflict data");
		ConflictData conflictData = createConflictDataForTestCase();

		LOG.info("write conflict data");
		ConflictingDirectionsWriter writer = new ConflictingDirectionsWriter(conflictData);
		String filename = this.testUtils.getOutputDirectory() + "signalConflictData.xml";
		writer.write(filename);

		LOG.info("read conflict data");
		ConflictData readConflictData = new ConflictDataImpl();
		ConflictingDirectionsReader reader = new ConflictingDirectionsReader(readConflictData);
		reader.readFile(filename);

		LOG.info("compare written and read conflict data");
		compare(conflictData, readConflictData);
	}

	private void compare(ConflictData conflictData1, ConflictData conflictData2) {
		Assertions.assertEquals(conflictData1.getConflictsPerNode().size(), conflictData2.getConflictsPerNode().size(), "not the same number of intersections");
		for (IntersectionDirections intersection1 : conflictData1.getConflictsPerSignalSystem().values()) {
			IntersectionDirections intersection2 = conflictData2.getConflictsPerSignalSystem().get(intersection1.getSignalSystemId());
			Assertions.assertNotNull(intersection2, "no conflict data exists for signal system " + intersection1.getSignalSystemId());
			Assertions.assertEquals(intersection1.getNodeId(), intersection2.getNodeId(), "not the same node, but the same signal system " + intersection1.getSignalSystemId());
			Assertions.assertEquals(intersection1.getDirections().size(), intersection2.getDirections().size(), "not the same number of direction at node " + intersection1.getNodeId());
			for (Direction dir1 : intersection1.getDirections().values()) {
				Direction dir2 = intersection2.getDirections().get(dir1.getId());
				Assertions.assertNotNull(dir2, "no direction exists for id " + dir1.getId());
				Assertions.assertEquals(dir1.getFromLink(), dir2.getFromLink(), "direction " + dir1.getId() + " has not the same from link");
				Assertions.assertEquals(dir1.getToLink(), dir2.getToLink(), "direction " + dir1.getId() + " has not the same to link");
				Assertions.assertEquals(dir1.getConflictingDirections().size(), dir2.getConflictingDirections().size(), "not the same number of conflicting directions for direction " + dir1.getId());
				Assertions.assertEquals(dir1.getDirectionsWithRightOfWay().size(), dir2.getDirectionsWithRightOfWay().size(), "not the same number of directions with right of way for direction " + dir1.getId());
				Assertions.assertEquals(dir1.getDirectionsWhichMustYield().size(), dir2.getDirectionsWhichMustYield().size(), "not the same number of directions which must yield for direction " + dir1.getId());
				Assertions.assertEquals(dir1.getNonConflictingDirections().size(), dir2.getNonConflictingDirections().size(), "not the same number of non-conflicting directions for direction " + dir1.getId());
				for (Id<Direction> conflDir1 : dir1.getConflictingDirections()) {
					Assertions.assertTrue(dir2.getConflictingDirections().contains(conflDir1), "direction " + conflDir1 + " is not a conflicting direction for " + dir1.getId());
				}
				for (Id<Direction> conflDir1 : dir1.getDirectionsWithRightOfWay()) {
					Assertions.assertTrue(dir2.getDirectionsWithRightOfWay().contains(conflDir1), "direction " + conflDir1 + " is not a direction with right of way for " + dir1.getId());
				}
				for (Id<Direction> conflDir1 : dir1.getDirectionsWhichMustYield()) {
					Assertions.assertTrue(dir2.getDirectionsWhichMustYield().contains(conflDir1), "direction " + conflDir1 + " is not a direction which must yield for " + dir1.getId());
				}
				for (Id<Direction> conflDir1 : dir1.getNonConflictingDirections()) {
					Assertions.assertTrue(dir2.getNonConflictingDirections().contains(conflDir1), "direction " + conflDir1 + " is not a non-conflicting direction for " + dir1.getId());
				}
			}
		}
	}

	/* test network:
	 *               ^
	 *               |
	 *               2
	 *               |
	 *               v
	 * < --- 1 --- > x < --- 3 --- > x < --- 4 --- >
	 */
	private ConflictData createConflictDataForTestCase() {

		Id<SignalSystem> signalSystemId1 = Id.create("sys1", SignalSystem.class);
		Id<Node> nodeId1 = Id.createNodeId("node1");
		Id<Link> linkId1 = Id.createLinkId("link1");
		Id<Link> linkId2 = Id.createLinkId("link2");
		Id<Link> linkId3 = Id.createLinkId("link3");
		Id<SignalSystem> signalSystemId2 = Id.create("sys2", SignalSystem.class);
		Id<Node> nodeId2 = Id.createNodeId("node2");
		Id<Link> linkId4 = Id.createLinkId("link4");
		Id<Direction> dirId13 = Id.create(linkId1 + "-" + linkId3, Direction.class);
		Id<Direction> dirId31 = Id.create(linkId3 + "-" + linkId1, Direction.class);
		Id<Direction> dirId12 = Id.create(linkId1 + "-" + linkId2, Direction.class);
		Id<Direction> dirId23 = Id.create(linkId2 + "-" + linkId3, Direction.class);
		Id<Direction> dirId34 = Id.create(linkId3 + "-" + linkId4, Direction.class);
		Id<Direction> dirId43 = Id.create(linkId4 + "-" + linkId3, Direction.class);

		ConflictData conflictData = new ConflictDataImpl();
		IntersectionDirections intersection1 = conflictData.getFactory().createConflictingDirectionsContainerForIntersection(signalSystemId1, nodeId1);
		conflictData.addConflictingDirectionsForIntersection(signalSystemId1, nodeId1, intersection1);

		Direction dir13 = conflictData.getFactory().createDirection(signalSystemId1, nodeId1, linkId1, linkId3, dirId13);
		intersection1.addDirection(dir13);
		dir13.addNonConflictingDirection(dirId31);
		dir13.addNonConflictingDirection(dirId12);
		dir13.addConflictingDirection(dirId23);

		Direction dir31 = conflictData.getFactory().createDirection(signalSystemId1, nodeId1, linkId3, linkId1, dirId31);
		intersection1.addDirection(dir31);
		dir31.addNonConflictingDirection(dirId13);
		dir31.addDirectionWhichMustYield(dirId12);
		dir31.addConflictingDirection(dirId23);

		Direction dir12 = conflictData.getFactory().createDirection(signalSystemId1, nodeId1, linkId1, linkId2, dirId12);
		intersection1.addDirection(dir12);
		dir12.addNonConflictingDirection(dirId13);
		dir12.addDirectionWithRightOfWay(dirId31);
		dir12.addConflictingDirection(dirId23);

		Direction dir23 = conflictData.getFactory().createDirection(signalSystemId1, nodeId1, linkId2, linkId3, dirId23);
		intersection1.addDirection(dir23);
		dir23.addConflictingDirection(dirId12);
		dir23.addConflictingDirection(dirId13);
		dir23.addConflictingDirection(dirId31);

		IntersectionDirections intersection2 = conflictData.getFactory().createConflictingDirectionsContainerForIntersection(signalSystemId2, nodeId2);
		conflictData.addConflictingDirectionsForIntersection(signalSystemId2, nodeId2, intersection2);

		Direction dir34 = conflictData.getFactory().createDirection(signalSystemId2, nodeId2, linkId3, linkId4, dirId34);
		intersection2.addDirection(dir34);
		dir34.addNonConflictingDirection(dirId43);

		Direction dir43 = conflictData.getFactory().createDirection(signalSystemId2, nodeId2, linkId4, linkId3, dirId43);
		intersection2.addDirection(dir43);
		dir43.addNonConflictingDirection(dirId43);

		return conflictData;
	}

}
