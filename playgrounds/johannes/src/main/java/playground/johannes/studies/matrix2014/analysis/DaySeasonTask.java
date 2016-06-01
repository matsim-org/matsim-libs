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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.analysis.AttributeProvider;
import playground.johannes.synpop.analysis.Collector;
import playground.johannes.synpop.analysis.LegCollector;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class DaySeasonTask implements playground.johannes.synpop.analysis.AnalyzerTask<Collection<? extends Person>> {

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        Collector<String> dayCollector = new LegPersonCollector<>(new AttributeProvider<Person>(CommonKeys.DAY));
        Collector<String> seasonCollector = new LegPersonCollector<>(new AttributeProvider<Person>(MiDKeys
                .PERSON_MONTH));
        Collector<String> purposeCollector = new LegCollector<>(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));

        List<String> days = dayCollector.collect(persons);
        List<String> months = seasonCollector.collect(persons);
        List<String> purposes =purposeCollector.collect(persons);

        Map<String, TObjectDoubleHashMap<String>> map = new HashMap<>();

        for(int i = 0; i < days.size(); i++) {
            String day = days.get(i);
            String month = months.get(i);
            String purpose = purposes.get(i);

            if(day != null && month != null) {
                String season = "S";
                if(month.equalsIgnoreCase(MiDValues.NOVEMBER) ||
                        month.equalsIgnoreCase(MiDValues.DECEMBER) ||
                        month.equalsIgnoreCase(MiDValues.JANUARY) ||
                        month.equalsIgnoreCase(MiDValues.FEBRUARY) ||
                        month.equalsIgnoreCase(MiDValues.MARCH)) {
                    season = "W";
                }

                String key = String.format("%s.%s", purpose, season);
                TObjectDoubleHashMap<String> hist = map.get(key);
                if(hist == null) {
                    hist = new TObjectDoubleHashMap<>();
                    map.put(key, hist);
                }

                hist.adjustOrPutValue(day, 1, 1);
            }
        }

        for(Map.Entry<String, TObjectDoubleHashMap<String>> entry : map.entrySet()) {
            System.out.print(entry.getKey());
            System.out.print(":");

            TObjectDoubleHashMap<String> hist = entry.getValue();
            Histogram.normalize(hist);

            TObjectDoubleIterator<String> it = hist.iterator();
            for(int i = 0; i < hist.size(); i++) {
                it.advance();
                System.out.print(" ");
                System.out.print(it.key());
                System.out.print("=");
                System.out.print(String.format("%.2f", it.value()));
                System.out.print(",");
            }
            System.out.println();
        }

    }
}
