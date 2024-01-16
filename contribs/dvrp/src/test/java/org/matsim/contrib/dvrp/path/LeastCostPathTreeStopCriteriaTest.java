/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.matsim.contrib.dvrp.path.LeastCostPathTreeStopCriteria.*;

import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.router.speedy.LeastCostPathTree.StopCriterion;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author Michal Maciejewski (michalm)
 */
public class LeastCostPathTreeStopCriteriaTest {

	@Test
	void testAnd() {
		StopCriterion scTrue = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> true;
		StopCriterion scFalse = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> false;

		// true && true  == true; otherwise false
		assertThat(and(scTrue, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(and(scTrue, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
		assertThat(and(scFalse, scTrue).stop(0, 0, 0, 0, 0)).isFalse();
		assertThat(and(scFalse, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
	}

	@Test
	void testOr() {
		StopCriterion scTrue = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> true;
		StopCriterion scFalse = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> false;

		// false || false == false; otherwise true
		assertThat(or(scTrue, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scTrue, scFalse).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scFalse, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scFalse, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
	}

	@Test
	void testMaxTravelTime() {
		StopCriterion sc = maxTravelTime(100);

		//TT is 100 - continue
		assertThat(sc.stop(0, 500, 0, 0, 400)).isFalse();

		//TT is 101 - stop
		assertThat(sc.stop(0, 501, 0, 0, 400)).isTrue();
	}

	@Test
	void testAllEndNodesReached_noEndNodes() {
		assertThatThrownBy(() -> allEndNodesReached(List.of())).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("At least one end node must be provided.");
	}

	@Test
	void testAllEndNodesReached_oneEndNode() {
		var endNode = new FakeNode(Id.createNodeId("end_node"));
		var otherNode = new FakeNode(Id.createNodeId("other_node"));
		StopCriterion sc = allEndNodesReached(List.of(endNode));

		//endNode not yet reached
		assertThat(sc.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode now reached
		assertThat(sc.stop(endNode.getId().index(), 0, 0, 0, 0)).isTrue();

		//endNode already reached
		assertThat(sc.stop(otherNode.getId().index(), 0, 0, 0, 0)).isTrue();
	}

	@Test
	void testAllEndNodesReached_twoEndNodes() {
		var endNode1 = new FakeNode(Id.createNodeId("end_node_1"));
		var endNode2 = new FakeNode(Id.createNodeId("end_node_2"));
		var otherNode = new FakeNode(Id.createNodeId("other_node"));
		StopCriterion sc = allEndNodesReached(List.of(endNode1, endNode2));

		//none end node yet reached
		assertThat(sc.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode1 now reached
		assertThat(sc.stop(endNode1.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode2 not yet reached
		assertThat(sc.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode2 now reached
		assertThat(sc.stop(endNode2.getId().index(), 0, 0, 0, 0)).isTrue();

		//both end nodes already reached
		assertThat(sc.stop(otherNode.getId().index(), 0, 0, 0, 0)).isTrue();
	}

	@Test
	void testLeastCostEndNodeReached_noEndNodes() {
		assertThatThrownBy(() -> new LeastCostEndNodeReached(List.of(), value -> 0)).isExactlyInstanceOf(
				IllegalArgumentException.class).hasMessage("At least one end node must be provided.");
	}

	@Test
	void testLeastCostEndNodeReached_oneEndNode() {
		var endNodeId = Id.createNodeId("end_node");
		var otherNodeId = Id.createNodeId("other_node");

		IntToDoubleFunction endNodeAdditionalCost = index -> {
			if (index == endNodeId.index()) {
				return 10;
			}
			throw new IllegalArgumentException("not an end node");
		};
		LeastCostEndNodeReached sc = new LeastCostEndNodeReached(List.of(new FakeNode(endNodeId)),
				endNodeAdditionalCost);

		//endNode not yet reached
		assertThat(sc.stop(otherNodeId.index(), 0, 0, 0, 0)).isFalse();
		assertThat(sc.getBestEndNodeIndex()).isEmpty();

		//endNode now reached
		assertThat(sc.stop(endNodeId.index(), 0, 0, 0, 0)).isTrue();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId.index());

		//endNode already reached
		assertThat(sc.stop(otherNodeId.index(), 0, 0, 0, 0)).isTrue();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId.index());
	}

	@Test
	void testLeastCostEndNodeReached_twoEndNodes_stopBeforeReachingTheOtherEndNode() {
		var endNodeId1 = Id.createNodeId("end_node");
		var endNodeId2 = Id.createNodeId("end_node_2");
		var otherNodeId = Id.createNodeId("other_node");

		IntToDoubleFunction endNodeAdditionalCost = Map.of(endNodeId1.index(), 3, endNodeId2.index(), 2)::get;
		LeastCostEndNodeReached sc = new LeastCostEndNodeReached(
				List.of(new FakeNode(endNodeId1), new FakeNode(endNodeId2)), endNodeAdditionalCost);

		//endNode1 reached
		//travelCost == 2, totalCost == 2 + 3 == 5
		assertThat(sc.stop(endNodeId1.index(), 0, 2, 0, 0)).isFalse();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId1.index());

		//stop - travelCost >= 5
		assertThat(sc.stop(otherNodeId.index(), 0, 5, 0, 0)).isTrue();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId1.index());
	}

	@Test
	void testLeastCostEndNodeReached_twoEndNodes_bothVisited_fartherEndNodeWithLowerTotalCost() {
		var endNodeId1 = Id.createNodeId("end_node");
		var endNodeId2 = Id.createNodeId("end_node_2");

		IntToDoubleFunction endNodeAdditionalCost = Map.of(endNodeId1.index(), 10, endNodeId2.index(), 1)::get;
		LeastCostEndNodeReached sc = new LeastCostEndNodeReached(
				List.of(new FakeNode(endNodeId1), new FakeNode(endNodeId2)), endNodeAdditionalCost);

		//endNode1 now reached
		//travelCost == 2, totalCost == 2 + 10 == 12
		assertThat(sc.stop(endNodeId1.index(), 0, 2, 0, 0)).isFalse();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId1.index());

		//endNode2 now reached
		//travelCost == 3, totalCost == 3 + 1 == 4
		assertThat(sc.stop(endNodeId2.index(), 0, 3, 0, 0)).isTrue();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId2.index());
	}

	@Test
	void testLeastCostEndNodeReached_twoEndNodes_noAdditionalCost_stopAfterVisitingFirstEndNode() {
		var endNodeId1 = Id.createNodeId("end_node");
		var endNodeId2 = Id.createNodeId("end_node_2");

		IntToDoubleFunction endNodeAdditionalCost = Map.of(endNodeId1.index(), 0, endNodeId2.index(), 0)::get;
		LeastCostEndNodeReached sc = new LeastCostEndNodeReached(
				List.of(new FakeNode(endNodeId1), new FakeNode(endNodeId2)), endNodeAdditionalCost);

		//endNode1 now reached
		//travelCost == 2, totalCost == 2 + 10 == 12
		assertThat(sc.stop(endNodeId1.index(), 0, 2, 0, 0)).isTrue();
		assertThat(sc.getBestEndNodeIndex()).hasValue(endNodeId1.index());
	}
}
