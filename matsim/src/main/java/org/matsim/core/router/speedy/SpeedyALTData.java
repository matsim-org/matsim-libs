package org.matsim.core.router.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.speedy.SpeedyGraph.LinkIterator;
import org.matsim.core.router.util.TravelDisutility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Preprocessed data for the ALT algorithm, see {@link SpeedyALT}.
 *
 * This class is thread-safe and can safely be used by multiple threads.
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
class SpeedyALTData {

	private final static Logger LOG = LogManager.getLogger(SpeedyALTData.class);

	final SpeedyGraph graph;
	private final int landmarksCount;
	private final TravelDisutility travelCosts;
	private final int[] landmarksNodeIndices;
	private final double[] nodesData; // for each node: 2 values per landmark
	private final int[] deadendData;
	private final double minTravelCostPerLength;

	public SpeedyALTData(SpeedyGraph graph, int landmarksCount, TravelDisutility travelCosts) {
		this.graph = graph;
		this.landmarksCount = landmarksCount;
		this.travelCosts = travelCosts;
		this.landmarksNodeIndices = new int[landmarksCount];
		this.nodesData = new double[graph.nodeCount * (landmarksCount * 2)];
		this.deadendData = new int[graph.nodeCount];

		this.findDeadEnds();
		this.calcLandmarks();
		this.minTravelCostPerLength = this.calcMinTravelCostPerLength();
	}

	private void findDeadEnds() {
		LOG.info("find dead ends...");

		LinkIterator outLI = this.graph.getOutLinkIterator();
		LinkIterator inLI = this.graph.getInLinkIterator();
		Arrays.fill(this.deadendData, -1);
		Map<Integer, Integer> mergedDeadends = new HashMap<>();

		for (int nodeIdx = 0; nodeIdx < this.graph.nodeCount; nodeIdx++) {
			Node node = this.graph.getNode(nodeIdx);
			if (node == null) continue; // not all indices might be in use

			if (this.deadendData[nodeIdx] >= 0) continue; // already detected as part of dead-end

			int nIdx = nodeIdx;
			int otherNodeIndex = checkNodeForDeadend(this.deadendData, mergedDeadends, nIdx, nodeIdx, outLI, inLI);
			while (otherNodeIndex >= 0) {
				this.deadendData[nIdx] = nodeIdx;
				nIdx = otherNodeIndex;
				otherNodeIndex = checkNodeForDeadend(this.deadendData, mergedDeadends, nIdx, nodeIdx, outLI, inLI);
			}
		}
		Map<Integer, Integer> mergers = new HashMap<>();
		mergedDeadends.forEach((fromIdx, toIdx) -> {
			int finalToIdx = toIdx;
			Integer newToIdx = mergedDeadends.get(toIdx);
			while (newToIdx != null && newToIdx != finalToIdx) {
				finalToIdx = newToIdx;
				newToIdx = mergedDeadends.get(finalToIdx);
			}
			mergers.put(fromIdx, finalToIdx);
		});
		for (int nodeIdx = 0; nodeIdx < this.graph.nodeCount; nodeIdx++) {
			int deadend = this.deadendData[nodeIdx];
			if (deadend >= 0) {
				this.deadendData[nodeIdx] = mergers.getOrDefault(deadend, deadend);
			}
		}
	}

	private int checkNodeForDeadend(int[] deadends, Map<Integer, Integer> mergedDeadends, int nodeIdx, int currentDeadend, LinkIterator outLI, LinkIterator inLI) {
		int otherNodeIndex = -1;

		outLI.reset(nodeIdx);
		while (outLI.next()) {
			int toNodeIdx = outLI.getToNodeIndex();
			if (deadends[toNodeIdx] >= 0) continue;

			if (toNodeIdx != otherNodeIndex) {
				if (otherNodeIndex == -1) otherNodeIndex = toNodeIdx;
				else return -1; // there are more than one non-dead-end incident nodes
			}
		}

		inLI.reset(nodeIdx);
		while (inLI.next()) {
			int fromNodeIdx = inLI.getFromNodeIndex();
			if (deadends[fromNodeIdx] >= 0) continue;

			if (fromNodeIdx != otherNodeIndex) {
				if (otherNodeIndex == -1) otherNodeIndex = fromNodeIdx;
				else return -1; // there are more than one non-dead-end incident nodes
			}
		}

		outLI.reset(nodeIdx);
		while (outLI.next()) {
			int toNodeIdx = outLI.getToNodeIndex();
			int deadend = deadends[toNodeIdx];
			if (deadend >= 0) mergedDeadends.put(deadend, currentDeadend);
		}
		inLI.reset(nodeIdx);
		while (inLI.next()) {
			int fromNodeIdx = inLI.getFromNodeIndex();
			int deadend = deadends[fromNodeIdx];
			if (deadend >= 0) mergedDeadends.put(deadend, currentDeadend);
		}

		return otherNodeIndex;
	}

	private void calcLandmarks() {
		LOG.info("calculate landmarks...");
		Node firstNode = null;
		for (int i = 0; i < this.graph.nodeCount; i++) {
			firstNode = this.graph.getNode(i);
			if (firstNode != null) {
				break;
			}
		}
		if (firstNode == null) {
			LOG.warn("Network does not contain any nodes!");
			return;
		}

		Future<double[]>[] trees = new Future[this.landmarksCount * 2];
		ExecutorService executor = Executors.newFixedThreadPool(4);

		int firstLandmarkIndex = firstNode.getId().index();
		this.landmarksNodeIndices[0] = firstLandmarkIndex;
		trees[0] = executor.submit(() -> calculateTreeForward(firstLandmarkIndex));
		trees[1] = executor.submit(() -> calculateTreeBackward(firstLandmarkIndex));

		for (int i = 1; i < this.landmarksCount; i++) {
			int nextLandmark = calculateNextLandmark(i);
			this.landmarksNodeIndices[i] = nextLandmark;

			trees[i * 2] = executor.submit(() -> calculateTreeForward(nextLandmark));
			trees[i * 2 + 1] = executor.submit(() -> calculateTreeBackward(nextLandmark));
		}

		for (int i = 0; i < trees.length; i++) {
			try {
				double[] data = trees[i].get();
				setNodeData(data, i);
			} catch (InterruptedException | ExecutionException e) {
				LOG.error(e);
			}
		}
		executor.shutdown();
	}

