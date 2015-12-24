/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.analysis;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class GeoDistNumTripsTask implements AnalyzerTask<Collection<? extends Person>> {

    private final Predicate<Segment> predicate;

    private final FileIOContext ioContext;

    public GeoDistNumTripsTask(FileIOContext ioContext, Predicate<Segment> predicate) {
        this.ioContext = ioContext;
        this.predicate = predicate;
    }
    @Override
    public void analyze(Collection<? extends Person> object, List<StatsContainer> containers) {
        TDoubleArrayList numsTrips = new TDoubleArrayList();
        TDoubleArrayList dists = new TDoubleArrayList();

        for(Person p : object) {
            for(Episode e : p.getEpisodes()) {
                int trips = 0;
                double sum = 0;
                for(Segment leg : e.getLegs()) {
                    if(predicate == null || predicate.test(leg)) {
                        String value = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                        if(value != null) {
                            sum += Double.parseDouble(value);
                            trips++;
                        }
                    }
                }

                numsTrips.add(trips);
                dists.add(sum/(double)trips);
            }
        }

        TDoubleDoubleHashMap correl = Correlations.mean(numsTrips.toArray(), dists.toArray());
        try {
            StatsWriter.writeHistogram(correl, "trips", "distance", String.format("%s/geoDistNumTrips.txt", ioContext.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
