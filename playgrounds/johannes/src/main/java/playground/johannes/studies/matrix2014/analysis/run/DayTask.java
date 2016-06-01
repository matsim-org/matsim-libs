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

package playground.johannes.studies.matrix2014.analysis.run;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import playground.johannes.studies.matrix2014.analysis.SetSeason;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class DayTask implements AnalyzerTask<Collection<? extends Person>> {

    private final FileIOContext ioContext;

    public DayTask(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        /*
        Get all purposes.
         */
        LegCollector<String> purposeCollector = new LegCollector<>(new AttributeProvider<>(CommonKeys.LEG_PURPOSE));
        Set<String> purposes = new HashSet<>(purposeCollector.collect(persons));
        purposes.remove(null);
        /*
        Get all seasons
         */
        PersonCollector<String> seasonCollector = new PersonCollector<>(new AttributeProvider<>(SetSeason.SEASON_KEY));
        Set<String> seasons = new HashSet<>(seasonCollector.collect(persons));
        seasons.remove(null);
        /*
        Get all days
         */
        PersonCollector<String> dayCollector = new PersonCollector<>(new AttributeProvider<>(CommonKeys.DAY));
        Set<String> days = new HashSet<>(dayCollector.collect(persons));
        days.remove(null);

        List<Pair<Map<String, String>, TObjectDoubleMap<String>>> values = new ArrayList<>();

        Predicate<Segment> modePredicate = new LegAttributePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);

        TDoubleArrayList borders = new TDoubleArrayList();
        borders.add(-1);
        borders.add(50000);
        borders.add(100000);
        borders.add(1000000);
        borders.add(Double.MAX_VALUE);
        Discretizer discretizer = new FixedBordersDiscretizer(borders.toArray());

        for (String purpose : purposes) {
            Predicate<Segment> purposePredicate = new LegAttributePredicate(CommonKeys.LEG_PURPOSE, purpose);

            for (String season : seasons) {
                Predicate<Segment> seasonPredicate = new LegPersonAttributePredicate(SetSeason.SEASON_KEY, season);

                for (int idx = 1; idx < borders.size() - 1; idx++) {
                    Predicate<Segment> distPrediate = new DistancePredicate(idx, discretizer);

                    TObjectDoubleMap<String> hist = new TObjectDoubleHashMap<>();
                    for (String day : days) {
                        Predicate<Segment> dayPredicate = new LegPersonAttributePredicate(CommonKeys.DAY, day);

                        Predicate<Segment> predicate = PredicateAndComposite.create(modePredicate,
                                purposePredicate,
                                seasonPredicate,
                                distPrediate,
                                dayPredicate);

                        LegCollector<Segment> counter = new LegCollector(new EntityProvider());
                        counter.setPredicate(predicate);

                        hist.put(day, counter.collect(persons).size());
                    }
                    Histogram.normalize(hist);

                    Map<String, String> dimensions = new HashMap<>();
                    dimensions.put(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);
                    dimensions.put(CommonKeys.LEG_PURPOSE, purpose);
                    dimensions.put(SetSeason.SEASON_KEY, season);
                    dimensions.put(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(borders.get(idx)));

                    values.add(new ImmutablePair<>(dimensions, hist));
                }
            }
        }

        if(ioContext != null) {
            write(values);
        }
    }

    private void write(List<Pair<Map<String, String>, TObjectDoubleMap<String>>> values) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/day-share.txt", ioContext.getPath())));

            if (!values.isEmpty()) {

                Set<String> predKeys = values.get(0).getLeft().keySet();
                for (String key : predKeys) {
                    writer.write(key);
                    writer.write("\t");
                }

                Set<String> dayKeys = values.get(0).getRight().keySet();
                for(String key : dayKeys) {
                    writer.write(key);
                    writer.write("\t");
                }

                writer.newLine();

                for (Pair<Map<String, String>, TObjectDoubleMap<String>> pair : values) {
                    Map<String, String> dimensions = pair.getLeft();
                    TObjectDoubleMap<String> hist = pair.getRight();
                    for (String key : predKeys) {
                        writer.write(dimensions.get(key));
                        writer.write("\t");
                    }

                    for(String key : dayKeys) {
                        writer.write(String.format(Locale.US, "%.4f", hist.get(key)));
                        writer.write("\t");
                    }

                    writer.newLine();
                }

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class EntityProvider implements ValueProvider<Integer, Attributable> {

        @Override
        public Integer get(Attributable attributable) {
            return 1;
        }
    }

    private static class DistancePredicate implements Predicate<Segment> {

        private final int index;

        private final Discretizer discretizer;

        public DistancePredicate(int index, Discretizer discretizer) {
            this.index = index;
            this.discretizer = discretizer;
        }

        @Override
        public boolean test(Segment segment) {
            String val = segment.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
            if(val != null) {
                double d = Double.parseDouble(val);
                int idx = discretizer.index(d);
                return idx == index;
            }

            return false;
        }
    }
}
