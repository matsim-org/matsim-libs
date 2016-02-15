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

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class ActTypeDistanceTask extends AnalyzerTask {

    private Map<Double, TObjectDoubleHashMap<String>> histograms;

    @Override
    public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
        Map<String, Predicate<Segment>> actTypePredicates = Predicates.actTypePredicates(persons, true);
        Predicate<Segment> modePredicate = new LegAttributePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);
        LegCollector distColletor = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE));

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/acttypedist.txt"));

            writer.write("type");
            for (double key = 0; key <= 1000000; key += 100000) {
                writer.write("\t");
                writer.write(String.valueOf(key));
            }
            writer.newLine();

            histograms = new HashMap<>();

            for (Map.Entry<String, Predicate<Segment>> entry : actTypePredicates.entrySet()) {
                LegPurposePredicate purposePredicate = new LegPurposePredicate(entry.getValue());
                PredicateAndComposite<Segment> pred = new PredicateAndComposite<>();
                pred.addComponent(modePredicate);
                pred.addComponent(purposePredicate);

                distColletor.setPredicate(pred);

                List<Double> dists = distColletor.collect(persons);
                double[] distArray = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(dists);
                TDoubleDoubleHashMap hist = org.matsim.contrib.common.stats.Histogram.createHistogram(distArray, new LinearDiscretizer(100000), false);

                String purpose = entry.getKey();
                writer.write(purpose);

                for (double key = 0; key <= 1000000; key += 100000) {
                    double val = hist.get(key);

                    TObjectDoubleHashMap<String> purposeHist = histograms.get(key);
                    if(purposeHist == null) {
                        purposeHist = new TObjectDoubleHashMap<>();
                        histograms.put(key, purposeHist);
                    }
                    purposeHist.put(purpose, val);

                    writer.write("\t");
                    writer.write(String.valueOf(val));
                }
                writer.newLine();
            }
            writer.close();

            for(TObjectDoubleHashMap<String> hist : histograms.values()) {
                Histogram.normalize(hist);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Map<Double, TObjectDoubleHashMap<String>> getHistrograms() {
        return histograms;
    }
}
