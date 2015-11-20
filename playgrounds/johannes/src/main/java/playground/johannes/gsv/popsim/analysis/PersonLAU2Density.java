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

package playground.johannes.gsv.popsim.analysis;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneData;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.generator.PersonMunicipalityClassHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class PersonLAU2Density extends AbstractAnalyzerTask<Collection<? extends Person>> {

    private TDoubleDoubleHashMap popLau2;

    public PersonLAU2Density(ZoneCollection zones) {
        popLau2 = new TDoubleDoubleHashMap();

        for(Zone zone : zones.getZones()) {
            String val = zone.getAttribute(ZoneData.POPULATION_KEY);
            if(val != null) {
                double inhabs = Double.parseDouble(val);
                int lau2class = PersonMunicipalityClassHandler.getCategory((int)inhabs);

                popLau2.adjustOrPutValue(lau2class, inhabs, inhabs);
            }
        }
    }
    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        if(ioContext != null) {
            TDoubleDoubleHashMap trips = new TDoubleDoubleHashMap();
            TDoubleDoubleHashMap personsLau2 = new TDoubleDoubleHashMap();
            TDoubleArrayList lau2Classes = new TDoubleArrayList();
            TDoubleArrayList numTrips = new TDoubleArrayList();

            for(Person person : persons) {
                String lau2Val = person.getAttribute(MiDKeys.PERSON_LAU2_CLASS);
                if(lau2Val != null) {
                    double lau2 = Double.parseDouble(lau2Val);
                    double tripsum = 0;
                    for (Episode episode : person.getEpisodes()) {
                        int personTripSum = 0;
                        for(Segment leg : episode.getLegs()) {
                            if(CommonValues.LEG_MODE_CAR.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
                                tripsum++;
                                personTripSum++;
                            }
                        }
                        if(personTripSum > 0) {
                            lau2Classes.add(lau2);
                            numTrips.add(personTripSum);
                            personsLau2.adjustOrPutValue(lau2, 1, 1);
                        }
//                        tripsum += episode.getLegs().size();
                    }
                    trips.adjustOrPutValue(lau2, tripsum, tripsum);
                }
            }

            TDoubleDoubleHashMap correl = Correlations.mean(lau2Classes.toArray(), numTrips.toArray());
//            List<Double> lau2Vals = new PersonCollector<Double>(new NumericAttributeProvider<Person>(MiDKeys
//                    .PERSON_LAU2_CLASS)).collect(persons);
//            double[] values = CollectionUtils.toNativeArray(lau2Vals);
//            TDoubleDoubleHashMap hist = Histogram.createHistogram(values, DummyDiscretizer.getInstance(), false);
//            Histogram.normalize(hist);
//            Histogram.normalize(trips);
            try {
                StatsWriter.writeHistogram(trips, "lau2Class", "trips", String.format("%s/personLau2Density.txt",
                        ioContext.getPath()));
                StatsWriter.writeHistogram(popLau2, "lau2Class", "inhabs", String.format("%s/lau2inhabs.txt",
                        ioContext.getPath()));
                StatsWriter.writeHistogram(personsLau2, "lau2Class", "mobPersons", String.format("%s/mobPresons.txt",
                 ioContext.getPath()));
                StatsWriter.writeHistogram(correl, "lau2Class", "avrTrips", String.format("%s/lau2Avrtrips.txt",
                        ioContext.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
