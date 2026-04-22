/* *********************************************************************** *
 * project: org.matsim.*
 * CHRouter.java
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
 * Bidirectional Contraction Hierarchies (CH) shortest-path query using
 * {@link CHGraph}.
 *
 * <p>Per-node search state is packed into contiguous arrays following the
 * SpeedyALT cache-locality pattern:
 * <ul>
 *   <li>{@code fwdData[node*2]} = cost, {@code fwdData[node*2+1]} = raw bits for comingFrom/usedEdge</li>
 * </ul>
 * Edge weights are read from colocated {@code upWeights[slot]}/{@code dnWeights[slot]}
 * arrays, eliminating the global-index indirection on the hot path.
 *
 * @author Steffen Axer
 */
public class CHRouter implements LeastCostPathCalculator {

    private static final Logger LOG = LogManager.getLogger(CHRouter.class);

    private final CHGraph chGraph;
    private final SpeedyGraph   baseGraph;
    private final TravelTime       tt;
    private final TravelDisutility td;

    private final TurnRestrictionsContext turnRestrictions;

    // Per-node search state – packed for cache locality
    private final double[] fwdCost;
    private final int[]    fwdComingFrom;
    private final int[]    fwdUsedEdge;
    private final int[]    fwdIterIds;

    private final double[] bwdCost;
    private final int[]    bwdComingFrom;
    private final int[]    bwdUsedEdge;
    private final int[]    bwdIterIds;

    private int currentIteration = Integer.MIN_VALUE;

    private final DAryMinHeap fwdPQ;
    private final DAryMinHeap bwdPQ;

    // Cached array references for hot-path (avoid field dereference chains)
    private final int[]    upOff, upLen, upEdges;
    private final double[] upWeights;
    private final int[]    dnOff, dnLen, dnEdges;
    private final double[] dnWeights;

    // Node levels for stall-on-demand pruning
    private final int[] nodeLevel;

