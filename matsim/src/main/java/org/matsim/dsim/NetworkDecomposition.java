package org.matsim.dsim;

import edu.metis.Metis;
import edu.metis.MetisOptions;
import it.unimi.dsi.fastutil.ints.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NetworkDecomposition {

	public final static String PARTITION_ATTR_KEY = "partition";

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork(args[0]);
		Population population = PopulationUtils.readPopulation(args[1]);

		bisection(network, population, Integer.parseInt(args[2]));

		String out = args[0].replace(".xml", "-partitioned.xml");
		NetworkUtils.writeNetwork(network, out);
	}

	/**
	 * Partition the network into numParts parts, only if not partitioned already. Uses METIS if available.
	 */
	public static void partition(Network network, Population population, Config config, int numParts) {

		var dsimConfig = ConfigUtils.addOrGetModule(config, DSimConfigGroup.class);

		switch (dsimConfig.getPartitioning()) {
			case bisect -> bisection(network, population, numParts);
			case metis -> metis(network, population, numParts);
			// none means don't do anything
		}
	}

	/**
	 * Perform network decomposition using METIS.
	 */
	public static void metis(Network network, Population population, int numParts) {
		if (numParts < 1) {
			throw new IllegalArgumentException("Only positive number of parts allowed.");
		}

		// No lib needed for one partition
		if (numParts == 1) {
			bisection(network, population, 1);
			return;
		}

		if (!Metis.isAvailable()) {
			System.err.println("### METIS not available, falling back to simple partitioning. ###");
			bisection(network, population, numParts);
			return;
		}

		// Collect number of agents traversing each node
		Int2IntMap nodeWeights = new Int2IntOpenHashMap();
		Int2IntMap linkWeights = new Int2IntOpenHashMap();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (Leg leg : TripStructureUtils.getLegs(plan)) {
				if (leg.getRoute() instanceof NetworkRoute nr) {
					for (Id<Link> linkId : nr.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						nodeWeights.mergeInt(link.getFromNode().getId().index(), 1, Integer::sum);
						linkWeights.mergeInt(link.getId().index(), 1, Integer::sum);
					}
				}
			}
		}

		Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap<>();
		for (Node node : network.getNodes().values()) {
			nodes.put(node.getId().index(), node);
		}

		Metis.Graph g = new Metis.Graph() {
			@Override
			public int getNumVertices() {
				return network.getNodes().size();
			}

			@Override
			public int getNumEdges() {
				return network.getLinks().size() * 2;
			}

			@Override
			public int getVertexComputationWeight(int vertex) {
				return nodeWeights.getOrDefault(vertex, 1);
			}

			@Override
			public void getEdges(int vertex, Metis.EdgeBuilder builder) {
				// Graph needs to contain all edges, because it is undirected in metis
				nodes.get(vertex).getOutLinks().values().forEach(
					link -> builder.addEdge(link.getToNode().getId().index(), linkWeights.getOrDefault(link.getId().index(), 1))
				);
				nodes.get(vertex).getInLinks().values().forEach(
					link -> builder.addEdge(link.getToNode().getId().index(), linkWeights.getOrDefault(link.getId().index(), 1))
				);
			}
		};

		try {
			int[] partitions = Metis.partitionGraphKway(g, numParts, MetisOptions.of());
			for (int i = 0; i < partitions.length; i++) {
				Node node = nodes.get(i);
				node.getAttributes().putAttribute(PARTITION_ATTR_KEY, partitions[i]);
				for (var link : node.getInLinks().values()) {
					link.getAttributes().putAttribute(PARTITION_ATTR_KEY, partitions[i]);
				}
			}
		} catch (UnsatisfiedLinkError e) {

			System.err.println("### METIS failed with error, failing back to simple algorithm. ###");
			e.printStackTrace(System.err);
			bisection(network, population, numParts);
		}
	}

	public static void scattered(Network network, int numParts) {
		if (numParts < 1) {
			throw new IllegalArgumentException("Only positive number of parts allowed.");
		}

		for (var node : network.getNodes().values()) {
			int index = node.getId().index();
			int partition = index % numParts;
			node.getAttributes().putAttribute(PARTITION_ATTR_KEY, partition);
			for (var link : node.getInLinks().values()) {
				link.getAttributes().putAttribute(PARTITION_ATTR_KEY, partition);
			}
		}
	}

	/**
	 * For a good overview, see <a href="https://www.tu-chemnitz.de/sfb393/Files/PDF/sfb97-27.pdf">here</a>.
	 */
	public static void bisection(Network network, Population population, int numParts) {
		if (numParts < 1) {
			throw new IllegalArgumentException("Only positive number of parts allowed.");
		}

		Int2IntMap nodeWeights = new Int2IntOpenHashMap();

		// Initialize node weights as 1
		for (Node n : network.getNodes().values()) {
			nodeWeights.put(n.getId().index(), 1);
		}

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (Leg leg : TripStructureUtils.getLegs(plan)) {
				if (leg.getRoute() instanceof NetworkRoute nr) {
					for (Id<Link> linkId : nr.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						nodeWeights.mergeInt(link.getFromNode().getId().index(), 1, Integer::sum);
					}
				}
			}
		}

		// Calculate total weight of the network
		long totalWeight = nodeWeights.values().intStream().sum();

		// Create a list of nodes sorted by their x-coordinate
		List<Node> sortedNodes = new ArrayList<>(network.getNodes().values());
		sortedNodes.sort(Comparator.comparingDouble(node -> node.getCoord().getX()));

		// Perform recursive bisection
		recursiveBisection(sortedNodes, 0, sortedNodes.size() - 1, 0, numParts, nodeWeights, totalWeight);

		// Assign partitions to links based on their from node
		for (Node node : network.getNodes().values()) {
			int partition = (int) node.getAttributes().getAttribute(PARTITION_ATTR_KEY);
			for (var link : node.getInLinks().values()) {
				link.getAttributes().putAttribute(PARTITION_ATTR_KEY, partition);
			}
		}
	}

	private static void recursiveBisection(List<Node> nodes, int start, int end, int partitionOffset, int numParts, Int2IntMap nodeWeights, long totalWeight) {
		if (numParts == 1) {
			for (int i = start; i <= end; i++) {
				nodes.get(i).getAttributes().putAttribute(PARTITION_ATTR_KEY, partitionOffset);
			}
			return;
		}

		IntIntPair p = findMedian(nodes, start, end, totalWeight / 2, nodeWeights);
		int mid = p.leftInt();
		int leftParts = numParts / 2;
		int rightParts = numParts - leftParts;

		recursiveBisection(nodes, start, mid, partitionOffset, leftParts, nodeWeights, p.rightInt());
		recursiveBisection(nodes, mid + 1, end, partitionOffset + leftParts, rightParts, nodeWeights, totalWeight - p.rightInt());
	}

	private static IntIntPair findMedian(List<Node> nodes, int start, int end, long targetWeight, Int2IntMap nodeWeights) {
		int currentWeight = 0;
		for (int i = start; i <= end; i++) {
			currentWeight += nodeWeights.get(nodes.get(i).getId().index());
			if (currentWeight >= targetWeight) {
				return IntIntPair.of(i, currentWeight);
			}
		}
		return IntIntPair.of(end, currentWeight);
	}

}
