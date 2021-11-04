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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SparseMatrix {
	//Range of unsigned short: 0-65535 (18:12:15)
	//In case 18 hours is not enough, we can reduce the resolution from seconds to tens of seconds
	private static final int MAX_UNSIGNED_SHORT = Short.MAX_VALUE - Short.MIN_VALUE;

	public static class NodeAndTime {
		private final int nodeIdx;
		private final double time;

		public NodeAndTime(int nodeIdx, double time) {
			this.nodeIdx = nodeIdx;
			this.time = time;
		}
	}

	public static class SparseRow {
		private final int[] nodeIndices; // sorted for binary search
		private final short[] values; // aligned with nodeIndices (using 'short' as in Matrix)

		private final BitSet presentNodes = new BitSet();

		public SparseRow(List<NodeAndTime> nodeAndTimes) {
			var nodeAndTimeArray = nodeAndTimes.toArray(new NodeAndTime[0]);
			Arrays.sort(nodeAndTimeArray, Comparator.comparingInt(nodeAndTime -> nodeAndTime.nodeIdx));

			this.nodeIndices = new int[nodeAndTimeArray.length];
			this.values = new short[nodeAndTimeArray.length];

			for (int i = 0; i < nodeAndTimeArray.length; i++) {
				var e = nodeAndTimeArray[i];
				Preconditions.checkArgument(Double.isFinite(e.time) && e.time >= 0 && e.time < MAX_UNSIGNED_SHORT);
				nodeIndices[i] = e.nodeIdx;
				values[i] = (short)e.time;
				presentNodes.set(e.nodeIdx);
			}
		}

		public int get(int toNodeIndex) {
			return presentNodes.get(toNodeIndex) ?
					values[Arrays.binarySearch(nodeIndices, toNodeIndex)] :
					-1; // value not present in the row
		}
	}

	private final SparseRow[] rows = new SparseRow[Id.getNumberOfIds(Node.class)];

	public int get(Node fromNode, Node toNode) {
		var row = rows[fromNode.getId().index()];
		return row != null ? row.get(toNode.getId().index()) // get the value from the selected row
				: -1; // value not present if no row
	}

	public void setRow(Node fromNode, SparseRow row) {
		rows[fromNode.getId().index()] = row;
	}
}
