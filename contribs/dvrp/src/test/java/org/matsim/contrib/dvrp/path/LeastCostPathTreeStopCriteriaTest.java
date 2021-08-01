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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.router.speedy.LeastCostPathTree.StopCriterion;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author Michal Maciejewski (michalm)
 */
public class LeastCostPathTreeStopCriteriaTest {

	@Test
	public void testAnd() {
		StopCriterion scTrue = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> true;
		StopCriterion scFalse = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> false;

		// true && true  == true; otherwise false
		assertThat(and(scTrue, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(and(scTrue, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
		assertThat(and(scFalse, scTrue).stop(0, 0, 0, 0, 0)).isFalse();
		assertThat(and(scFalse, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
	}

	@Test
	public void testOr() {
		StopCriterion scTrue = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> true;
		StopCriterion scFalse = (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> false;

		// false || false == false; otherwise true
		assertThat(or(scTrue, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scTrue, scFalse).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scFalse, scTrue).stop(0, 0, 0, 0, 0)).isTrue();
		assertThat(or(scFalse, scFalse).stop(0, 0, 0, 0, 0)).isFalse();
	}

	@Test
	public void testMaxTravelTime() {
		StopCriterion max100 = maxTravelTime(100);

		//TT is 100 - continue
		assertThat(max100.stop(0, 500, 0, 0, 400)).isFalse();

		//TT is 101 - stop
		assertThat(max100.stop(0, 501, 0, 0, 400)).isTrue();
	}

	@Test
	public void testAllEndNodesReached_noEndNodes() {
		assertThatThrownBy(() -> allEndNodesReached(List.of())).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("At least one end node must be provided.");
	}

	@Test
	public void testAllEndNodesReached_oneEndNode() {
		var endNode = new FakeNode(Id.createNodeId("end_node"));
		var otherNode = new FakeNode(Id.createNodeId("other_node"));
		StopCriterion noEndNodes = allEndNodesReached(List.of(endNode));

		//endNode not yet reached
		assertThat(noEndNodes.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode now reached
		assertThat(noEndNodes.stop(endNode.getId().index(), 0, 0, 0, 0)).isTrue();

		//endNode already reached
		assertThat(noEndNodes.stop(otherNode.getId().index(), 0, 0, 0, 0)).isTrue();
	}

	@Test
	public void testAllendNodesReached_twoEndNodes() {
		var endNode1 = new FakeNode(Id.createNodeId("end_node_1"));
		var endNode2 = new FakeNode(Id.createNodeId("end_node_2"));
		var otherNode = new FakeNode(Id.createNodeId("other_node"));
		StopCriterion noEndNodes = allEndNodesReached(List.of(endNode1, endNode2));

		//none end node yet reached
		assertThat(noEndNodes.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode1 now reached
		assertThat(noEndNodes.stop(endNode1.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode2 not yet reached
		assertThat(noEndNodes.stop(otherNode.getId().index(), 0, 0, 0, 0)).isFalse();

		//endNode2 now reached
		assertThat(noEndNodes.stop(endNode2.getId().index(), 0, 0, 0, 0)).isTrue();

		//both end nodes already reached
		assertThat(noEndNodes.stop(otherNode.getId().index(), 0, 0, 0, 0)).isTrue();
	}
}
