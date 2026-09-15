/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingParameterTunerTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RoutingParameterTuner} to ensure auto-tuned parameters
 * are sane, monotonic where expected, and within clamp bounds.
 *
 * @author Steffen Axer
 */
public class RoutingParameterTunerTest {

    // ---- Typical road-network profiles for testing ----

    /** Small city network (~50k nodes), typical road topology. */
    private static final NetworkProfile SMALL = new NetworkProfile(
            50_000, 120_000, 2.4, 8, 4, 2.4, 0.03, 1, 50_000, 200, 1.5, 2.0);

    /** Medium network (~100k nodes, Berlin-like). */
    private static final NetworkProfile MEDIUM = new NetworkProfile(
            100_000, 240_000, 2.4, 10, 4, 2.4, 0.04, 1, 100_000, 300, 1.8, 2.5);

    /** Large network (~300k nodes, Duesseldorf-like). */
    private static final NetworkProfile LARGE = new NetworkProfile(
            300_000, 720_000, 2.4, 12, 5, 2.4, 0.05, 2, 298_000, 500, 2.0, 3.0);

    /** Very large network (~750k nodes, Metropole-Ruhr-like). */
    private static final NetworkProfile VERY_LARGE = new NetworkProfile(
            750_000, 1_800_000, 2.4, 15, 5, 2.4, 0.05, 3, 745_000, 700, 2.2, 3.5);

    /** Hub-rich network (e.g. PT overlay, high-degree fraction). */
    private static final NetworkProfile HUB_RICH = new NetworkProfile(
            200_000, 600_000, 3.0, 50, 8, 3.0, 0.15, 1, 200_000, 400, 5.0, 7.0);

    /** Long corridor network (high diameter relative to sqrt(n)). */
    private static final NetworkProfile CORRIDOR = new NetworkProfile(
            100_000, 220_000, 2.2, 6, 3, 2.2, 0.02, 1, 100_000, 900, 1.2, 1.5);

    // ======================================================================
    // CHBuilderParams tests
    // ======================================================================

    @Test
    void testDeferredHopLimitSmallerThanHopLimit() {
        for (NetworkProfile profile : new NetworkProfile[]{SMALL, MEDIUM, LARGE, VERY_LARGE, HUB_RICH, CORRIDOR}) {
            CHBuilderParams p = RoutingParameterTuner.tuneCHParams(profile);
            assertTrue(p.deferredHopLimit() < p.hopLimit(),
                    "deferredHopLimit (" + p.deferredHopLimit() + ") should be < hopLimit (" + p.hopLimit()
                            + ") for " + profile.nodeCount() + " nodes");
            assertTrue(p.deferredHopLimit() >= 30,
                    "deferredHopLimit should be >= 30 (clamp lower bound)");
        }
    }

    @Test
    void testDeferredHopLimitApproximatelyHalfOfHopLimit() {
        for (NetworkProfile profile : new NetworkProfile[]{SMALL, MEDIUM, LARGE, VERY_LARGE}) {
            CHBuilderParams p = RoutingParameterTuner.tuneCHParams(profile);
            double ratio = (double) p.deferredHopLimit() / p.hopLimit();
            assertTrue(ratio >= 0.3 && ratio <= 0.7,
                    "deferredHopLimit/hopLimit ratio should be ~0.5 (got " + ratio
                            + " for " + profile.nodeCount() + " nodes)");
        }
    }

    @Test
    void testHopLimitMonotonicallyIncreasing() {
        NetworkProfile[] profiles = {SMALL, MEDIUM, LARGE, VERY_LARGE};
        for (int i = 1; i < profiles.length; i++) {
            CHBuilderParams prev = RoutingParameterTuner.tuneCHParams(profiles[i - 1]);
            CHBuilderParams curr = RoutingParameterTuner.tuneCHParams(profiles[i]);
            assertTrue(curr.hopLimit() >= prev.hopLimit(),
                    "hopLimit should increase with network size: "
                            + profiles[i - 1].nodeCount() + "→" + profiles[i].nodeCount());
        }
    }

    @Test
    void testSettledLimitMonotonicallyIncreasing() {
        NetworkProfile[] profiles = {SMALL, MEDIUM, LARGE, VERY_LARGE};
        for (int i = 1; i < profiles.length; i++) {
            CHBuilderParams prev = RoutingParameterTuner.tuneCHParams(profiles[i - 1]);
            CHBuilderParams curr = RoutingParameterTuner.tuneCHParams(profiles[i]);
            assertTrue(curr.settledLimit() >= prev.settledLimit(),
                    "settledLimit should increase with network size: "
                            + profiles[i - 1].nodeCount() + "→" + profiles[i].nodeCount());
        }
    }

