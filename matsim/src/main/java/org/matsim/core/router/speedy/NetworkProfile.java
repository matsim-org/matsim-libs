/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkProfile.java
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

/**
 * Structural profile of a {@link SpeedyGraph}, capturing key metrics
 * that drive parameter tuning for CH and ALT routing algorithms.
 *
 * <p>All metrics are computed once by {@link NetworkAnalyzer} and then
 * consumed by {@link RoutingParameterTuner} to derive optimal parameters
 * via continuous interpolation instead of discrete tier boundaries.
 *
 * @author Steffen Axer
 */
public record NetworkProfile(
        /** Number of nodes in the graph. */
        int nodeCount,
        /** Number of directed links in the graph. */
        int linkCount,
        /** Average out-degree (links / nodes). */
        double avgOutDegree,
        /** Maximum out-degree of any node. */
        int maxOutDegree,
        /** 95th percentile out-degree. */
        int p95OutDegree,
        /** Edge-to-node ratio (linkCount / nodeCount). */
        double edgeNodeRatio,
        /** Fraction of nodes with out-degree >= 6 (high-degree hub indicator). */
        double highDegreeNodeFraction,
        /** Number of connected components (via undirected BFS). */
        int connectedComponents,
        /** Size of the largest connected component. */
        int largestComponentSize,
        /** Estimated graph diameter (max BFS distance from sample nodes). */
        int estimatedDiameter,
        /** Degree variance: measures spread of degree distribution. */
        double degreeVariance,
        /** Skewness of the degree distribution (positive = right-skewed = hub-rich). */
        double degreeSkewness
) {
    /**
     * Returns a human-readable summary string for logging/display.
     */
    public String toSummaryString() {
        return String.format(
                "NetworkProfile{nodes=%,d, links=%,d, avgDeg=%.2f, maxDeg=%d, p95Deg=%d, " +
                "e/n=%.2f, highDegFrac=%.4f, components=%d, largestComp=%,d, " +
                "estDiameter=%d, degVar=%.2f, degSkew=%.2f}",
                nodeCount, linkCount, avgOutDegree, maxOutDegree, p95OutDegree,
                edgeNodeRatio, highDegreeNodeFraction, connectedComponents, largestComponentSize,
                estimatedDiameter, degreeVariance, degreeSkewness);
    }
}

