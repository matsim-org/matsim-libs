/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.run.benchmark.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Generates a synthetic grid network for DRT benchmarks.
 *
 * @author Steffen Axer
 */
public class GridNetworkGenerator {

	private final int gridSize;
	private final double cellSize;
	private final double freespeed;
	private final double capacity;
	private final double lanes;

	public GridNetworkGenerator(int gridSize, double cellSize, double freespeed, double capacity, double lanes) {
		this.gridSize = gridSize;
		this.cellSize = cellSize;
		this.freespeed = freespeed;
		this.capacity = capacity;
		this.lanes = lanes;
	}

	public void generate(Scenario scenario) {
		Network network = scenario.getNetwork();

		for (int i = 0; i <= gridSize; i++) {
			for (int j = 0; j <= gridSize; j++) {
				Node node = network.getFactory().createNode(
					Id.createNodeId("n_" + i + "_" + j),
					new Coord(i * cellSize, j * cellSize));
				network.addNode(node);
			}
		}

		int linkId = 0;
		for (int i = 0; i < gridSize; i++) {
			for (int j = 0; j < gridSize; j++) {
				Node from = network.getNodes().get(Id.createNodeId("n_" + i + "_" + j));
				Node right = network.getNodes().get(Id.createNodeId("n_" + (i + 1) + "_" + j));
				Node down = network.getNodes().get(Id.createNodeId("n_" + i + "_" + (j + 1)));

				linkId = createBidirectional(network, from, right, linkId);
				linkId = createBidirectional(network, from, down, linkId);
			}
		}
	}

	private int createBidirectional(Network network, Node a, Node b, int id) {
		createLink(network, a, b, id++);
		createLink(network, b, a, id++);
		return id;
	}

	private void createLink(Network network, Node from, Node to, int id) {
		Link link = network.getFactory().createLink(Id.createLinkId("l_" + id), from, to);
		link.setLength(cellSize);
		link.setFreespeed(freespeed);
		link.setCapacity(capacity);
		link.setNumberOfLanes(lanes);
		network.addLink(link);
	}
}
