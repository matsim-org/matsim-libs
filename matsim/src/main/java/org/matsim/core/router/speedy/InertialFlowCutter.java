/* *********************************************************************** *
 * project: org.matsim.*
 * InertialFlowCutter.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Computes a nested-dissection node ordering for a {@link SpeedyGraph} using
 * coordinate-based inertial flow partitioning with graph-based refinement.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Project all nodes in the sub-graph onto several axis directions
 *       (horizontal, vertical, and two diagonals).</li>
 *   <li>For each direction, split nodes at the projection median into two halves.
 *       The direction with the best (smallest, most balanced) separator is used.</li>
 *   <li>The boundary is converted to a <b>one-sided vertex separator</b>: only
 *       nodes on the smaller boundary side become separator nodes.</li>
 *   <li><b>Degree-weighted scoring</b>: separators are scored by
 *       &sum;(subgraphDeg&sup2;) rather than uniform node count, since each
 *       separator node creates O(deg&sup2;) shortcuts during CH contraction.</li>
 *   <li><b>FM refinement</b>: Fiduccia&ndash;Mattheyses local search minimises
 *       the edge cut of the underlying bipartition, using a bucket linked-list
 *       for O(1) max-gain lookup.  Up to 5 passes until fixpoint.</li>
 *   <li><b>Max-flow refinement</b>: Dinic&rsquo;s algorithm on a node-split flow
 *       network finds the minimum vertex separator between the two partition
 *       sides.  Each internal node is split into v_in/v_out with capacity 1;
 *       original edges have capacity &infin;.</li>
 *   <li>The separator nodes receive the highest contraction levels.
 *       The two partitions are recursively dissected.</li>
 * </ol>
 *
 * <p>This eliminates witness searches during ordering and is dramatically faster
 * than priority-queue-based ordering for large networks.
 *
 * <h3>Data-structure optimisations</h3>
 * <ul>
 *   <li><b>CSR adjacency</b>: the symmetric (undirected) adjacency is stored in
 *       Compressed Sparse Row format ({@code adjData} / {@code adjOffset}) built
 *       once at construction time.  This replaces the previous {@code int[][]}
 *       representation (one heap object per node), giving a single contiguous
 *       allocation, better cache behaviour, and lower GC pressure.</li>
 *   <li><b>Pre-allocated scratch arrays</b>: {@code bfsDist} and {@code toCompact}
 *       (each of size {@code nodeCount}) are allocated once and reused across all
 *       max-flow calls, avoiding ~8 MB heap pressure per call.  Concurrent
 *       ForkJoin tasks write only to their own (disjoint) node indices.</li>
 *   <li><b>Per-thread scratch buffers</b>: projection arrays, sort indices/temp,
 *       and FM move-tracking arrays are held in {@code ThreadLocal} storage and
 *       resized only when a larger subgraph is encountered, eliminating per-call
 *       allocation throughout the recursive decomposition.</li>
 *   <li><b>Explicit generation passing</b>: the subgraph generation stamp is
 *       returned from {@link #markSubGraph} and threaded as a plain {@code int}
 *       parameter instead of a {@code ThreadLocal}, eliminating a hash-table
 *       lookup on every subgraph-membership test in hot inner loops.</li>
 *   <li><b>Static direction arrays</b>: the projection-direction constants are
 *       pre-built as {@code static final} arrays, avoiding repeated inline
 *       allocation inside {@code findSeparator}.</li>
 * </ul>
 *
 * <p>References: Dibbelt et al. (2016) "Customizable Contraction Hierarchies",
 * Hamann &amp; Strasser (2018) "Graph Bisection with Pareto Optimization",
 * Fiduccia &amp; Mattheyses (1982) "A Linear-Time Heuristic for Improving
 * Network Partitions".
 *
 * @author Steffen Axer
 */
public class InertialFlowCutter {

    /**
     * Result of {@link #computeOrderWithBatches()}: contraction order plus
     * partition structure for parallel contraction.
     */
    public static class NDOrderResult {
        public final int[] order;
        public final List<List<int[]>> rounds;
        /** Wall-clock time spent computing the ordering (nanoseconds). */
        public final long elapsedNanos;

        NDOrderResult(int[] order, List<List<int[]>> rounds, long elapsedNanos) {
            this.order = order;
            this.rounds = rounds;
            this.elapsedNanos = elapsedNanos;
        }
    }

    private static final Logger LOG = LogManager.getLogger(InertialFlowCutter.class);

    /** Minimum sub-graph size below which we stop recursing and order arbitrarily. */
    private static final int MIN_PARTITION_SIZE = 2;

    // ---- Network-size-adaptive parameters ----

    private final int fmMinSize;
    private final int fmMaxPasses;
    private final int maxflowMinSize;
    private final int maxflowBorderDepth;
    private final int parallelMinSize;
    private final int reducedDirectionsThreshold;
    private final int reducedRatiosThreshold;

    private final SpeedyGraph graph;

    // Node coordinates (extracted once, read-only)
    private double[] nodeX;
    private double[] nodeY;

    // ---- CSR adjacency (symmetric/undirected, built once in constructor) ----
    /**
     * Concatenated neighbor data in CSR format.
     * Node {@code i}'s neighbors are {@code adjData[adjOffset[i] .. adjOffset[i+1])}.
     */
    private int[] adjData;
    /**
     * CSR offset array (size {@code nodeCount + 1}).
     */
    private int[] adjOffset;

    // ---- Pre-allocated scratch arrays (size = nodeCount) ----
    // Concurrent ForkJoin tasks operate on disjoint node-index ranges -> thread-safe.
    // Each user must reset touched entries to 0 after use.
    private int[] scratchSide;
    private int[] scratchBoundary;   // generation-stamped boundary markers
    private int[] inSubGraphGen;     // subgraph generation stamp per node
    private int[] bfsDist;           // max-flow BFS distances  (0 = not visited)
    private int[] toCompact;         // max-flow compact mapping (0 = not in subgraph)
    private int[] fmGain;
    private int[] fmNext;
    private int[] fmPrev;

    private final AtomicInteger scratchBoundaryGen = new AtomicInteger(0);
    private final AtomicInteger subGraphGenCounter  = new AtomicInteger(0);

    // ---- Per-thread scratch buffers (static: shared across all IFC instances) ----
    // Resized lazily (capacity doubled) to handle arbitrarily large subgraphs.
    private static final ThreadLocal<double[]> TL_PROJ     = ThreadLocal.withInitial(() -> new double[1024]);
    private static final ThreadLocal<int[]>    TL_SORT_IDX = ThreadLocal.withInitial(() -> new int[1024]);
    private static final ThreadLocal<int[]>    TL_SORT_TMP = ThreadLocal.withInitial(() -> new int[1024]);
    private static final ThreadLocal<int[]>    TL_ALLNODES = ThreadLocal.withInitial(() -> new int[2048]);
    private static final ThreadLocal<int[]>    TL_MOVENODE = ThreadLocal.withInitial(() -> new int[2048]);
    private static final ThreadLocal<int[]>    TL_MOVEFROM = ThreadLocal.withInitial(() -> new int[2048]);

    // ---- Static direction arrays (avoid per-call allocation in findSeparator) ----
    private static final double[][] ALL_DIRECTIONS = {
        {1, 0}, {0, 1}, {1, 1}, {1, -1},
        {2, 1}, {1, 2}, {2, -1}, {1, -2},
        {3, 1}, {1, 3}, {3, -1}, {1, -3},
        {4, 1}, {1, 4}, {4, -1}, {1, -4}
    };
    private static final double[][] DIRS_4 = Arrays.copyOf(ALL_DIRECTIONS, 4);
    private static final double[][] DIRS_8 = Arrays.copyOf(ALL_DIRECTIONS, 8);

