/* *********************************************************************** *
 * project: org.matsim.*
 * CHLeastCostPathTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * CH-based least-cost-path tree.  Computes shortest paths from one source
 * to ALL reachable nodes using the CH overlay graph, which is dramatically
 * faster than plain Dijkstra on the full base graph.
 *
 * <h3>Algorithm (forward search)</h3>
 * <ol>
 *   <li><b>Phase 1 – Upward Dijkstra</b>: From the source, explore only upward
 *       CH edges (towards higher-ranked nodes).  This settles all nodes
 *       reachable via upward paths in the CH overlay.</li>
 *   <li><b>Phase 2 – Downward sweep</b>: Process all nodes in <em>decreasing</em>
 *       rank order.  For each node w, check its incoming downward edges (from
 *       higher-ranked nodes u).  If {@code cost[u] + weight(u→w) < cost[w]},
 *       update w.  After the sweep, every node has its optimal shortest path
 *       cost.</li>
 * </ol>
 *
 * <h3>Algorithm (backward search)</h3>
 * <ol>
 *   <li><b>Phase 1 – "Reverse upward" Dijkstra</b>: From the target, explore
 *       downward in-edges in reverse.  For each node w being processed, its
 *       {@code dnEdges} give edges from higher-ranked u to w; going backward
 *       settles u with cost {@code cost[w] + weight(u→w)}.</li>
 *   <li><b>Phase 2 – Downward sweep</b>: Process all nodes in decreasing rank
 *       order.  For each node w, check its upward out-edges (to higher-ranked u).
 *       Going backward: {@code cost[w] = min(cost[w], cost[u] + weight(w→u))}.</li>
 * </ol>
 *
 * <p>This class implements the same interface pattern as {@link LeastCostPathTree}
 * so it can be used as a drop-in replacement in {@code OneToManyPathSearch}.
 *
 * <p>The implementation does not allocate any memory in the calculate methods.
 * All required memory is pre-allocated in the constructor. This makes the
 * implementation NOT thread-safe.
 *
 * @author Steffen Axer
 */
public class CHLeastCostPathTree implements ShortestPathTree {

    private final CHGraph chGraph;
    private final SpeedyGraph baseGraph;
    private final TravelTime tt;
    private final TravelDisutility td;

    // Per-node data: 3 entries (cost, time, distance)
    private final double[] data;
    private final int[] comingFrom;       // parent node index in the tree
    private final int[] fromEdgeGIdx;     // CH global edge index used to reach this node
    private final int[] iterIds;          // iteration stamp to detect unvisited nodes

    private int currentIteration = Integer.MIN_VALUE;

    /** true after {@link #calculate}, false after {@link #calculateBackwards}. */
    private boolean lastForwardSearch;

    private final DAryMinHeap pq;

    // Cached CH arrays for hot-path access
    private final int[] upOff, upLen, upEdges;
    private final double[] upWeights;
    private final int[] dnOff, dnLen, dnEdges;
    private final double[] dnWeights;
    private final int[] sweepOrder;
    private final double[] ttf;
    private final int totalEdgeCount;

    // Reverse CSR arrays for push-based Phase 2 sweep
    private final int[] dnOutOff, dnOutLen, dnOutEdges;
    private final double[] dnOutWeights;   // colocated minTTF per dnOut slot
    private final int[] upInOff, upInLen, upInEdges;
    private final double[] upInWeights;    // colocated minTTF per upIn slot
    private final int nodeCount;

