/* *********************************************************************** *
 * project: org.matsim.*
 * CHRouterTimeDep.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.turnRestrictions.TurnRestrictionsContext;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Time-dependent CATCHUp bidirectional CH query using
 * {@link CHGraph} with flat TTF array.
 *
 * @author Steffen Axer
 */
public class CHRouterTimeDep implements LeastCostPathCalculator {

    private static final Logger LOG = LogManager.getLogger(CHRouterTimeDep.class);

    private final CHGraph chGraph;
    private final SpeedyGraph   baseGraph;
    private final TravelTime       tt;
    private final TravelDisutility td;

    // Resolved once in constructor – avoids Optional.isPresent() on hot path.
    private final TurnRestrictionsContext turnRestrictions; // null if none

    // Forward search state
    private final double[] fwdArrival;
    private final double[] fwdCost;
    private final int[]    fwdComingFrom;
    private final int[]    fwdUsedEdge;  // stores global edge index
    private final int[]    fwdIterIds;

    // Backward search state
    private final double[] bwdLB;
    private final int[]    bwdComingFrom;
    private final int[]    bwdUsedEdge;
    private final int[]    bwdIterIds;

    private int currentIteration = Integer.MIN_VALUE;

    private final DAryMinHeap fwdPQ;
    private final DAryMinHeap bwdPQ;

    // Local references for hot-path access (avoid field dereference chains)
    private final int[]    upOff, upLen, upEdges;
    private final double[] upWeights;
    private final int[]    dnOff, dnLen, dnEdges;
    private final double[] dnWeights;
    private final double[] ttf;
    private final double[] minTTF;
    private final int      totalEdgeCount; // stride for bin-major TTF layout

    // -------------------------------------------------------------------------
    // Stats tracking (search space size — the primary CH quality metric)
    // -------------------------------------------------------------------------

    /** Total forward PQ polls across all queries since last {@link #resetStats()}. */
    private long totalFwdSettled = 0;
    /** Total backward PQ polls across all queries since last {@link #resetStats()}. */
    private long totalBwdSettled = 0;
    /** Number of queries executed since last {@link #resetStats()}. */
    private long chQueryCount = 0;

    /** Resets all accumulated stats (call between warmup and measurement phase). */
    public void resetStats() {
        totalFwdSettled = 0;
        totalBwdSettled = 0;
        chQueryCount    = 0;
    }

    public long getTotalFwdSettled() { return totalFwdSettled; }
    public long getTotalBwdSettled() { return totalBwdSettled; }
    public long getChQueryCount()    { return chQueryCount; }

    public CHRouterTimeDep(CHGraph chGraph, TravelTime tt, TravelDisutility td) {
        this.chGraph   = chGraph;
        this.baseGraph = chGraph.getBaseGraph();
        this.tt        = tt;
        this.td        = td;
        this.turnRestrictions = baseGraph.getTurnRestrictions().orElse(null);

        int n = chGraph.nodeCount;
        this.fwdArrival    = new double[n];
        this.fwdCost       = new double[n];
        this.fwdComingFrom = new int[n];
        this.fwdUsedEdge   = new int[n];
        this.fwdIterIds    = new int[n];

        this.bwdLB         = new double[n];
        this.bwdComingFrom = new int[n];
        this.bwdUsedEdge   = new int[n];
        this.bwdIterIds    = new int[n];

        Arrays.fill(fwdIterIds, currentIteration);
        Arrays.fill(bwdIterIds, currentIteration);

        this.fwdPQ = new DAryMinHeap(n, 6);
        this.bwdPQ = new DAryMinHeap(n, 6);

        // Cache array references for hot-path
        this.upOff     = chGraph.upOff;
        this.upLen     = chGraph.upLen;
        this.upEdges   = chGraph.upEdges;
        this.upWeights = chGraph.upWeights;
        this.dnOff     = chGraph.dnOff;
        this.dnLen     = chGraph.dnLen;
        this.dnEdges   = chGraph.dnEdges;
        this.dnWeights = chGraph.dnWeights;
        this.ttf       = chGraph.ttf;
        this.minTTF    = chGraph.minTTF;
        this.totalEdgeCount = chGraph.totalEdgeCount;
    }

