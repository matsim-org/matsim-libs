/* *********************************************************************** *
 * project: org.matsim.*
 * CHGraph.java
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

/**
 * CH (Contraction Hierarchies) overlay graph built on top of a {@link SpeedyGraph}.
 *
 * <h3>Memory layout (CSR – Compressed Sparse Row)</h3>
 * <p>Upward out-edges and downward in-edges are stored in two separate, contiguous
 * {@code int[]} arrays ({@link #upEdges}, {@link #dnEdges}).  For each node the
 * offset into that array and the number of edges are stored in
 * {@link #upOff}/{@link #upLen} and {@link #dnOff}/{@link #dnLen}.
 * This gives O(1) random access per node and sequential memory access when
 * iterating a node's edges — far better cache behaviour than a linked list.
 *
 * <p>Per-edge data (2 ints each, packed tightly):
 * <pre>
 *   [0] toNode / fromNode index   (toNode for up-edges, fromNode for dn-edges)
 *   [1] global edge index         (for TTF/weight lookup and edge unpacking)
 * </pre>
 * Edge metadata (origLink, lowerEdge1/2) is stored in separate global arrays
 * indexed by the global edge index, keeping the CSR compact for the query hot path.
 *
 * <h3>Global edge index layout</h3>
 * <p>Global indices are assigned contiguously per CSR slot:
 * up-edges use indices {@code [0, upEdgeCount)}, down-edges use
 * {@code [upEdgeCount, upEdgeCount + dnEdgeCount)}.  This ensures that
 * edges of the same node are contiguous in the TTF array.
 *
 * <h3>Weight layout</h3>
 * <p>Edge weights are colocated in contiguous {@code double[]} arrays
 * ({@link #upWeights}, {@link #dnWeights}) indexed by edge slot, so that
 * iterating a node's edges reads weight from the same cache line as the
 * target node index.
 *
 * <h3>TTF layout (bin-major, flat contiguous)</h3>
 * <p>{@link #ttf} is a single {@code double[NUM_BINS * edgeCount]} array where
 * {@code ttf[bin * edgeCount + globalIdx]} is the travel time for edge
 * {@code globalIdx} departing in time bin {@code bin}.  The bin-major layout
 * gives sequential memory access when iterating a node's edges (constant bin,
 * contiguous globalIdx values), which is the dominant access pattern in the
 * forward CH query.
 *
 * @author Steffen Axer
 */
public class CHGraph {

    /** Ints per edge in the CSR edge arrays (toNode + globalIdx only). */
    static final int E_STRIDE = 2;
    static final int E_NODE   = 0; // toNode (up) or fromNode (dn)
    static final int E_GIDX   = 1; // global edge index

    final int nodeCount;

    // --- upward out-edges (used by forward search) ---
    final int   upEdgeCount;
    final int[] upOff;       // upOff[node] = start index in upEdges for this node
    final int[] upLen;       // upLen[node] = number of upward edges for this node
    final int[] upEdges;     // E_STRIDE ints per edge, packed
    final double[] upWeights;   // upWeights[slot] = edge weight, colocated for cache locality

    // --- downward in-edges (used by backward search) ---
    final int   dnEdgeCount;
    final int[] dnOff;
    final int[] dnLen;
    final int[] dnEdges;
    final double[] dnWeights;

    // --- per-edge (global index) data ---
    final int totalEdgeCount;       // = upEdgeCount + dnEdgeCount (every edge appears in exactly one list)
    final double[] edgeWeights;     // edgeWeights[globalIdx] (kept for customizer compatibility)
    final int[]    edgeOrigLink;    // originalLinkIndex per global edge
    final int[]    edgeLower1;      // lowerEdge1 per global edge
    final int[]    edgeLower2;      // lowerEdge2 per global edge

    // Time-dependent TTF – bin-major flat contiguous array.
    // ttf[bin * totalEdgeCount + globalIdx] = travel time (seconds).
    // Bin-major layout gives sequential access when iterating a node's edges
    // (constant bin, contiguous globalIdx values).
    double[] ttf;
    double[] minTTF;

    // Change detection for incremental TTF customization.
    // ttfHash[globalIdx] = sum of TTF bins; NaN until first customization.
    double[] ttfHash;