    /**
     * Creates an InertialFlowCutter with auto-tuned parameters.
     *
     * @param graph  the base graph
     * @param params pre-computed parameters from {@link RoutingParameterTuner}
     */
    public InertialFlowCutter(SpeedyGraph graph, IFCParams params) {
        this.graph = graph;
        int n = graph.nodeCount;

        this.fmMinSize                 = params.fmMinSize();
        this.fmMaxPasses               = params.fmMaxPasses();
        this.maxflowMinSize            = params.maxflowMinSize();
        this.maxflowBorderDepth        = params.maxflowBorderDepth();
        this.parallelMinSize           = params.parallelMinSize();
        this.reducedDirectionsThreshold = params.reducedDirectionsThreshold();
        this.reducedRatiosThreshold    = params.reducedRatiosThreshold();

        LOG.debug("IFC parameters (auto-tuned) for {} nodes: fmMinSize={}, maxflowMinSize={}, " +
                        "reducedRatiosThreshold={}", n, fmMinSize, maxflowMinSize, reducedRatiosThreshold);

        initScratch(n);
    }

    /**
     * Creates an InertialFlowCutter with legacy 3-tier parameter selection.
     * Kept for backward compatibility.
     */
    public InertialFlowCutter(SpeedyGraph graph) {
        this.graph = graph;
        int n = graph.nodeCount;

        if (n < 200_000) {
            fmMinSize                 = 200;
            fmMaxPasses               = 3;
            maxflowMinSize            = 500;
            maxflowBorderDepth        = 5;
            parallelMinSize           = 1000;
            reducedDirectionsThreshold = 300;
            reducedRatiosThreshold    = 10_000;
        } else if (n < 500_000) {
            fmMinSize                 = 100;
            fmMaxPasses               = 4;
            maxflowMinSize            = 200;
            maxflowBorderDepth        = 6;
            parallelMinSize           = 2000;
            reducedDirectionsThreshold = 500;
            reducedRatiosThreshold    = 5000;
        } else {
            fmMinSize                 = 200;
            fmMaxPasses               = 3;
            maxflowMinSize            = 1000;
            maxflowBorderDepth        = 5;
            parallelMinSize           = 2000;
            reducedDirectionsThreshold = 1000;
            reducedRatiosThreshold    = 8000;
        }

        LOG.debug("IFC parameters (legacy tier) for {} nodes: fmMinSize={}, maxflowMinSize={}, " +
                        "reducedRatiosThreshold={}", n, fmMinSize, maxflowMinSize, reducedRatiosThreshold);

        initScratch(n);
    }

    /**
     * Common initialisation: extract node coordinates, allocate scratch arrays,
     * and build the CSR adjacency from the directed SpeedyGraph.
     */
    private void initScratch(int n) {
        this.nodeX = new double[n];
        this.nodeY = new double[n];

        for (int i = 0; i < n; i++) {
            var node = graph.getNode(i);
            if (node != null) {
                nodeX[i] = node.getCoord().getX();
                nodeY[i] = node.getCoord().getY();
            }
        }

        this.scratchSide     = new int[n];
        this.scratchBoundary = new int[n];
        this.inSubGraphGen   = new int[n];
        this.bfsDist         = new int[n];
        this.toCompact       = new int[n];
        this.fmGain          = new int[n];
        this.fmNext          = new int[n];
        this.fmPrev          = new int[n];

        buildSymmetricAdjacency();
    }

    // ---- Subgraph membership helpers ----

    /**
     * Marks all nodes as belonging to the current subgraph.
     * Returns the generation stamp; pass this to all methods that test membership.
     */
    private int markSubGraph(int[] subNodes) {
        int gen = subGraphGenCounter.incrementAndGet();
        for (int node : subNodes) inSubGraphGen[node] = gen;
        return gen;
    }

    /** Gets a unique boundary generation value (thread-safe). */
    private int nextBoundaryGen() {
        return scratchBoundaryGen.incrementAndGet();
    }

    // ---- ThreadLocal buffer helpers ----

    /** Ensures the {@code double[]} ThreadLocal buffer has at least {@code minLen} capacity. */
    private static double[] ensureCapDbl(ThreadLocal<double[]> tl, int minLen) {
        double[] buf = tl.get();
        if (buf.length >= minLen) return buf;
        int cap = buf.length;
        while (cap < minLen) cap <<= 1;
        tl.set(buf = new double[cap]);
        return buf;
    }

    /** Ensures the {@code int[]} ThreadLocal buffer has at least {@code minLen} capacity. */
    private static int[] ensureCapInt(ThreadLocal<int[]> tl, int minLen) {
        int[] buf = tl.get();
        if (buf.length >= minLen) return buf;
        int cap = buf.length;
        while (cap < minLen) cap <<= 1;
        tl.set(buf = new int[cap]);
        return buf;
    }

    /**
     * Computes a nested-dissection contraction order.
     * @return array where {@code order[i]} is the contraction level of node {@code i}.
     */
    public int[] computeOrder() {
        int n = graph.nodeCount;
        int[] order = new int[n];
        Arrays.fill(order, -1);

        int[] nodes = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (graph.getNode(i) != null) nodes[count++] = i;
        }
        nodes = Arrays.copyOf(nodes, count);

        int[] levelCounter = new int[]{0};
        recursiveDissect(nodes, order, levelCounter);

        for (int i = 0; i < n; i++) {
            if (order[i] < 0) order[i] = levelCounter[0]++;
        }

