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

public final class DistributedContext implements ExecutionContext {

	private static final Logger log = LogManager.getLogger(DistributedContext.class);

	private final Communicator comm;

	public Communicator getComm() {
		return comm;
	}

	private final Topology topology;

	private final ComputeNode computeNode;

	private final SerializationProvider serializer;

	public SerializationProvider getSerializer() {
		return serializer;
	}

	private DistributedContext(Communicator comm, Topology topology, SerializationProvider serializer) {
		this.comm = comm;
		this.topology = topology;
		this.serializer = serializer;
		this.computeNode = topology.getNode(comm.getRank());
	}

	/**
	 * Create a local distributed context with the given number of threads.
	 */
	public static DistributedContext createLocal(Config config) {
		return create(new NullCommunicator(), config.dsim().getThreads());
	}

	/**
	 * Create a local distributed context with the given number of threads.
	 */
	public static DistributedContext createLocal(Communicator comm, Topology topology) {

		try {
			comm.connect();
		} catch (Exception e) {
			throw new RuntimeException("Local communication problem", e);
		}

		SerializationProvider serializer = new SerializationProvider();

		log.info("Local topology has {} partitions.", topology.getTotalPartitions());

		return new DistributedContext(comm, topology, serializer);
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

	@Override
	public Topology getTopology() {
		return topology;
	}

	@Override
	public ComputeNode getComputeNode() {
		return computeNode;
	}
}
