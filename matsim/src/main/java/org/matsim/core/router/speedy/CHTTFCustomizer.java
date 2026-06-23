/* *********************************************************************** *
 * project: org.matsim.*
 * CHTTFCustomizer.java
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Time-dependent customization of a {@link CHGraph} using
 * Travel Time Functions (TTFs).
 *
 * <p>After this customizer runs, {@link CHGraph#ttf},
 * {@link CHGraph#minTTF}, and {@link CHGraph#edgeWeights}
 * are populated and ready for use by {@link CHRouterTimeDep}.
 *
 * <p>TTF storage uses a bin-major flat contiguous {@code double[]} array:
 * {@code ttf[bin * edgeCount + globalIdx]} for time bin {@code bin}, edge
 * {@code globalIdx}.  This layout gives sequential memory access when
 * iterating a node's edges in the query (constant bin, contiguous globalIdx).
 *
 * <h3>Parallelism</h3>
 * <p>Real edges (original network links) have no inter-edge dependencies and
 * are processed in parallel using a {@link ForkJoinPool}.  Shortcut edges
 * depend on their lower edges and are processed sequentially in topological
 * order.  This two-phase approach parallelizes the most expensive part
 * (96 {@code getLinkTravelTime} calls per real edge) while maintaining
 * correctness for shortcuts.
 *
 * @author Steffen Axer
 */
public class CHTTFCustomizer {

    private static final Logger LOG = LogManager.getLogger(CHTTFCustomizer.class);

    /** Number of time bins covering a full 24-hour day.
     *  Aligned with the MATSim default {@code travelTimeBinSize} of 900 s (15 min)
     *  in {@link org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup}. */
    public static final int NUM_BINS = 96;

    /** Duration of each time bin in seconds (15 minutes).
     *  Matches the MATSim default {@code travelTimeBinSize} of 900 s. */
    public static final double BIN_SIZE = 900.0; // 15 min × 96 = 24 h

    /** Precomputed reciprocal for fast bin computation. */
    static final double INV_BIN_SIZE = 1.0 / BIN_SIZE;

    /** Number of original edges to sample for the fast fingerprint check. */
    private static final int FINGERPRINT_SAMPLES = 20;
    /** Time bins sampled per edge in the fingerprint (midnight, 8 AM, 4 PM). */
    private static final int[] FINGERPRINT_BINS = {0, 32, 64};

    /** Minimum number of real edges to trigger parallel processing. */
    private static final int PARALLEL_REAL_EDGE_THRESHOLD = 200;