    @Test
    void testDeferDegreeProductMonotonicallyDecreasing() {
        // Larger networks need MORE aggressive deferral (lower threshold)
        NetworkProfile[] profiles = {SMALL, MEDIUM, LARGE, VERY_LARGE};
        for (int i = 1; i < profiles.length; i++) {
            CHBuilderParams prev = RoutingParameterTuner.tuneCHParams(profiles[i - 1]);
            CHBuilderParams curr = RoutingParameterTuner.tuneCHParams(profiles[i]);
            assertTrue(curr.deferDegreeProduct() <= prev.deferDegreeProduct(),
                    "deferDegreeProduct should decrease with network size: "
                            + profiles[i - 1].nodeCount() + "→" + profiles[i].nodeCount());
        }
    }

    @Test
    void testClampBoundsRespected() {
        // Test with extreme profiles that might push values to limits
        NetworkProfile tiny = new NetworkProfile(
                1_000, 2_000, 2.0, 4, 3, 2.0, 0.01, 1, 1_000, 30, 0.5, 0.5);
        NetworkProfile huge = new NetworkProfile(
                5_000_000, 12_000_000, 2.4, 20, 6, 2.4, 0.06, 5, 4_900_000, 2000, 3.0, 4.0);

        CHBuilderParams pTiny = RoutingParameterTuner.tuneCHParams(tiny);
        CHBuilderParams pHuge = RoutingParameterTuner.tuneCHParams(huge);

        // hopLimit clamp: [50, 600]
        assertTrue(pTiny.hopLimit() >= 50 && pTiny.hopLimit() <= 600);
        assertTrue(pHuge.hopLimit() >= 50 && pHuge.hopLimit() <= 600);

        // deferredHopLimit clamp: [30, 300]
        assertTrue(pTiny.deferredHopLimit() >= 30 && pTiny.deferredHopLimit() <= 300);
        assertTrue(pHuge.deferredHopLimit() >= 30 && pHuge.deferredHopLimit() <= 300);

        // settledLimit clamp: [200, 8000]
        assertTrue(pTiny.settledLimit() >= 200 && pTiny.settledLimit() <= 8000);
        assertTrue(pHuge.settledLimit() >= 200 && pHuge.settledLimit() <= 8000);

        // deferDegreeProduct clamp: [500, 8000]
        assertTrue(pTiny.deferDegreeProduct() >= 500 && pTiny.deferDegreeProduct() <= 8000);
        assertTrue(pHuge.deferDegreeProduct() >= 500 && pHuge.deferDegreeProduct() <= 8000);
    }

    @Test
    void testHubRichNetworkIncreasesSkipWitness() {
        // Hub-rich network should have higher skipWitnessDegreeProduct than normal
        CHBuilderParams normal = RoutingParameterTuner.tuneCHParams(MEDIUM);
        CHBuilderParams hubRich = RoutingParameterTuner.tuneCHParams(HUB_RICH);
        assertTrue(hubRich.skipWitnessDegreeProduct() > normal.skipWitnessDegreeProduct(),
                "Hub-rich network should have higher skipWitnessDegreeProduct");
    }

    @Test
    void testHubRichNetworkDecreasesDeferDegreeProduct() {
        // Hub-rich network should defer more aggressively (lower threshold)
        CHBuilderParams normal = RoutingParameterTuner.tuneCHParams(MEDIUM);
        CHBuilderParams hubRich = RoutingParameterTuner.tuneCHParams(HUB_RICH);
        assertTrue(hubRich.deferDegreeProduct() < normal.deferDegreeProduct(),
                "Hub-rich network should have lower deferDegreeProduct (more deferral)");
    }

    @Test
    void testCorridorNetworkIncreasesHopLimits() {
        // Corridor network (high diameter) should get higher hop limits
        CHBuilderParams normal = RoutingParameterTuner.tuneCHParams(MEDIUM);
        CHBuilderParams corridor = RoutingParameterTuner.tuneCHParams(CORRIDOR);
        // Corridor has same nodeCount as MEDIUM but much larger diameter
        assertTrue(corridor.hopLimit() >= normal.hopLimit(),
                "Corridor network should have >= hopLimit than normal network of same size");
    }

