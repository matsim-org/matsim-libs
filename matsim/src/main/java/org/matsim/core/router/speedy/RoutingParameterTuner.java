/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingParameterTuner.java
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

/**
 * Maps a {@link NetworkProfile} to optimal {@link CHBuilderParams} and
 * {@link IFCParams} using continuous interpolation between known-good
 * anchor points instead of discrete tier boundaries.
 *
 * <h3>Design</h3>
 * <p>The old 3-tier approach (small/medium/large) in CHBuilder and
 * InertialFlowCutter used hard boundaries at 200k and 500k nodes.
 * This caused parameter sensitivity: a 199k-node network gets dramatically
 * different parameters than a 201k-node network.
 *
 * <p>This tuner uses:
 * <ul>
 *   <li><b>Log-linear interpolation</b> on nodeCount for base scaling</li>
 *   <li><b>Degree-distribution corrections</b>: networks with high-degree
 *       hubs (e.g. PT overlay) get more conservative deferral thresholds</li>
 *   <li><b>Diameter corrections</b>: large-diameter networks need higher
 *       hop limits for witness searches</li>
 * </ul>
 *
 * <p>Anchor points are derived from the existing hand-tuned values for
 * Berlin (88k), Duesseldorf (~300k), and Metropole Ruhr (752k).
 *
 * @author Steffen Axer
 */
public class RoutingParameterTuner {

    private static final Logger LOG = LogManager.getLogger(RoutingParameterTuner.class);

    // Anchor points: (nodeCount, paramValue) for interpolation
    // Anchors: 50k (very small), 100k (small), 300k (medium), 750k (large), 1500k (very large)
    private static final double[] N_ANCHORS = {50_000, 100_000, 300_000, 750_000, 1_500_000};

    private RoutingParameterTuner() {}

