/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptancePropaCategoryTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import gnu.trove.iterator.TDoubleObjectIterator;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinLogDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AttributePartition;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 */
public class EdgeLengthCategoryTask extends ModuleAnalyzerTask<Accessibility> {

    private Geometry boundary;

    public EdgeLengthCategoryTask(Accessibility module) {
        this.setModule(module);
    }

    public void setBoundary(Geometry boundary) {
        this.boundary = boundary;
    }

    @Override
    public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
        Accessibility access = module;

        Set<Vertex> vertices = new HashSet<Vertex>();
        for (Vertex v : graph.getVertices()) {
            Point p = ((SpatialVertex) v).getPoint();
            if (p != null) {
                vertices.add(v);
            }
        }

        TObjectDoubleHashMap<Vertex> normValues = access.values(vertices);

        AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(normValues.values(), 1, 2));
        TDoubleObjectHashMap<?> partitions = partitioner.partition(normValues);
        TDoubleObjectIterator<?> it = partitions.iterator();

        EdgeLength propa = new EdgeLength();

        Map<String, TDoubleDoubleHashMap> histograms = new HashMap<String, TDoubleDoubleHashMap>();
        Map<String, DescriptiveStatistics> distributions = new HashMap<String, DescriptiveStatistics>();
        double sum = 0;

        for (int i = 0; i < partitions.size(); i++) {
            it.advance();
            double key = it.key();
            Set<SpatialVertex> partition = (Set<SpatialVertex>) it.value();
            System.out.println("Partition size = " + partition.size() + "; key = " + key);
            Set<SpatialEdge> edges = new HashSet<SpatialEdge>();
            for (SpatialVertex v : partition) {
                edges.addAll(v.getEdges());
            }
            DescriptiveStatistics distr = propa.statistics(edges);
            try {
                double[] values = distr.getValues();
                System.out.println("Num samples = " + values.length);
                if (values.length > 0) {
                    TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, FixedSampleSizeDiscretizer.create(values, 1, 50), true);
                    sum += Histogram.sum(hist);
                    histograms.put(String.format("d-cat%1$.4f", key), hist);
                    distributions.put(String.format("d-cat%1$.4f", key), distr);
                }
                writeHistograms(distr, new LinLogDiscretizer(1000.0, 2), String.format("d-cat%1$.4f.log", key), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Entry<String, TDoubleDoubleHashMap> entry : histograms.entrySet()) {
            String key = entry.getKey();
            TDoubleDoubleHashMap histogram = entry.getValue();
            Histogram.normalize(histogram, sum);
            try {
                StatsWriter.writeHistogram(histogram, "d", "p", String.format("%1$s/%2$s.txt", getOutputDirectory(), key));
            } catch (IOException e) {
                e.printStackTrace();
            }


            histogram = Histogram.createCumulativeHistogram(histogram);
            Histogram.complementary(histogram);
            try {
                StatsWriter.writeHistogram(histogram, "d", "p", String.format("%1$s/%2$s.cum.txt", getOutputDirectory(), key));
            } catch (IOException e) {
                e.printStackTrace();
            }

            DescriptiveStatistics stats = distributions.get(key);
            writeRawData(stats, key);
        }
    }

}
