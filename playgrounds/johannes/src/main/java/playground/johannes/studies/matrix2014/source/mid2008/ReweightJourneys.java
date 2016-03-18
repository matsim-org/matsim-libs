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

package playground.johannes.studies.matrix2014.source.mid2008;

import gnu.trove.function.TDoubleFunction;
import gnu.trove.iterator.TDoubleDoubleIterator;
import gnu.trove.iterator.TDoubleObjectIterator;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.processing.PersonsTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class ReweightJourneys implements PersonsTask {

    private final static Logger logger = Logger.getLogger(ReweightJourneys.class);

    private Discretizer discretizer;

    private TDoubleDoubleHashMap referenceHist;

    private double threshold;

    public ReweightJourneys() {
        discretizer = new LinearDiscretizer(50000);
        threshold = 300000;

        referenceHist = new TDoubleDoubleHashMap();
        try {
            BufferedReader reader = new BufferedReader(new FileReader
                    ("/Users/johannes/gsv/matrix2014/popgen/mid-fusion/tomtom.dist.txt"));
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                double key = Double.parseDouble(tokens[0]);
                double value = Double.parseDouble(tokens[1]);
                referenceHist.put(key, value);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Histogram.normalize(referenceHist);
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
        TDoubleObjectHashMap<Set<Person>> personsMap = new TDoubleObjectHashMap<>();

        for (Person person : persons) {
            if (person.getEpisodes().size() > 1) {
                logger.warn("Runs only with one leg per person");
            } else {
                double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));

                Episode episode = person.getEpisodes().get(0);
                if (episode.getLegs().size() > 1) {
                    logger.warn("Runs only with one leg per person");
                } else {
                    Segment leg = episode.getLegs().get(0);
                    String value = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                    if (value != null) {
                        if(CommonValues.LEG_MODE_CAR.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
                            double d = Double.parseDouble(value);
                            d = discretizer.discretize(d);

                            hist.adjustOrPutValue(d, w, w);

                            Set<Person> pset = personsMap.get(d);
                            if (pset == null) {
                                pset = new HashSet<>();
                                personsMap.put(d, pset);
                            }
                            pset.add(person);
                        }
                    }
                }
            }
        }

        double sumRef = 0;
        TDoubleDoubleIterator it = referenceHist.iterator();
        for (int i = 0; i < referenceHist.size(); i++) {
            it.advance();
            if (it.key() >= threshold) {
                sumRef += it.value();
            }
        }

        Histogram.normalize(hist);
        double sumMid = 0;
        TDoubleDoubleIterator it2 = hist.iterator();
        for (int i = 0; i < hist.size(); i++) {
            it2.advance();
            if (it2.key() >= threshold) {
                sumMid += it2.value();
            }
        }

        final double factor = sumRef / sumMid;
        hist.transformValues(new TDoubleFunction() {
            @Override
            public double execute(double v) {
                return v * factor;
            }
        });

        TDoubleDoubleHashMap weigths = new TDoubleDoubleHashMap();
//        it2 = hist.iterator();
        double[] keys = hist.keys();
        Arrays.sort(keys);
        for (int i = 0; i < hist.size(); i++) {
//            it2.advance();
//            if (it2.key() >= threshold) {
            double d = keys[i];
            double vol = hist.get(d);
//                double f = referenceHist.get(it2.key()) / it2.value();
            double f = referenceHist.get(d) / vol;
                weigths.put(d, f);
                logger.info(String.format("Weight for distance %s: %s.", d, f));
//            }
        }

        TDoubleObjectIterator<Set<Person>> it3 = personsMap.iterator();
        for (int i = 0; i < personsMap.size(); i++) {
            it3.advance();
            if (it3.key() >= threshold) {
                double f = weigths.get(it3.key());
                for (Person person : it3.value()) {
                    double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
                    w = w * f;
                    person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w));
                }
            } else {
                for (Person person : it3.value()) {
                    double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
                    w = w * 0.5;
                    person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w));
                }
            }
        }
    }
}