    public CHLeastCostPathTree(CHGraph chGraph, TravelTime tt, TravelDisutility td) {
        this.chGraph = chGraph;
        this.baseGraph = chGraph.getBaseGraph();
        this.tt = tt;
        this.td = td;

        int n = chGraph.nodeCount;
        this.data = new double[n * 3];
        this.comingFrom = new int[n];
        this.fromEdgeGIdx = new int[n];
        this.iterIds = new int[n];
        this.pq = new DAryMinHeap(n, 4);

        this.upOff = chGraph.upOff;
        this.upLen = chGraph.upLen;
        this.upEdges = chGraph.upEdges;
        this.upWeights = chGraph.upWeights;
        this.dnOff = chGraph.dnOff;
        this.dnLen = chGraph.dnLen;
        this.dnEdges = chGraph.dnEdges;
        this.dnWeights = chGraph.dnWeights;
        this.sweepOrder = chGraph.sweepOrder;
        this.ttf = chGraph.ttf;
        this.totalEdgeCount = chGraph.totalEdgeCount;

        // Reverse CSR for push-based Phase 2
        this.dnOutOff = chGraph.dnOutOff;
        this.dnOutLen = chGraph.dnOutLen;
        this.dnOutEdges = chGraph.dnOutEdges;
        this.dnOutWeights = chGraph.dnOutWeights;
        this.upInOff = chGraph.upInOff;
        this.upInLen = chGraph.upInLen;
        this.upInEdges = chGraph.upInEdges;
        this.upInWeights = chGraph.upInWeights;
        this.nodeCount = n;
    }

    // -------------------------------------------------------------------------
    // Forward search
    // -------------------------------------------------------------------------

