package org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;

import com.google.common.base.Verify;

/**
 * Default implementation for the shareability graph.
 * 
 * @author sebhoerl
 */
public class DefaultRequestGraph implements RequestGraph {
	private final AlonsoMoraFunction function;
	private final ForkJoinPool pool;

	private final Map<AlonsoMoraRequest, Set<AlonsoMoraRequest>> edges = new HashMap<>();
	private final Set<AlonsoMoraRequest> requests = new HashSet<>();

	public DefaultRequestGraph(AlonsoMoraFunction function, ForkJoinPool pool) {
		this.function = function;
		this.pool = pool;
	}

	@Override
	public void addRequest(AlonsoMoraRequest request, double now) {
		Verify.verify(requests.add(request), "Request is already in graph");

		pool.submit(() -> {
			requests.parallelStream().forEach(existingRequest -> {
				if (existingRequest != request && function.checkShareability(existingRequest, request, now)) {
					synchronized (edges) {
						edges.computeIfAbsent(request, r -> new HashSet<>()).add(existingRequest);
						edges.computeIfAbsent(existingRequest, r -> new HashSet<>()).add(request);
					}
				}
			});
		}).join();
	}

	@Override
	public Collection<AlonsoMoraRequest> getShareableRequests(AlonsoMoraRequest request) {
		return Collections.unmodifiableCollection(edges.getOrDefault(request, Collections.emptySet()));
	}

	@Override
	public int getSize() {
		return edges.size();
	}
}
