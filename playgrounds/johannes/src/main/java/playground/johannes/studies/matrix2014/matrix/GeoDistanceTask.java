/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.matrix;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class GeoDistanceTask implements AnalyzerTask<NumericMatrix> {

    private static final Logger logger = Logger.getLogger(GeoDistanceTask.class);

    private final ZoneCollection zones;

    private final DistanceCalculator distanceCalculator;

    private final DiscretizerBuilder discretizerBuilder;

    private final FileIOContext ioContext;

    public GeoDistanceTask(ZoneCollection zones, FileIOContext ioContext) {
        this(zones, ioContext, new StratifiedDiscretizerBuilder(50, 50), new OrthodromicDistanceCalculator());
    }

    public GeoDistanceTask(ZoneCollection zones, FileIOContext ioContext, DiscretizerBuilder discretizerBuilder, DistanceCalculator calculator) {
        this.zones = zones;
        this.ioContext = ioContext;
        this.discretizerBuilder = discretizerBuilder;
        this.distanceCalculator = calculator;
    }

    @Override
    public void analyze(NumericMatrix m, List<StatsContainer> containers) {
        TDoubleArrayList values = new TDoubleArrayList();
        TDoubleArrayList weights = new TDoubleArrayList();

        NumericMatrix distanceMatrix = new NumericMatrix();

        Set<String> notfound = new HashSet<>();
        Set<String> keys = m.keys();
        for(String i : keys) {
            for(String j : keys) {
                Double vol = m.get(i, j);
                if(vol != null && vol > 0) {
                    double d = getDistance(i, j, distanceMatrix);
                    if(!Double.isInfinite(d)) {
                        values.add(d);
                        weights.add(vol);
                    } else {
                        if(zones.get(i) == null) notfound.add(i);
                        if(zones.get(j) == null) notfound.add(j);
                    }
                }
            }
        }

        if(!notfound.isEmpty()) logger.warn(String.format("Zone %s not found.", notfound.toString()));



        try {
            Discretizer discretizer = discretizerBuilder.build(values.toArray());
            TDoubleDoubleMap hist = Histogram.createHistogram(values.toArray(), weights.toArray(), discretizer, true);
            StatsWriter.writeHistogram((TDoubleDoubleHashMap) hist, "Distance", "Probability", ioContext.getPath() + "/geoDistance.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getDistance(String i, String j, NumericMatrix distanceMatrix) {
        Double d = distanceMatrix.get(i, j);

        if(d == null) {
            Zone z_i = zones.get(i);
            Zone z_j = zones.get(j);

            if(z_i != null && z_j != null) {
                Point p_i = z_i.getGeometry().getCentroid();
                Point p_j = z_j.getGeometry().getCentroid();

                d = distanceCalculator.distance(p_i, p_j);
                distanceMatrix.set(i, j, d);
            } else {
                d = Double.POSITIVE_INFINITY;
            }
        }

        return d;
    }
}
