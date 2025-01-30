package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.serialization.SerializationProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Class to hold information and communication needed for distributed simulation.
 */
@Getter
@Log4j2
public final class DistributedContext {

	/**
	 * Local distributed context with a single partition.
	 */
	public final static DistributedContext LOCAL = new DistributedContext(
		new NullCommunicator(),
		Topology.builder()
			.totalPartitions(1)
			.nodes(List.of(SimulationNode.SINGLE_INSTANCE))
			.build(),
		null);

	private final Communicator comm;

	private final Topology topology;

	private final SimulationNode node;

	private final SerializationProvider serializer;

	private DistributedContext(Communicator comm, Topology topology, SerializationProvider serializer) {
		this.comm = comm;
		this.topology = topology;
		this.serializer = serializer;
		this.node = topology.getNode(comm.getRank());
	}

	/**
	 * Create a local distributed context with the given number of threads.
	 */
	public static DistributedContext createLocal(Config config) {
		return create(new NullCommunicator(), config.dsim().getThreads());
	}

	/**
	 * Create distributed context with the given communicator and configuration.
	 */
	public static DistributedContext create(Communicator comm, Config config) {
		return create(comm, config.dsim().getThreads());
	}

	/**
	 * Create distributed context with the given communicator and number of threads.
	 */
	private static DistributedContext create(Communicator comm, int threads) {

		log.info("Waiting for {} other nodes to connect...", comm.getSize() - 1);
		try {
			comm.connect();
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to other nodes", e);
		}

		log.info("All nodes connected");

		SerializationProvider serializer = new SerializationProvider();

		// This may be relevant if we want to partition the network or other lps
		Topology topology = createTopology(comm, threads, serializer);

		log.info("Topology has {} partitions on {} nodes. Node {} has parts: {}",
			topology.getTotalPartitions(), topology.getNodesCount(), comm.getRank(), topology.getNode(comm.getRank()).getParts());

		return new DistributedContext(comm, topology, serializer);
	}

	/**
	 * Whether the simulation is distributed across multiple nodes.
	 */
	public boolean isDistributed() {
		return topology.isDistributed();
	}

	private static Topology createTopology(Communicator comm, int threads, SerializationProvider serializer) {

		SimulationNode node = SimulationNode.builder()
			.cores(threads)
			.rank(comm.getRank())
			.build();

		// Receive node information from all ranks
		List<SimulationNode> nodes = comm.allGather(node, 0, serializer);
		nodes.sort(Comparator.comparingInt(SimulationNode::getRank));

		Topology.TopologyBuilder topology = Topology.builder();
		List<SimulationNode> topoNodes = new ArrayList<>();


		int total = 0;
		for (SimulationNode value : nodes) {

			SimulationNode.NodeBuilder n = value.toBuilder();
			int parts = Math.max(1, value.getCores());

			n.parts(IntStream.range(total, total + parts).collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll));
			n.distributed(nodes.size() > 1);

			total += parts;
			topoNodes.add(n.build());
		}

		// head nodes needs to build topology with all partition info
		return topology
			.nodes(topoNodes)
			.totalPartitions(total)
			.build();
	}
}
