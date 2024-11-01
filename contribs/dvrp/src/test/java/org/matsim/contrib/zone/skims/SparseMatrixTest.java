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

package org.matsim.contrib.zone.skims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.zone.skims.SparseMatrix.SparseRow;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.skims.SparseMatrix.NodeAndTime;
import org.matsim.testcases.fakes.FakeNode;

import com.google.common.collect.Lists;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SparseMatrixTest {

	private final Node nodeA = new FakeNode(Id.create("A", Node.class));
	private final Node nodeB = new FakeNode(Id.create("B", Node.class));
	private final Node nodeC = new FakeNode(Id.create("C", Node.class));
	private final List<Node> allNodes = List.of(nodeA, nodeB, nodeC);

	@Test
	void emptyMatrix() {
		var matrix = new SparseMatrix();
		for (Node from : allNodes) {
			for (Node to : allNodes) {
				assertThat(matrix.get(from, to)).isEqualTo(-1);
			}
		}
	}

	@Test
	void triangularMatrix() {
		var matrix = new SparseMatrix();
		//A -> A, B, C
		matrix.setRow(nodeA,
				new SparseRow(List.of(nodeAndTime(nodeA, 0), nodeAndTime(nodeB, 1), nodeAndTime(nodeC, 2))));
		//B -> B, C
		matrix.setRow(nodeB, new SparseRow(List.of(nodeAndTime(nodeB, 3), nodeAndTime(nodeC, 4))));
		//C -> C
		matrix.setRow(nodeC, new SparseRow(List.of(nodeAndTime(nodeC, 5))));

		assertThat(matrix.get(nodeA, nodeA)).isEqualTo(0);
		assertThat(matrix.get(nodeA, nodeB)).isEqualTo(1);
		assertThat(matrix.get(nodeA, nodeC)).isEqualTo(2);
		assertThat(matrix.get(nodeB, nodeA)).isEqualTo(-1);
		assertThat(matrix.get(nodeB, nodeB)).isEqualTo(3);
		assertThat(matrix.get(nodeB, nodeC)).isEqualTo(4);
		assertThat(matrix.get(nodeC, nodeA)).isEqualTo(-1);
		assertThat(matrix.get(nodeC, nodeB)).isEqualTo(-1);
		assertThat(matrix.get(nodeC, nodeC)).isEqualTo(5);
	}

	@Test
	void nodeAndTimeOrderNotImportant() {
		//A -> A, B, C
		var nodeAndTimes = List.of(nodeAndTime(nodeA, 0), nodeAndTime(nodeB, 1), nodeAndTime(nodeC, 2));

		for (boolean reversedOrder : List.of(false, true)) {
			var matrix = new SparseMatrix();
			matrix.setRow(nodeA, new SparseRow(reversedOrder ? Lists.reverse(nodeAndTimes) : nodeAndTimes));

			assertThat(matrix.get(nodeA, nodeA)).isEqualTo(0);
			assertThat(matrix.get(nodeA, nodeB)).isEqualTo(1);
			assertThat(matrix.get(nodeA, nodeC)).isEqualTo(2);
		}
	}

	private NodeAndTime nodeAndTime(Node node, double time) {
		return new NodeAndTime(node.getId().index(), time);
	}
}