    // Topological processing order for customization.
    // Ensures lower (component) edges are processed before their parent shortcuts.
    final int[] customizeOrder;

    // Node levels (contraction rank): nodeLevel[i] = level of node i.
    // Level 0 = contracted first, nodeCount-1 = contracted last.
    // Used by CH-based LeastCostPathTree for the downward sweep.
    final int[] nodeLevel;

    // Precomputed sweep order: nodes sorted by DECREASING level (highest first).
    // sweepOrder[0] = node with highest level (contracted last).
    // Used for the downward sweep in CH one-to-all search.
    final int[] sweepOrder;

    // --- Reverse CSR for push-based Phase 2 sweep ---
    //
    // dnOutOff/dnOutLen/dnOutEdges: for each node u, its OUTGOING downward edges
    // (u→w where rank(u) > rank(w)).  This is the reverse of dnEdges (which stores
    // incoming downward edges per node w).  Used by forward Phase 2 to push costs
    // from settled nodes to lower-ranked successors.
    final int[] dnOutOff;
    final int[] dnOutLen;
    final int[] dnOutEdges;     // E_STRIDE ints per edge: [targetNode, globalEdgeIdx]
    double[] dnOutWeights;      // colocated minTTF per reverse-dnOut edge slot

    // upInOff/upInLen/upInEdges: for each node u, its INCOMING upward edges
    // (w→u where rank(w) < rank(u)).  This is the reverse of upEdges (which stores
    // outgoing upward edges per node v).  Used by backward Phase 2 to push costs
    // from settled nodes to lower-ranked predecessors.
    final int[] upInOff;
    final int[] upInLen;
    final int[] upInEdges;      // E_STRIDE ints per edge: [sourceNode, globalEdgeIdx]
    double[] upInWeights;       // colocated minTTF per reverse-upIn edge slot

    /**
     * Fingerprint of the last TTF customization.  Used by {@link CHTTFCustomizer}
     * to skip redundant re-customization when travel times have not changed.
     * The fingerprint is the sum of travel times sampled at a small number of
     * representative (edge, time-bin) pairs.  {@code Double.NaN} when no
     * customization has been performed yet.
     */
    volatile double customizationFingerprint = Double.NaN;

    /** Returns the total number of edges (upward + downward) in the CH overlay graph. */
    public int getTotalEdgeCount() {
        return this.totalEdgeCount;
    }

    private final SpeedyGraph baseGraph;

