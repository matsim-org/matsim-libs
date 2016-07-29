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
import gnu.trove.map.TDoubleDoubleMap;
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
import playground.johannes.studies.matrix2014.analysis.HistogramComparator;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.*;
import playground.johannes.synpop.util.Executor;

import java.util.*;

/**
 * @author jillenberger
 */
public class GeoDistanceZoneHamiltonian2 {

    private static final Logger logger = Logger.getLogger(GeoDistanceZoneHamiltonian2.class);

    public static final String MODULE_NAME = "geoDistanceHamiltonian";

    public static final String PERSON_ZONE_IDX = "zoneIndex";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
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

        LegAttributeHistogramBuilder refHistBuilder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);
        refHistBuilder.setPredicate(engine.getLegPredicate());
        TDoubleDoubleMap refHist = refHistBuilder.build(engine.getRefPersons());
        /*
        Index zones
         */
        ActivityFacilities facilities = ((FacilityData) engine.getDataPool().get(FacilityDataLoader.KEY)).getAll();
        ZoneCollection zones = ((ZoneData) engine.getDataPool().get(ZoneDataLoader.KEY)).getLayer("nuts3");
        TObjectIntMap<Zone> indices = indexZones(engine.getSimPersons(), facilities, zones);

        Map<String, Set<Person>> simPersonsMap = getSimPersons(engine.getSimPersons());

        int emptyZones = 0;

        logger.debug("Creating zone hamiltonians...");
        List<UnivariatFrequency2> hamiltonians = new ArrayList<>(indices.size());
        ProgressLogger.init(indices.size(), 2, 10);

        int[] indexArray = indices.values();
        Arrays.sort(indexArray);
        int maxKey = indexArray[indexArray.length - 1];

        for (int i = 0; i <= maxKey; i++) {

            Set<Person> simPersons = simPersonsMap.get(String.valueOf(i));

            if(simPersons.size() > 0) {
            /*
            Create and add the hamiltonian.
            */
                LegAttributeHistogramBuilder simHistBuilder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);
                simHistBuilder.setPredicate(PredicateAndComposite.create(
                        engine.getLegPredicate(),
                        new LegPersonAttributePredicate(PERSON_ZONE_IDX, String.valueOf(i))));
                UnivariatFrequency2 hamiltonian = new UnivariatFrequency2(
                        refHist,
                        simHistBuilder,
                        CommonKeys.LEG_GEO_DISTANCE,
                        discretizer,
                        engine.getUseWeights(),
                        false);

                hamiltonian.setErrorExponent(2.0);

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

//        refHistBuilder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);
//        refHistBuilder.setPredicate(engine.getLegPredicate());

        HistogramComparator comparator = new HistogramComparator(
                refHist,
                refHistBuilder,
                String.format("%s.%s", CommonKeys.LEG_GEO_DISTANCE, engine.getLegPredicateName()));
        comparator.setFileIoContext(engine.getIOContext());
        engine.getHamiltonianAnalyzers().addComponent(comparator);
        /*
        Add a hamiltonian logger.
         */
        engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                engine.getLoggingInterval(),
                CommonKeys.LEG_GEO_DISTANCE,
                engine.getIOContext().getRoot(),
                annealingHamiltonian.getStartIteration()));

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

    private static Map<String, Set<Person>> getSimPersons(Set<? extends Person> persons) {
        Map<String, Set<Person>> personsMap = new LinkedHashMap<>();
        for (Person p : persons) {
            String zoneIndex = p.getAttribute(PERSON_ZONE_IDX);

            if (zoneIndex == null) throw new NullPointerException();

            Set<Person> zonePersons = personsMap.get(zoneIndex);
            if (zonePersons == null) {
                zonePersons = new HashSet<>();
                personsMap.put(zoneIndex, zonePersons);
            }
            zonePersons.add(p);
        }

        return personsMap;
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

        private final List<UnivariatFrequency2> hamiltonians;

        private Object dataKey;

        private final Object indexDataKey = new Object();

        private boolean isInitialized = false;

        private double sum;

        private Collection<CachedPerson> simPersons;

        public HamiltonianWrapper(List<UnivariatFrequency2> hamiltonians) {
            this.hamiltonians = hamiltonians;
        }

        @Override
        public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
            if(isInitialized) {
                if (this.dataKey == null) this.dataKey = Converters.register(
                        CommonKeys.LEG_GEO_DISTANCE,
                        new DoubleConverter());

                if (this.dataKey.equals(dataKey)) {
                    UnivariatFrequency2 uf = getHamiltonian(element);
                    double h = uf.evaluate(simPersons);
                    uf.onChange(dataKey, oldValue, newValue, element);
                    double h2 = uf.evaluate(simPersons);

                    sum += (h2 - h);
                }
            }
        }

        @Override
        public double evaluate(Collection<CachedPerson> population) {
            if(!isInitialized) {
                simPersons = population;
                logger.info("Initializing hamiltonians...");
                ProgressLogger.init(hamiltonians.size(), 2, 10);
                sum = 0;

                List<RunThread> threads = new ArrayList<>(hamiltonians.size());
                for (int i = 0; i < hamiltonians.size(); i++) {
                    threads.add(new RunThread(population, hamiltonians.get(i)));
                }

                Executor.submitAndWait(threads);

                for(RunThread thread : threads) sum += thread.getResult();

                ProgressLogger.terminate();
                isInitialized = true;
            }

            return sum / (double) hamiltonians.size();
        }

        private UnivariatFrequency2 getHamiltonian(CachedElement element) {
            CachedSegment leg = (CachedSegment)element;
            CachedPerson person = (CachedPerson) leg.getEpisode().getPerson();

            Integer index = (Integer)person.getData(indexDataKey);
            if(index == null) {
                index = new Integer(person.getAttribute(PERSON_ZONE_IDX));
                person.setData(indexDataKey, index);
            }

            return hamiltonians.get(index);
        }

        private class RunThread implements Runnable {

            private final Collection<CachedPerson> persons;

            private final Hamiltonian h;

            double val;

            public RunThread(Collection<CachedPerson> persons, Hamiltonian h) {
                this.persons = persons;
                this.h = h;
            }

            @Override
            public void run() {
                val = h.evaluate(persons);
                ProgressLogger.step();
            }

            public double getResult() {
                return val;
            }
        }
    }
}
