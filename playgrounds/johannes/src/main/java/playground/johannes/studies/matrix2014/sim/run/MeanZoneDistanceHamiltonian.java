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

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.studies.matrix2014.sim.CopyPersonAttToLeg;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.BivariatMean;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class MeanZoneDistanceHamiltonian {

    public static final String MODULE_NAME = "meanDistanceHamiltonian";

    public static final String PERSON_ZONE_IDX = "zoneIndex";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);

        DataPool dataPool = engine.getDataPool();
        ActivityFacilities facilities = ((FacilityData) dataPool.get(FacilityDataLoader.KEY)).getAll();
        ZoneCollection zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        TObjectIntMap<Zone> indices = indexZones(engine.getSimPersons(), facilities, zones);
        TIntDoubleMap meanDistances = lau2MeanDistance(engine.getRefPersons(), engine);
        TIntDoubleMap refValues = generateRefValues(indices, meanDistances, zones);
//        /*
//        Copy the lau2 class attribute from the person element to the corresponding leg elements.
//         */
//        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), engine.getRefPersons());
        TaskRunner.run(new CopyPersonAttToLeg(PERSON_ZONE_IDX), engine.getSimPersons());
        /*
        Get the legs.
         */
//        Set<Attributable> refLegs = getCarLegs(engine.getRefPersons(), engine.getLegPredicate());
        Set<Attributable> simLegs = getCarLegs(engine.getSimPersons(), engine.getLegPredicate());
        /*
        Build and add the hamiltonian.
         */
        Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());
        Converters.register(PERSON_ZONE_IDX, DoubleConverter.getInstance());

        BivariatMean hamiltonian = new BivariatMean(
                refValues,
                simLegs,
                PERSON_ZONE_IDX,
                CommonKeys.LEG_GEO_DISTANCE,
                new LinearDiscretizer(1.0),
                engine.getUseWeights());

        AnnealingHamiltonian annealingHamiltonian = AnnealingHamiltonianConfigurator.configure(
                hamiltonian,
                configGroup);
        engine.getHamiltonian().addComponent(annealingHamiltonian);
        engine.getAttributeListeners().get(CommonKeys.LEG_GEO_DISTANCE).addComponent(hamiltonian);
        /*
        Add a hamiltonian logger.
         */
        engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                engine.getLoggingInterval(),
                "meanZoneDistance",
                engine.getIOContext().getRoot()));

    }

    private static TObjectIntMap indexZones(Set<? extends Person> simPersons, ActivityFacilities facilities,
                                            ZoneCollection zones) {
        TObjectIntMap indices = new TObjectIntHashMap();
        int maxIdx = 0;
        for (Person person : simPersons) {
            Facility f = null;
            for (Episode episode : person.getEpisodes()) {
                for (Segment act : episode.getActivities()) {
                    if (ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                        String facilityId = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                        f = facilities.getFacilities().get(Id.create(facilityId, ActivityFacility.class));
                        break;
                    }
                }
            }

            if(f == null) {
                person.setAttribute(PERSON_ZONE_IDX, String.valueOf(0.0));
            } else {
                Zone z = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
                if (z != null) {
                    int idx = indices.get(z);
                    if (idx == 0) {
                        maxIdx++;
                        indices.put(z, maxIdx);
                        idx = maxIdx;
                    }

                    person.setAttribute(PERSON_ZONE_IDX, String.valueOf((double) idx));
                } else {
                    person.setAttribute(PERSON_ZONE_IDX, String.valueOf(0.0));
                }
            }
        }

        return indices;
    }

    private static TIntDoubleMap lau2MeanDistance(Set<? extends Person> refPersons, Simulator engine) {
        TIntDoubleMap meanDistances = new TIntDoubleHashMap();

        for (int klass = 0; klass < 6; klass++) {
            Predicate<Segment> lauPred = new LegPersonAttributePredicate(MiDKeys.PERSON_LAU2_CLASS, String.valueOf(klass));
            Predicate<Segment> predicate = PredicateAndComposite.create(engine.getLegPredicate(), lauPred);

            NumericAnalyzer analyzer = NumericLegAnalyzer.create(
                    CommonKeys.LEG_GEO_DISTANCE,
                    engine.getUseWeights(),
                    predicate,
                    null,
                    null);

            List<StatsContainer> containers = new ArrayList<>();
            analyzer.analyze(refPersons, containers);
            meanDistances.put(klass, containers.get(0).getMean());
        }

        return meanDistances;
    }

    private static TIntDoubleMap generateRefValues(TObjectIntMap<Zone> indices, TIntDoubleMap meanDistances,
                                                   ZoneCollection zones) {
        double dummyMean = StatUtils.mean(meanDistances.values());
        TIntDoubleMap values = new TIntDoubleHashMap();
        TObjectIntIterator<Zone> it = indices.iterator();
        for (int i = 0; i < indices.size(); i++) {
            it.advance();
            int idx = it.value();
            if(idx == 0) {
                values.put(idx, dummyMean);
            } else {
                Zone zone = it.key();
                int klass = Integer.parseInt(zone.getAttribute(MiDKeys.PERSON_LAU2_CLASS));
                double mean = meanDistances.get(klass);

                values.put(idx, mean);
            }
        }

        return values;
    }

    private static Set<Attributable> getCarLegs(Set<? extends Person> persons, Predicate<Segment> predicate) {
        Set<Attributable> legs = new HashSet<>();
        for (Person p : persons) {
            Episode e = p.getEpisodes().get(0);
            for (Segment leg : e.getLegs()) {
                if (predicate.test(leg)) legs.add(leg);
            }
        }

        return legs;
    }

}
