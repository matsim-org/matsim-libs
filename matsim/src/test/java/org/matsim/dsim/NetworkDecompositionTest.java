package org.matsim.dsim;

import edu.metis.Metis;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NetworkDecompositionTest {

	/**
	 * Real networks have gaps in the node id indices, links in both directions, parallel links and self-loops.
	 * None of these must end up in the graph passed to METIS.
	 */
	@Test
	void metisWithRealisticNetwork() {

		assumeTrue(Metis.isAvailable(), "METIS library not available");

		int n = 20;
		int parts = 4;

		Network network = NetworkUtils.createNetwork();
		Node[][] grid = new Node[n][n];
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				// create unused ids, so that node id indices are not consecutive
				Id.createNodeId("gap-" + x + "-" + y);
				grid[x][y] = NetworkUtils.createAndAddNode(network, Id.createNodeId("n-" + x + "-" + y), new Coord(x * 100, y * 100));
			}
		}

		int links = 0;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				if (x < n - 1) {
					addLink(network, links++, grid[x][y], grid[x + 1][y]);
					addLink(network, links++, grid[x + 1][y], grid[x][y]);
					// parallel link
					addLink(network, links++, grid[x][y], grid[x + 1][y]);
				}
				if (y < n - 1) {
					addLink(network, links++, grid[x][y], grid[x][y + 1]);
					addLink(network, links++, grid[x][y + 1], grid[x][y]);
				}
			}
		}
		// self-loop
		addLink(network, links, grid[0][0], grid[0][0]);

		NetworkDecomposition.metis(network, PopulationUtils.createPopulation(ConfigUtils.createConfig()), parts);

		int[] sizes = new int[parts];
		for (Node node : network.getNodes().values()) {
			int p = (int) node.getAttributes().getAttribute(NetworkDecomposition.PARTITION_ATTR_KEY);
			assertThat(p).isBetween(0, parts - 1);
			sizes[p]++;
		}

		assertThat(Arrays.stream(sizes).max().orElseThrow())
			.as("Partition sizes %s", Arrays.toString(sizes))
			.isLessThanOrEqualTo((int) (1.05 * n * n / parts));

		assertThat(network.getLinks().values())
			.allMatch(l -> l.getAttributes().getAttribute(NetworkDecomposition.PARTITION_ATTR_KEY) != null);
	}

	@Test
	void scaleWeights() {

		int[] weights = {Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2, 1, 0};
		NetworkDecomposition.scaleWeights(weights);

		assertThat(Arrays.stream(weights).asLongStream().sum()).isLessThanOrEqualTo((1L << 28) + weights.length);
		assertThat(Arrays.stream(weights).min().orElseThrow()).isGreaterThanOrEqualTo(1);
		assertThat(weights[0]).isEqualTo(weights[1]);
	}

	private static void addLink(Network network, int id, Node from, Node to) {
		NetworkUtils.createAndAddLink(network, Id.createLinkId("l" + id), from, to, 100, 10, 1000, 1);
	}
}
