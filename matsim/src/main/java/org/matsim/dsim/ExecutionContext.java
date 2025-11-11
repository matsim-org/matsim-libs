package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.serialization.SerializationProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Provides the execution context of the simulation including:
 * 1. Communicator to exchange messages with other simulation processes in case of a distributed execution
 * 2. Topology of computing environment, i.e. how many processes manage how many threads/partitions
 * 3. ComputeNode which contains information about the compute node of this instance
 */
public final class ExecutionContext {

	private static final Logger log = LogManager.getLogger(ExecutionContext.class);

	private final Communicator comm;

	public Communicator getComm() {
		return comm;
	}

	private final Topology topology;

	public Topology getTopology() {
		return topology;
	}

	private final ComputeNode computeNode;

	public ComputeNode getComputeNode() {
		return computeNode;
	}

	private final SerializationProvider serializer;

	public SerializationProvider getSerializer() {
		return serializer;
	}

	/**
	 * Create a local distributed context with the given number of threads.
	 */
	public static ExecutionContext createLocal(Config config) {
		return create(new NullCommunicator(), config.dsim().getThreads());
	}

	public static ExecutionContext create(Communicator comm, Config config) {
		return create(comm, config.dsim().getThreads());
	}

	public static ExecutionContext create(Communicator comm, int threads) {

		connect(comm);
		SerializationProvider serializer = new SerializationProvider();
		Topology topology = createTopology(comm, threads, serializer);
		logTopology(comm, topology);

		return new ExecutionContext(comm, topology, serializer);
	}

	public static ExecutionContext create(Communicator comm, Topology topology) {

		connect(comm);
		SerializationProvider serializer = new SerializationProvider();
		logTopology(comm, topology);

		return new ExecutionContext(comm, topology, serializer);
	}

	private ExecutionContext(Communicator comm, Topology topology, SerializationProvider serializer) {
		this.comm = comm;
		this.topology = topology;
		this.serializer = serializer;
		this.computeNode = topology.getNode(comm.getRank());
	}

	public boolean isDistributed() {
		return topology.isDistributed();
	}

	private static void connect(Communicator comm) {
		log.info("Waiting for {} other nodes to connect...", comm.getSize() - 1);
		try {
			comm.connect();
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to other nodes", e);
		}
		log.info("All nodes connected");
	}

	private static void logTopology(Communicator comm, Topology topology) {
		log.info("Topology has {} partitions on {} nodes. Node {} has parts: {}",
			topology.getTotalPartitions(), topology.getNodesCount(), comm.getRank(), topology.getNode(comm.getRank()).getParts());
	}

	private static Topology createTopology(Communicator comm, int threads, SerializationProvider serializer) {

		ComputeNode node = ComputeNode.builder()
			.cores(threads == 0 ? Runtime.getRuntime().availableProcessors() : threads)
			.rank(comm.getRank())
			.build();

		// Receive node information from all ranks
		List<ComputeNode> computeNodes = comm.allGather(node, 0, serializer);
		computeNodes.sort(Comparator.comparingInt(ComputeNode::getRank));

		Topology.TopologyBuilder topology = Topology.builder();
		List<ComputeNode> topoNodes = new ArrayList<>();


		int total = 0;
		for (ComputeNode computeNode : computeNodes) {

			ComputeNode.NodeBuilder n = computeNode.toBuilder();
			int parts = Math.max(1, computeNode.getCores());

			n.parts(IntStream.range(total, total + parts).collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll));
			n.distributed(computeNodes.size() > 1);

			total += parts;
			topoNodes.add(n.build());
		}

		// head nodes needs to build topology with all partition info
		return topology
			.computeNodes(topoNodes)
			.totalPartitions(total)
			.build();
	}
}
