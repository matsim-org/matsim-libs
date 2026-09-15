package edu.metis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Structure for simple graphs. Vertices must not be explicitly created, indexes starting at 0 are used.
 */
public class SimpleGraph implements Metis.Graph {

    private int numVertices;
    private int numEdges;

    public Map<Integer, Set<Integer>> edges = new HashMap<>();

    public SimpleGraph() {
    }

    /**
     * Read a graph from a file, in the format as described in the metis samples.
     * @see #readGraph(BufferedReader)
     */
    public static SimpleGraph readGraph(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return SimpleGraph.readGraph(reader);
        }
    }

    /**
     * Read a graph from a file, in the format as described in the metis samples.
     */
    public static SimpleGraph readGraph(BufferedReader reader) throws IOException {

        SimpleGraph g = new SimpleGraph();

        int numVertices = 0;
        int numEdges = 0;

        int i = -1;

        String line = null;
        while ((line = reader.readLine()) != null) {

            line = line.strip();

            if (line.startsWith("%")) {
                continue;
            }

            String[] parts = Pattern.compile("\\s+").split(line);

            if (i == -1) {
                numVertices = Integer.parseInt(parts[0]);
                numEdges = Integer.parseInt(parts[1]);
            } else {
                for (String part : parts) {
                    // File is 1-based, we use 0-based
                    g.addEdge(i, Integer.parseInt(part) - 1);
                }
            }
            i++;
        }

        return g;
    }

    public void addEdge(int from, int to) {
        numVertices = Math.max(numVertices, Math.max(from, to));
        edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        numEdges++;
    }

    @Override
    public int getNumVertices() {
        return numVertices + 1;
    }

    @Override
    public int getNumEdges() {
        return numEdges;
    }

    @Override
    public void getEdges(int vertex, Metis.EdgeBuilder builder) {
        edges.getOrDefault(vertex, Set.of()).forEach(to -> builder.addEdge(to, 1));
    }

}
