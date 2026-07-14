/* *********************************************************************** *
 * project: org.matsim.*
 * RouterBenchmark.java
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
package org.matsim.benchmark;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.speedy.CHBuilder;
import org.matsim.core.router.speedy.CHBuilderParams;
import org.matsim.core.router.speedy.CHGraph;
import org.matsim.core.router.speedy.CHRouterTimeDep;
import org.matsim.core.router.speedy.CHTTFCustomizer;
import org.matsim.core.router.speedy.IFCParams;
import org.matsim.core.router.speedy.InertialFlowCutter;
import org.matsim.core.router.speedy.NetworkAnalyzer;
import org.matsim.core.router.speedy.NetworkProfile;
import org.matsim.core.router.speedy.RoutingParameterTuner;
import org.matsim.core.router.speedy.SpeedyALT;
import org.matsim.core.router.speedy.SpeedyALTData;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Standalone benchmark comparing <b>router variants</b> on a MATSim network.
 *
 * <p>Supports two built-in networks that are automatically downloaded when
 * {@code --network} is not specified:
 * <ul>
 *   <li><b>berlin</b> (default) — Berlin v7.0 (~88k nodes, ~200k links)</li>
 *   <li><b>duesseldorf</b> — Düsseldorf v1.2 network</li>
 * </ul>
 *
 * <p>Routers benchmarked:
 * <ol>
 *   <li>SpeedyALT (Z-order)</li>
 *   <li>CH Time-Dependent (Z-order)</li>
 * </ol>
 *
 * <p>OD pair generation modes:
 * <ul>
 *   <li><b>mid</b> (default) — Distance-weighted sampling inspired by
 *       MiD 2017 (Mobilität in Deutschland). Beeline distances follow a
 *       log-normal distribution (median ≈ 5 km, mean ≈ 10 km).</li>
 *   <li><b>random</b> — Uniform random origin and destination nodes.</li>
 * </ul>
 *
 * <p>Run with sufficient heap, e.g.:
 * <pre>
 *   java -Xmx8G -cp ... org.matsim.benchmark.RouterBenchmark \
 *        [--network path/to/network.xml.gz | berlin | duesseldorf] \
 *        [--queries 2000] [--warmup 200] [--landmarks 16] \
 *        [--od-mode mid|random]
 * </pre>
 *
 * <p>If {@code --network} is omitted, the Berlin v7.0 network is used.
 *
 * @author Steffen Axer
 */
public class RouterBenchmark {

    private static final String BERLIN_NETWORK_URL =
            "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/"
                    + "berlin-v7.0/input/berlin-v7.0-network.xml.gz";

    private static final String DUESSELDORF_NETWORK_URL =
            "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/duesseldorf/"
                    + "duesseldorf-v1.0/input/duesseldorf-v1.2-network.xml.gz";

    private static int warmupQueries    = 200;
    private static int benchmarkQueries = 2_000;
    private static int altLandmarks     = 16;
    private static String odMode        = "mid";

    record RouterEntry(String name, LeastCostPathCalculator router) {}

