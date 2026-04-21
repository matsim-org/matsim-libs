package org.matsim.core.router.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Analyses a {@link SpeedyGraph} and produces a {@link NetworkProfile}
 * capturing structural metrics relevant for routing parameter tuning.
 *
 * <p>The analysis is O(n + m) and takes negligible time compared to
 * CH preprocessing.  Results should be cached alongside the graph.
 *
 * @author Steffen Axer
 */
public class NetworkAnalyzer {

    private static final Logger LOG = LogManager.getLogger(NetworkAnalyzer.class);

    private NetworkAnalyzer() {}

    /**
     * Analyses the given graph and returns a {@link NetworkProfile}.
     */
    public static NetworkProfile analyze(SpeedyGraph graph) {
        long t0 = System.nanoTime();
        int nodeCount = graph.nodeCount;
        int linkCount = graph.linkCount;

        int[] outDegree = new int[nodeCount];
        SpeedyGraph.LinkIterator outLI = graph.getOutLinkIterator();
        for (int node = 0; node < nodeCount; node++) {
            if (graph.getNode(node) == null) continue;
            outLI.reset(node);
            while (outLI.next()) {
                outDegree[node]++;
            }
        }

        int validNodes = 0;
        long sumDegree = 0;
        int maxOutDeg = 0;
        int highDegCount = 0;
        int[] degreesForSort = new int[nodeCount];
        int degIdx = 0;

        for (int i = 0; i < nodeCount; i++) {
            if (graph.getNode(i) == null) continue;
            validNodes++;
            int deg = outDegree[i];
            sumDegree += deg;
            degreesForSort[degIdx++] = deg;
            if (deg > maxOutDeg) maxOutDeg = deg;
            if (deg >= 6) highDegCount++;
        }

        if (validNodes == 0) {
            return new NetworkProfile(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double avgDeg = (double) sumDegree / validNodes;
        double highDegFraction = (double) highDegCount / validNodes;

        int[] sortedDeg = Arrays.copyOf(degreesForSort, degIdx);
        Arrays.sort(sortedDeg);
        int p95Idx = Math.min((int) (degIdx * 0.95), degIdx - 1);
        int p95OutDegree = sortedDeg[p95Idx];

        double sumSqDev = 0;
        double sumCubDev = 0;
        for (int i = 0; i < degIdx; i++) {
            double dev = degreesForSort[i] - avgDeg;
            sumSqDev += dev * dev;
            sumCubDev += dev * dev * dev;
        }
        double variance = sumSqDev / degIdx;
        double stddev = Math.sqrt(variance);
        double skewness = (stddev > 0) ? (sumCubDev / degIdx) / (stddev * stddev * stddev) : 0;
        double edgeNodeRatio = (double) linkCount / validNodes;

        // Connected components (undirected BFS)
        int[] componentId = new int[nodeCount];
        Arrays.fill(componentId, -1);
        int[] bfsQueue = new int[validNodes];
        int numComponents = 0;
        int largestComponent = 0;

        SpeedyGraph.LinkIterator outLI2 = graph.getOutLinkIterator();
        SpeedyGraph.LinkIterator inLI = graph.getInLinkIterator();

        for (int startNode = 0; startNode < nodeCount; startNode++) {
            if (graph.getNode(startNode) == null) continue;
            if (componentId[startNode] >= 0) continue;

            int compId = numComponents++;
            int qHead = 0, qTail = 0;
            bfsQueue[qTail++] = startNode;
            componentId[startNode] = compId;
            int compSize = 0;

            while (qHead < qTail) {
                int u = bfsQueue[qHead++];
                compSize++;

                outLI2.reset(u);
                while (outLI2.next()) {
                    int w = outLI2.getToNodeIndex();
                    if (componentId[w] < 0 && graph.getNode(w) != null) {
                        componentId[w] = compId;
                        bfsQueue[qTail++] = w;
                    }
                }
                inLI.reset(u);
                while (inLI.next()) {
                    int w = inLI.getFromNodeIndex();
                    if (componentId[w] < 0 && graph.getNode(w) != null) {
                        componentId[w] = compId;
                        bfsQueue[qTail++] = w;
                    }
                }
            }
            if (compSize > largestComponent) largestComponent = compSize;
        }

        // Estimated diameter (multi-source BFS)
        int estimatedDiameter = estimateDiameter(graph, validNodes, bfsQueue);

        long elapsedUs = (System.nanoTime() - t0) / 1_000;
        LOG.info("Network analysis complete in {} us: {} nodes, {} links, avgDeg={}, " +
                "maxDeg={}, p95Deg={}, components={}, diameter~{}",
                elapsedUs, validNodes, linkCount,
                String.format("%.2f", avgDeg), maxOutDeg, p95OutDegree,
                numComponents, estimatedDiameter);

        return new NetworkProfile(
                validNodes, linkCount, avgDeg, maxOutDeg, p95OutDegree,
                edgeNodeRatio, highDegFraction,
                numComponents, largestComponent,
                estimatedDiameter, variance, skewness);
    }

    private static int estimateDiameter(SpeedyGraph graph, int validNodes, int[] bfsQueue) {
        if (validNodes < 2) return 0;

        int[] sampleNodes = new int[3];
        int step = Math.max(1, graph.nodeCount / 4);
        int found = 0;
        for (int i = 0; i < graph.nodeCount && found < 3; i += step) {
            if (graph.getNode(i) != null) sampleNodes[found++] = i;
        }
        for (int i = 0; found < 3 && i < graph.nodeCount; i++) {
            if (graph.getNode(i) != null) {
                boolean dup = false;
                for (int j = 0; j < found; j++) {
                    if (sampleNodes[j] == i) { dup = true; break; }
                }
                if (!dup) sampleNodes[found++] = i;
            }
        }

        int maxDist = 0;
        int[] dist = new int[graph.nodeCount];
        SpeedyGraph.LinkIterator li = graph.getOutLinkIterator();

        for (int s = 0; s < found; s++) {
            Arrays.fill(dist, -1);
            int qHead = 0, qTail = 0;
            bfsQueue[qTail++] = sampleNodes[s];
            dist[sampleNodes[s]] = 0;

            while (qHead < qTail) {
                int u = bfsQueue[qHead++];
                int d = dist[u];
                if (d > maxDist) maxDist = d;

                li.reset(u);
                while (li.next()) {
                    int w = li.getToNodeIndex();
                    if (dist[w] < 0) {
                        dist[w] = d + 1;
                        bfsQueue[qTail++] = w;
                    }
                }
            }
        }
        return maxDist;
    }
}
