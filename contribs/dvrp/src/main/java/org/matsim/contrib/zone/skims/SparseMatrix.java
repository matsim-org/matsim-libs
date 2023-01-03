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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
final class SparseMatrix {
	//Range of unsigned short: 0-65535 (18:12:15)
	//In case 18 hours is not enough, we can reduce the resolution from seconds to tens of seconds
	private static final int MAX_UNSIGNED_SHORT = Short.MAX_VALUE - Short.MIN_VALUE;

	record NodeAndTime(int nodeIdx, double time) {
	}

	private static final class Bucket {
		private final int[] nodeIndices; // sorted for binary search
		private final short[] values; // aligned with nodeIndices (using 'short' as in Matrix)

		private Bucket(List<NodeAndTime> nodeAndTimes) {
			var nodeAndTimeArray = nodeAndTimes.toArray(new NodeAndTime[0]);
			Arrays.sort(nodeAndTimeArray, Comparator.comparingInt(nodeAndTime -> nodeAndTime.nodeIdx));

			this.nodeIndices = new int[nodeAndTimeArray.length];
			this.values = new short[nodeAndTimeArray.length];

			for (int i = 0; i < nodeAndTimeArray.length; i++) {
				var e = nodeAndTimeArray[i];
				Preconditions.checkArgument(Double.isFinite(e.time) && e.time >= 0 && e.time < MAX_UNSIGNED_SHORT);
				nodeIndices[i] = e.nodeIdx;
				values[i] = (short)e.time;
			}
		}

		private int get(int toNodeIndex) {
			return values[Arrays.binarySearch(nodeIndices, toNodeIndex)];
		}
	}

	static final class SparseRow {
		// 64-128 seemed to work best when micro-benchmarking the berlin network from the robo-taxi papers
		// for a bigger neighbourhood (maxNeighborDistance = 4000). On average, there is 620 neighbouring nodes
		// (min 2; max 1941).
		// Interestingly, the old version (not using buckets) was minimally worse (the tests included all nodes,
		// so in most of the cases from-to nodes were not neighbours and get filtered out by the bit set)
		private static final int MAX_AVERAGE_BUCKET_SIZE = 64;

		private final int mask;
		private final Bucket[] buckets;

		private final BitSet presentNodes = new BitSet();

		SparseRow(List<NodeAndTime> nodeAndTimes) {
			if (nodeAndTimes.isEmpty()) {
				mask = 0;
				buckets = null;
				return;
			}

			int roundedDownBucketCount = nodeAndTimes.size() / MAX_AVERAGE_BUCKET_SIZE;
			//must be power of 2 due to masking
			int bucketCount = Math.max(Integer.highestOneBit(2 * roundedDownBucketCount), 1);
			mask = bucketCount - 1;
			buckets = new Bucket[bucketCount];

			List<List<NodeAndTime>> nodeAndTimeLists = new ArrayList<>(bucketCount);

			for (int i = 0; i < bucketCount; i++) {
				nodeAndTimeLists.add(new ArrayList<>());
			}

			for (NodeAndTime e : nodeAndTimes) {
				int index = e.nodeIdx & mask;
				nodeAndTimeLists.get(index).add(e);
				presentNodes.set(e.nodeIdx);
			}

			for (int i = 0; i < bucketCount; i++) {
				buckets[i] = new Bucket(nodeAndTimeLists.get(i));
			}
		}

		int get(int toNodeIndex) {
			return presentNodes.get(toNodeIndex) ? buckets[toNodeIndex & mask].get(toNodeIndex) : -1; // value not present in the row
		}
	}

	private final SparseRow[] rows = new SparseRow[Id.getNumberOfIds(Node.class)];

	int get(int fromNode, int toNode) {
		var row = rows[fromNode];
		return row != null ? row.get(toNode) // get the value from the selected row
				: -1; // value not present if no row
	}

	int get(Node fromNode, Node toNode) {
		return get(fromNode.getId().index(), toNode.getId().index());
	}

	void setRow(Node fromNode, SparseRow row) {
		rows[fromNode.getId().index()] = row;
	}
}