    @Override
    public Path calcLeastCostPath(Node startNode, Node endNode,
                                  double startTime, Person person, Vehicle vehicle) {
        int startIdx = baseGraph.getNodeIndex(startNode);
        int endIdx   = baseGraph.getNodeIndex(endNode);
        Path path    = calcLeastCostPathImpl(startIdx, endIdx, startTime, person, vehicle);
        if (path == null) logNoRoute("node " + startNode.getId(), "node " + endNode.getId());
        return path;
    }

    @Override
    public Path calcLeastCostPath(Link fromLink, Link toLink,
                                  double startTime, Person person, Vehicle vehicle) {
        int startIdx = baseGraph.getNodeIndex(fromLink.getToNode());
        int endIdx   = baseGraph.getNodeIndex(toLink.getFromNode());

        if (turnRestrictions != null) {
            Map<Id<Link>, TurnRestrictionsContext.ColoredLink> replaced = turnRestrictions.replacedLinks;
            if (replaced.containsKey(fromLink.getId())) {
                startIdx = baseGraph.getInternalIndex(replaced.get(fromLink.getId()).toColoredNode.index());
            }
        }

        Path path = calcLeastCostPathImpl(startIdx, endIdx, startTime, person, vehicle);
        if (path == null) logNoRoute("link " + fromLink.getId(), "link " + toLink.getId());
        return path;
    }

    // -------------------------------------------------------------------------
    // Bidirectional CATCHUp query
    // -------------------------------------------------------------------------