    public void customize(CHGraph chGraph, TravelTime tt, TravelDisutility td) {
        // Quick-check: sample a few original edges to detect if travel times
        // changed since the last customization.  This avoids the expensive full
        // scan (~20M getLinkTravelTime calls for a 200k-link network) when nothing
        // has changed — the common case when multiple threads or callers request
        // a path calculator within the same MATSim iteration.
        if (!needsRecustomization(chGraph, tt)) {
            return;
        }

        long customizeStart = System.nanoTime();
        SpeedyGraph baseGraph = chGraph.getBaseGraph();
        int    edgeCount  = chGraph.totalEdgeCount;
        int[]  origLink   = chGraph.edgeOrigLink;
        int[]  lower1     = chGraph.edgeLower1;
        int[]  lower2     = chGraph.edgeLower2;
        double[] ttf      = chGraph.ttf;
        double[] minTTF   = chGraph.minTTF;
        double[] weights  = chGraph.edgeWeights;
        double[] ttfHash  = chGraph.ttfHash;
        int[]  order      = chGraph.customizeOrder;
        boolean[] dirty   = new boolean[edgeCount];

        // Find boundary between real edges and shortcuts in topological order.
        // Real edges are imported first (by initEdges), so they precede all shortcuts.
        int realEdgeEnd = 0;
        while (realEdgeEnd < edgeCount && origLink[order[realEdgeEnd]] >= 0) {
            realEdgeEnd++;
        }

        // Phase 1: Process real edges — no inter-edge dependencies, fully parallel.
        // Each real edge reads from TravelTime (thread-safe) and writes to its own
        // ttf[k * edgeCount + e], dirty[e], ttfHash[e], minTTF[e], weights[e] slots.
        if (realEdgeEnd >= PARALLEL_REAL_EDGE_THRESHOLD) {
            int nThreads = Runtime.getRuntime().availableProcessors();
            int batchSize = Math.max(1, realEdgeEnd / (nThreads * 4));
            ForkJoinPool pool = ForkJoinPool.commonPool();
            List<ForkJoinTask<?>> tasks = new ArrayList<>();

            for (int from = 0; from < realEdgeEnd; from += batchSize) {
                final int start = from;
                final int end = Math.min(from + batchSize, realEdgeEnd);
                tasks.add(pool.submit(() -> {
                    for (int i = start; i < end; i++) {
                        int e = order[i];
                        Link link = baseGraph.getLink(origLink[e]);
                        double min = Double.POSITIVE_INFINITY;
                        double sum = 0;
                        for (int k = 0; k < NUM_BINS; k++) {
                            double t = tt.getLinkTravelTime(link, k * BIN_SIZE, null, null);
                            ttf[k * edgeCount + e] = t;
                            sum += t;
                            if (t < min) min = t;
                        }
                        if (sum != ttfHash[e]) {
                            dirty[e] = true;
                            ttfHash[e] = sum;
                        }
                        minTTF[e] = min;
                        weights[e] = min;
                    }
                }));
            }
            for (ForkJoinTask<?> t : tasks) t.join();
        } else {
            // Sequential fallback for small graphs
            for (int i = 0; i < realEdgeEnd; i++) {
                int e = order[i];
                Link link = baseGraph.getLink(origLink[e]);
                double min = Double.POSITIVE_INFINITY;
                double sum = 0;
                for (int k = 0; k < NUM_BINS; k++) {
                    double t = tt.getLinkTravelTime(link, k * BIN_SIZE, null, null);
                    ttf[k * edgeCount + e] = t;
                    sum += t;
                    if (t < min) min = t;
                }
                if (sum != ttfHash[e]) {
                    dirty[e] = true;
                    ttfHash[e] = sum;
                }
                minTTF[e] = min;
                weights[e] = min;
            }
        }

        // Phase 2: Process shortcuts — sequential in topological order (dependencies).
        for (int i = realEdgeEnd; i < edgeCount; i++) {
            int e = order[i];
            double min = Double.POSITIVE_INFINITY;

            // Shortcut: skip recomposition if both lower edges are unchanged
            int l1 = lower1[e];
            int l2 = lower2[e];
            if (!dirty[l1] && !dirty[l2]) {
                // TTF unchanged — reuse cached values
                continue;
            }
            dirty[e] = true;
            double sum = 0;
            for (int k = 0; k < NUM_BINS; k++) {
                double t1         = ttf[k * edgeCount + l1];
                double arrivalSec = k * BIN_SIZE + t1;
                int    arrBin     = timeToBin(arrivalSec);
                double t2         = ttf[arrBin * edgeCount + l2];
                double composed   = t1 + t2;
                ttf[k * edgeCount + e] = composed;
                sum += composed;
                if (composed < min) min = composed;
            }
            ttfHash[e] = sum;

            minTTF[e]  = min;
            weights[e] = min;
        }

        // Propagate weights into colocated CSR weight arrays for cache-local access
        propagateWeightsToCSR(chGraph);

        // Update the fingerprint so subsequent calls can skip if nothing changed.
        chGraph.customizationFingerprint = computeFingerprint(chGraph, tt);

        LOG.info("[CH] TTF customization: {}s ({} edges, {} bins)",
                String.format(java.util.Locale.US, "%.1f", (System.nanoTime() - customizeStart) / 1_000_000_000.0),
                chGraph.totalEdgeCount, NUM_BINS);
    }

