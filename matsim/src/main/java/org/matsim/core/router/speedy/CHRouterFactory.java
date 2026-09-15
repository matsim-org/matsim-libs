/* *********************************************************************** *
 * project: org.matsim.*
 * CHRouterFactory.java
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for the time-dependent CATCHUp router ({@link CHRouterTimeDep}).
 *
 * <p>Follows the CCH (Customizable Contraction Hierarchies) paradigm where the
 * expensive contraction is performed once and then the CH overlay graph is merely
 * <em>customized</em> (edge-weight assignment) on subsequent calls. This dramatically
 * reduces repeated preprocessing cost because:
 * <ol>
 *   <li>The base {@link SpeedyGraph} is cached per {@link Network}.</li>
 *   <li>The contracted {@link CHGraph} <em>skeleton</em> is cached per network.
 *       The contraction topology (node ordering + shortcuts) is determined by the
 *       network structure; the actual edge weights are overwritten by customization.</li>
 *   <li>On each {@link #createPathCalculator} call only the O(m) customization steps
 *       run: {@link CHTTFCustomizer} for time-dependent TTFs, or
 *       {@link CHCustomizer} for a static variant.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>This class is {@link Singleton} and thread-safe: every call returns a new,
 * independent {@link CHRouterTimeDep} instance. The underlying CH structure may be
 * shared, but the query objects are per-thread. The TTF customization step is
 * synchronized per CH graph instance to prevent concurrent writes to the shared
 * TTF arrays when multiple threads call {@code createPathCalculator} simultaneously.
 *
 * @author Steffen Axer
 */
@Singleton
public class CHRouterFactory implements LeastCostPathCalculatorFactory {

    private static final Logger LOG = LogManager.getLogger(CHRouterFactory.class);

    private final Map<Network, SpeedyGraph> baseGraphs = new ConcurrentHashMap<>();

    /** Cached network profiles per graph (cheap to compute, but no reason to repeat). */
    private final Map<SpeedyGraph, NetworkProfile> profileCache = new ConcurrentHashMap<>();

    /**
     * Cache keyed by SpeedyGraph → contracted CH graph.
     * The contraction topology depends on the node ordering (from InertialFlowCutter,
     * which is purely topology-based) and the witness-search edge weights.  After
     * contraction the edge weights are always overwritten by {@link CHTTFCustomizer},
     * so the same CH skeleton can be reused regardless of which {@link TravelDisutility}
     * was used during the initial build.
     */
    private final Map<SpeedyGraph, CHGraph> chGraphCache = new ConcurrentHashMap<>();

    private final int nThreads;

    /**
     * No-arg constructor that defaults to {@link GlobalConfigGroup}'s default
     * thread count (2).  Useful when no Guice context is available.
     */
    public CHRouterFactory() {
        this(new GlobalConfigGroup());
    }

    @Inject
    public CHRouterFactory(GlobalConfigGroup globalConfig) {
        this.nThreads = Math.max(1, globalConfig.getNumberOfThreads());
    }

    @Override
    public LeastCostPathCalculator createPathCalculator(
            Network network, TravelDisutility travelCosts, TravelTime travelTimes) {

        SpeedyGraph baseGraph = baseGraphs.computeIfAbsent(network, SpeedyGraphBuilder::buildWithSpatialOrdering);

        // Look up or build the contracted CH graph (expensive; cached per network).
        CHGraph chGraph = chGraphCache.computeIfAbsent(baseGraph, key -> {

            // ---- Auto-tune parameters from network structure ----
            NetworkProfile profile = profileCache.computeIfAbsent(key, NetworkAnalyzer::analyze);
            CHBuilderParams chParams = RoutingParameterTuner.tuneCHParams(profile);
            IFCParams ifcParams = RoutingParameterTuner.tuneIFCParams(profile);

            LOG.info("[CH] Preparing contraction hierarchy for {} nodes, {} links ({} threads)",
                    fmt(baseGraph.nodeCount), fmt(baseGraph.linkCount), nThreads);
            LOG.info("[CH]   Network profile: {}", profile.toSummaryString());

            long totalStart = System.nanoTime();

            InertialFlowCutter.NDOrderResult ndOrder =
                    new InertialFlowCutter(baseGraph, ifcParams).computeOrderWithBatches();
            LOG.info("[CH]   Nested dissection ordering: {}s ({} rounds)",
                    secs(ndOrder.elapsedNanos), ndOrder.rounds.size());

            CHBuilder builder = new CHBuilder(baseGraph, travelCosts, chParams);
            CHGraph result = builder.buildWithOrderParallel(ndOrder, nThreads);
            CHBuilder.BuildStats stats = builder.getLastBuildStats();

            LOG.info("[CH]   Contraction: {}s ({} base + {} shortcuts = {} edges)",
                    secs(stats.contractionNanos()),
                    fmt(stats.baseEdges()), fmt(stats.shortcuts()), fmt(stats.totalEdges()));
            if (stats.deferredNodes() > 0) {
                LOG.info("[CH]     Deferred {} high-degree nodes → {}s",
                        fmt(stats.deferredNodes()), secs(stats.deferredNanos()));
            }
            LOG.info("[CH]   Overlay graph: {}s", secs(stats.overlayBuildNanos()));

            double totalSecs = (System.nanoTime() - totalStart) / 1_000_000_000.0;
            LOG.info("[CH] CH preprocessing complete: {}s total",
                    String.format(Locale.US, "%.1f", totalSecs));
            return result;
        });

        // Customise with time-dependent TTFs (fast O(edges × bins) pass).
        //
        // Double-checked locking: the volatile customizationFingerprint field on
        // CHGraph allows a fast unsynchronized pre-check.  Only when travel times
        // have actually changed (typically once per MATSim iteration) do we enter
        // the synchronized block.  This eliminates lock contention in the common
        // case where many threads request a path calculator within the same
        // iteration — they all see the matching fingerprint and skip the lock.
        if (CHTTFCustomizer.needsRecustomization(chGraph, travelTimes)) {
            synchronized (chGraph) {
                // Re-check under lock: another thread may have customized while we waited.
                new CHTTFCustomizer().customize(chGraph, travelTimes, travelCosts);
            }
        }

        return new CHRouterTimeDep(chGraph, travelTimes, travelCosts);
    }

    /** Formats an integer with thousands separators (e.g. 195,246). */
    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n);
    }

    /** Formats nanoseconds as seconds with one decimal (e.g. "4.1"). */
    private static String secs(long nanos) {
        return String.format(Locale.US, "%.1f", nanos / 1_000_000_000.0);
    }

}
