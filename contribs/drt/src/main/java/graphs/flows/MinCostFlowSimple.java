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

package graphs.flows;

import java.util.Arrays;

/*
 * The original source code: https://github.com/indy256/codelibrary
 * 
 * "indy256/codelibrary" is licensed under the "Unlicense":
 * A license with no conditions whatsoever which dedicates works to the public domain.
 * Unlicensed works, modifications, and larger works may be distributed under different terms and without source code.
 * 
 * See: https://github.com/indy256/codelibrary/blob/master/UNLICENSE 
 */

/**
 * Maximum flow of minimum cost in O(V^3*FLOW)
 */
public class MinCostFlowSimple {

	public static int[] minCostFlow(int[][] cap, int[][] cost, int s, int t) {
		int n = cap.length;
		int[] d = new int[n];
		int[] p = new int[n];
		for (int flow = 0, flowCost = 0;; ++flow) {
			Arrays.fill(d, Integer.MAX_VALUE);
			d[s] = 0;
			for (int i = 0; i < n - 1; i++)
				for (int j = 0; j < n; j++)
					for (int k = 0; k < n; k++)
						if (cap[j][k] > 0 && d[j] < Integer.MAX_VALUE && d[k] > d[j] + cost[j][k]) {
							d[k] = d[j] + cost[j][k];
							p[k] = j;
						}
			if (d[t] == Integer.MAX_VALUE)
				return new int[] { flowCost, flow };
			flowCost += d[t];
			for (int v = t; v != s; v = p[v]) {
				--cap[p[v]][v];
				++cap[v][p[v]];
			}
		}
	}

	public static void addEdge(int[][] cap, int[][] cost, int u, int v, int edgeCapacity, int edgeCost) {
		cap[u][v] = edgeCapacity;
		cost[u][v] = edgeCost;
		cost[v][u] = -edgeCost;
	}

	// Usage example
	public static void main(String[] args) {
		int n = 3;
		int[][] capacity = new int[n][n];
		int[][] cost = new int[n][n];
		addEdge(capacity, cost, 0, 1, 3, 1);
		addEdge(capacity, cost, 0, 2, 2, 1);
		addEdge(capacity, cost, 1, 2, 2, 1);
		int[] costFlow = minCostFlow(capacity, cost, 0, 2);
		System.out.println(6 == costFlow[0]);
		System.out.println(4 == costFlow[1]);
	}
}