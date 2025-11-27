package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.List;
import java.util.stream.IntStream;

public final class LocalContext implements ExecutionContext {

	/**
	 * Local context with a single partition.
	 */
	private final static LocalContext INSTANCE = new LocalContext(
		Topology.builder()
			.totalPartitions(1)
			.computeNodes(List.of(ComputeNode.SINGLE_INSTANCE))
			.build()
	);

	private final Topology topology;

	private LocalContext(Topology topology) {
		this.topology = topology;
	}

	/**
	 * Create a local context. Uses DSim config if available to determine the number of partitions.
	 */
	public static LocalContext create(@Nullable Config config) {
		if (config != null && ConfigUtils.hasModule(config, DSimConfigGroup.class)) {
			return new LocalContext(createTopology(ConfigUtils.addOrGetModule(config, DSimConfigGroup.class).getThreads()));
		}

		return INSTANCE;
	}

	/**
	 * Create local topology with number of threads and partitions.
	 */
	private static Topology createTopology(int threads) {

		int cores = threads == 0 ? Runtime.getRuntime().availableProcessors() : threads;
		int parts = Math.max(1, cores);

		ComputeNode node = ComputeNode.builder()
			.cores(cores)
			.rank(0)
			.parts(IntStream.range(0, parts).collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll))
			.distributed(false)
			.build();

		return Topology.builder()
			.computeNodes(List.of(node))
			.totalPartitions(parts)
			.build();
	}

	@Override
	public Topology getTopology() {
		return topology;
	}

	@Override
	public ComputeNode getComputeNode() {
		return topology.getNode(0);
	}
}