    public void calculate(Link startLink, double startTime, Person person, Vehicle vehicle) {
        calculate(startLink, startTime, person, vehicle,
                (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculate(Link startLink, double startTime, Person person, Vehicle vehicle,
                          LeastCostPathTree.StopCriterion stopCriterion) {
        lastForwardSearch = true;
        int startNode = baseGraph.getNodeIndex(startLink.getToNode());
        calculateForward(startNode, startTime, stopCriterion);
    }

    private void calculateForward(int startNode, double startTime,
                                  LeastCostPathTree.StopCriterion stopCriterion) {
        advanceIteration();

        final int S = CHGraph.E_STRIDE;
        final int NUM_BINS = CHTTFCustomizer.NUM_BINS;
        final double INV_BIN = CHTTFCustomizer.INV_BIN_SIZE;

        // Phase 1: Upward Dijkstra from source
        // Uses time-dependent TTF for travel time; cost = travel time (consistent
        // with CHRouterTimeDep which also uses TTF as the cost metric).
        setNode(startNode, 0.0, startTime, 0.0, -1, -1);
        pq.clear();
        pq.insert(startNode, 0.0);

        // Track the cost at which Phase 1 terminates.  All unsettled nodes
        // have cost ≥ this value (Dijkstra monotonicity), so the sweep only
        // needs to propagate from nodes with cost < earlyTermCost.
        double earlyTermCost = Double.POSITIVE_INFINITY;

        while (!pq.isEmpty()) {
            int v = pq.poll();
            double cost = getCost(v);
            double arr = getTimeRaw(v);

            if (stopCriterion.stop(baseGraph.getNode(v).getId().index(),
                    arr, cost, getDistance(v), startTime)) {
                earlyTermCost = cost;
                break;
            }

            // Compute time bin for TTF lookup
            int bin = ((int) (arr * INV_BIN)) % NUM_BINS;
            if (bin < 0) bin += NUM_BINS;
            int binOff = bin * totalEdgeCount;

            // Iterate upward out-edges
            int uOff = upOff[v];
            int uEnd = uOff + upLen[v];
            for (int slot = uOff; slot < uEnd; slot++) {
                int eBase = slot * S;
                int w = upEdges[eBase];
                int gIdx = upEdges[eBase + CHGraph.E_GIDX];

                double tTime = ttf[binOff + gIdx];
                double newCost = cost + tTime;
                double newArr = arr + tTime;

                if (iterIds[w] == currentIteration) {
                    if (newCost < getCost(w)) {
                        setNode(w, newCost, newArr, 0.0, v, gIdx);
                        pq.decreaseKey(w, newCost);
                    }
                } else {
                    setNode(w, newCost, newArr, 0.0, v, gIdx);
                    pq.insert(w, newCost);
                }
            }
        }

        // Phase 2: Downward propagation.
        // Always use the linear push-based sweep with cost-bounded pruning.
        // The linear scan of sweepOrder (decreasing rank) with push semantics
        // is both simpler and faster than the heap-based lazy approach:
        //   - No heap overhead (O(1) per node instead of O(log n))
        //   - Cost-bounded pruning skips nodes/edges beyond earlyTermCost
        //   - Sequential memory access on sweepOrder (cache-friendly)
        forwardSweepLinear(S, earlyTermCost);
    }

    /**
     * Cost-bounded linear push sweep: processes all nodes in decreasing rank
     * order (via sweepOrder), pushing costs from visited nodes to lower-ranked
     * successors via the reverse CSR (dnOutEdges).
     *
     * <p>Key optimizations over the previous heap-based lazy sweep:
     * <ul>
     *   <li><b>No heap overhead</b>: O(1) per node (array iteration) instead of
     *       O(log n) heap insert/poll.  The linear scan of sweepOrder is extremely
     *       cache-friendly.</li>
     *   <li><b>Cost-bounded pruning</b>: nodes with cost ≥ maxCost are skipped,
     *       preventing propagation beyond the query's time bound.  For bounded
     *       DRT queries (maxTT=120-300s), this prunes the vast majority of the
     *       sweep, making it proportional to the reachable set.</li>
     *   <li><b>Uses minTTF</b>: colocated dnOutWeights for cache locality.</li>
     * </ul>
     *
     * @param S       edge stride (CHGraph.E_STRIDE)
     * @param maxCost upper bound on useful cost (Phase 1 termination cost);
     *                Double.POSITIVE_INFINITY for unbounded queries
     */
    private void forwardSweepLinear(int S, double maxCost) {
        final int[] order = sweepOrder;
        final int n = order.length;
        final int iter = currentIteration;

        for (int i = 0; i < n; i++) {
            int u = order[i];

            // Skip nodes not visited (neither settled in Phase 1 nor reached by sweep)
            if (iterIds[u] != iter) continue;

            double uCost = data[u * 3];         // getCost(u) inlined
            // Cost pruning: if this node's cost ≥ the bound, every path through
            // it also exceeds the bound → skip propagation.
            if (uCost >= maxCost) continue;

            double uArr = data[u * 3 + 1];      // getTimeRaw(u) inlined

            // Push costs along u's outgoing downward edges (u→w, rank(w) < rank(u))
            int dOff = dnOutOff[u];
            int dEnd = dOff + dnOutLen[u];
            for (int slot = dOff; slot < dEnd; slot++) {
                int eBase = slot * S;
                int w = dnOutEdges[eBase];

                // Colocated weight (minTTF) — same cache region as edge data
                double tTime = dnOutWeights[slot];
                double newCost = uCost + tTime;

                // Edge-level pruning: skip if result exceeds bound
                if (newCost >= maxCost) continue;

                int wBase = w * 3;
                double wCost = (iterIds[w] == iter)
                        ? data[wBase] : Double.POSITIVE_INFINITY;

                if (newCost < wCost) {
                    // setNode inlined for hot-path performance
                    data[wBase] = newCost;
                    data[wBase + 1] = uArr + tTime;
                    data[wBase + 2] = 0.0;
                    comingFrom[w] = u;
                    fromEdgeGIdx[w] = dnOutEdges[eBase + CHGraph.E_GIDX];
                    iterIds[w] = iter;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Backward search
    // -------------------------------------------------------------------------

    public void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle) {
        calculateBackwards(arrivalLink, arrivalTime, person, vehicle,
                (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle,
                                   LeastCostPathTree.StopCriterion stopCriterion) {
        lastForwardSearch = false;
        int arrivalNode = baseGraph.getNodeIndex(arrivalLink.getFromNode());
        calculateBackwardImpl(arrivalNode, arrivalTime, stopCriterion);
    }

    private void calculateBackwardImpl(int targetNode, double arrivalTime,
                                        LeastCostPathTree.StopCriterion stopCriterion) {
        advanceIteration();

        final int S = CHGraph.E_STRIDE;

        // Phase 1: Backward "upward" Dijkstra from target.
        setNode(targetNode, 0.0, arrivalTime, 0.0, -1, -1);
        pq.clear();
        pq.insert(targetNode, 0.0);

        // Track the cost at which Phase 1 terminates (for sweep pruning).
        double earlyTermCost = Double.POSITIVE_INFINITY;

        while (!pq.isEmpty()) {
            int w = pq.poll();
            double cost = getCost(w);

            if (stopCriterion.stop(baseGraph.getNode(w).getId().index(),
                    arrivalTime, cost, getDistance(w), getTimeRaw(w))) {
                earlyTermCost = cost;
                break;
            }

            int dOff = dnOff[w];
            int dEnd = dOff + dnLen[w];
            for (int slot = dOff; slot < dEnd; slot++) {
                int eBase = slot * S;
                int u = dnEdges[eBase];
                int gIdx = dnEdges[eBase + CHGraph.E_GIDX];

                double edgeCost = chGraph.minTTF[gIdx];
                double newCost = cost + edgeCost;

                if (iterIds[u] == currentIteration) {
                    if (newCost < getCost(u)) {
                        double newTime = getTimeRaw(w) - edgeCost;
                        setNode(u, newCost, newTime, 0.0, w, gIdx);
                        pq.decreaseKey(u, newCost);
                    }
                } else {
                    double newTime = getTimeRaw(w) - edgeCost;
                    setNode(u, newCost, newTime, 0.0, w, gIdx);
                    pq.insert(u, newCost);
                }
            }
        }

        // Phase 2: Cost-bounded linear push sweep.
        backwardSweepLinear(S, earlyTermCost);
    }

    /**
     * Cost-bounded linear push sweep for backward search.
     * Pushes costs from visited nodes to lower-ranked predecessors via the
     * reverse CSR (upInEdges).  Uses minTTF colocated weights.
     *
     * @param S       edge stride
     * @param maxCost upper bound on useful cost
     */
    private void backwardSweepLinear(int S, double maxCost) {
        final int[] order = sweepOrder;
        final int n = order.length;
        final int iter = currentIteration;

        for (int i = 0; i < n; i++) {
            int u = order[i];

            if (iterIds[u] != iter) continue;

            double uCost = data[u * 3];         // getCost(u) inlined
            if (uCost >= maxCost) continue;      // cost-bounded pruning

            double uTime = data[u * 3 + 1];     // getTimeRaw(u) inlined

            // Push cost from u back to lower-ranked w via incoming up-edges (w→u)
            int iOff = upInOff[u];
            int iEnd = iOff + upInLen[u];
            for (int slot = iOff; slot < iEnd; slot++) {
                int eBase = slot * S;
                int w = upInEdges[eBase];       // lower-ranked source

                double edgeCost = upInWeights[slot];
                double newCost = uCost + edgeCost;

                if (newCost >= maxCost) continue; // edge-level pruning

                int wBase = w * 3;
                double wCost = (iterIds[w] == iter)
                        ? data[wBase] : Double.POSITIVE_INFINITY;

                if (newCost < wCost) {
                    // setNode inlined
                    data[wBase] = newCost;
                    data[wBase + 1] = uTime - edgeCost;
                    data[wBase + 2] = 0.0;
                    comingFrom[w] = u;
                    fromEdgeGIdx[w] = upInEdges[eBase + CHGraph.E_GIDX];
                    iterIds[w] = iter;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Accessors (compatible with LeastCostPathTree interface)
    // -------------------------------------------------------------------------

    public double getCost(int nodeIndex) {
        return data[nodeIndex * 3];
    }

    public int getNodeIndex(Node node) {
        return baseGraph.getNodeIndex(node);
    }

    private double getTimeRaw(int nodeIndex) {
        return data[nodeIndex * 3 + 1];
    }

    public OptionalTime getTime(int nodeIndex) {
        if (iterIds[nodeIndex] != currentIteration) return OptionalTime.undefined();
        double time = getTimeRaw(nodeIndex);
        if (Double.isInfinite(time)) return OptionalTime.undefined();
        return OptionalTime.defined(time);
    }

    public double getDistance(int nodeIndex) {
        return data[nodeIndex * 3 + 2];
    }

    /**
     * Returns a node-path iterator that walks from the given target node
     * back to the source through the CH tree.
     */
    @Override
    public Iterator<Node> getNodePathIterator(Node node) {
        return new CHPathIterator(node);
    }

    /**
     * Returns a link-path iterator that unpacks CH shortcuts into base-graph
     * links, walking from the given target node back to the source.
     */
    @Override
    public Iterator<Link> getLinkPathIterator(Node node) {
        return new CHLinkPathIterator(node);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void advanceIteration() {
        currentIteration++;
        if (currentIteration == Integer.MAX_VALUE) {
            Arrays.fill(iterIds, Integer.MIN_VALUE);
            currentIteration = Integer.MIN_VALUE + 1;
        }
    }

    private void setNode(int nodeIndex, double cost, double time, double distance,
                         int from, int edgeGIdx) {
        int base = nodeIndex * 3;
        data[base] = cost;
        data[base + 1] = time;
        data[base + 2] = distance;
        comingFrom[nodeIndex] = from;
        fromEdgeGIdx[nodeIndex] = edgeGIdx;
        iterIds[nodeIndex] = currentIteration;
    }

    // -------------------------------------------------------------------------
    // Path iterators (unpack CH shortcuts to base-graph nodes/links)
    // -------------------------------------------------------------------------

    /**
     * Iterates through the CH parent chain, yielding base-graph nodes by
     * unpacking shortcuts along the way. Compatible with
     * {@link LeastCostPathTree.PathIterator}.
     */
    private final class CHPathIterator implements Iterator<Node> {
        private int current;

        CHPathIterator(Node startNode) {
            current = baseGraph.getNodeIndex(startNode);
        }

        @Override
        public Node next() {
            current = comingFrom[current];
            if (current < 0) throw new NoSuchElementException();
            return baseGraph.getNode(current);
        }

        @Override
        public boolean hasNext() {
            return current >= 0 && comingFrom[current] >= 0;
        }
    }

    /**
     * Iterates through CH edges, unpacking shortcuts into base-graph links.
     * Compatible with {@link LeastCostPathTree.LinkPathIterator}.
     */
    private final class CHLinkPathIterator implements Iterator<Link> {
        // We build the full unpacked link list eagerly because shortcuts
        // expand into multiple links and the iterator interface is sequential.
        private final Link[] links;
        private int pos;

        CHLinkPathIterator(Node targetNode) {
            int target = baseGraph.getNodeIndex(targetNode);
            // Collect all CH edges on the path (from given node toward tree root)
            java.util.List<Integer> edgeGIdxList = new java.util.ArrayList<>();
            int curr = target;
            while (curr >= 0 && iterIds[curr] == currentIteration && comingFrom[curr] >= 0) {
                edgeGIdxList.add(fromEdgeGIdx[curr]);
                curr = comingFrom[curr];
            }
            // Unpack all CH edges to base-graph links.
            // For forward searches the iterator walks target→source but the
            // caller will flat-reverse the list, so each shortcut must be
            // unpacked in REVERSE order (lower2 before lower1) to keep the
            // within-shortcut link sequence correct after the outer reversal.
            // For backward searches no reversal is applied by the caller, so
            // shortcuts are unpacked in natural order (lower1 before lower2).
            java.util.List<Link> linkList = new java.util.ArrayList<>();
            for (int gIdx : edgeGIdxList) {
                unpackEdge(gIdx, linkList, lastForwardSearch);
            }
            links = linkList.toArray(new Link[0]);
            pos = 0;
        }

        private void unpackEdge(int gIdx, java.util.List<Link> linkList, boolean reverse) {
            if (gIdx < 0) return;
            int orig = chGraph.edgeOrigLink[gIdx];
            if (orig >= 0) {
                linkList.add(baseGraph.getLink(orig));
            } else if (reverse) {
                // Reverse order: unpack second half first, then first half
                unpackEdge(chGraph.edgeLower2[gIdx], linkList, true);
                unpackEdge(chGraph.edgeLower1[gIdx], linkList, true);
            } else {
                unpackEdge(chGraph.edgeLower1[gIdx], linkList, false);
                unpackEdge(chGraph.edgeLower2[gIdx], linkList, false);
            }
        }

        @Override
        public Link next() {
            if (pos >= links.length) throw new NoSuchElementException();
            return links[pos++];
        }

        @Override
        public boolean hasNext() {
            return pos < links.length;
        }
    }
}