    /**
     * Computes optimal CH builder parameters from the network profile.
     */
    public static CHBuilderParams tuneCHParams(NetworkProfile profile) {
        double n = profile.nodeCount();
        double hubFactor = hubCorrectionFactor(profile);
        double diamFactor = diameterCorrectionFactor(profile);

        // Base parameters via log-linear interpolation between anchor points.
        // Each parameter has 5 anchor values corresponding to N_ANCHORS.
        //
        // Anchor calibration notes (2025-04):
        //   50k  = very small (synthetic / small city)
        //   100k = small (Berlin 88k)
        //   300k = medium (Duesseldorf ~300k)
        //   750k = large (Metropole Ruhr 752k) -- primary calibration target
        //   1500k = very large (DE-wide / multi-region)
        //
        // The 750k anchors are calibrated to match the legacy 3-tier >=500k values
        // which achieved ~30s preprocessing on Metropole Ruhr (9.5M edges).

        int hopLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{80, 100, 200, 300, 400}) * diamFactor,
                50, 600);

        // Deferred-phase hop limit: the deferred phase contracts only ~1% of
        // nodes (high-degree hubs) on a tiny residual graph.  A full hopLimit
        // (e.g. 300) wastes time exploring cycles in a 6k-node graph.  Half
        // the ND-phase value is sufficient to discover all relevant witnesses
        // while cutting witness-search work by ~60%.  The few extra shortcuts
        // from missed witnesses are harmless (correct, <5% edge overhead).
        int deferredHopLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{40, 50, 100, 150, 200}) * diamFactor,
                30, 300);

        int settledLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{400, 500, 1500, 3000, 5000}),
                200, 8000);

        int maxSettledLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{8000, 10000, 30000, 50000, 80000}),
                5000, 150000);

        int deferredMaxSettledLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{4000, 5000, 10000, 10000, 15000}),
                3000, 30000);

        // Hub-rich networks benefit from higher skip thresholds (less witness search)
        int skipWitnessDegreeProduct = clampInt(
                interpLog(n, N_ANCHORS, new double[]{40, 50, 60, 80, 100}) * hubFactor,
                30, 150);

        int prioHopLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{8, 10, 12, 15, 20}) * diamFactor,
                5, 30);

        int prioSettledLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{150, 200, 400, 500, 800}),
                100, 1200);

        int deferredPrioHopLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{15, 20, 25, 30, 40}) * diamFactor,
                10, 60);

        int deferredPrioSettledLimit = clampInt(
                interpLog(n, N_ANCHORS, new double[]{300, 500, 1000, 1500, 2500}),
                200, 4000);

        int cellReorderThreshold = clampInt(
                interpLog(n, N_ANCHORS, new double[]{120, 100, 50, 50, 40}),
                20, 200);

        int adaptiveContractionThreshold = clampInt(
                interpLog(n, N_ANCHORS, new double[]{120, 100, 50, 50, 40}),
                20, 200);

        // deferDegreeProduct: the legacy best-performing value for 750k was 1500.
        // Lower = more deferred nodes (slower deferred phase but prevents edge explosion).
        // Higher = fewer deferred but risks exponential shortcut growth in late ND rounds.
        int deferDegreeProduct = clampInt(
                interpLog(n, N_ANCHORS, new double[]{6000, 5000, 2500, 1500, 1000}) / hubFactor,
                500, 8000);

        int reestimateSkipDegree = clampInt(
                interpLog(n, N_ANCHORS, new double[]{6, 5, 4, 4, 3}),
                2, 8);

        int reestimateInterval = clampInt(
                interpLog(n, N_ANCHORS, new double[]{6, 5, 3, 3, 2}),
                1, 8);

        CHBuilderParams params = new CHBuilderParams(
                hopLimit, deferredHopLimit, settledLimit, maxSettledLimit, deferredMaxSettledLimit,
                skipWitnessDegreeProduct, prioHopLimit, prioSettledLimit,
                deferredPrioHopLimit, deferredPrioSettledLimit,
                cellReorderThreshold, adaptiveContractionThreshold,
                deferDegreeProduct, reestimateSkipDegree, reestimateInterval);

        LOG.info("Auto-tuned CH params for {} nodes (hubFactor={}, diamFactor={}): {}",
                profile.nodeCount(),
                String.format("%.2f", hubFactor),
                String.format("%.2f", diamFactor),
                params);

        return params;
    }

    /**
     * Computes optimal InertialFlowCutter parameters from the network profile.
     */
    public static IFCParams tuneIFCParams(NetworkProfile profile) {
        double n = profile.nodeCount();

        int fmMinSize = clampInt(
                interpLog(n, N_ANCHORS, new double[]{250, 200, 100, 200, 300}),
                50, 500);

        int fmMaxPasses = clampInt(
                interpLog(n, N_ANCHORS, new double[]{3, 3, 4, 3, 3}),
                2, 6);

        int maxflowMinSize = clampInt(
                interpLog(n, N_ANCHORS, new double[]{600, 500, 200, 1000, 1500}),
                100, 2000);

        int maxflowBorderDepth = clampInt(
                interpLog(n, N_ANCHORS, new double[]{5, 5, 6, 5, 4}),
                3, 10);

        int parallelMinSize = clampInt(
                interpLog(n, N_ANCHORS, new double[]{800, 1000, 2000, 2000, 3000}),
                500, 5000);

        int reducedDirectionsThreshold = clampInt(
                interpLog(n, N_ANCHORS, new double[]{200, 300, 500, 1000, 1500}),
                100, 2000);

        int reducedRatiosThreshold = clampInt(
                interpLog(n, N_ANCHORS, new double[]{12000, 10000, 5000, 8000, 10000}),
                3000, 15000);

        IFCParams params = new IFCParams(
                fmMinSize, fmMaxPasses, maxflowMinSize, maxflowBorderDepth,
                parallelMinSize, reducedDirectionsThreshold, reducedRatiosThreshold);

        LOG.info("Auto-tuned IFC params for {} nodes: {}", profile.nodeCount(), params);

        return params;
    }

    /**
     * Suggests optimal landmark count for SpeedyALT based on network size.
     * Larger networks benefit from more landmarks (tighter bounds).
     *
     * @param profile       the network profile
     * @param userOverride  user-configured landmark count (0 = auto)
     * @return recommended landmark count
     */
    public static int tuneLandmarkCount(NetworkProfile profile, int userOverride) {
        if (userOverride > 0) return userOverride;

        int n = profile.nodeCount();
        // Scale from 12 (tiny) to 24 (very large) landmarks
        int landmarks = clampInt(
                interpLog(n, N_ANCHORS, new double[]{12, 16, 16, 20, 24}),
                8, 32);

        LOG.info("Auto-tuned landmark count for {} nodes: {}", n, landmarks);
        return landmarks;
    }

    // ---- Correction factors ----

    /**
     * Hub correction factor: networks with many high-degree nodes (e.g. PT overlay)
     * need more aggressive deferral and higher skip-witness thresholds.
     * Returns a multiplier >= 1.0.
     */
    private static double hubCorrectionFactor(NetworkProfile profile) {
        // highDegreeNodeFraction: fraction of nodes with deg >= 6
        // Typical road network: ~0.02-0.05
        // PT overlay network: ~0.10-0.20
        double hdf = profile.highDegreeNodeFraction();
        // Skewness also indicates hub presence (right-skewed = hub-rich)
        double skew = Math.max(0, profile.degreeSkewness());

        // Base factor from high-degree fraction: 1.0 at 0.04, ~1.3 at 0.15
        // Dampened vs original (was 0.5 gain → now 0.3) to avoid over-correction
        // on typical road networks like Metropole Ruhr (hdf ~0.04-0.06).
        double hdfFactor = 1.0 + Math.max(0, (hdf - 0.04) / 0.12) * 0.3;

        // Skewness bonus: 1.0 at skew=3, 1.15 at skew=9
        double skewFactor = 1.0 + Math.max(0, (skew - 3.0) / 6.0) * 0.15;

        return Math.min(1.6, hdfFactor * skewFactor);
    }

    /**
     * Diameter correction factor: large-diameter networks need higher hop limits
     * for witness searches to find paths that span the network.
     * Returns a multiplier >= 1.0.
     */
    private static double diameterCorrectionFactor(NetworkProfile profile) {
        int diam = profile.estimatedDiameter();
        int n = profile.nodeCount();

        // Expected diameter for a "typical" road network: ~sqrt(n) * 0.5
        double expectedDiam = Math.sqrt(n) * 0.5;
        double ratio = diam / Math.max(1, expectedDiam);

        // If diameter is much larger than expected (e.g. long corridor network),
        // increase hop limits.  Dampened correction: only triggers at ratio > 1.5
        // (was 1.2) with gentler slope (0.15 instead of 0.25) to avoid over-
        // correcting on typical road networks which have moderate diameters.
        if (ratio > 1.5) {
            return Math.min(1.4, 1.0 + (ratio - 1.5) * 0.15);
        }
        return 1.0;
    }

    // ---- Interpolation helpers ----

    /**
     * Log-linear interpolation: maps x to y using anchor points.
     * Uses log(x) for interpolation to handle the wide range of network sizes
     * (50k to 1.5M) smoothly.
     */
    private static double interpLog(double x, double[] xAnchors, double[] yAnchors) {
        double logX = Math.log(Math.max(1, x));

        // Clamp to range
        double logMin = Math.log(xAnchors[0]);
        double logMax = Math.log(xAnchors[xAnchors.length - 1]);

        if (logX <= logMin) return yAnchors[0];
        if (logX >= logMax) return yAnchors[yAnchors.length - 1];

        // Find segment
        for (int i = 0; i < xAnchors.length - 1; i++) {
            double logLo = Math.log(xAnchors[i]);
            double logHi = Math.log(xAnchors[i + 1]);
            if (logX <= logHi) {
                double t = (logX - logLo) / (logHi - logLo);
                return yAnchors[i] + t * (yAnchors[i + 1] - yAnchors[i]);
            }
        }
        return yAnchors[yAnchors.length - 1];
    }

    private static int clampInt(double value, int min, int max) {
        return Math.max(min, Math.min(max, (int) Math.round(value)));
    }
}