    CHGraph(SpeedyGraph baseGraph, int nodeCount,
                  int upEdgeCount, int[] upOff, int[] upLen, int[] upEdges, double[] upWeights,
                  int dnEdgeCount, int[] dnOff, int[] dnLen, int[] dnEdges, double[] dnWeights,
                  int totalEdgeCount, int[] edgeOrigLink, int[] edgeLower1, int[] edgeLower2,
                  int[] customizeOrder, int[] nodeLevel) {
        this.baseGraph      = baseGraph;
        this.nodeCount      = nodeCount;
        this.upEdgeCount    = upEdgeCount;
        this.upOff          = upOff;
        this.upLen          = upLen;
        this.upEdges        = upEdges;
        this.upWeights      = upWeights;
        this.dnEdgeCount    = dnEdgeCount;
        this.dnOff          = dnOff;
        this.dnLen          = dnLen;
        this.dnEdges        = dnEdges;
        this.dnWeights      = dnWeights;
        this.totalEdgeCount = totalEdgeCount;
        this.edgeOrigLink   = edgeOrigLink;
        this.edgeLower1     = edgeLower1;
        this.edgeLower2     = edgeLower2;
        this.customizeOrder = customizeOrder;
        this.nodeLevel      = nodeLevel;
        this.edgeWeights    = new double[totalEdgeCount];

        // Precompute sweep order: nodes sorted by decreasing level
        this.sweepOrder = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) sweepOrder[i] = i;
        // Sort by decreasing nodeLevel using a simple insertion sort partitioned
        // by level (levels are contiguous 0..nodeCount-1, so we can use inverse mapping).
        int[] invLevel = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            invLevel[nodeLevel[i]] = i;
        }
        for (int i = 0; i < nodeCount; i++) {
            sweepOrder[i] = invLevel[nodeCount - 1 - i];
        }

        // Pre-allocate bin-major flat TTF arrays.
        this.ttf    = new double[CHTTFCustomizer.NUM_BINS * totalEdgeCount];
        this.minTTF = new double[totalEdgeCount];
        this.ttfHash = new double[totalEdgeCount];
        java.util.Arrays.fill(this.ttfHash, Double.NaN);

        // Build reverse CSR: dnOutEdges (reverse of dnEdges).
        // dnEdges[w] stores incoming edges u→w.  dnOutEdges[u] stores outgoing edges u→w.
        this.dnOutOff = new int[nodeCount];
        this.dnOutLen = new int[nodeCount];
        // Pass 1: count outgoing dn-edges per source node u
        for (int w = 0; w < nodeCount; w++) {
            int dOff = dnOff[w];
            int dEnd = dOff + dnLen[w];
            for (int slot = dOff; slot < dEnd; slot++) {
                int u = dnEdges[slot * E_STRIDE + E_NODE];
                dnOutLen[u]++;
            }
        }
        // Compute offsets (prefix sum)
        int runSum = 0;
        for (int u = 0; u < nodeCount; u++) {
            dnOutOff[u] = runSum;
            runSum += dnOutLen[u];
        }
        // Pass 2: fill reverse CSR
        this.dnOutEdges = new int[runSum * E_STRIDE];
        int[] cursor = new int[nodeCount];
        for (int w = 0; w < nodeCount; w++) {
            int dOff = dnOff[w];
            int dEnd = dOff + dnLen[w];
            for (int slot = dOff; slot < dEnd; slot++) {
                int eBase = slot * E_STRIDE;
                int u    = dnEdges[eBase + E_NODE];
                int gIdx = dnEdges[eBase + E_GIDX];
                int outSlot = dnOutOff[u] + cursor[u];
                int outBase = outSlot * E_STRIDE;
                dnOutEdges[outBase + E_NODE] = w;
                dnOutEdges[outBase + E_GIDX] = gIdx;
                cursor[u]++;
            }
        }

        // Build reverse CSR: upInEdges (reverse of upEdges).
        // upEdges[v] stores outgoing edges v→w.  upInEdges[w] stores incoming edges v→w.
        this.upInOff = new int[nodeCount];
        this.upInLen = new int[nodeCount];
        // Pass 1: count incoming up-edges per target node w
        for (int v = 0; v < nodeCount; v++) {
            int uOff = upOff[v];
            int uEnd = uOff + upLen[v];
            for (int slot = uOff; slot < uEnd; slot++) {
                int w = upEdges[slot * E_STRIDE + E_NODE];
                upInLen[w]++;
            }
        }
        // Compute offsets
        runSum = 0;
        for (int w = 0; w < nodeCount; w++) {
            upInOff[w] = runSum;
            runSum += upInLen[w];
        }
        // Pass 2: fill reverse CSR
        this.upInEdges = new int[runSum * E_STRIDE];
        java.util.Arrays.fill(cursor, 0);
        for (int v = 0; v < nodeCount; v++) {
            int uOff = upOff[v];
            int uEnd = uOff + upLen[v];
            for (int slot = uOff; slot < uEnd; slot++) {
                int eBase = slot * E_STRIDE;
                int w    = upEdges[eBase + E_NODE];
                int gIdx = upEdges[eBase + E_GIDX];
                int inSlot = upInOff[w] + cursor[w];
                int inBase = inSlot * E_STRIDE;
                upInEdges[inBase + E_NODE] = v;
                upInEdges[inBase + E_GIDX] = gIdx;
                cursor[w]++;
            }
        }

        // Allocate colocated weight arrays for reverse CSRs.
        // Populated by the customizer's propagateWeightsToCSR method.
        this.dnOutWeights = new double[this.dnOutEdges.length / E_STRIDE];
        this.upInWeights  = new double[this.upInEdges.length / E_STRIDE];
    }

    SpeedyGraph getBaseGraph() {
        return baseGraph;
    }

    Node getNode(int nodeIndex) {
        return baseGraph.getNode(nodeIndex);
    }
}
