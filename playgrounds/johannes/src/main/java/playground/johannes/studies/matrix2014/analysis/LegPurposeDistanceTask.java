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
public class LegPurposeDistanceTask implements AnalyzerTask<Collection<? extends Person>> {

    private Predicate<Segment> legPredicate;

    private Discretizer discretizer;

    private FileIOContext ioContext;

    private static final String SEPARATOR = "\t";

    public LegPurposeDistanceTask() {
        discretizer = new LinearDiscretizer(50000);
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

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        Collector<String> collector = new LegCollector<>(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));
        Set<String> types = new HashSet<>(collector.collect(persons));
        types.remove(null);

        LegAttributeHistogramBuilder builder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);

        Map<String, TDoubleDoubleMap> histograms = new HashMap<>();
        for(String type : types) {
            Predicate<Segment> predicate = new LegAttributePredicate(CommonKeys.LEG_PURPOSE, type);
            if(legPredicate != null) predicate = PredicateAndComposite.create(predicate, legPredicate);
            builder.setPredicate(predicate);

            TDoubleDoubleMap hist = builder.build(persons);
            histograms.put(type, hist);
        }

        if(ioContext != null) {
            try {
                writeHistograms(histograms, String.format("%s/purposeGeoDistance.txt", ioContext.getPath()));

                TDoubleDoubleMap hist = histograms.values().iterator().next();
                double keys[] = hist.keys();
                Arrays.sort(keys);

                for(double key : keys) {
                    double sum = 0;
                    for(TDoubleDoubleMap h : histograms.values()) {
                        sum += h.get(key);
                    }

                    for(TDoubleDoubleMap h : histograms.values()) {
                        h.put(key, h.get(key) / sum);
                    }
                }

                writeHistograms(histograms, String.format("%s/purposeGeoDistance.norm.txt", ioContext.getPath()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeHistograms(Map<String, TDoubleDoubleMap> histograms, String file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        boolean headerWritten = false;

        for(Map.Entry<String, TDoubleDoubleMap> entry : histograms.entrySet()) {
            String type = entry.getKey();
            TDoubleDoubleMap hist = entry.getValue();

            if(!headerWritten) {
                writer.write("purpose");

                double keys[] = hist.keys();
                Arrays.sort(keys);

                for(double key : keys) {
                    writer.write(SEPARATOR);
                    writer.write(String.valueOf(key));
                }

                writer.newLine();
                headerWritten = true;
            }

            writer.write(type);

            double keys[] = hist.keys();
            Arrays.sort(keys);

            for(double key : keys) {
                writer.write(SEPARATOR);
                writer.write(String.valueOf(hist.get(key)));
            }

            writer.newLine();
        }

        writer.close();
    }
}