        LOG.debug("Nested dissection ordering computed for {} nodes", n);
        return order;
    }

    /**
     * Computes a nested-dissection contraction order plus a partition
     * structure for parallel contraction.
     */
    public NDOrderResult computeOrderWithBatches() {
        long startNanos = System.nanoTime();
        int n = graph.nodeCount;
        int[] order = new int[n];
        Arrays.fill(order, -1);

        int[] nodes = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (graph.getNode(i) != null) nodes[count++] = i;
        }
        nodes = Arrays.copyOf(nodes, count);

        int nThreads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = (count >= parallelMinSize && nThreads > 1) ? new ForkJoinPool(nThreads) : null;

        NDCell root = recursiveDissectWithBatches(nodes, 0, pool);

        if (pool != null) pool.shutdown();

        Map<Integer, List<int[]>> cellsByDepth = new java.util.HashMap<>();
        int[] levelCounter = {0};
        assignLevels(root, order, levelCounter, cellsByDepth);

        for (int i = 0; i < n; i++) {
            if (order[i] < 0) order[i] = levelCounter[0]++;
        }

        int maxDepth = 0;
        for (int d : cellsByDepth.keySet()) {
            if (d > maxDepth) maxDepth = d;
        }
        List<List<int[]>> rounds = new ArrayList<>();
        for (int d = maxDepth; d >= 0; d--) {
            List<int[]> cells = cellsByDepth.get(d);
            if (cells != null && !cells.isEmpty()) rounds.add(cells);
        }

        long elapsed = System.nanoTime() - startNanos;
        LOG.debug("Nested dissection ordering with batches: {} nodes, {} rounds, {}s",
                n, rounds.size(), String.format(java.util.Locale.US, "%.1f", elapsed / 1_000_000_000.0));
        return new NDOrderResult(order, rounds, elapsed);
    }

    /**
     * Lightweight tree node representing one recursive dissection step.
     */
    private static class NDCell {
        final int depth;
        final int[] leafNodes;
        final int[] separator;
        final NDCell childA;
        final NDCell childB;

        NDCell(int depth, int[] leafNodes) {
            this.depth = depth;
            this.leafNodes = leafNodes;
            this.separator = null;
            this.childA = null;
            this.childB = null;
        }

        NDCell(int depth, int[] separator, NDCell childA, NDCell childB) {
            this.depth = depth;
            this.leafNodes = null;
            this.separator = separator;
            this.childA = childA;
            this.childB = childB;
        }
    }

    private NDCell recursiveDissectWithBatches(int[] subNodes, int depth, ForkJoinPool pool) {
        if (subNodes.length <= MIN_PARTITION_SIZE) {
            return new NDCell(depth, subNodes);
        }

        int[][] result = findSeparator(subNodes);
        int[] partA     = result[0];
        int[] separator = result[1];
        int[] partB     = result[2];

        NDCell cellA, cellB;
        if (pool != null && (partA.length + partB.length) >= parallelMinSize) {
            ForkJoinTask<NDCell> taskA = pool.submit(() ->
                    recursiveDissectWithBatches(partA, depth + 1, pool));
            cellB = recursiveDissectWithBatches(partB, depth + 1, pool);
            cellA = taskA.join();
        } else {
            cellA = recursiveDissectWithBatches(partA, depth + 1, pool);
            cellB = recursiveDissectWithBatches(partB, depth + 1, pool);
        }

        return new NDCell(depth, separator, cellA, cellB);
    }

    private static void assignLevels(NDCell cell, int[] order, int[] levelCounter,
                                      Map<Integer, List<int[]>> cellsByDepth) {
        if (cell.leafNodes != null) {
            int ci = 0;
            int[] cellOrdered = new int[cell.leafNodes.length];
            for (int node : cell.leafNodes) {
                if (order[node] < 0) {
                    order[node] = levelCounter[0]++;
                    cellOrdered[ci++] = node;
                }
            }
            if (ci > 0) {
                cellsByDepth.computeIfAbsent(cell.depth, k -> new ArrayList<>())
                        .add(Arrays.copyOf(cellOrdered, ci));
            }
            return;
        }

        assignLevels(cell.childA, order, levelCounter, cellsByDepth);
        assignLevels(cell.childB, order, levelCounter, cellsByDepth);

        int si = 0;
        int[] sepOrdered = new int[cell.separator.length];
        for (int node : cell.separator) {
            if (order[node] < 0) {
                order[node] = levelCounter[0]++;
                sepOrdered[si++] = node;
            }
        }
        if (si > 0) {
            cellsByDepth.computeIfAbsent(cell.depth, k -> new ArrayList<>())
                    .add(Arrays.copyOf(sepOrdered, si));
        }
    }

    private void recursiveDissect(int[] subNodes, int[] order, int[] levelCounter) {
        if (subNodes.length <= MIN_PARTITION_SIZE) {
            for (int node : subNodes) {
                if (order[node] < 0) order[node] = levelCounter[0]++;
            }
            return;
        }

        int[][] result = findSeparator(subNodes);
        int[] partA     = result[0];
        int[] separator = result[1];
        int[] partB     = result[2];

        recursiveDissect(partA, order, levelCounter);
        recursiveDissect(partB, order, levelCounter);

        for (int node : separator) {
            if (order[node] < 0) order[node] = levelCounter[0]++;
        }
    }

    /**
     * Find a separator for the given sub-graph using inertial flow with
     * graph-based refinement (FM + max-flow).
     * Returns [partitionA, separator, partitionB].
     */
    private int[][] findSeparator(int[] subNodes) {
        int n = subNodes.length;

        double[][] directions;
        if (n < reducedDirectionsThreshold) {
            directions = DIRS_4;
        } else if (n < 5000) {
            directions = DIRS_8;
        } else {
            directions = ALL_DIRECTIONS;
        }

        int gen = markSubGraph(subNodes);

        int[][] bestResult = null;
        long bestScore = Long.MAX_VALUE;

        for (double[] dir : directions) {
            int[][] result = tryDirection(subNodes, dir[0], dir[1]);
            if (result != null) {
                int balance = Math.abs(result[0].length - result[2].length);
                long score = scoreSimple(result[1], balance);
                if (score < bestScore) {
                    bestScore = score;
                    bestResult = result;
                }
            }
        }

        if (bestResult == null) {
            return trivialSplit(subNodes);
        }

        long refScore = scoreSeparator(bestResult[1],
                Math.abs(bestResult[0].length - bestResult[2].length), gen);

        if (n >= fmMinSize) {
            bestResult = tryFMBothOrientations(bestResult, subNodes, refScore, gen);
            refScore = scoreSeparator(bestResult[1],
                    Math.abs(bestResult[0].length - bestResult[2].length), gen);
        }

        if (n >= maxflowMinSize) {
            int adaptiveDepth = Math.max(3, Math.min(maxflowBorderDepth,
                    (int) (12.0 - Math.log(n) / Math.log(2))));
            int[][] mfResult = maxFlowRefineWideDepth(bestResult, adaptiveDepth, gen);
            if (mfResult != null && isValidSeparator(mfResult, gen)) {
                int mfBal = Math.abs(mfResult[0].length - mfResult[2].length);
                long mfScore = scoreSeparator(mfResult[1], mfBal, gen);
                if (mfScore < refScore) {
                    bestResult = mfResult;
                }
            }
        }

        return bestResult;
    }

    // ========================= Degree-weighted scoring =========================

    private int[][] tryFMBothOrientations(int[][] current, int[] subNodes,
                                           long currentScore, int gen) {
        int[][] best = current;
        long bestScore = currentScore;

        int[][] fmResult1 = fmRefineSeparator(best, subNodes, false, gen);
        if (isValidSeparator(fmResult1, gen)) {
            int fmBal = Math.abs(fmResult1[0].length - fmResult1[2].length);
            long fmScore = scoreSeparator(fmResult1[1], fmBal, gen);
            if (fmScore < bestScore) {
                best = fmResult1;
                bestScore = fmScore;
            }
        }
        int[][] fmResult2 = fmRefineSeparator(best, subNodes, true, gen);
        if (isValidSeparator(fmResult2, gen)) {
            int fmBal = Math.abs(fmResult2[0].length - fmResult2[2].length);
            long fmScore = scoreSeparator(fmResult2[1], fmBal, gen);
            if (fmScore < bestScore) {
                best = fmResult2;
            }
        }
        return best;
    }

    /**
     * Degree-weighted separator score: 256 per node plus sum(subgraphDeg^2).
     *
     * @param gen current subgraph generation stamp
     */
    private long scoreSeparator(int[] separator, int balance, int gen) {
        long cost = 0;
        final int[] data = adjData;
        final int[] off  = adjOffset;
        for (int s : separator) {
            int subDeg = 0;
            for (int ei = off[s], eiEnd = off[s + 1]; ei < eiEnd; ei++) {
                if (inSubGraphGen[data[ei]] == gen) subDeg++;
            }
            cost += 256L + (long) subDeg * subDeg;
        }
        return cost + balance;
    }

    private static long scoreSimple(int[] separator, int balance) {
        return separator.length * 256L + balance;
    }

    private boolean isValidSeparator(int[][] result, int gen) {
        int[] partA = result[0];
        int[] sep   = result[1];
        int[] partB = result[2];
        if (partA.length == 0 || partB.length == 0 || sep.length == 0) return false;

        int genSep = nextBoundaryGen();
        int genB   = nextBoundaryGen();
        int[] mark = scratchBoundary;
        for (int s : sep) mark[s] = genSep;
        for (int b : partB) mark[b] = genB;

        final int[] data = adjData;
        final int[] off  = adjOffset;
        for (int a : partA) {
            for (int ei = off[a], eiEnd = off[a + 1]; ei < eiEnd; ei++) {
                int w = data[ei];
                if (inSubGraphGen[w] == gen && mark[w] == genB) return false;
            }
        }
        return true;
    }

    // ========================= FM Refinement ==================================

    /**
     * Fiduccia-Mattheyses refinement on the bipartition underlying the separator.
     * Uses per-thread scratch buffers to avoid per-call heap allocation.
     *
     * @param gen current subgraph generation stamp
     */
    private int[][] fmRefineSeparator(int[][] result, int[] subNodes,
                                       boolean sepToA, int gen) {
        int[] partA = result[0];
        int[] sep   = result[1];
        int[] partB = result[2];

        int totalN = partA.length + sep.length + partB.length;
        if (totalN < fmMinSize || sep.length <= 1) return result;

        int[] allNodes = ensureCapInt(TL_ALLNODES, totalN);
        System.arraycopy(partA, 0, allNodes, 0, partA.length);
        System.arraycopy(sep, 0, allNodes, partA.length, sep.length);
        System.arraycopy(partB, 0, allNodes, partA.length + sep.length, partB.length);

        int[] side = scratchSide;
        if (sepToA) {
            for (int v : partA) side[v] = 1;
            for (int v : sep) side[v] = 1;
            for (int v : partB) side[v] = 2;
        } else {
            for (int v : partA) side[v] = 1;
            for (int v : sep) side[v] = 2;
            for (int v : partB) side[v] = 2;
        }

        int sizeA = sepToA ? (partA.length + sep.length) : partA.length;
        int sizeB = sepToA ? partB.length : (sep.length + partB.length);
        int minSize = Math.max(2, totalN / 4);

        int[] gain = fmGain;
        final int[] data = adjData;
        final int[] off  = adjOffset;
        int maxDeg = 0;
        for (int i = 0; i < totalN; i++) {
            int v = allNodes[i];
            int ext = 0, internal = 0;
            for (int ei = off[v], eiEnd = off[v + 1]; ei < eiEnd; ei++) {
                int w = data[ei];
                if (inSubGraphGen[w] != gen) continue;
                if (side[w] != side[v]) ext++;
                else internal++;
            }
            gain[v] = ext - internal;
            int deg = ext + internal;
            if (deg > maxDeg) maxDeg = deg;
        }

        if (maxDeg == 0) return result;

        int bucketOffset = maxDeg;
        int numBuckets = 2 * maxDeg + 1;
        int[] bucketHead = new int[numBuckets];
        int[] fmN = fmNext;
        int[] fmP = fmPrev;

        boolean improved = true;
        for (int pass = 0; pass < fmMaxPasses && improved; pass++) {
            improved = false;

            if (pass > 0) {
                maxDeg = 0;
                for (int i = 0; i < totalN; i++) {
                    int v = allNodes[i];
                    int ext = 0, internal = 0;
                    for (int ei = off[v], eiEnd = off[v + 1]; ei < eiEnd; ei++) {
                        int w = data[ei];
                        if (inSubGraphGen[w] != gen) continue;
                        if (side[w] != side[v]) ext++;
                        else internal++;
                    }
                    gain[v] = ext - internal;
                    int deg = ext + internal;
                    if (deg > maxDeg) maxDeg = deg;
                }
                if (maxDeg == 0) break;
                bucketOffset = maxDeg;
                numBuckets = 2 * maxDeg + 1;
                if (bucketHead.length < numBuckets) {
                    bucketHead = new int[numBuckets];
                }
            }

            Arrays.fill(bucketHead, 0, numBuckets, -1);
            for (int i = 0; i < totalN; i++) {
                int v = allNodes[i];
                fmN[v] = -1;
                fmP[v] = -1;
            }

            for (int i = 0; i < totalN; i++) {
                int v = allNodes[i];
                int b = gain[v] + bucketOffset;
                if (b < 0) b = 0;
                if (b >= numBuckets) b = numBuckets - 1;
                fmBucketInsert(v, b, bucketHead, fmN, fmP);
            }

            int lockGen = nextBoundaryGen();
            int[] locked = scratchBoundary;

            int[] moveNode = ensureCapInt(TL_MOVENODE, totalN);
            int[] moveFrom = ensureCapInt(TL_MOVEFROM, totalN);
            int bestCumGain = 0;
            int bestStep = -1;
            int cumGain = 0;
            int step = 0;
            int topBucket = numBuckets - 1;

            while (step < totalN) {
                int bestNode = -1;
                while (topBucket >= 0 && bucketHead[topBucket] == -1) topBucket--;
                if (topBucket < 0) break;

                for (int b = topBucket; b >= 0; b--) {
                    int v = bucketHead[b];
                    while (v >= 0) {
                        if (locked[v] != lockGen) {
                            int fromSide = side[v];
                            int newA = sizeA + (fromSide == 1 ? -1 : 1);
                            int newB = sizeB + (fromSide == 2 ? -1 : 1);
                            if (newA >= minSize && newB >= minSize) {
                                bestNode = v;
                                break;
                            }
                        }
                        v = fmN[v];
                    }
                    if (bestNode >= 0) break;
                }
                if (bestNode < 0) break;

                int nodeGain = gain[bestNode];
                locked[bestNode] = lockGen;
                int b = nodeGain + bucketOffset;
                if (b < 0) b = 0;
                if (b >= numBuckets) b = numBuckets - 1;
                fmBucketRemove(bestNode, b, bucketHead, fmN, fmP);

                int fromSide = side[bestNode];
                int toSide = (fromSide == 1) ? 2 : 1;
                side[bestNode] = toSide;
                if (fromSide == 1) { sizeA--; sizeB++; }
                else { sizeA++; sizeB--; }

                moveNode[step] = bestNode;
                moveFrom[step] = fromSide;

                cumGain += nodeGain;
                if (cumGain > bestCumGain) {
                    bestCumGain = cumGain;
                    bestStep = step;
                }

                for (int ei = off[bestNode], eiEnd = off[bestNode + 1]; ei < eiEnd; ei++) {
                    int w = data[ei];
                    if (inSubGraphGen[w] != gen || locked[w] == lockGen) continue;

                    int oldGain = gain[w];
                    int oldB = oldGain + bucketOffset;
                    if (oldB < 0) oldB = 0;
                    if (oldB >= numBuckets) oldB = numBuckets - 1;

                    if (side[w] == fromSide) gain[w] += 2;
                    else gain[w] -= 2;

                    int newB = gain[w] + bucketOffset;
                    if (newB < 0) newB = 0;
                    if (newB >= numBuckets) newB = numBuckets - 1;

                    if (oldB != newB) {
                        fmBucketRemove(w, oldB, bucketHead, fmN, fmP);
                        fmBucketInsert(w, newB, bucketHead, fmN, fmP);
                        if (newB > topBucket) topBucket = newB;
                    }
                }

                step++;
            }

            if (bestCumGain > 0) {
                improved = true;
                for (int s = step - 1; s > bestStep; s--) {
                    int v = moveNode[s];
                    side[v] = moveFrom[s];
                    if (moveFrom[s] == 1) { sizeA++; sizeB--; }
                    else { sizeA--; sizeB++; }
                }
            } else {
                for (int s = step - 1; s >= 0; s--) {
                    int v = moveNode[s];
                    side[v] = moveFrom[s];
                    if (moveFrom[s] == 1) { sizeA++; sizeB--; }
                    else { sizeA--; sizeB++; }
                }
            }
        }

        int[][] extracted = extractSeparatorFromBipartition(allNodes, totalN, side, gen);

        if (!isValidSeparator(extracted, gen) && totalN >= maxflowMinSize) {
            int countA = 0, countB = 0;
            for (int i = 0; i < totalN; i++) {
                if (side[allNodes[i]] == 1) countA++;
                else countB++;
            }
            if (countA > 0 && countB > 0) {
                int[] sA = new int[countA];
                int[] sB = new int[countB];
                int ai = 0, bi = 0;
                for (int i = 0; i < totalN; i++) {
                    if (side[allNodes[i]] == 1) sA[ai++] = allNodes[i];
                    else sB[bi++] = allNodes[i];
                }
                int[][] mfResult = maxFlowRefineBipartition(sA, sB);
                if (mfResult != null && isValidSeparator(mfResult, gen)) {
                    return mfResult;
                }
            }
        }

        return extracted;
    }

    private void fmBucketInsert(int v, int b, int[] bucketHead, int[] next, int[] prev) {
        prev[v] = -1;
        next[v] = bucketHead[b];
        if (bucketHead[b] >= 0) prev[bucketHead[b]] = v;
        bucketHead[b] = v;
    }

    private void fmBucketRemove(int v, int b, int[] bucketHead, int[] next, int[] prev) {
        if (prev[v] >= 0) next[prev[v]] = next[v];
        else bucketHead[b] = next[v];
        if (next[v] >= 0) prev[next[v]] = prev[v];
        next[v] = -1;
        prev[v] = -1;
    }

    private int[][] extractSeparatorFromBipartition(int[] allNodes, int totalN,
                                                     int[] side, int gen) {
        int genB = nextBoundaryGen();
        int[] boundaryMark = scratchBoundary;
        int boundaryACount = 0, boundaryBCount = 0;

        final int[] data = adjData;
        final int[] off  = adjOffset;

        for (int i = 0; i < totalN; i++) {
            int node = allNodes[i];
            int mySide = side[node];
            for (int ei = off[node], eiEnd = off[node + 1]; ei < eiEnd; ei++) {
                int w = data[ei];
                if (inSubGraphGen[w] != gen) continue;
                if (side[w] != 0 && side[w] != mySide) {
                    if (boundaryMark[node] != genB) {
                        boundaryMark[node] = genB;
                        if (mySide == 1) boundaryACount++;
                        else boundaryBCount++;
                    }
                    break;
                }
            }
        }

        if (boundaryACount == 0 && boundaryBCount == 0) {
            int half = totalN / 2;
            int[] pA = Arrays.copyOfRange(allNodes, 0, half);
            int[] sepArr = new int[]{allNodes[half]};
            int[] pB = Arrays.copyOfRange(allNodes, half + 1, totalN);
            return new int[][]{pA, sepArr, pB};
        }

        int[][] bestSideResult = null;
        long bestSideScore = Long.MAX_VALUE;

        for (int sepSide : new int[]{1, 2}) {
            int sepCount = (sepSide == 1) ? boundaryACount : boundaryBCount;
            if (sepCount == 0 || sepCount >= totalN - 1) continue;

            int countA = 0, countB = 0;
            for (int i = 0; i < totalN; i++) {
                int node = allNodes[i];
                boolean isSep = (boundaryMark[node] == genB && side[node] == sepSide);
                if (isSep) continue;
                if (side[node] == 1) countA++;
                else countB++;
            }
            if (countA == 0 || countB == 0) continue;

            int[] pA = new int[countA];
            int[] sep = new int[sepCount];
            int[] pB = new int[countB];
            int ia = 0, is = 0, ib = 0;
            for (int i = 0; i < totalN; i++) {
                int node = allNodes[i];
                boolean isSep = (boundaryMark[node] == genB && side[node] == sepSide);
                if (isSep) {
                    sep[is++] = node;
                } else if (side[node] == 1) {
                    pA[ia++] = node;
                } else {
                    pB[ib++] = node;
                }
            }

            int[][] candidate = new int[][]{pA, sep, pB};
            if (sep.length > 1) {
                candidate = thinSeparator(candidate);
            }

            int thinnedBalance = Math.abs(candidate[0].length - candidate[2].length);
            long thinnedScore = scoreSeparator(candidate[1], thinnedBalance, gen);

            if (thinnedScore < bestSideScore) {
                bestSideScore = thinnedScore;
                bestSideResult = candidate;
            }
        }

        return bestSideResult != null ? bestSideResult :
                new int[][]{Arrays.copyOfRange(allNodes, 0, totalN / 2),
                            new int[]{allNodes[totalN / 2]},
                            Arrays.copyOfRange(allNodes, totalN / 2 + 1, totalN)};
    }

    // ========================= Max-Flow / Min-Cut ==============================

    /**
     * Find the minimum vertex separator using Dinic's algorithm on a node-split
     * flow network with a wide cuttable region.
     * Uses pre-allocated {@code bfsDist} and {@code toCompact} member arrays.
     *
     * @param gen current subgraph generation stamp
     */
    private int[][] maxFlowRefineWideDepth(int[][] result, int depth, int gen) {
        int[] partA = result[0];
        int[] sep   = result[1];
        int[] partB = result[2];

        int totalN = partA.length + sep.length + partB.length;
        if (totalN < maxflowMinSize || sep.length <= 1) return null;

        int genCuttable = nextBoundaryGen();
        int[] cuttableMark = scratchBoundary;
        for (int s : sep) cuttableMark[s] = genCuttable;

        final int[] data = adjData;
        final int[] off  = adjOffset;
        int[] dist = bfsDist;

        int[] bfsQueue = new int[totalN];
        int qHead = 0, qTail = 0;
        for (int s : sep) {
            bfsQueue[qTail++] = s;
            dist[s] = 1;
        }
        while (qHead < qTail) {
            int u = bfsQueue[qHead++];
            int d = dist[u];
            if (d > depth) continue;
            for (int ei = off[u], eiEnd = off[u + 1]; ei < eiEnd; ei++) {
                int w = data[ei];
                if (inSubGraphGen[w] != gen || dist[w] != 0) continue;
                dist[w] = d + 1;
                cuttableMark[w] = genCuttable;
                if (qTail < bfsQueue.length) bfsQueue[qTail++] = w;
            }
        }
        for (int i = 0; i < qTail; i++) dist[bfsQueue[i]] = 0;

        int[] nodeId = new int[totalN];
        int[] compactMap = toCompact;
        int ci = 0;
        for (int v : partA) { nodeId[ci] = v; compactMap[v] = ci + 1; ci++; }
        for (int v : sep)   { nodeId[ci] = v; compactMap[v] = ci + 1; ci++; }
        for (int v : partB) { nodeId[ci] = v; compactMap[v] = ci + 1; ci++; }

        int aLen = partA.length;
        int sLen = sep.length;

        int fN = 2 * totalN + 2;
        int superS = fN - 2;
        int superT = fN - 1;
        int maxEdges = (totalN + 6 * totalN + totalN) * 2;
        int[] eTo = new int[maxEdges];
        int[] eCap = new int[maxEdges];
        int[] eCount = {0};
        int[][] fAdj = new int[fN][];
        int[] fAdjLen = new int[fN];
        int infCap = totalN + 1;

        for (int i = 0; i < totalN; i++) {
            int cap = (cuttableMark[nodeId[i]] == genCuttable) ? 1 : infCap;
            flowAddEdge(2 * i, 2 * i + 1, cap, eTo, eCap, eCount, fAdj, fAdjLen);
        }

        for (int i = 0; i < totalN; i++) {
            int v = nodeId[i];
            for (int ei = off[v], eiEnd = off[v + 1]; ei < eiEnd; ei++) {
                int j = compactMap[data[ei]] - 1;
                if (j < 0 || j <= i) continue;
                flowAddEdge(2 * i + 1, 2 * j, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
                flowAddEdge(2 * j + 1, 2 * i, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
            }
        }

        boolean hasSource = false;
        for (int i = 0; i < aLen; i++) {
            if (cuttableMark[nodeId[i]] != genCuttable) {
                flowAddEdge(superS, 2 * i, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
                hasSource = true;
            }
        }
        boolean hasSink = false;
        for (int i = aLen + sLen; i < totalN; i++) {
            if (cuttableMark[nodeId[i]] != genCuttable) {
                flowAddEdge(2 * i + 1, superT, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
                hasSink = true;
            }
        }
        if (!hasSource) {
            for (int i = 0; i < aLen; i++)
                flowAddEdge(superS, 2 * i, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
        }
        if (!hasSink) {
            for (int i = aLen + sLen; i < totalN; i++)
                flowAddEdge(2 * i + 1, superT, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
        }

        int[] level = new int[fN];
        int[] iter = new int[fN];
        int[] queue = new int[fN];

        while (true) {
            Arrays.fill(level, -1);
            level[superS] = 0;
            int bfsH = 0, bfsT = 0;
            queue[bfsT++] = superS;
            while (bfsH < bfsT) {
                int u = queue[bfsH++];
                int len = fAdjLen[u];
                int[] edges = fAdj[u];
                for (int ei = 0; ei < len; ei++) {
                    int e = edges[ei];
                    int w = eTo[e];
                    if (level[w] < 0 && eCap[e] > 0) {
                        level[w] = level[u] + 1;
                        queue[bfsT++] = w;
                    }
                }
            }
            if (level[superT] < 0) break;
            Arrays.fill(iter, 0);
            while (dinicDFS(superS, superT, infCap, level, iter, eTo, eCap, fAdj, fAdjLen) > 0) {}
        }

        boolean[] reachable = new boolean[fN];
        reachable[superS] = true;
        {
            int rH = 0, rT = 0;
            queue[rT++] = superS;
            while (rH < rT) {
                int u = queue[rH++];
                int len = fAdjLen[u];
                int[] edges = fAdj[u];
                for (int ei = 0; ei < len; ei++) {
                    int e = edges[ei];
                    int w = eTo[e];
                    if (!reachable[w] && eCap[e] > 0) {
                        reachable[w] = true;
                        queue[rT++] = w;
                    }
                }
            }
        }

        for (int i = 0; i < totalN; i++) compactMap[nodeId[i]] = 0;

        int newSepCount = 0, newACount = 0, newBCount = 0;
        for (int i = 0; i < totalN; i++) {
            if (reachable[2 * i] && !reachable[2 * i + 1]) newSepCount++;
            else if (reachable[2 * i]) newACount++;
            else newBCount++;
        }

        if (newSepCount == 0 || newACount == 0 || newBCount == 0) return null;

        int[] newSep = new int[newSepCount];
        int[] newA = new int[newACount];
        int[] newB = new int[newBCount];
        int ai = 0, si = 0, bi = 0;
        for (int i = 0; i < totalN; i++) {
            if (reachable[2 * i] && !reachable[2 * i + 1]) newSep[si++] = nodeId[i];
            else if (reachable[2 * i]) newA[ai++] = nodeId[i];
            else newB[bi++] = nodeId[i];
        }

        int[][] mfResult = new int[][]{newA, newSep, newB};
        if (newSep.length > 1) mfResult = thinSeparator(mfResult);
        return mfResult;
    }

    private static void flowAddEdge(int from, int to, int cap,
                                     int[] eTo, int[] eCap, int[] eCount,
                                     int[][] fAdj, int[] fAdjLen) {
        int e = eCount[0];
        eTo[e] = to;       eCap[e] = cap;
        eTo[e + 1] = from; eCap[e + 1] = 0;
        flowAddToAdj(from, e, fAdj, fAdjLen);
        flowAddToAdj(to, e + 1, fAdj, fAdjLen);
        eCount[0] = e + 2;
    }

    private static void flowAddToAdj(int node, int edgeIdx, int[][] fAdj, int[] fAdjLen) {
        int len = fAdjLen[node];
        if (fAdj[node] == null) {
            fAdj[node] = new int[4];
        } else if (len == fAdj[node].length) {
            fAdj[node] = Arrays.copyOf(fAdj[node], len * 2);
        }
        fAdj[node][len] = edgeIdx;
        fAdjLen[node] = len + 1;
    }

    private static int dinicDFS(int u, int sink, int pushed,
                                 int[] level, int[] iter,
                                 int[] eTo, int[] eCap,
                                 int[][] fAdj, int[] fAdjLen) {
        if (u == sink) return pushed;
        int len = fAdjLen[u];
        int[] edges = fAdj[u];
        for (; iter[u] < len; iter[u]++) {
            int e = edges[iter[u]];
            int v = eTo[e];
            if (level[v] != level[u] + 1 || eCap[e] <= 0) continue;
            int d = dinicDFS(v, sink, Math.min(pushed, eCap[e]),
                             level, iter, eTo, eCap, fAdj, fAdjLen);
            if (d > 0) {
                eCap[e] -= d;
                eCap[e ^ 1] += d;
                return d;
            }
        }
        return 0;
    }

    /**
     * Find minimum vertex separator between sideA and sideB using Dinic's max-flow.
     * Uses pre-allocated {@code toCompact} member array.
     */
    private int[][] maxFlowRefineBipartition(int[] sideA, int[] sideB) {
        int totalN = sideA.length + sideB.length;
        if (totalN < 10) return null;

        int[] nodeId = new int[totalN];
        int[] compactMap = toCompact;
        int ci = 0;
        for (int v : sideA) { nodeId[ci] = v; compactMap[v] = ci + 1; ci++; }
        for (int v : sideB) { nodeId[ci] = v; compactMap[v] = ci + 1; ci++; }

        int aLen = sideA.length;
        int fN = 2 * totalN + 2;
        int superS = fN - 2;
        int superT = fN - 1;
        int infCap = totalN + 1;

        int maxEdges = (totalN + 6 * totalN + totalN) * 2;
        int[] eTo = new int[maxEdges];
        int[] eCap = new int[maxEdges];
        int[] eCount = {0};
        int[][] fAdj = new int[fN][];
        int[] fAdjLen = new int[fN];

        final int[] data = adjData;
        final int[] off  = adjOffset;

        for (int i = 0; i < totalN; i++) {
            flowAddEdge(2 * i, 2 * i + 1, 1, eTo, eCap, eCount, fAdj, fAdjLen);
        }

        for (int i = 0; i < totalN; i++) {
            int v = nodeId[i];
            for (int ei = off[v], eiEnd = off[v + 1]; ei < eiEnd; ei++) {
                int j = compactMap[data[ei]] - 1;
                if (j < 0 || j <= i) continue;
                flowAddEdge(2 * i + 1, 2 * j, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
                flowAddEdge(2 * j + 1, 2 * i, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
            }
        }

        for (int i = 0; i < aLen; i++) {
            flowAddEdge(superS, 2 * i, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
            eCap[i * 2] = infCap;
        }
        for (int i = aLen; i < totalN; i++) {
            flowAddEdge(2 * i + 1, superT, infCap, eTo, eCap, eCount, fAdj, fAdjLen);
            eCap[i * 2] = infCap;
        }

        int[] level = new int[fN];
        int[] iter = new int[fN];
        int[] queue = new int[fN];

        while (true) {
            Arrays.fill(level, -1);
            level[superS] = 0;
            int qHead = 0, qTail = 0;
            queue[qTail++] = superS;
            while (qHead < qTail) {
                int u = queue[qHead++];
                int len = fAdjLen[u];
                int[] edges = fAdj[u];
                for (int ei = 0; ei < len; ei++) {
                    int e = edges[ei];
                    int w = eTo[e];
                    if (level[w] < 0 && eCap[e] > 0) {
                        level[w] = level[u] + 1;
                        queue[qTail++] = w;
                    }
                }
            }
            if (level[superT] < 0) break;
            Arrays.fill(iter, 0);
            while (dinicDFS(superS, superT, infCap, level, iter, eTo, eCap, fAdj, fAdjLen) > 0) {}
        }

        boolean[] reachable = new boolean[fN];
        reachable[superS] = true;
        int qHead = 0, qTail = 0;
        queue[qTail++] = superS;
        while (qHead < qTail) {
            int u = queue[qHead++];
            int len = fAdjLen[u];
            int[] edges = fAdj[u];
            for (int ei = 0; ei < len; ei++) {
                int e = edges[ei];
                int w = eTo[e];
                if (!reachable[w] && eCap[e] > 0) {
                    reachable[w] = true;
                    queue[qTail++] = w;
                }
            }
        }

        int newSepCount = 0, newACount = 0, newBCount = 0;
        for (int i = 0; i < totalN; i++) {
            if (reachable[2 * i] && !reachable[2 * i + 1]) newSepCount++;
            else if (reachable[2 * i]) newACount++;
            else newBCount++;
        }

        for (int i = 0; i < totalN; i++) compactMap[nodeId[i]] = 0;

        if (newSepCount == 0 || newACount == 0 || newBCount == 0) return null;

        int[] newSep = new int[newSepCount];
        int[] newA = new int[newACount];
        int[] newB = new int[newBCount];
        int ai = 0, si = 0, bi = 0;
        for (int i = 0; i < totalN; i++) {
            if (reachable[2 * i] && !reachable[2 * i + 1]) newSep[si++] = nodeId[i];
            else if (reachable[2 * i]) newA[ai++] = nodeId[i];
            else newB[bi++] = nodeId[i];
        }

        int[][] mfResult = new int[][]{newA, newSep, newB};
        if (newSep.length > 1) mfResult = thinSeparator(mfResult);
        return mfResult;
    }

    /**
     * Split ratios to try for each projection direction.
     */
    private static final double[] SPLIT_RATIOS = {
        0.25, 0.28, 0.30, 0.33, 0.36, 0.38, 0.40, 0.42, 0.44, 0.46, 0.48,
        0.50, 0.52, 0.54, 0.56, 0.58, 0.60, 0.62, 0.64, 0.67, 0.70, 0.72, 0.75
    };

    private static final double[] REDUCED_SPLIT_RATIOS = {
        0.30, 0.37, 0.43, 0.50, 0.57, 0.63, 0.70
    };

    /**
     * Try a single projection direction with multiple split ratios.
     * Uses per-thread scratch buffers to avoid per-call allocation.
     *
     * <p>Thinning is deferred: {@link #trySplitAt} returns unthinned separators,
     * and only the final best result per direction is thinned before returning.
     * This avoids hundreds of redundant {@link #thinSeparator} calls during
     * ratio scanning.
     */
    private int[][] tryDirection(int[] subNodes, double dx, double dy) {
        int n = subNodes.length;
        if (n < 3) return null;

        double[] projections = ensureCapDbl(TL_PROJ, n);
        for (int i = 0; i < n; i++) {
            projections[i] = nodeX[subNodes[i]] * dx + nodeY[subNodes[i]] * dy;
        }
        int[] sortedIdx = sortByProjection(projections, n);

        int[][] bestResult = null;
        long bestScore = Long.MAX_VALUE;

        double[] ratios = (n < reducedRatiosThreshold) ? REDUCED_SPLIT_RATIOS : SPLIT_RATIOS;

        for (double ratio : ratios) {
            int splitAt = Math.max(1, Math.min(n - 1, (int) (n * ratio)));
            int[][] result = trySplitAt(subNodes, sortedIdx, n, splitAt);
            if (result != null) {
                int balance = Math.abs(result[0].length - result[2].length);
                long score = scoreSimple(result[1], balance);
                if (score < bestScore) {
                    bestScore = score;
                    bestResult = result;
                }
            }
        }

        // Thin only the final best separator (deferred from trySplitAt)
        if (bestResult != null && bestResult[1].length > 1) {
            bestResult = thinSeparator(bestResult);
        }
        return bestResult;
    }

    /**
     * Try splitting the sorted projection at a specific index.
     *
     * <p>Uses generation-stamped side values to merge the subgraph-membership
     * check and side comparison into a single array read, eliminating the
     * {@code inSubGraphGen} access from the hot inner loop.  Each call obtains
     * two fresh unique values ({@code sideAVal}, {@code sideBVal}) via
     * {@link #nextBoundaryGen()}, so stale values from earlier calls or
     * concurrent ForkJoin tasks on disjoint node sets are never equal to the
     * current values.
     *
     * <p>Partition counts are derived arithmetically from the boundary counts
     * and {@code splitAt}, avoiding two extra O(n) counting loops.
     *
     * <p>Does <b>not</b> thin the separator; thinning is deferred to
     * {@link #tryDirection} for the final best result only, avoiding hundreds
     * of redundant {@link #thinSeparator} calls during ratio scanning.
     */
    private int[][] trySplitAt(int[] subNodes, int[] sortedIdx,
                                int n, int splitAt) {
        int[] side = scratchSide;

        // Gen-stamped side values: unique per call, so stale values from
        // earlier calls (or concurrent ForkJoin tasks on disjoint nodes)
        // are never equal to the current values.
        int sideAVal = nextBoundaryGen();
        int sideBVal = nextBoundaryGen();

        for (int i = 0; i < splitAt; i++) side[subNodes[sortedIdx[i]]] = sideAVal;
        for (int i = splitAt; i < n; i++) side[subNodes[sortedIdx[i]]] = sideBVal;

        final int[] data = adjData;
        final int[] off  = adjOffset;

        int boundaryACount = 0, boundaryBCount = 0;
        int[] boundaryMark = scratchBoundary;
        int bgen = nextBoundaryGen();

        for (int idx = 0; idx < n; idx++) {
            int node = subNodes[idx];
            int mySide = side[node];
            int otherSide = (mySide == sideAVal) ? sideBVal : sideAVal;
            for (int ei = off[node], eiEnd = off[node + 1]; ei < eiEnd; ei++) {
                if (side[data[ei]] == otherSide) {
                    if (boundaryMark[node] != bgen) {
                        boundaryMark[node] = bgen;
                        if (mySide == sideAVal) boundaryACount++;
                        else boundaryBCount++;
                    }
                    break;
                }
            }
        }

        if (boundaryACount == 0 && boundaryBCount == 0) return null;

        // Pick the better separator side using precomputed counts (no extra loop).
        // sepSide A: sep = boundary-A nodes, pA = non-boundary A, pB = all B
        // sepSide B: sep = boundary-B nodes, pA = all A,          pB = non-boundary B
        int pACountA = splitAt - boundaryACount;
        int pBCountA = n - splitAt;
        long scoreA = (boundaryACount > 0 && boundaryACount < n - 1
                       && pACountA > 0 && pBCountA > 0)
                      ? boundaryACount * 256L + Math.abs(pACountA - pBCountA)
                      : Long.MAX_VALUE;

        int pACountB = splitAt;
        int pBCountB = n - splitAt - boundaryBCount;
        long scoreB = (boundaryBCount > 0 && boundaryBCount < n - 1
                       && pACountB > 0 && pBCountB > 0)
                      ? boundaryBCount * 256L + Math.abs(pACountB - pBCountB)
                      : Long.MAX_VALUE;

        if (scoreA == Long.MAX_VALUE && scoreB == Long.MAX_VALUE) return null;

        int sepSideVal;
        int sepCount, countA, countB;
        if (scoreA <= scoreB) {
            sepSideVal = sideAVal;
            sepCount = boundaryACount;  countA = pACountA;  countB = pBCountA;
        } else {
            sepSideVal = sideBVal;
            sepCount = boundaryBCount;  countA = pACountB;  countB = pBCountB;
        }

        // Build partition arrays in a single pass
        int[] pA  = new int[countA];
        int[] sep = new int[sepCount];
        int[] pB  = new int[countB];
        int ia = 0, is = 0, ib = 0;
        for (int idx = 0; idx < n; idx++) {
            int node = subNodes[idx];
            if (boundaryMark[node] == bgen && side[node] == sepSideVal) {
                sep[is++] = node;
            } else if (side[node] == sideAVal) {
                pA[ia++] = node;
            } else {
                pB[ib++] = node;
            }
        }

        return new int[][]{pA, sep, pB};
    }

    /**
     * Greedy separator thinning: removes separator nodes not adjacent to partA.
     */
    private int[][] thinSeparator(int[][] result) {
        int[] partA = result[0];
        int[] separator = result[1];
        int[] partB = result[2];

        if (separator.length <= 1) return result;

        final int[] data = adjData;
        final int[] off  = adjOffset;

        boolean changed = true;
        while (changed && separator.length > 1) {
            changed = false;

            int genP1 = nextBoundaryGen();
            int[] mark = scratchBoundary;
            for (int n : partA) mark[n] = genP1;

            boolean[] removable = new boolean[separator.length];
            int removeCount = 0;

            for (int i = 0; i < separator.length; i++) {
                int s = separator[i];
                boolean hasP1Neighbor = false;
                for (int ei = off[s], eiEnd = off[s + 1]; ei < eiEnd; ei++) {
                    if (mark[data[ei]] == genP1) {
                        hasP1Neighbor = true;
                        break;
                    }
                }
                if (!hasP1Neighbor) {
                    removable[i] = true;
                    removeCount++;
                }
            }

            if (removeCount == 0) break;
            if (removeCount == separator.length) break;

            int[] newSep = new int[separator.length - removeCount];
            int[] newPartB = new int[partB.length + removeCount];
            System.arraycopy(partB, 0, newPartB, 0, partB.length);
            int si = 0, bi = partB.length;
            for (int i = 0; i < separator.length; i++) {
                if (removable[i]) {
                    newPartB[bi++] = separator[i];
                } else {
                    newSep[si++] = separator[i];
                }
            }

            separator = newSep;
            partB = newPartB;
            changed = true;
        }

        return new int[][]{partA, separator, partB};
    }

    /**
     * Sort indices [0..n) by the corresponding projection values.
     * Uses per-thread scratch buffers ({@link #TL_SORT_IDX}, {@link #TL_SORT_TMP}).
     */
    private static int[] sortByProjection(double[] projections, int n) {
        int[] idx = ensureCapInt(TL_SORT_IDX, n);
        for (int i = 0; i < n; i++) idx[i] = i;
        int[] tmp = ensureCapInt(TL_SORT_TMP, n);
        mergeSort(idx, tmp, projections, 0, n);
        return idx;
    }

    private static void mergeSort(int[] arr, int[] tmp, double[] keys, int lo, int hi) {
        if (hi - lo <= 16) {
            for (int i = lo + 1; i < hi; i++) {
                int t = arr[i];
                double k = keys[t];
                int j = i - 1;
                while (j >= lo && keys[arr[j]] > k) {
                    arr[j + 1] = arr[j];
                    j--;
                }
                arr[j + 1] = t;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(arr, tmp, keys, lo, mid);
        mergeSort(arr, tmp, keys, mid, hi);

        System.arraycopy(arr, lo, tmp, lo, hi - lo);
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) {
            arr[k++] = (keys[tmp[i]] <= keys[tmp[j]]) ? tmp[i++] : tmp[j++];
        }
        while (i < mid) arr[k++] = tmp[i++];
        while (j < hi)  arr[k++] = tmp[j++];
    }

    private int[][] trivialSplit(int[] subNodes) {
        int half = subNodes.length / 2;
        int sepSize = Math.max(1, subNodes.length / 10);
        int partASize = half - sepSize / 2;
        if (partASize < 1) partASize = 1;
        int partBSize = subNodes.length - partASize - sepSize;
        if (partBSize < 0) {
            partBSize = 0;
            sepSize = subNodes.length - partASize;
        }

        int[] partA = Arrays.copyOfRange(subNodes, 0, partASize);
        int[] separator = Arrays.copyOfRange(subNodes, partASize, partASize + sepSize);
        int[] partB = Arrays.copyOfRange(subNodes, partASize + sepSize, subNodes.length);
        return new int[][]{partA, separator, partB};
    }

    /**
     * Build the symmetric (undirected) CSR adjacency from the directed SpeedyGraph.
     * Results are stored in {@link #adjData} and {@link #adjOffset}.
     *
     * <p>Deduplication works in-place: because the unique neighbor count never
     * exceeds the degree, the write pointer never overtakes the read pointer.
     */
    private void buildSymmetricAdjacency() {
        int n = graph.nodeCount;

        int[] degree = new int[n];
        SpeedyGraph.LinkIterator outLI = graph.getOutLinkIterator();
        // getToNodeIndex() always returns a valid index in [0, nodeCount) because
        // SpeedyGraph only stores links whose endpoints are registered nodes.
        for (int node = 0; node < n; node++) {
            outLI.reset(node);
            while (outLI.next()) {
                degree[node]++;
                degree[outLI.getToNodeIndex()]++;
            }
        }

        int[] initOffset = new int[n + 1];
        for (int i = 0; i < n; i++) initOffset[i + 1] = initOffset[i] + degree[i];

        int totalEdges = initOffset[n];
        int[] data = new int[totalEdges];

        int[] fillPos = Arrays.copyOf(initOffset, n);
        for (int node = 0; node < n; node++) {
            outLI.reset(node);
            while (outLI.next()) {
                int to = outLI.getToNodeIndex();
                data[fillPos[node]++] = to;
                data[fillPos[to]++] = node;
            }
        }

        // Deduplicate in-place using generation stamps (O(d) per node).
        // writePos <= initOffset[i] for every node i being processed,
        // so the write pointer never overtakes the read pointer.
        int[] dedupGen = new int[n];
        int dedupGeneration = 0;
        int[] newOffset = new int[n + 1];
        int writePos = 0;

        for (int i = 0; i < n; i++) {
            newOffset[i] = writePos;
            int end = initOffset[i + 1];
            if (initOffset[i] == end) continue;
            dedupGeneration++;
            for (int j = initOffset[i]; j < end; j++) {
                int nb = data[j];
                if (dedupGen[nb] != dedupGeneration) {
                    dedupGen[nb] = dedupGeneration;
                    data[writePos++] = nb;
                }
            }
        }
        newOffset[n] = writePos;

        this.adjOffset = newOffset;
        this.adjData = writePos < data.length ? Arrays.copyOf(data, writePos) : data;
    }
}