    /**
     * Result of OD pair generation, including beeline distance statistics.
     */
    record ODPairResult(int[][] pairs, double[] beelineDistances, double metersPerUnit) {
        double medianKm() { return percentileKm(50); }
        double meanKm() {
            double sum = 0;
            for (double d : beelineDistances) sum += d;
            return (sum / beelineDistances.length) * metersPerUnit / 1000.0;
        }
        double percentileKm(int p) {
            double[] sorted = beelineDistances.clone();
            Arrays.sort(sorted);
            int idx = Math.min((int) (sorted.length * p / 100.0), sorted.length - 1);
            return sorted[idx] * metersPerUnit / 1000.0;
        }
        double minKm() {
            double min = Double.MAX_VALUE;
            for (double d : beelineDistances) min = Math.min(min, d);
            return min * metersPerUnit / 1000.0;
        }
        double maxKm() {
            double max = 0;
            for (double d : beelineDistances) max = Math.max(max, d);
            return max * metersPerUnit / 1000.0;
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        Configurator.setLevel(CHBuilder.class.getName(), Level.DEBUG);
        Configurator.setLevel(InertialFlowCutter.class.getName(), Level.DEBUG);
        Configurator.setLevel(SpeedyGraphBuilder.class.getName(), Level.DEBUG);
        Configurator.setLevel(NetworkAnalyzer.class.getName(), Level.DEBUG);
        Configurator.setLevel(RoutingParameterTuner.class.getName(), Level.DEBUG);

        String networkPath = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--network"   -> networkPath       = args[++i];
                case "--queries"   -> benchmarkQueries   = Integer.parseInt(args[++i]);
                case "--warmup"    -> warmupQueries       = Integer.parseInt(args[++i]);
                case "--landmarks" -> altLandmarks        = Integer.parseInt(args[++i]);
                case "--od-mode"   -> odMode              = args[++i].toLowerCase(Locale.ROOT);
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Usage: java ... RouterBenchmark "
                            + "[--network <path|berlin|duesseldorf>] [--queries <n>] [--warmup <n>] "
                            + "[--landmarks <n>] [--od-mode mid|random]");
                    System.exit(1);
                }
            }
        }

        if (!odMode.equals("mid") && !odMode.equals("random")) {
            System.err.println("ERROR: --od-mode must be 'mid' or 'random' (got '" + odMode + "')");
            System.exit(1);
        }

        // ...existing code... (heap check, network loading, graph building, CH/ALT build)
        long maxHeapMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        System.out.printf("JVM max heap: %,d MB%n", maxHeapMB);
        if (maxHeapMB < 3000) {
            System.err.println("ERROR: Need at least -Xmx4G. Current max heap: " + maxHeapMB + " MB.");
            System.err.println("  Usage: java -Xmx8G -cp <classpath> " + RouterBenchmark.class.getName());
            System.exit(1);
        }

        Network network = loadNetwork(networkPath);
        System.out.printf("Network: %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());

        FreespeedTravelTimeAndDisutility tc =
                new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        System.out.println();
        System.out.println("Building SpeedyGraph (Morton Z-order) ...");
        SpeedyGraph graph = SpeedyGraphBuilder.buildWithSpatialOrdering(network);

        System.out.println();
        System.out.println("Analysing network structure ...");
        NetworkProfile profile = NetworkAnalyzer.analyze(graph);
        System.out.println("  " + profile.toSummaryString());

        CHBuilderParams chParams = RoutingParameterTuner.tuneCHParams(profile);
        IFCParams ifcParams = RoutingParameterTuner.tuneIFCParams(profile);
        int autoLandmarks = RoutingParameterTuner.tuneLandmarkCount(profile, 0);
        int effectiveLandmarks = altLandmarks > 0 ? altLandmarks : autoLandmarks;

        System.out.printf("  Auto-tuned CH params:  %s%n", chParams);
        System.out.printf("  Auto-tuned IFC params: %s%n", ifcParams);
        System.out.printf("  Landmarks: %d (auto=%d, cli=%d)%n", effectiveLandmarks, autoLandmarks, altLandmarks);

        System.out.println();
        System.out.println("Computing InertialFlowCutter ND order (auto-tuned) ...");
        long t0 = System.nanoTime();
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(graph, ifcParams).computeOrderWithBatches();
        long orderMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("  ND Order:  %,6d ms%n", orderMs);

        System.out.println();
        System.out.println("Building CH (auto-tuned) ...");
        long t1 = System.nanoTime();
        CHGraph chGraph = new CHBuilder(graph, tc, chParams).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        long totalBuildMs = orderMs + (System.nanoTime() - t1) / 1_000_000;
        System.out.printf("  CH build:  %,6d ms  (%,d edges)%n", totalBuildMs, chGraph.getTotalEdgeCount());

        System.out.println();
        System.out.println("Building SpeedyALT landmarks ...");
        long altStart = System.nanoTime();
        int altThreads = Runtime.getRuntime().availableProcessors();
        SpeedyALTData altData = new SpeedyALTData(graph, Math.min(effectiveLandmarks, graph.getNodeCount()), tc, altThreads);
        long altMs = (System.nanoTime() - altStart) / 1_000_000;
        System.out.printf("  ALT build: %,6d ms  (%d landmarks, %d threads)%n", altMs, effectiveLandmarks, altThreads);

        List<RouterEntry> routers = buildRouters(altData, chGraph, tc);

        List<Node> nodeList = new ArrayList<>(network.getNodes().values());

        // ---- Generate OD pairs ----
        System.out.println();
        System.out.printf("Generating OD pairs (mode=%s) ...%n", odMode);
        ODPairResult warmupOD = generateODPairs(nodeList, warmupQueries, new Random(42), odMode);
        ODPairResult benchOD  = generateODPairs(nodeList, benchmarkQueries, new Random(123), odMode);

        System.out.printf("  Benchmark beeline distances: min=%.1f km, median=%.1f km, mean=%.1f km, "
                        + "P90=%.1f km, P95=%.1f km, max=%.1f km%n",
                benchOD.minKm(), benchOD.medianKm(), benchOD.meanKm(),
                benchOD.percentileKm(90), benchOD.percentileKm(95), benchOD.maxKm());

        System.out.println();
        System.out.printf("Warming up (%d queries per router) ...%n", warmupQueries);
        for (int i = 0; i < warmupQueries; i++) {
            Node s = nodeList.get(warmupOD.pairs()[i][0]);
            Node d = nodeList.get(warmupOD.pairs()[i][1]);
            double depTime = 8.0 * 3600;
            for (RouterEntry entry : routers) {
                entry.router().calcLeastCostPath(s, d, depTime, null, null);
            }
        }

        // Reset CH stats now so warmup queries don't pollute the measurement.
        CHRouterTimeDep chRouter = (CHRouterTimeDep) routers.stream()
                .filter(e -> e.router() instanceof CHRouterTimeDep)
                .map(RouterEntry::router)
                .findFirst().orElseThrow();
        chRouter.resetStats();

        System.out.printf("Running benchmark (%,d queries per router) ...%n", benchmarkQueries);
        int[][] pairs = benchOD.pairs();

        long[] elapsedNs = new long[routers.size()];
        for (int r = 0; r < routers.size(); r++) {
            elapsedNs[r] = benchmarkRouter(routers.get(r).router(), nodeList, pairs, routers.get(r).name());
        }

        LeastCostPathCalculator refRouter = routers.getFirst().router();
        int mismatches = 0;
        double maxCostDiff = 0;
        for (int i = 0; i < Math.min(200, benchmarkQueries); i++) {
            Node s = nodeList.get(pairs[i][0]);
            Node d = nodeList.get(pairs[i][1]);
            Path refPath = refRouter.calcLeastCostPath(s, d, 8.0 * 3600, null, null);
            for (int r = 1; r < routers.size(); r++) {
                Path otherPath = routers.get(r).router().calcLeastCostPath(s, d, 8.0 * 3600, null, null);
                if (refPath != null && otherPath != null) {
                    double diff = Math.abs(refPath.travelCost - otherPath.travelCost);
                    maxCostDiff = Math.max(maxCostDiff, diff);
                    if (diff > 1e-3) {
                        mismatches++;
                        if (mismatches <= 5) {
                            System.err.printf("  MISMATCH #%d: %s->%s  %s=%.4f  %s=%.4f  diff=%.6f%n",
                                    mismatches, s.getId(), d.getId(),
                                    routers.getFirst().name(), refPath.travelCost,
                                    routers.get(r).name(), otherPath.travelCost, diff);
                        }
                    }
                }
            }
        }

        double edgeOverhead = ((double) chGraph.getTotalEdgeCount() / network.getLinks().size() - 1) * 100;

        int nodeCount = network.getNodes().size();
        List<String[]> resultRows = new ArrayList<>();
        resultRows.add(new String[]{ "Network" });
        resultRows.add(new String[]{ "  Nodes",           String.format("%,d", nodeCount) });
        resultRows.add(new String[]{ "  Links",           String.format("%,d", network.getLinks().size()) });
        resultRows.add(new String[]{ "  CH edges",        String.format("%,d  (%+.1f%% overhead)", chGraph.getTotalEdgeCount(), edgeOverhead) });
        resultRows.add(new String[]{ "  Param mode",      "auto-tuned" });
        resultRows.add(new String[]{ "  Avg out-degree",  String.format("%.2f", profile.avgOutDegree()) });
        resultRows.add(new String[]{ "  P95 out-degree",  String.valueOf(profile.p95OutDegree()) });
        resultRows.add(new String[]{ "  Max out-degree",  String.valueOf(profile.maxOutDegree()) });
        resultRows.add(new String[]{ "  Hub fraction",    String.format("%.4f (deg>=6)", profile.highDegreeNodeFraction()) });
        resultRows.add(new String[]{ "  Deg skewness",    String.format("%.2f", profile.degreeSkewness()) });
        resultRows.add(new String[]{ "  Est. diameter",   String.valueOf(profile.estimatedDiameter()) });
        resultRows.add(new String[]{ "  Components",      String.valueOf(profile.connectedComponents()) });
        resultRows.add(null);
        resultRows.add(new String[]{ "OD Pairs",          String.format("(mode=%s)", odMode) });
        resultRows.add(new String[]{ "  Beeline min",     String.format("%.1f km", benchOD.minKm()) });
        resultRows.add(new String[]{ "  Beeline median",  String.format("%.1f km", benchOD.medianKm()) });
        resultRows.add(new String[]{ "  Beeline mean",    String.format("%.1f km", benchOD.meanKm()) });
        resultRows.add(new String[]{ "  Beeline P90",     String.format("%.1f km", benchOD.percentileKm(90)) });
        resultRows.add(new String[]{ "  Beeline P95",     String.format("%.1f km", benchOD.percentileKm(95)) });
        resultRows.add(new String[]{ "  Beeline max",     String.format("%.1f km", benchOD.maxKm()) });
        resultRows.add(null);
        resultRows.add(new String[]{ "Preprocessing" });
        resultRows.add(new String[]{ "  ND Order",        String.format("%,d ms", orderMs) });
        resultRows.add(new String[]{ "  CH build",        String.format("%,d ms  (incl. order)", totalBuildMs) });
        resultRows.add(new String[]{ "  ALT build",       String.format("%,d ms  (%d landmarks)", altMs, effectiveLandmarks) });
        resultRows.add(null);
        resultRows.add(new String[]{ "Query Performance", String.format("(%,d queries, %d warmup)", benchmarkQueries, warmupQueries) });
        double refUs = elapsedNs[0] / (benchmarkQueries * 1000.0);
        for (int r = 0; r < routers.size(); r++) {
            double avgUs = elapsedNs[r] / (benchmarkQueries * 1000.0);
            String value = r == 0
                    ? String.format("%,.0f µs/query", avgUs)
                    : String.format("%,.0f µs/query  (%.1fx)", avgUs, refUs / avgUs);
            resultRows.add(new String[]{ "  " + routers.get(r).name(), value });
        }
        resultRows.add(null);
        resultRows.add(new String[]{ "Correctness" });
        resultRows.add(new String[]{ "  Max cost diff",   String.format("%.6f", maxCostDiff) });
        resultRows.add(new String[]{ "  Mismatches",      String.format("%d  (threshold: 1e-3)", mismatches) });

        // CH Quality: search space size is the hardware-independent quality metric.
        // A good CH settles << 1% of all nodes per query direction.
        long chQueries = chRouter.getChQueryCount();
        if (chQueries > 0) {
            double avgFwd = (double) chRouter.getTotalFwdSettled() / chQueries;
            double avgBwd = (double) chRouter.getTotalBwdSettled() / chQueries;
            double avgTotal = avgFwd + avgBwd;
            double searchSpaceRatio = avgTotal / nodeCount * 100.0;
            double dijkstraSpeedup = nodeCount / Math.max(1, avgTotal);
            resultRows.add(null);
            resultRows.add(new String[]{ "CH Quality  (search space, lower = better)" });
            resultRows.add(new String[]{ "  Avg settled fwd",    String.format("%,.0f nodes/query", avgFwd) });
            resultRows.add(new String[]{ "  Avg settled bwd",    String.format("%,.0f nodes/query", avgBwd) });
            resultRows.add(new String[]{ "  Avg settled total",  String.format("%,.0f nodes/query", avgTotal) });
            resultRows.add(new String[]{ "  Search space ratio", String.format("%.3f%%  of %,d nodes", searchSpaceRatio, nodeCount) });
            resultRows.add(new String[]{ "  vs Dijkstra",        String.format("~%.0fx fewer settled nodes", dijkstraSpeedup) });
        }

        System.out.println();
        printBox("Routing Benchmark  —  ALT vs CH Comparison", resultRows.toArray(String[][]::new));
    }

    // ---- OD pair generation ----

    /**
     * Generates OD pairs according to the selected mode.
     *
     * @param nodeList  all network nodes
     * @param count     number of pairs to generate
     * @param rng       random number generator (seeded for reproducibility)
     * @param mode      "mid" for MiD-inspired distribution, "random" for uniform
     * @return OD pairs with beeline distance statistics
     */
    private static ODPairResult generateODPairs(List<Node> nodeList, int count, Random rng, String mode) {
        return switch (mode) {
            case "mid" -> generateMiDPairs(nodeList, count, rng);
            case "random" -> generateRandomPairs(nodeList, count, rng);
            default -> throw new IllegalArgumentException("Unknown OD mode: " + mode);
        };
    }

    /**
     * Generates uniform random OD pairs (original behavior).
     */
    private static ODPairResult generateRandomPairs(List<Node> nodeList, int count, Random rng) {
        int n = nodeList.size();
        double metersPerUnit = detectMetersPerUnit(nodeList);

        int[][] pairs = new int[count][2];
        double[] distances = new double[count];
        for (int i = 0; i < count; i++) {
            int o = rng.nextInt(n);
            int d = rng.nextInt(n);
            pairs[i][0] = o;
            pairs[i][1] = d;
            distances[i] = beeline(nodeList.get(o), nodeList.get(d));
        }
        return new ODPairResult(pairs, distances, metersPerUnit);
    }

    /**
     * Generates OD pairs with a distance distribution inspired by
     * <b>MiD 2017</b> (Mobilität in Deutschland).
     *
     * <p>Beeline distances are sampled from a <b>log-normal distribution</b>:
     * <ul>
     *   <li>Median ≈ 5 km  (μ = ln(5000) ≈ 8.52 in meters)</li>
     *   <li>σ = 1.1  →  Mean ≈ 9.3 km, P90 ≈ 21 km, P99 ≈ 72 km</li>
     * </ul>
     *
     * <p>This approximates the MiD 2017 findings where:
     * <ul>
     *   <li>~30% of trips are &lt; 3 km (walking, cycling)</li>
     *   <li>~50% are &lt; 6 km</li>
     *   <li>~80% are &lt; 15 km</li>
     *   <li>~95% are &lt; 40 km</li>
     *   <li>~99% are &lt; 100 km</li>
     * </ul>
     *
     * <p>For each pair, a random origin is selected and a destination is found
     * by picking the best match among 50 random candidates whose beeline
     * distance is closest to the sampled target distance.  This is efficient
     * (no spatial index needed) and produces a smooth distribution.
     *
     * @see <a href="https://www.mobilitaet-in-deutschland.de/">MiD 2017</a>
     */
    private static ODPairResult generateMiDPairs(List<Node> nodeList, int count, Random rng) {
        int n = nodeList.size();
        double metersPerUnit = detectMetersPerUnit(nodeList);

        // Log-normal parameters (in coordinate units):
        // median = 5000 m / metersPerUnit
        double medianCU = 5_000.0 / metersPerUnit;
        double muLn = Math.log(medianCU);
        double sigmaLn = 1.1;

        // Network extent for clamping
        double diagonal = networkDiagonal(nodeList);
        double minDist = 200.0 / metersPerUnit;   // minimum 200 m beeline
        double maxDist = diagonal * 0.9;           // cap at 90% of diagonal

        int candidates = 50;  // candidates per OD pair for distance matching

        int[][] pairs = new int[count][2];
        double[] distances = new double[count];

        for (int i = 0; i < count; i++) {
            int origin = rng.nextInt(n);

            // Sample target beeline distance from log-normal
            double targetDist = Math.exp(muLn + sigmaLn * rng.nextGaussian());
            targetDist = Math.max(minDist, Math.min(maxDist, targetDist));

            // Find best matching destination among candidates
            int bestDest = (origin + 1) % n;  // fallback
            double bestDiff = Double.MAX_VALUE;
            for (int c = 0; c < candidates; c++) {
                int dest = rng.nextInt(n);
                double dist = beeline(nodeList.get(origin), nodeList.get(dest));
                double diff = Math.abs(dist - targetDist);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestDest = dest;
                }
            }

            pairs[i][0] = origin;
            pairs[i][1] = bestDest;
            distances[i] = beeline(nodeList.get(origin), nodeList.get(bestDest));
        }

        return new ODPairResult(pairs, distances, metersPerUnit);
    }

    /**
     * Detects whether coordinates are in meters (projected CRS like UTM) or
     * degrees (WGS84) by examining the bounding box diagonal.
     *
     * @return approximate meters per coordinate unit
     */
    private static double detectMetersPerUnit(List<Node> nodeList) {
        double diagonal = networkDiagonal(nodeList);
        // If diagonal > 10 km worth of units, assume meters.
        // Typical German UTM network: diagonal 50k-900k (meters).
        // WGS84 for Germany: diagonal ~5-12 (degrees).
        if (diagonal > 10_000) {
            return 1.0;  // already in meters
        } else if (diagonal > 100) {
            return 100.0; // some intermediate CRS (rare)
        } else {
            return 111_000.0; // degrees → approximate meters at ~50°N
        }
    }

    private static double networkDiagonal(List<Node> nodeList) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node node : nodeList) {
            double x = node.getCoord().getX();
            double y = node.getCoord().getY();
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return Math.sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY));
    }

    private static double beeline(Node a, Node b) {
        double dx = a.getCoord().getX() - b.getCoord().getX();
        double dy = a.getCoord().getY() - b.getCoord().getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ---- Benchmarking ----

    private static long benchmarkRouter(LeastCostPathCalculator router,
                                        List<Node> nodeList, int[][] pairs, String label) {
        int total = pairs.length;
        System.out.printf("  %-20s  ", label);
        System.out.flush();
        long startNs = System.nanoTime();
        for (int i = 0; i < total; i++) {
            Node s = nodeList.get(pairs[i][0]);
            Node d = nodeList.get(pairs[i][1]);
            router.calcLeastCostPath(s, d, 8.0 * 3600, null, null);
            if ((i + 1) % 500 == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        long elapsedNs = System.nanoTime() - startNs;
        double avgUs = elapsedNs / (total * 1000.0);
        System.out.printf("  %,.0f µs/query%n", avgUs);
        return elapsedNs;
    }

    // ---- Network loading ----

    private static Network loadNetwork(String networkPath) {
        if (networkPath == null || "berlin".equalsIgnoreCase(networkPath)) {
            return downloadAndLoadNetwork(BERLIN_NETWORK_URL, "berlin-v7.0-network.xml.gz", "Berlin v7.0");
        }
        if ("duesseldorf".equalsIgnoreCase(networkPath)) {
            return downloadAndLoadNetwork(DUESSELDORF_NETWORK_URL,
                    "duesseldorf-v1.2-network.xml.gz", "Düsseldorf v1.2");
        }
        System.out.println("Loading network from " + networkPath + " ...");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
        return scenario.getNetwork();
    }

    private static List<RouterEntry> buildRouters(
            SpeedyALTData altData,
            CHGraph chGraph,
            FreespeedTravelTimeAndDisutility tc) {
        List<RouterEntry> routers = new ArrayList<>();
        routers.add(new RouterEntry("SpeedyALT",     new SpeedyALT(altData, tc, tc)));
        routers.add(new RouterEntry("CH (time-dep)",  new CHRouterTimeDep(chGraph, tc, tc)));
        return routers;
    }

    private static Network downloadAndLoadNetwork(String url, String filename, String label) {
        java.nio.file.Path localPath = Paths.get(System.getProperty("java.io.tmpdir"), filename);

        if (!Files.exists(localPath) || fileSize(localPath) < 1000) {
            System.out.println("Downloading " + label + " network ...");
            System.out.println("  URL:   " + url);
            System.out.println("  Cache: " + localPath);
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(300))
                        .GET()
                        .build();
                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("HTTP " + response.statusCode());
                }
                try (InputStream in = response.body()) {
                    Files.copy(in, localPath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.printf("  Downloaded %.1f MB%n", fileSize(localPath) / (1024.0 * 1024.0));
            } catch (Exception e) {
                throw new RuntimeException("Cannot download " + label + " network: " + e.getMessage(), e);
            }
        } else {
            System.out.printf("Using cached %s network: %s (%.1f MB)%n",
                    label, localPath, fileSize(localPath) / (1024.0 * 1024.0));
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(localPath.toString());
        return scenario.getNetwork();
    }

    private static long fileSize(java.nio.file.Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void printBox(String title, String[][] rows) {
        int labelW = 0;
        int valueW = 0;
        for (String[] row : rows) {
            if (row == null) continue;
            labelW = Math.max(labelW, row[0].length());
            if (row.length > 1) valueW = Math.max(valueW, row[1].length());
        }
        int innerW = Math.max(Math.max(title.length() + 4, labelW + 3 + valueW + 2), 50);
        String hBar = "═".repeat(innerW + 2);
        System.out.println("╔" + hBar + "╗");
        int pad = (innerW + 2 - title.length()) / 2;
        System.out.println("║" + " ".repeat(pad) + title + " ".repeat(innerW + 2 - pad - title.length()) + "║");
        for (String[] row : rows) {
            if (row == null) {
                System.out.println("╠" + "─".repeat(innerW + 2) + "╣");
            } else if (row.length == 1) {
                String heading = " " + row[0];
                System.out.println("║" + heading + " ".repeat(innerW + 2 - heading.length()) + "║");
            } else {
                String line = String.format(" %-" + labelW + "s : %s", row[0], row[1]);
                if (line.length() < innerW + 2) line = line + " ".repeat(innerW + 2 - line.length());
                System.out.println("║" + line + "║");
            }
        }
        System.out.println("╚" + hBar + "╝");
    }
}
