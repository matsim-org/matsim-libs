/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.HistogramWriter;
import playground.johannes.synpop.analysis.PassThroughDiscretizerBuilder;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.sim.AttributeChangeListener;
import playground.johannes.synpop.sim.Hamiltonian;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.UnivariatFrequency;
import playground.johannes.synpop.sim.data.*;

import java.util.*;

/**
 * @author jillenberger
 */
public class GeoDistanceZoneHamiltonian {

    private static final Logger logger = Logger.getLogger(GeoDistanceZoneHamiltonian.class);


    public static final String MODULE_NAME = "geoDistanceHamiltonian";

    public static final String PERSON_ZONE_IDX = "zoneIndex";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Get the elements the hamiltonian runs on.
         */
        Set<Attributable> refLegs = getCarLegs(engine.getRefPersons(), engine.getLegPredicate());
        //Set<Attributable> simLegs = getCarLegs(engine.getSimPersons(), engine.getLegPredicate());
        /*
        Create the geo distance discretizer.
         */
        TDoubleArrayList borders = new TDoubleArrayList();
        borders.add(-1);
        for (int d = 2000; d < 10000; d += 2000) borders.add(d);
        for (int d = 10000; d < 50000; d += 10000) borders.add(d);
        for (int d = 50000; d < 500000; d += 50000) borders.add(d);
        for (int d = 500000; d < 1000000; d += 100000) borders.add(d);
        borders.add(Double.MAX_VALUE);
        Discretizer discretizer = new FixedBordersDiscretizer(borders.toArray());
        /*
        Index zones
         */
        ActivityFacilities facilities = ((FacilityData) engine.getDataPool().get(FacilityDataLoader.KEY)).getAll();
        ZoneCollection zones = ((ZoneData) engine.getDataPool().get(ZoneDataLoader.KEY)).getLayer("nuts3");
        TObjectIntMap<Zone> indices = indexZones(engine.getSimPersons(), facilities, zones);

        Map<String, Set<Attributable>> simLegsMap = getSimLegs(engine.getSimPersons(), engine.getLegPredicate());

        int emptyZones = 0;

        logger.debug("Creating zone hamiltonians...");
        List<UnivariatFrequency> hamiltonians = new ArrayList<>(indices.size());
        ProgressLogger.init(indices.size(), 2, 10);

        int[] indexArray = indices.values();
        Arrays.sort(indexArray);
        int maxKey = indexArray[indexArray.length - 1];

        for (int i = 0; i <= maxKey; i++) {

            Set<Attributable> simLegs = simLegsMap.get(String.valueOf(i));

            if(simLegs.size() > 0) {
            /*
            Create and add the hamiltonian.
            */
                UnivariatFrequency hamiltonian = new UnivariatFrequency(
                        refLegs,
                        simLegs,
                        CommonKeys.LEG_GEO_DISTANCE,
                        discretizer,
                        engine.getUseWeights());

                hamiltonian.setErrorExponent(1.0);

                hamiltonians.add(hamiltonian);
            } else {
                hamiltonians.add(null);
                emptyZones++;
            }
            ProgressLogger.step();
        }
        ProgressLogger.terminate();
        if(emptyZones > 0) logger.warn(String.format("%s empty zones.", emptyZones));

        HamiltonianWrapper hamiltonian = new HamiltonianWrapper(hamiltonians);

