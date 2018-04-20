/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import graphs.flows.MinCostFlow;
import graphs.flows.MinCostFlow.Edge;

/**
 * @author michalm
 */
public class TransportProblem<P, C> {
	private final ToIntBiFunction<P, C> costFunction;

	public TransportProblem(ToIntBiFunction<P, C> costFunction) {
		this.costFunction = costFunction;
	}

	public List<Triple<P, C, Integer>> solve(List<Pair<P, Integer>> supply, List<Pair<C, Integer>> demand) {
		final int P = supply.size();
		final int C = demand.size();
		final int N = P + C + 2;

		// N nodes, which indices are:
		// 0 - source
		// 1..P - producers 1..P
		// P+1..P+C - consumers 1..C
		// P+C+1 - sink

		@SuppressWarnings("unchecked")
		List<Edge>[] graph = Stream.generate(ArrayList::new).limit(N).toArray(List[]::new);

		// source -> producers
		int totalSupply = 0;
		for (int i = 0; i < P; i++) {
			int supplyValue = supply.get(i).getValue();
			MinCostFlow.addEdge(graph, 0, 1 + i, supplyValue, 0);
			totalSupply += supplyValue;
		}

		// producers --> consumers
		for (int i = 0; i < P; i++) {
			Pair<P, Integer> producer = supply.get(i);
			for (int j = 0; j < C; j++) {
				Pair<C, Integer> consumer = demand.get(j);
				int capacity = Math.min(producer.getValue(), consumer.getValue());
				int cost = costFunction.applyAsInt(producer.getKey(), consumer.getKey());
				MinCostFlow.addEdge(graph, 1 + i, 1 + P + j, capacity, cost);
			}
		}

		// consumers -> sink
		int totalDemand = 0;
		for (int j = 0; j < C; j++) {
			int demandValue = demand.get(j).getValue();
			MinCostFlow.addEdge(graph, 1 + P + j, N - 1, demandValue, 0);
			totalDemand += demandValue;
		}

		// solve min cost flow problem
		int[] result = MinCostFlow.minCostFlow(graph, 0, N - 1, Math.min(totalSupply, totalDemand), false);
		if (result[0] == 0) {
			return Collections.emptyList();
		}

		// extract flows
		List<Triple<P, C, Integer>> flows = new ArrayList<>();
		for (int i = 0; i < P; i++) {
			P from = supply.get(i).getKey();
			for (Edge e : graph[1 + i]) {
				int flow = e.getFlow();
				if (flow > 0) {
					int j = e.getTo() - (1 + P);
					C to = demand.get(j).getKey();
					flows.add(Triple.of(from, to, flow));
				}
			}
		}
		return flows;
	}
}
