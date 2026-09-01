/* *********************************************************************** *
 * project: org.matsim.*
 * StaticCHRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.Link;
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
 * Factory for the time-independent {@link CHRouter}, for use cases without
 * time-of-day travel-time variation (skims, accessibility, link strippers,
 * freespeed-only routing, etc.).
 *
 * <p>Compared with {@link CHRouterFactory} (which always emits
 * {@link CHRouterTimeDep}), this factory:
 * <ul>
 *   <li>builds the {@link CHGraph} via
 *       {@link CHBuilder#withTimeDependence(boolean) CHBuilder.withTimeDependence(false)},
 *       skipping the {@code NUM_BINS x totalEdgeCount} TTF allocation entirely
 *       (tens of GB on large multi-mode networks);</li>
 *   <li>customises via {@link CHCustomizer} (one
 *       {@link TravelDisutility#getLinkMinimumTravelDisutility(Link)} lookup per
 *       real edge) instead of {@link CHTTFCustomizer}'s
 *       {@code NUM_BINS x edgeCount} {@link TravelTime#getLinkTravelTime} pass;</li>
 *   <li>returns {@link CHRouter}, whose inner loop reads {@code edgeWeights[]}
 *       directly and skips the per-edge time-bin computation present in
 *       {@code CHRouterTimeDep}.</li>
 * </ul>
 *
 * <h3>Disutility contract</h3>
 * <p>The supplied {@link TravelDisutility} must return the actual per-link cost
 * from {@link TravelDisutility#getLinkMinimumTravelDisutility(Link)
 * getLinkMinimumTravelDisutility}, because {@link CHCustomizer} writes that
 * value directly into the CH edge weights. Implementations that return
 * {@code 0} (the upstream default for several MATSim disutilities) will
 * degenerate the search to "any path". The supplied {@link TravelTime} is
 * forwarded to {@link CHRouter} but never queried by the search itself.
 *
 * <p>Note: on a static {@link CHGraph}, the inner Dijkstra advances its
 * arrival-time field by the same scalar {@code edgeWeights[]} value it uses as
 * cost. As a result, {@code getTime()} on any resulting path (or on a
 * {@link CHLeastCostPathTree} backed by a static graph) is only meaningful when
 * the supplied {@link TravelDisutility} returns travel time in seconds;
 * otherwise only {@code getCost()} and {@code getDistance()} carry meaning.
 *
 * <h3>Thread safety</h3>
 * <p>This class is {@link Singleton} and thread-safe: every call returns a new,
 * independent {@link CHRouter} instance. The underlying {@link CHGraph} is
 * cached per {@link Network} (the expensive contraction runs only once); the
 * cheap {@link CHCustomizer} pass runs on every call to reflect the current
 * {@link TravelDisutility}.
 */
@Singleton
public class StaticCHRouterFactory implements LeastCostPathCalculatorFactory {

    private static final Logger LOG = LogManager.getLogger(StaticCHRouterFactory.class);

    private final Map<Network, SpeedyGraph> baseGraphs = new ConcurrentHashMap<>();
    private final Map<SpeedyGraph, CHGraph> chGraphCache = new ConcurrentHashMap<>();

    private final int nThreads;

    /**
     * No-arg constructor that defaults to {@link GlobalConfigGroup}'s default
     * thread count. Useful when no Guice context is available.
     */
    public StaticCHRouterFactory() {
        this(new GlobalConfigGroup());
    }

    @Inject
    public StaticCHRouterFactory(GlobalConfigGroup globalConfig) {
        this.nThreads = Math.max(1, globalConfig.getNumberOfThreads());
    }

    @Override
    public LeastCostPathCalculator createPathCalculator(
            Network network, TravelDisutility travelCosts, TravelTime travelTimes) {

        SpeedyGraph baseGraph = baseGraphs.computeIfAbsent(network, SpeedyGraphBuilder::buildWithSpatialOrdering);

        CHGraph chGraph = chGraphCache.computeIfAbsent(baseGraph, key -> {
            LOG.info("[CH-static] Preparing contraction hierarchy for {} nodes, {} links ({} threads)",
                    fmt(key.nodeCount), fmt(key.linkCount), nThreads);
            long t0 = System.nanoTime();
            InertialFlowCutter.NDOrderResult ndOrder =
                    new InertialFlowCutter(key).computeOrderWithBatches();
            CHGraph result = new CHBuilder(key, travelCosts)
                    .withTimeDependence(false)
                    .buildWithOrderParallel(ndOrder, nThreads);
            double totalSecs = (System.nanoTime() - t0) / 1_000_000_000.0;
            LOG.info("[CH-static] CH preprocessing complete: {}s total",
                    String.format(Locale.US, "%.1f", totalSecs));
            return result;
        });

        // Customise on every call to reflect the current disutility. This is an
        // O(edges) pass with one getLinkMinimumTravelDisutility lookup per real
        // edge — far cheaper than CHTTFCustomizer's NUM_BINS x edges pass.
        synchronized (chGraph) {
            new CHCustomizer().customize(chGraph, travelCosts);
        }

        return new CHRouter(chGraph, travelTimes, travelCosts);
    }

    /** Formats an integer with thousands separators (e.g. 195,246). */
    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n);
    }

}
