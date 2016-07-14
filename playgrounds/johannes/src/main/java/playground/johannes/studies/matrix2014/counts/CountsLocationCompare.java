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

package playground.johannes.studies.matrix2014.counts;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class CountsLocationCompare {

    public static void main(String args[]) throws IOException {
        String refFile = "/home/johannes/gsv/germany-scenario/counts/counts.2009.osm.xml";
        String targetFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.osm20140909.xml";

        Counts<Link> refCounts = new Counts<>();
        MatsimCountsReader countsReader = new MatsimCountsReader(refCounts);
        countsReader.parse(refFile);

        Map<String, Coord> refCoords = new HashMap<>();
        for(Count<Link> count : refCounts.getCounts().values()) {
            refCoords.put(count.getCsId(), count.getCoord());
        }

        Counts<Link> targetCounts = new Counts<>();
        countsReader = new MatsimCountsReader(targetCounts);
//        countsReader.setValidating(false);
        countsReader.parse(targetFile);

        Map<String, Coord> targetCoords = new HashMap<>();
        for(Count<Link> count : targetCounts.getCounts().values()) {
            targetCoords.put(count.getCsId(), count.getCoord());
        }
        DistanceCalculator distanceCalculator = CartesianDistanceCalculator.getInstance();

        TDoubleArrayList distances = new TDoubleArrayList();

        for(Map.Entry<String, Coord> refEntry : refCoords.entrySet()) {
            Coord targetCoord = targetCoords.get(refEntry.getKey());

            if(targetCoord != null) {
                Point p1 = MatsimCoordUtils.coordToPoint(refEntry.getValue());
                Point p2 = MatsimCoordUtils.coordToPoint(targetCoord);

                distances.add(distanceCalculator.distance(p1, p2));
            }
        }

        double[] values = distances.toArray();
        System.out.println(String.format("Average distance: %s", StatUtils.mean(values)));
        System.out.println(String.format("Min distance: %s", StatUtils.min(values)));
        System.out.println(String.format("Max distance: %s", StatUtils.max(values)));

        TDoubleDoubleHashMap hist = Histogram.createHistogram(values, new LinearDiscretizer(50), false);
        StatsWriter.writeHistogram(hist, "distance", "frequency", "/home/johannes/gsv/matrix2014/counts/counts" +
                ".distances.txt");
    }
}