	private double calcMinTravelCostPerLength() {
		LOG.info("calculate min travelcost...");
		double minCost = Double.POSITIVE_INFINITY;
		for (int linkIdx = 0; linkIdx < graph.linkCount; linkIdx++) {
			Link link = this.graph.getLink(linkIdx);
			if (link != null) {
				double cost = this.travelCosts.getLinkMinimumTravelDisutility(link) / link.getLength();
				if (cost < minCost) {
					minCost = cost;
				}
			}
		}
		return minCost;
	}

	private void setNodeData(double[] data, int offset) {
		int multiplier = this.landmarksCount * 2;
		for (int i = 0; i < this.graph.nodeCount; i++) {
			this.nodesData[i * multiplier + offset] = data[i];
		}
	}

	private int calculateNextLandmark(int existingCount) {
		double[] data = new double[this.graph.nodeCount];
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator outLI = this.graph.getOutLinkIterator();

		for (int i = 0; i < existingCount; i++) {
			data[this.landmarksNodeIndices[i]] = 0;
		}

		NodeMinHeap pq = new NodeMinHeap(this.graph.nodeCount, i -> data[i], (i, c) -> data[i] = c);
		for (int i = 0; i < existingCount; i++) {
			pq.insert(this.landmarksNodeIndices[i]);
		}

		int lastNodeIdx = -1;
		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			lastNodeIdx = nodeIdx;
			double currCost = data[nodeIdx];

			outLI.reset(nodeIdx);
			while (outLI.next()) {
				int toNode = outLI.getToNodeIndex();

				double newCost = currCost + 1;

				double oldCost = data[toNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(toNode, newCost);
					}
				} else {
					data[toNode] = newCost;
					pq.insert(toNode);
				}
			}
		}
		return lastNodeIdx;
	}

	private double[] calculateTreeForward(int node) {
		double[] data = new double[this.graph.nodeCount];
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator outLI = this.graph.getOutLinkIterator();

		data[node] = 0;

		NodeMinHeap pq = new NodeMinHeap(this.graph.nodeCount, i -> data[i], (i, c) -> data[i] = c);
		pq.insert(node);

		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			double currCost = data[nodeIdx];

			outLI.reset(nodeIdx);
			while (outLI.next()) {
				int toNode = outLI.getToNodeIndex();

				double newCost = currCost + this.travelCosts.getLinkMinimumTravelDisutility(this.graph.getLink(outLI.getLinkIndex()));

				double oldCost = data[toNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(toNode, newCost);
					}
				} else {
					data[toNode] = newCost;
					pq.insert(toNode);
				}
			}
		}

		if(graph.getTurnRestrictions().isPresent()) {
			consolidateColoredNodes(data);
		}

		return data;
	}

	private double[] calculateTreeBackward(int node) {
		double[] data = new double[this.graph.nodeCount];
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator inLI = this.graph.getInLinkIterator();

		data[node] = 0;

		NodeMinHeap pq = new NodeMinHeap(this.graph.nodeCount, i -> data[i], (i, c) -> data[i] = c);
		pq.insert(node);

		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			double currCost = data[nodeIdx];

			inLI.reset(nodeIdx);
			while (inLI.next()) {
				int fromNode = inLI.getFromNodeIndex();

				double newCost = currCost + this.travelCosts.getLinkMinimumTravelDisutility(this.graph.getLink(inLI.getLinkIndex()));

				double oldCost = data[fromNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(fromNode, newCost);
					}
				} else {
					data[fromNode] = newCost;
					pq.insert(fromNode);
				}
			}
		}

		if(graph.getTurnRestrictions().isPresent()) {
			consolidateColoredNodes(data);
		}

		return data;
	}

	private void consolidateColoredNodes(double[] data) {
		// update node values with the minimum of their colored copies, if any
		for (int i = 0; i < graph.nodeCount; i++) {
			Node uncoloredNode = graph.getNode(i);
			if (uncoloredNode != null) {

				// the index points to a node with a different index -> colored copy
				if (uncoloredNode.getId().index() != i) {
					int uncoloredIndex = uncoloredNode.getId().index();
					double uncoloredCost = data[uncoloredIndex];
					double coloredCost = data[i];

					if (Double.isFinite(uncoloredCost)) {
						if (coloredCost < uncoloredCost) {
							data[uncoloredIndex] = coloredCost;
						}
					} else {
						data[uncoloredIndex] = coloredCost;
					}
				}
			}
		}
	}

	int getNodeDeadend(int nodeIndex) {
		return this.deadendData[nodeIndex];
	}

	int getLandmarksCount() {
		return this.landmarksCount;
	}

	double getTravelCostFromLandmark(int nodeIndex, int landmarkIndex) {
		return this.nodesData[nodeIndex * (this.landmarksCount * 2) + 2 * landmarkIndex];
	}

	double getTravelCostToLandmark(int nodeIndex, int landmarkIndex) {
		return this.nodesData[nodeIndex * (this.landmarksCount * 2) + 2 * landmarkIndex + 1];
	}

	public double getMinTravelCostPerLength() {
		return this.minTravelCostPerLength;
	}
}