    private Path calcLeastCostPathImpl(int startIdx, int endIdx,
                                       double startTime, Person person, Vehicle vehicle) {
        advanceIteration();

        if (startIdx == endIdx) {
            chQueryCount++;
            return new Path(
                    Collections.singletonList(baseGraph.getNode(startIdx)),
                    Collections.emptyList(), 0.0, 0.0);
        }

        // Initialize forward search
        setFwd(startIdx, startTime, 0.0, -1, -1);
        fwdPQ.clear();
        fwdPQ.insert(startIdx, 0.0);

        // Initialize backward search
        setBwd(endIdx, 0.0, -1, -1);
        bwdPQ.clear();
        bwdPQ.insert(endIdx, 0.0);

        // Seed colored copies for turn restrictions.
        // Colored nodes duplicate real nodes to model forbidden turn sequences.
        // We must seed all colored copies of start/end so the bidirectional
        // search can find paths that traverse these colored subgraphs.
        // IMPORTANT: cn.index() is an EXTERNAL index from TurnRestrictionsContext;
        // it must be mapped to the internal (spatially reordered) index via
        // getInternalIndex() before accessing any CH array or priority queue.
        if (turnRestrictions != null) {
            for (TurnRestrictionsContext.ColoredNode cn : turnRestrictions.coloredNodes) {
                int origIdx = baseGraph.getNodeIndex(cn.node());
                int coloredIdx = baseGraph.getInternalIndex(cn.index());
                if (coloredIdx < 0) continue; // unmapped node (should not happen)
                if (origIdx == endIdx && coloredIdx != endIdx) {
                    setBwd(coloredIdx, 0.0, -1, -1);
                    bwdPQ.insert(coloredIdx, 0.0);
                }
                if (origIdx == startIdx && coloredIdx != startIdx) {
                    setFwd(coloredIdx, startTime, 0.0, -1, -1);
                    fwdPQ.insert(coloredIdx, 0.0);
                }
            }
        }

        final int S = CHGraph.E_STRIDE;
        final int NUM_BINS = CHTTFCustomizer.NUM_BINS;
        final double INV_BIN = CHTTFCustomizer.INV_BIN_SIZE;

        double bestBound   = Double.POSITIVE_INFINITY;
        int    meetingNode = -1;

        // Per-query counters (accumulated into totals at the end)
        int fwdSettled = 0;
        int bwdSettled = 0;

        while (!fwdPQ.isEmpty() || !bwdPQ.isEmpty()) {
            double fMin = fwdPQ.isEmpty() ? Double.POSITIVE_INFINITY : fwdCost[fwdPQ.peek()];
            double bMin = bwdPQ.isEmpty() ? Double.POSITIVE_INFINITY : bwdLB[bwdPQ.peek()];
            if (fMin >= bestBound && bMin >= bestBound) break;

            boolean expandForward = !fwdPQ.isEmpty()
                    && (bwdPQ.isEmpty() || fMin <= bMin);

            if (expandForward) {
                int v      = fwdPQ.poll();
                fwdSettled++;
                double arr = fwdArrival[v];
                double cost = fwdCost[v];

                if (bwdIterIds[v] == currentIteration) {
                    double bound = cost + bwdLB[v];
                    if (bound < bestBound) {
                        bestBound   = bound;
                        meetingNode = v;
                    }
                }

                // Stall-on-demand (forward): if v can be reached more cheaply
                // via a downward edge from a higher-level node already settled
                // in the forward search, stall v's upward expansion.
                // Uses minTTF as lower bound on the dn-edge travel time.
                boolean fwdStalled = false;
                {
                    int dOff2 = dnOff[v];
                    int dEnd2 = dOff2 + dnLen[v];
                    for (int slot = dOff2; slot < dEnd2; slot++) {
                        int u = dnEdges[slot * S];
                        if (fwdIterIds[u] == currentIteration) {
                            int gIdx2 = dnEdges[slot * S + CHGraph.E_GIDX];
                            double altCost = fwdCost[u] + minTTF[gIdx2];
                            if (altCost < cost) {
                                fwdStalled = true;
                                break;
                            }
                        }
                    }
                }

                if (!fwdStalled) {
                    // Compute time bin once for this node
                    int bin = ((int) (arr * INV_BIN)) % NUM_BINS;
                    if (bin < 0) bin += NUM_BINS;
                    int binOff = bin * totalEdgeCount; // bin-major TTF offset

                    // Iterate upward out-edges (CSR: contiguous in memory)
                    int uOff = upOff[v];
                    int uEnd = uOff + upLen[v];
                    for (int slot = uOff; slot < uEnd; slot++) {
                        int eBase      = slot * S;
                        int w          = upEdges[eBase]; // toNode
                        int gIdx       = upEdges[eBase + CHGraph.E_GIDX];
                        double tTime   = ttf[binOff + gIdx];
                        double newArr  = arr + tTime;
                        double newCost = cost + tTime;

                        if (fwdIterIds[w] == currentIteration) {
                            if (newCost < fwdCost[w]) {
                                fwdArrival[w]    = newArr;
                                fwdCost[w]       = newCost;
                                fwdComingFrom[w] = v;
                                fwdUsedEdge[w]   = gIdx;
                                fwdPQ.decreaseKey(w, newCost);
                            } else if (newCost == fwdCost[w] && gIdx < fwdUsedEdge[w]) {
                                fwdArrival[w]    = newArr;
                                fwdComingFrom[w] = v;
                                fwdUsedEdge[w]   = gIdx;
                            }
                        } else {
                            setFwd(w, newArr, newCost, v, gIdx);
                            fwdPQ.insert(w, newCost);
                        }
                    }
                }

            } else {
                int v   = bwdPQ.poll();
                bwdSettled++;
                double lb = bwdLB[v];

                if (fwdIterIds[v] == currentIteration) {
                    double bound = fwdCost[v] + lb;
                    if (bound < bestBound) {
                        bestBound   = bound;
                        meetingNode = v;
                    }
                }

                // Stall-on-demand (backward): if v can be reached more cheaply
                // via an upward edge from a lower-level node already settled
                // in the backward search, stall v's downward expansion.
                boolean bwdStalled = false;
                {
                    int uOff2 = upOff[v];
                    int uEnd2 = uOff2 + upLen[v];
                    for (int slot = uOff2; slot < uEnd2; slot++) {
                        int w = upEdges[slot * S];
                        if (bwdIterIds[w] == currentIteration) {
                            int gIdx2 = upEdges[slot * S + CHGraph.E_GIDX];
                            double altCost = bwdLB[w] + minTTF[gIdx2];
                            if (altCost < lb) {
                                bwdStalled = true;
                                break;
                            }
                        }
                    }
                }

                if (!bwdStalled) {
                    // Iterate downward in-edges (CSR)
                    int dOff = dnOff[v];
                    int dEnd = dOff + dnLen[v];
                    for (int slot = dOff; slot < dEnd; slot++) {
                        int eBase = slot * S;
                        int y     = dnEdges[eBase]; // fromNode (higher-level)
                        int gIdx  = dnEdges[eBase + CHGraph.E_GIDX];
                        double newLB = lb + minTTF[gIdx];

                        if (bwdIterIds[y] == currentIteration) {
                            if (newLB < bwdLB[y]) {
                                bwdLB[y]         = newLB;
                                bwdComingFrom[y] = v;
                                bwdUsedEdge[y]   = gIdx;
                                bwdPQ.decreaseKey(y, newLB);
                            } else if (newLB == bwdLB[y] && gIdx < bwdUsedEdge[y]) {
                                bwdComingFrom[y] = v;
                                bwdUsedEdge[y]   = gIdx;
                            }
                        } else {
                            setBwd(y, newLB, v, gIdx);
                            bwdPQ.insert(y, newLB);
                        }
                    }
                }
            }
        }

        totalFwdSettled += fwdSettled;
        totalBwdSettled += bwdSettled;
        chQueryCount++;

        if (meetingNode < 0) return null;
        return constructPath(startIdx, meetingNode, startTime, person, vehicle);
    }

