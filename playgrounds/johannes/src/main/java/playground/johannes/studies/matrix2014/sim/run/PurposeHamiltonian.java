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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.analysis.HistogramComparator;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.AttributeChangeListener;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.LegAttributeHistogramBuilder;
import playground.johannes.synpop.sim.UnivariatFrequency2;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;

import java.util.*;

/**
 * @author johannes
 */
public class PurposeHamiltonian {

    private static final Logger logger = Logger.getLogger(PurposeHamiltonian.class);

    private static final String MODULE_NAME = "purposeHamiltonian";

    private static final String PURPOSE_IDX_KEY = "purpose_idx";

    private static final String GEO_DISTANCE_IDX_KEY = "geo_distance_idx";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Setup discretizer.
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
        Make indices.
         */
        logger.info("Indexing leg purposes...");
        Map<String, Integer> purpose2Index = makePurposeIndex(engine.getRefPersons());
        applyPurposeIndex(purpose2Index, engine.getRefPersons());
        applyPurposeIndex(purpose2Index, engine.getSimPersons());
        logger.info("Indexing distance categories....");
//        int[] indices = makeDistanceIndex(engine.getRefPersons(), discretizer);

        Object key = Converters.register(PURPOSE_IDX_KEY, new DoubleConverter());
        GeoDistanceMediator mediator = new GeoDistanceMediator(discretizer, key, borders.size()-1);
        engine.getAttributeListeners().get(CommonKeys.LEG_GEO_DISTANCE).addComponent(mediator);

        logger.info("Initializing purpose hamiltonians...");
        for(int dIdx = 1; dIdx < borders.size() - 1; dIdx++) {
            Predicate<Segment> distIdxPredicate = new DistancePredicate(discretizer, dIdx);

            LegAttributeHistogramBuilder builder = new LegAttributeHistogramBuilder(PURPOSE_IDX_KEY, new LinearDiscretizer(1));
            builder.setPredicate(PredicateAndComposite.create(engine.getLegPredicate(), distIdxPredicate));
            TDoubleDoubleMap refHist = builder.build(engine.getRefPersons());

            UnivariatFrequency2 h = new UnivariatFrequency2(
                    refHist,
                    builder,
                    PURPOSE_IDX_KEY,
                    new LinearDiscretizer(1),
                    true,
                    false);
//            h.setPredicate(distIdxPredicate);
            /*
            Add the hamiltonian to the attribute change listener.
            */
            mediator.addListener(h, dIdx);
            /*
            Create annealing hamiltonian wrapper.
             */
            AnnealingHamiltonian ah = AnnealingHamiltonianConfigurator.configure(h, configGroup);
            engine.getHamiltonian().addComponent(ah);
            engine.getEngineListeners().addComponent(ah);
            /*
            Add histogram comparator.
             */
            String predicateName = String.format("%s.%s", CommonKeys.LEG_PURPOSE, borders.get(dIdx));
            HistogramComparator comparator = new HistogramComparator(
                    refHist,
                    builder,
                    predicateName);
            comparator.setFileIoContext(engine.getIOContext());
            engine.getHamiltonianAnalyzers().addComponent(comparator);
            /*
            Add a hamiltonian logger.
             */
            engine.getEngineListeners().addComponent(new HamiltonianLogger(h,
                    engine.getLoggingInterval(),
                    predicateName,
                    engine.getIOContext().getRoot(),
                    ah.getStartIteration()));

            logger.info(String.format("Initialized hamiltonian %s.", borders.get(dIdx)));
        }
    }

    private static class GeoDistanceMediator implements AttributeChangeListener {

        private Discretizer discretizer;

        private AttributeChangeListener[] listeners;

        private Object purposeIdxObjectKey;

        public GeoDistanceMediator(Discretizer discretizer, Object purposeIdxObjectKey, int maxIdx) {
            this.discretizer = discretizer;
            this.purposeIdxObjectKey = purposeIdxObjectKey;
//            listeners = new ArrayList<>();
            listeners = new AttributeChangeListener[maxIdx+1];
        }

        public void addListener(AttributeChangeListener listener, int idx) {
            listeners[idx] = listener;
        }

        @Override
        public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
            int oldIdx = discretizer.index((Double)oldValue);
            int newIdx = discretizer.index((Double)newValue);

//            AttributeChangeListener old = listeners.get(oldIdx);
//            AttributeChangeListener newListener = listeners.get(newIdx);
            AttributeChangeListener old = listeners[oldIdx];
            AttributeChangeListener newListener = listeners[newIdx];

            Double purpose = (Double) element.getData(purposeIdxObjectKey);

            old.onChange(purposeIdxObjectKey, purpose, null, element);
            newListener.onChange(purposeIdxObjectKey, null, purpose, element);
        }
    }

    private static Map<String, Integer> makePurposeIndex(Collection<? extends Person> persons) {
        Collector<String> collector = new LegCollector<>(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));
        Set<String> purposes = new HashSet<>(collector.collect(persons));
        purposes.remove(null);

        final Map<String, Integer> purpose2Idx = new HashMap<>();
        int idx = 0;
        for(String purpose : purposes) {
            purpose2Idx.put(purpose, idx);
            idx++;
            logger.info(String.format("Purpose %s -> index %s.", purpose, idx));
        }

        return purpose2Idx;
    }

    private static void applyPurposeIndex(Map<String, Integer> purpose2Index, Collection<? extends Person> persons) {
        TaskRunner.run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment leg : episode.getLegs()) {
                    String purpose = leg.getAttribute(CommonKeys.LEG_PURPOSE);
                    Integer idx = purpose2Index.get(purpose);
                    if(idx != null) leg.setAttribute(PURPOSE_IDX_KEY, idx.toString());
                }
            }
        }, persons);
    }

    private static int[] makeDistanceIndex(Collection<? extends Person> persons, final Discretizer discretizer) {
        final Set<Integer> indices = new HashSet<>();

        TaskRunner.run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment leg : episode.getLegs()) {
                    String val = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                    if(val != null) {
                        double dist = Double.parseDouble(val);
                        int idx = discretizer.index(dist);
                        leg.setAttribute(GEO_DISTANCE_IDX_KEY, String.valueOf(idx));
                        indices.add(idx);
                    }
                }
            }
        }, persons);

        int[] nativeIndices = new int[indices.size()];
        int i = 0;
        for(Integer idx : indices) {
            nativeIndices[i] = idx;
            i++;
        }
        Arrays.sort(nativeIndices);
        return nativeIndices;
    }

    private static class DistancePredicate implements Predicate<Segment> {

        private final int index;

        private final Discretizer discretizer;

        public DistancePredicate(Discretizer discretizer, int index) {
            this.discretizer = discretizer;
            this.index = index;
        }

        @Override
        public boolean test(Segment segment) {
            String val = segment.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
            if(val != null) {
                double d = Double.parseDouble(val);
                int idx = discretizer.index(d);
                return idx == index;
            }

            return false;
        }
    }
}