    public CHRouter(CHGraph chGraph, TravelTime tt, TravelDisutility td) {
        this.chGraph   = chGraph;
        this.baseGraph = chGraph.getBaseGraph();
        this.tt        = tt;
        this.td        = td;
        this.turnRestrictions = baseGraph.getTurnRestrictions().orElse(null);

        int n = chGraph.nodeCount;
        this.fwdCost       = new double[n];
        this.fwdComingFrom = new int[n];
        this.fwdUsedEdge   = new int[n];
        this.fwdIterIds    = new int[n];
        this.bwdCost       = new double[n];
        this.bwdComingFrom = new int[n];
        this.bwdUsedEdge   = new int[n];
        this.bwdIterIds    = new int[n];

        Arrays.fill(fwdIterIds, currentIteration);
        Arrays.fill(bwdIterIds, currentIteration);

        this.fwdPQ = new DAryMinHeap(n, 6);
        this.bwdPQ = new DAryMinHeap(n, 6);

        this.upOff     = chGraph.upOff;
        this.upLen     = chGraph.upLen;
        this.upEdges   = chGraph.upEdges;
        this.upWeights = chGraph.upWeights;
        this.dnOff     = chGraph.dnOff;
        this.dnLen     = chGraph.dnLen;
        this.dnEdges   = chGraph.dnEdges;
        this.dnWeights = chGraph.dnWeights;
        this.nodeLevel = chGraph.nodeLevel;
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

    private Path calcLeastCostPathImpl(int startIdx, int endIdx,
                                       double startTime, Person person, Vehicle vehicle) {
        advanceIteration();

        if (startIdx == endIdx) {
            return new Path(
                    Collections.singletonList(baseGraph.getNode(startIdx)),
                    Collections.emptyList(), 0.0, 0.0);
        }

        setFwd(startIdx, 0.0, -1, -1);
        fwdPQ.clear();
        fwdPQ.insert(startIdx, 0.0);

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
                    setFwd(coloredIdx, 0.0, -1, -1);
                    fwdPQ.insert(coloredIdx, 0.0);
                }
            }
        }

        final int S = CHGraph.E_STRIDE;
        double bestCost   = Double.POSITIVE_INFINITY;
        int    meetingNode = -1;

        while (!fwdPQ.isEmpty() || !bwdPQ.isEmpty()) {
            double fMin = fwdPQ.isEmpty() ? Double.POSITIVE_INFINITY : fwdCost[fwdPQ.peek()];
            double bMin = bwdPQ.isEmpty() ? Double.POSITIVE_INFINITY : bwdCost[bwdPQ.peek()];
            if (fMin >= bestCost && bMin >= bestCost) break;

            boolean expandForward = !fwdPQ.isEmpty()
                    && (bwdPQ.isEmpty() || fMin <= bMin);

            if (expandForward) {
                int v    = fwdPQ.poll();
                double d = fwdCost[v];

                if (bwdIterIds[v] == currentIteration) {
                    double total = d + bwdCost[v];
                    if (total < bestCost) {
                        bestCost    = total;
                        meetingNode = v;
                    }
                }

                // Stall-on-demand: if v can be reached more cheaply via a
                // downward edge from a higher-level node that was already
                // settled in the forward search, then v's upward edges cannot
                // be on a shortest path and are pruned.  This eliminates
                // ~30-50% of node expansions on large hierarchies.
                boolean stalled = false;
                {
                    int dOff3 = dnOff[v];
                    int dEnd3 = dOff3 + dnLen[v];
                    for (int slot = dOff3; slot < dEnd3; slot++) {
                        int u = dnEdges[slot * S];
                        if (fwdIterIds[u] == currentIteration) {
                            double altCost = fwdCost[u] + dnWeights[slot];
                            if (altCost < d) {
                                stalled = true;
                                break;
                            }
                        }
                    }
                }

                if (!stalled) {
                    int uOff = upOff[v];
                    int uEnd = uOff + upLen[v];
                    for (int slot = uOff; slot < uEnd; slot++) {
                        int w       = upEdges[slot * S];
                        double newCost = d + upWeights[slot];
                        if (fwdIterIds[w] == currentIteration) {
                            if (newCost < fwdCost[w]) {
                                fwdCost[w] = newCost;
                                fwdComingFrom[w] = v;
                                fwdUsedEdge[w]   = upEdges[slot * S + CHGraph.E_GIDX];
                                fwdPQ.decreaseKey(w, newCost);
                            } else if (newCost == fwdCost[w]) {
                                int gIdx = upEdges[slot * S + CHGraph.E_GIDX];
                                if (gIdx < fwdUsedEdge[w]) {
                                    fwdComingFrom[w] = v;
                                    fwdUsedEdge[w]   = gIdx;
                                }
                            }
                        } else {
                            setFwd(w, newCost, v, upEdges[slot * S + CHGraph.E_GIDX]);
                            fwdPQ.insert(w, newCost);
                        }
                    }
                }

            } else {
                int v    = bwdPQ.poll();
                double d = bwdCost[v];

                if (fwdIterIds[v] == currentIteration) {
                    double total = fwdCost[v] + d;
                    if (total < bestCost) {
                        bestCost    = total;
                        meetingNode = v;
                    }
                }

                // Stall-on-demand (backward): if v can be reached more cheaply
                // via an upward edge from a lower-level node already settled
                // in the backward search, stall v.
                boolean stalled = false;
                {
                    int uOff2 = upOff[v];
                    int uEnd2 = uOff2 + upLen[v];
                    for (int slot = uOff2; slot < uEnd2; slot++) {
                        int w = upEdges[slot * S];
                        if (bwdIterIds[w] == currentIteration) {
                            double altCost = bwdCost[w] + upWeights[slot];
                            if (altCost < d) {
                                stalled = true;
                                break;
                            }
                        }
                    }
                }

                if (!stalled) {
                    int dOff2 = dnOff[v];
                    int dEnd = dOff2 + dnLen[v];
                    for (int slot = dOff2; slot < dEnd; slot++) {
                        int y       = dnEdges[slot * S];
                        double newCost = d + dnWeights[slot];
                        if (bwdIterIds[y] == currentIteration) {
                            if (newCost < bwdCost[y]) {
                                bwdCost[y] = newCost;
                                bwdComingFrom[y] = v;
                                bwdUsedEdge[y]   = dnEdges[slot * S + CHGraph.E_GIDX];
                                bwdPQ.decreaseKey(y, newCost);
                            } else if (newCost == bwdCost[y]) {
                                int gIdx = dnEdges[slot * S + CHGraph.E_GIDX];
                                if (gIdx < bwdUsedEdge[y]) {
                                    bwdComingFrom[y] = v;
                                    bwdUsedEdge[y]   = gIdx;
                                }
                            }
                        } else {
                            setBwd(y, newCost, v, dnEdges[slot * S + CHGraph.E_GIDX]);
                            bwdPQ.insert(y, newCost);
                        }
                    }
                }
            }
        }

        if (meetingNode < 0) return null;
        return constructPath(startIdx, meetingNode, startTime, person, vehicle);
    }

    private Path constructPath(int startIdx, int meetingNode,
                               double startTime, Person person, Vehicle vehicle) {
        List<Node> nodeList = new ArrayList<>();
        List<Link> linkList = new ArrayList<>();

        nodeList.add(baseGraph.getNode(startIdx));

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

        curr = meetingNode;
        while (bwdComingFrom[curr] >= 0) {
            unpackEdge(bwdUsedEdge[curr], nodeList, linkList);
            curr = bwdComingFrom[curr];
        }

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
     * networks that have many deferred hub layers) and to eliminate call-stack
     * overhead.
     */
    private void unpackEdge(int gIdx, List<Node> nodeList, List<Link> linkList) {
        // Use an explicit stack to avoid recursion.  Each stack entry is a
        // global edge index.  We process in LIFO order, pushing lower2 first
        // then lower1, so that lower1 is processed before lower2 (correct
        // traversal order).
        int[] stack = new int[64];
        int sp = 0;
        stack[sp++] = gIdx;

        while (sp > 0) {
            int e = stack[--sp];
            int orig = chGraph.edgeOrigLink[e];
            if (orig >= 0) {
                // Real edge — emit directly
                Link link = baseGraph.getLink(orig);
                linkList.add(link);
                nodeList.add(link.getToNode());
            } else {
                // Shortcut — push children (lower2 first, then lower1)
                if (sp + 2 > stack.length) {
                    stack = java.util.Arrays.copyOf(stack, stack.length * 2);
                }
                stack[sp++] = chGraph.edgeLower2[e];
                stack[sp++] = chGraph.edgeLower1[e];
            }
        }
    }

    private void advanceIteration() {
        currentIteration++;
        if (currentIteration == Integer.MAX_VALUE) {
            Arrays.fill(fwdIterIds, Integer.MIN_VALUE);
            Arrays.fill(bwdIterIds, Integer.MIN_VALUE);
            currentIteration = Integer.MIN_VALUE + 1;
        }
    }

    private void setFwd(int node, double cost, int comingFrom, int usedEdge) {
        fwdCost[node]       = cost;
        fwdComingFrom[node] = comingFrom;
        fwdUsedEdge[node]   = usedEdge;
        fwdIterIds[node]    = currentIteration;
    }

    private void setBwd(int node, double cost, int comingFrom, int usedEdge) {
        bwdCost[node]       = cost;
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