    // -------------------------------------------------------------------------
    // Path construction
    // -------------------------------------------------------------------------

    private Path constructPath(int startIdx, int meetingNode,
                               double startTime, Person person, Vehicle vehicle) {
        List<Node> nodeList = new ArrayList<>();
        List<Link> linkList = new ArrayList<>();

        nodeList.add(baseGraph.getNode(startIdx));

        // Forward part
        List<Integer> fwdEdges = new ArrayList<>();
        int curr = meetingNode;
        while (fwdComingFrom[curr] >= 0) {
            fwdEdges.add(fwdUsedEdge[curr]);
            curr = fwdComingFrom[curr];
        }
        Collections.reverse(fwdEdges);
        for (int gIdx : fwdEdges) {
            unpackEdge(gIdx, nodeList, linkList);
        }

        // Backward part
        curr = meetingNode;
        while (bwdComingFrom[curr] >= 0) {
            unpackEdge(bwdUsedEdge[curr], nodeList, linkList);
            curr = bwdComingFrom[curr];
        }

        // Compute actual travel time and cost
        double time = startTime;
        double cost = 0.0;
        for (Link link : linkList) {
            double travelTime = tt.getLinkTravelTime(link, time, person, vehicle);
            cost += td.getLinkTravelDisutility(link, time, person, vehicle);
            time += travelTime;
        }

        return new Path(nodeList, linkList, time - startTime, cost);
    }

    /**
     * Iterative (stack-based) edge unpacking.  Replaces the recursive version
     * to avoid stack overflow on deep shortcut chains (common with 750k+ node
     * networks) and to eliminate call-stack overhead.
     */
    private void unpackEdge(int gIdx, List<Node> nodeList, List<Link> linkList) {
        int[] stack = new int[64];
        int sp = 0;
        stack[sp++] = gIdx;

        while (sp > 0) {
            int e = stack[--sp];
            int orig = chGraph.edgeOrigLink[e];
            if (orig >= 0) {
                linkList.add(baseGraph.getLink(orig));
                nodeList.add(baseGraph.getLink(orig).getToNode());
            } else {
                if (sp + 2 > stack.length) {
                    stack = java.util.Arrays.copyOf(stack, stack.length * 2);
                }
                stack[sp++] = chGraph.edgeLower2[e];
                stack[sp++] = chGraph.edgeLower1[e];
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void advanceIteration() {
        currentIteration++;
        if (currentIteration == Integer.MAX_VALUE) {
            Arrays.fill(fwdIterIds, Integer.MIN_VALUE);
            Arrays.fill(bwdIterIds, Integer.MIN_VALUE);
            currentIteration = Integer.MIN_VALUE + 1;
        }
    }

    private void setFwd(int node, double arrival, double cost, int comingFrom, int usedEdge) {
        fwdArrival[node]    = arrival;
        fwdCost[node]       = cost;
        fwdComingFrom[node] = comingFrom;
        fwdUsedEdge[node]   = usedEdge;
        fwdIterIds[node]    = currentIteration;
    }

    private void setBwd(int node, double lb, int comingFrom, int usedEdge) {
        bwdLB[node]         = lb;
        bwdComingFrom[node] = comingFrom;
        bwdUsedEdge[node]   = usedEdge;
        bwdIterIds[node]    = currentIteration;
    }

    private static void logNoRoute(String from, String to) {
        LOG.warn("No route was found from {} to {}. Some possible reasons:", from, to);
        LOG.warn("  * Network is not connected.  Run NetworkUtils.cleanNetwork(...).");
        LOG.warn("  * Network for considered mode does not even exist.");
        LOG.warn("  * Network for considered mode is not connected to start/end point.");
        LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
    }
}
