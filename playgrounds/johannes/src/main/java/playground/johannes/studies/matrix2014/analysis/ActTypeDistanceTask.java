/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis;

import gnu.trove.map.TDoubleDoubleMap;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.sim.LegAttributeHistogramBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class ActTypeDistanceTask implements AnalyzerTask<Collection<? extends Person>> {

    private Predicate<Segment> legPredicate;

    private Discretizer discretizer;

    private FileIOContext ioContext;

    private static final String SEPARATOR = "\t";

    private boolean prevActMode;

    public ActTypeDistanceTask() {
        discretizer = new LinearDiscretizer(50000);
        prevActMode = false;
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.legPredicate = predicate;
    }

    public void setDiscretizer(Discretizer discretizer) {
        this.discretizer = discretizer;
    }

    public void setIoContext(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    public void setPrevActMode(boolean mode) {
        this.prevActMode = mode;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        Collector<String> collector;
        if(prevActMode) collector = new PrevCollector<>(new AttributeProvider<>(CommonKeys.ACTIVITY_TYPE));
        else collector = new LegNextCollector<>(new AttributeProvider<>(CommonKeys.ACTIVITY_TYPE));

        Set<String> types = new HashSet<>(collector.collect(persons));
        types.remove(null);

        LegAttributeHistogramBuilder builder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);

        Map<String, TDoubleDoubleMap> histograms = new HashMap<>();
        for(String type : types) {
            Predicate<Segment> predicate;
            if(prevActMode) predicate = new PrevAttributePredicate(CommonKeys.ACTIVITY_TYPE, type);
            else predicate = new NextAttributePredicate(CommonKeys.ACTIVITY_TYPE, type);

            if(legPredicate != null) predicate = PredicateAndComposite.create(predicate, legPredicate);
            builder.setPredicate(predicate);

            TDoubleDoubleMap hist = builder.build(persons);
            histograms.put(type, hist);
        }

        if(ioContext != null) {
            try {
                String suffix = "next";
                if(prevActMode) suffix = "prev";

                SortedSet<Double> keySet = new TreeSet<>();
                for(TDoubleDoubleMap hist : histograms.values()) {
                    double keys[] = hist.keys();
                    for(int i = 0; i < keys.length; i++) {
                        keySet.add(keys[i]);
                    }
                }

                writeHistograms(histograms, keySet, String.format("%s/actTypeGeoDistance.%s.txt", ioContext.getPath(), suffix));

                for(double key : keySet) {
                    double sum = 0;
                    for(TDoubleDoubleMap h : histograms.values()) {
                        sum += h.get(key);
                    }

                    for(TDoubleDoubleMap h : histograms.values()) {
                        h.put(key, h.get(key) / sum);
                    }
                }

                writeHistograms(histograms, keySet, String.format("%s/actTypeGeoDistance.%s.norm.txt", ioContext.getPath(), suffix));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: Consolidate with LegPurposeDistanceTask
    public void writeHistograms(Map<String, TDoubleDoubleMap> histograms, Set<Double> keys, String file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write("purpose");

        for(double key : keys) {
            writer.write(SEPARATOR);
            writer.write(String.valueOf(key));
        }

        writer.newLine();

        for(Map.Entry<String, TDoubleDoubleMap> entry : histograms.entrySet()) {
            String type = entry.getKey();
            TDoubleDoubleMap hist = entry.getValue();

            writer.write(type);

            for(double key : keys) {
                writer.write(SEPARATOR);
                writer.write(String.valueOf(hist.get(key)));
            }

            writer.newLine();
        }

        writer.close();
    }
}
