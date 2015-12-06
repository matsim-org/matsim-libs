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

package playground.johannes.gsv.popsim;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.gsv.popsim.analysis.LegNextCollector;
import playground.johannes.gsv.popsim.analysis.LegPersonCollector;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.AttributeProvider;
import playground.johannes.synpop.analysis.LegCollector;
import playground.johannes.synpop.analysis.NumericAttributeProvider;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class DistDayTypeTask extends AnalyzerTask {

    @Override
    public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
        LegCollector<Double> distCollector = new LegCollector<>(new NumericAttributeProvider<Segment>(CommonKeys
                .LEG_GEO_DISTANCE));
        LegPersonCollector<String> dayCollector = new LegPersonCollector<>(new AttributeProvider<Person>(CommonKeys
                .DAY));
        LegNextCollector<String> typeCollector = new LegNextCollector<>(new AttributeProvider<Segment>(CommonKeys
                .ACTIVITY_TYPE));

        List<Double> distances = distCollector.collect(persons);
        List<String> days = dayCollector.collect(persons);
        List<String> types = typeCollector.collect(persons);

        Discretizer discr = new LinearDiscretizer(100000);
        Set<Double> distCats = new TreeSet<>();
        for(Double d : distances) {
            if(d != null)
            distCats.add(discr.discretize(d));
        }

        Set<String> dayCats = new TreeSet<>();
        for(String s : days)
            if(s != null) dayCats.add(s);

        Set<String> typeCats = new TreeSet<>();
        for(String s : types)
            if(s != null) typeCats.add(s);



        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/purposes.txt"));


        writer.write("distance\tday\tpurpose\tfraction");
        writer.newLine();

        for(Double dCat : distCats) {
            for(String dayCat : dayCats) {
                TObjectDoubleHashMap<String> typeHist = new TObjectDoubleHashMap<>();

                for(int i = 0; i < distances.size(); i++) {
                    Double d = distances.get(i);
                    if(d != null) {
                        d = discr.discretize(d);
                        String day = days.get(i);
                        String type = types.get(i);

                        if (dCat.equals(d) && dayCat.equals(day) && type != null && !ActivityTypes.HOME.equalsIgnoreCase
                                (type)) {
                            typeHist.adjustOrPutValue(type, 1.0, 1.0);
                        }
                    }
                }

                Histogram.normalize(typeHist);

                TObjectDoubleIterator<String> it = typeHist.iterator();
                for(int i = 0; i < typeHist.size(); i++) {
                    it.advance();

                    writer.write(String.valueOf(dCat));
                    writer.write("\t");
                    writer.write(String.valueOf(dayCat));
                    writer.write("\t");
                    writer.write(it.key());
                    writer.write("\t");
                    writer.write(String.valueOf(it.value()));
                    writer.newLine();
                }

            }
        }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