    /**
     * Fast fingerprint check: determines whether travel times have changed
     * since the last customization by sampling a small number of original
     * edges at a few representative time bins.
     *
     * <p>In MATSim, travel times are updated for ALL links at the end of
     * each iteration, so sampling ~20 edges reliably detects any change.
     * The cost is ~60 {@code getLinkTravelTime} calls vs ~20 million for
     * the full customization of a 200k-link network.
     *
     * <p>This method is safe to call without holding the CH graph lock because
     * it only reads from the (volatile) fingerprint field and the immutable
     * base-graph structure, and calls {@code getLinkTravelTime} which is
     * thread-safe in MATSim's travel-time implementations.
     */
    public static boolean needsRecustomization(CHGraph chGraph, TravelTime tt) {
        // First-ever customization: ttfHash is NaN
        if (Double.isNaN(chGraph.customizationFingerprint)) return true;

        // Compare current fingerprint with stored one
        return computeFingerprint(chGraph, tt) != chGraph.customizationFingerprint;
    }

    /**
     * Computes a lightweight fingerprint by summing travel times for a
     * spread of original edges at a few time bins.
     */
    private static double computeFingerprint(CHGraph chGraph, TravelTime tt) {
        SpeedyGraph baseGraph = chGraph.getBaseGraph();
        int edgeCount = chGraph.totalEdgeCount;
        int[] origLink = chGraph.edgeOrigLink;

        double fingerprint = 0;
        int sampled = 0;
        // Step through edges spread across the index space; oversample to
        // compensate for shortcut edges (which have origLink < 0).
        int step = Math.max(1, edgeCount / (FINGERPRINT_SAMPLES * 3));
        for (int e = 0; e < edgeCount && sampled < FINGERPRINT_SAMPLES; e += step) {
            if (origLink[e] < 0) continue;
            Link link = baseGraph.getLink(origLink[e]);
            for (int k : FINGERPRINT_BINS) {
                fingerprint += tt.getLinkTravelTime(link, k * BIN_SIZE, null, null);
            }
            sampled++;
        }
        return fingerprint;
    }

    /**
     * Copies global edgeWeights into the colocated upWeights/dnWeights arrays
     * so that the query hot-path reads weight from the same cache region as
     * the target-node index.
     *
     * <p>The four CSR weight arrays are independent and processed in parallel.
     */
    static void propagateWeightsToCSR(CHGraph chGraph) {
        int S = CHGraph.E_STRIDE;
        double[] ew = chGraph.edgeWeights;

        // Process all four CSR directions in parallel
        ForkJoinTask<?> upTask = ForkJoinPool.commonPool().submit(() -> {
            int upTotal = chGraph.upEdgeCount;
            for (int slot = 0; slot < upTotal; slot++) {
                int gIdx = chGraph.upEdges[slot * S + CHGraph.E_GIDX];
                chGraph.upWeights[slot] = ew[gIdx];
            }
        });
        ForkJoinTask<?> dnTask = ForkJoinPool.commonPool().submit(() -> {
            int dnTotal = chGraph.dnEdgeCount;
            for (int slot = 0; slot < dnTotal; slot++) {
                int gIdx = chGraph.dnEdges[slot * S + CHGraph.E_GIDX];
                chGraph.dnWeights[slot] = ew[gIdx];
            }
        });
        ForkJoinTask<?> dnOutTask = ForkJoinPool.commonPool().submit(() -> {
            for (int slot = 0; slot < chGraph.dnOutWeights.length; slot++) {
                int gIdx = chGraph.dnOutEdges[slot * S + CHGraph.E_GIDX];
                chGraph.dnOutWeights[slot] = ew[gIdx];
            }
        });
        // Process upIn on current thread while others run
        for (int slot = 0; slot < chGraph.upInWeights.length; slot++) {
            int gIdx = chGraph.upInEdges[slot * S + CHGraph.E_GIDX];
            chGraph.upInWeights[slot] = ew[gIdx];
        }
        upTask.join();
        dnTask.join();
        dnOutTask.join();
    }

    /**
     * Maps an absolute time (seconds from midnight) to a TTF bin index,
     * wrapping around every 24 hours.
     */
    public static int timeToBin(double timeSecs) {
        int bin = (int) (timeSecs * INV_BIN_SIZE) % NUM_BINS;
        if (bin < 0) bin += NUM_BINS;
        return bin;
    }
}