    @Test
    void testAllParametersPositive() {
        for (NetworkProfile profile : new NetworkProfile[]{SMALL, MEDIUM, LARGE, VERY_LARGE, HUB_RICH, CORRIDOR}) {
            CHBuilderParams p = RoutingParameterTuner.tuneCHParams(profile);
            assertTrue(p.hopLimit() > 0, "hopLimit must be positive");
            assertTrue(p.deferredHopLimit() > 0, "deferredHopLimit must be positive");
            assertTrue(p.settledLimit() > 0, "settledLimit must be positive");
            assertTrue(p.maxSettledLimit() > 0, "maxSettledLimit must be positive");
            assertTrue(p.deferredMaxSettledLimit() > 0, "deferredMaxSettledLimit must be positive");
            assertTrue(p.skipWitnessDegreeProduct() > 0, "skipWitnessDegreeProduct must be positive");
            assertTrue(p.prioHopLimit() > 0, "prioHopLimit must be positive");
            assertTrue(p.prioSettledLimit() > 0, "prioSettledLimit must be positive");
            assertTrue(p.deferredPrioHopLimit() > 0, "deferredPrioHopLimit must be positive");
            assertTrue(p.deferredPrioSettledLimit() > 0, "deferredPrioSettledLimit must be positive");
            assertTrue(p.cellReorderThreshold() > 0, "cellReorderThreshold must be positive");
            assertTrue(p.adaptiveContractionThreshold() > 0, "adaptiveContractionThreshold must be positive");
            assertTrue(p.deferDegreeProduct() > 0, "deferDegreeProduct must be positive");
            assertTrue(p.reestimateSkipDegree() > 0, "reestimateSkipDegree must be positive");
            assertTrue(p.reestimateInterval() > 0, "reestimateInterval must be positive");
        }
    }

    // ======================================================================
    // IFCParams tests
    // ======================================================================

    @Test
    void testIFCParamsAllPositive() {
        for (NetworkProfile profile : new NetworkProfile[]{SMALL, MEDIUM, LARGE, VERY_LARGE}) {
            IFCParams p = RoutingParameterTuner.tuneIFCParams(profile);
            assertTrue(p.fmMinSize() > 0, "fmMinSize must be positive");
            assertTrue(p.fmMaxPasses() > 0, "fmMaxPasses must be positive");
            assertTrue(p.maxflowMinSize() > 0, "maxflowMinSize must be positive");
            assertTrue(p.maxflowBorderDepth() > 0, "maxflowBorderDepth must be positive");
            assertTrue(p.parallelMinSize() > 0, "parallelMinSize must be positive");
            assertTrue(p.reducedDirectionsThreshold() > 0, "reducedDirectionsThreshold must be positive");
            assertTrue(p.reducedRatiosThreshold() > 0, "reducedRatiosThreshold must be positive");
        }
    }

    // ======================================================================
    // Landmark count test
    // ======================================================================

    @Test
    void testLandmarkCountAutoTune() {
        int small = RoutingParameterTuner.tuneLandmarkCount(SMALL, 0);
        int large = RoutingParameterTuner.tuneLandmarkCount(VERY_LARGE, 0);
        assertTrue(small >= 8 && small <= 32, "Landmark count should be in [8, 32]");
        assertTrue(large >= small, "Larger networks should get >= landmarks");
    }

    @Test
    void testLandmarkCountUserOverride() {
        int result = RoutingParameterTuner.tuneLandmarkCount(MEDIUM, 24);
        assertEquals(24, result, "User override should be respected");
    }

    // ======================================================================
    // Regression: known calibration targets
    // ======================================================================

    @Test
    void testMetropoleRuhrCalibrationTarget() {
        // The 750k anchor is calibrated to match legacy >=500k tier values.
        // hopLimit should be ~300, settledLimit ~3000.
        CHBuilderParams p = RoutingParameterTuner.tuneCHParams(VERY_LARGE);
        assertTrue(p.hopLimit() >= 250 && p.hopLimit() <= 350,
                "750k network hopLimit should be ~300 (got " + p.hopLimit() + ")");
        assertTrue(p.deferredHopLimit() >= 120 && p.deferredHopLimit() <= 180,
                "750k network deferredHopLimit should be ~150 (got " + p.deferredHopLimit() + ")");
        assertTrue(p.settledLimit() >= 2500 && p.settledLimit() <= 3500,
                "750k network settledLimit should be ~3000 (got " + p.settledLimit() + ")");
        assertTrue(p.deferDegreeProduct() >= 1000 && p.deferDegreeProduct() <= 2000,
                "750k network deferDegreeProduct should be ~1500 (got " + p.deferDegreeProduct() + ")");
    }
}