        AnnealingHamiltonian annealingHamiltonian = AnnealingHamiltonianConfigurator.configure(
                hamiltonian,
                configGroup);
        engine.getHamiltonian().addComponent(annealingHamiltonian);
        engine.getEngineListeners().addComponent(annealingHamiltonian);
        /*
        Add the hamiltonian to the geo distance attribute change listener.
         */
        engine.getAttributeListeners().get(CommonKeys.LEG_GEO_DISTANCE).addComponent(hamiltonian);
        /*
        Add a geo distance analyzer.
         */
        HistogramWriter writer = new HistogramWriter(
                engine.getIOContext(),
                new PassThroughDiscretizerBuilder(discretizer, "default"));
        AnalyzerTask<Collection<? extends Person>> analyzer = NumericLegAnalyzer.create(
                CommonKeys.LEG_GEO_DISTANCE,
                engine.getUseWeights(),
                engine.getLegPredicate(),
                engine.getLegPredicateName(),
                writer);
        engine.getHamiltonianAnalyzers().addComponent(analyzer);
        /*
        Add a hamiltonian logger.
         */
        engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                engine.getLoggingInterval(),
                CommonKeys.LEG_GEO_DISTANCE,
                engine.getIOContext().getRoot()));

        logger.debug("Done setting up hamiltonian.");
    }

    private static Set<Attributable> getCarLegs(Set<? extends Person> persons, Predicate<Segment> predicate) {
        Set<Attributable> legs = new LinkedHashSet<>();
        for (Person p : persons) {
            Episode e = p.getEpisodes().get(0);
            for (Segment leg : e.getLegs()) {
                if (predicate.test(leg)) legs.add(leg);
            }
        }

        return legs;
    }

    private static Map<String, Set<Attributable>> getSimLegs(Set<? extends Person> persons, Predicate<Segment> predicate) {
        Map<String, Set<Attributable>> legsMap = new LinkedHashMap<>();
        for (Person p : persons) {
            String zoneIndex = p.getAttribute(PERSON_ZONE_IDX);

            if(zoneIndex == null) throw new NullPointerException();

            Episode e = p.getEpisodes().get(0);
            for (Segment leg : e.getLegs()) {
                if (predicate.test(leg)) {
                    Set<Attributable> legs = legsMap.get(zoneIndex);
                    if(legs == null) {
                        legs = new HashSet<>();
                        legsMap.put(zoneIndex, legs);
                    }
                    legs.add(leg);
                }
            }
        }

        return legsMap;
    }

    private static TObjectIntMap indexZones(Set<? extends Person> simPersons, ActivityFacilities facilities,
                                            ZoneCollection zones) {
        logger.info("Indexing zones...");
        ProgressLogger.init(simPersons.size(), 2, 10);

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

            if (f == null) {
                person.setAttribute(PERSON_ZONE_IDX, String.valueOf(0));
            } else {
                Zone z = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
                if (z != null) {
                    int idx = indices.get(z);
                    if (idx == 0) {
                        maxIdx++;
                        indices.put(z, maxIdx);
                        idx = maxIdx;
                    }

                    person.setAttribute(PERSON_ZONE_IDX, String.valueOf(idx));
                } else {
                    person.setAttribute(PERSON_ZONE_IDX, String.valueOf(0));
                }
            }
            ProgressLogger.step();
        }
        ProgressLogger.terminate();

        return indices;
    }

    private static class HamiltonianWrapper implements Hamiltonian, AttributeChangeListener {

        private final List<UnivariatFrequency> hamiltonians;

        private Object dataKey;

        private final Object indexDataKey = new Object();

        public HamiltonianWrapper(List<UnivariatFrequency> hamiltonians) {
            this.hamiltonians = hamiltonians;
        }

        @Override
        public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
            if(this.dataKey == null) this.dataKey = Converters.register(
                    CommonKeys.LEG_GEO_DISTANCE,
                    new DoubleConverter());

            if(this.dataKey.equals(dataKey)) {
                getHamiltonian(element).onChange(dataKey, oldValue, newValue, element);
            }

        }

        @Override
        public double evaluate(Collection<CachedPerson> population) {
            double sum = 0;
            for(int i = 0; i < hamiltonians.size(); i++) {
                sum += hamiltonians.get(i).evaluate(population);
            }

            return sum/(double)hamiltonians.size();
        }

        private UnivariatFrequency getHamiltonian(CachedElement element) {
            CachedSegment leg = (CachedSegment)element;
            CachedPerson person = (CachedPerson) leg.getEpisode().getPerson();

            Integer index = (Integer)person.getData(indexDataKey);
            if(index == null) {
                index = new Integer(person.getAttribute(PERSON_ZONE_IDX));
                person.setData(indexDataKey, index);
            }

            return hamiltonians.get(index);
        }
    }
}
