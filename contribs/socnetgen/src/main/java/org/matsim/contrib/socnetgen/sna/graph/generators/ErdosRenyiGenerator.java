/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractErdosRenyiGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.socnetgen.sna.graph.generators;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.graph.*;
import org.matsim.contrib.socnetgen.sna.graph.io.GraphMLWriter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Random graph generator that connects vertices with constant probability.
 *
 * @author illenberger
 */
public class ErdosRenyiGenerator<G extends Graph, V extends Vertex, E extends Edge> {

    private static final Logger logger = Logger.getLogger(ErdosRenyiGenerator.class);

    private GraphBuilder<G, V, E> builder;

    private boolean randomDraw = false;

    /**
     * Creates a new random graph generator.
     *
     * @param builder a graph builder to create new graphs.
     */
    public ErdosRenyiGenerator(GraphBuilder<G, V, E> builder) {
        this.builder = builder;
    }

    /**
     * Sets the mode of the generator to "random draw". Per default the generator will iterate over all possible edges
     * an insert edges with the given probability. If <tt>randomDraw</tt> is set to <tt>true</tt> the generator will
     * randomly draw <tt>p * n * (n - 1) / 2</tt> distinct pairs of vertices and connect them with edges.
     *
     * @param randomDraw <tt>true</tt> if the generator should operate in the "random draw" mode, <tt>false</tt>
     *                   otherwise.
     */
    public void setRandomDrawMode(boolean randomDraw) {
        this.randomDraw = randomDraw;
    }

    /**
     * Creates a new graph with the specified number of vertices that are connected with constant probability.
     *
     * @param numVertices the number of vertices.
     * @param p           the edge probability
     * @param randomSeed  a random seed
     * @return a new random graph.
     */
    public G generate(int numVertices, double p, long randomSeed) {
        G g = builder.createGraph();

        for (int i = 0; i < numVertices; i++)
            builder.addVertex(g);

        return generate(g, p, randomSeed);
    }

    /**
     * Connects the vertices of an existing graph with constant probability.
     *
     * @param graph      an existing graph
     * @param p          the edge probability
     * @param randomSeed a random seed
     * @return a graph with randomly inserted edges.
     */
    public G generate(G graph, double p, long randomSeed) {
        if (randomDraw)
            return randomDraw(graph, p, randomSeed);
        else
            return enumerate(graph, p, randomSeed);
    }

    @SuppressWarnings("unchecked")
    private G enumerate(G graph, double p, long randomSeed) {
        LinkedList<V> pending = new LinkedList<V>();
        pending.addAll((Collection<? extends V>) graph.getVertices());

        Random random = new Random(randomSeed);
        V v1;
        while ((v1 = pending.poll()) != null) {
            for (V v2 : pending) {
                if (random.nextDouble() <= p) {
                    builder.addEdge(graph, v1, v2);
                }
            }
        }

        return graph;
    }

    @SuppressWarnings("unchecked")
    private G randomDraw(G graph, double p, long randomSeed) {
        long n = graph.getVertices().size();
        long M = n * (n - 1) / 2;
        int m = (int) (p * M);

        Random random = new Random(randomSeed);
        List<V> vertices = new ArrayList<V>((Collection<? extends V>) graph.getVertices());
        ProgressLogger.init(m, 1, 5);
        for (int i = 0; i < m; i++) {
            E edge = null;
            while (edge == null) {
                V vi = vertices.get(random.nextInt((int) n));
                V vj = vertices.get(random.nextInt((int) n));
                edge = builder.addEdge(graph, vi, vj);
            }
            ProgressLogger.step();
//			if(i % 10000 == 0)
//				logger.info(String.format("Created %1$s of %2$s edges.", i+1, m));
        }
        ProgressLogger.termiante();
        return graph;
    }

    /**
     * Main-method to create random graphs.<br> Usage: ErdosRenyiGenerator graphMLFile numVertices probability
     * [randomSeed]
     *
     * @param args command line arguments.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String args[]) throws FileNotFoundException, IOException {
        int N = Integer.parseInt(args[1]);
        double p = Double.parseDouble(args[2]);
        long seed = (long) (Math.random() * 1000);
        if (args.length > 3)
            seed = Long.parseLong(args[3]);

        ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(
                new SparseGraphBuilder());
        generator.setRandomDrawMode(true);
        Graph g = generator.generate(N, p, seed);

        GraphMLWriter writer = new GraphMLWriter();
        writer.write(g, args[0]);
    }
}
