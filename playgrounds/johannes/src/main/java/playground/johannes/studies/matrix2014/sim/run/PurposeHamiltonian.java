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

package playground.johannes.studies.matrix2014.sim.run;

import gnu.trove.list.array.TDoubleArrayList;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.core.config.Config;
import playground.johannes.synpop.analysis.AttributeProvider;
import playground.johannes.synpop.analysis.Collector;
import playground.johannes.synpop.analysis.LegCollector;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;

import java.util.*;

/**
 * @author johannes
 */
public class PurposeHamiltonian {

    private static final String PURPOSE_IDX_KEY = "purpose_idx";

    private static final String GEO_DISTANCE_IDX_KEY = "geo_distance_idx";

    public static void build(Simulator engine, Config config) {
        makePurposeIndex(engine.getSimPersons());
        makeDistanceIndex(engine.getSimPersons());
    }

    private static Map<String, Integer> makePurposeIndex(Collection<? extends Person> persons) {
        Collector<String> collector = new LegCollector<>(new AttributeProvider<>(CommonKeys.LEG_PURPOSE));
        Set<String> purposes = new HashSet<>(collector.collect(persons));
        purposes.remove(null);

        final Map<String, Integer> purpose2Idx = new HashMap<>();
        int idx = 0;
        for(String purpose : purposes) {
            purpose2Idx.put(purpose, idx);
            idx++;
        }

        TaskRunner.run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment leg : episode.getLegs()) {
                    String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);
                    Integer idx = purpose2Idx.get(purpose);
                    if(idx != null) leg.setAttribute(PURPOSE_IDX_KEY, idx.toString());
                }
            }
        }, persons);

        return purpose2Idx;
    }

    private static void makeDistanceIndex(Collection<? extends Person> persons) {
        TDoubleArrayList borders = new TDoubleArrayList();
        borders.add(-1);
        for (int d = 2000; d < 10000; d += 2000) borders.add(d);
        for (int d = 10000; d < 50000; d += 10000) borders.add(d);
        for (int d = 50000; d < 500000; d += 50000) borders.add(d);
        for (int d = 500000; d < 1000000; d += 100000) borders.add(d);
        borders.add(Double.MAX_VALUE);
        final Discretizer discretizer = new FixedBordersDiscretizer(borders.toArray());

        TaskRunner.run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment leg : episode.getLegs()) {
                    double dist = Double.parseDouble(leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE));
                    int idx = discretizer.index(dist);
                    leg.setAttribute(GEO_DISTANCE_IDX_KEY, String.valueOf(idx));
                }
            }
        }, persons);
    }
}
