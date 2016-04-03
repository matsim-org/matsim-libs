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

import gnu.trove.map.TDoubleDoubleMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.analysis.HistogramComparator;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.LegAttributeHistogramBuilder;
import playground.johannes.synpop.sim.UnivariatFrequency2;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class GeoDistanceTypeHamiltonian {

    private static final Logger logger = Logger.getLogger(GeoDistanceTypeHamiltonian.class);

    private static final String MODULE_NAME = "geoDistanceTypeHamiltonian";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Create the geo distance discretizer.
         */
//        TDoubleArrayList borders = new TDoubleArrayList();
//        borders.add(-1);
//        for (int d = 2000; d < 10000; d += 2000) borders.add(d);
//        for (int d = 10000; d < 50000; d += 10000) borders.add(d);
//        for (int d = 50000; d < 500000; d += 50000) borders.add(d);
//        for (int d = 500000; d < 1000000; d += 100000) borders.add(d);
//        borders.add(Double.MAX_VALUE);
//        Discretizer discretizer = new FixedBordersDiscretizer(borders.toArray());
//        Discretizer discretizer = new LinearDiscretizer(50000);

//        Collector<String> collector = new LegCollector<>(new AttributeProvider<>(ReplaceActTypes.ORIGINAL_TYPE));
        Collector<String> collector = new LegCollector<>(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));
        Set<String> types = new HashSet<>(collector.collect(engine.getRefPersons()));
        types.remove(null);

//        Set<String> types = new HashSet<>();
//        types.add("wecommuter");
        for(String type : types) {

//            Predicate<Segment> typePredicate = new LegAttributePredicate(ReplaceActTypes.ORIGINAL_TYPE, type);
            Predicate<Segment> typePredicate = new LegAttributePredicate(CommonKeys.LEG_PURPOSE, type);
            Predicate<Segment> predicate = PredicateAndComposite.create(
                    engine.getLegPredicate(),
                    typePredicate);

            LegCollector<Double> distCollector = new LegCollector<>(new NumericAttributeProvider<Segment>(CommonKeys.LEG_GEO_DISTANCE));
            distCollector.setPredicate(predicate);
            List<Double> dists = distCollector.collect(engine.getRefPersons());
            dists.add(1000000.0);
            double[] values = CollectionUtils.toNativeArray(dists);
            Discretizer discretizer = FixedSampleSizeDiscretizer.create(values, 1, 10);

            LegAttributeHistogramBuilder refHistBuilder = new LegAttributeHistogramBuilder(CommonKeys.LEG_GEO_DISTANCE, discretizer);
            refHistBuilder.setPredicate(predicate);
            TDoubleDoubleMap refHist = refHistBuilder.build(engine.getRefPersons());

            UnivariatFrequency2 hamiltonian = new UnivariatFrequency2(
                    refHist,
                    refHistBuilder,
                    CommonKeys.LEG_GEO_DISTANCE,
                    discretizer,
                    engine.getUseWeights(),
                    false);
            hamiltonian.setPredicate(typePredicate);

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
            String predicateName = String.format("%s.%s.%s", CommonKeys.LEG_GEO_DISTANCE, engine.getLegPredicateName(), type);
            HistogramWriter writer = new HistogramWriter(
                    engine.getIOContext(),
                    new PassThroughDiscretizerBuilder(discretizer, "default"));
            AnalyzerTask<Collection<? extends Person>> analyzer = NumericLegAnalyzer.create(
                    CommonKeys.LEG_GEO_DISTANCE,
                    engine.getUseWeights(),
                    predicate,
                    predicateName,
                    writer);
            engine.getHamiltonianAnalyzers().addComponent(analyzer);

            HistogramComparator comparator = new HistogramComparator(
                    refHist,
                    refHistBuilder,
                    predicateName);
            comparator.setFileIoContext(engine.getIOContext());
            engine.getHamiltonianAnalyzers().addComponent(comparator);
        /*
        Add a hamiltonian logger.
         */
            engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                    engine.getLoggingInterval(),
                    predicateName,
                    engine.getIOContext().getRoot(),
                    annealingHamiltonian.getStartIteration()));

            logger.debug("Done setting up hamiltonian.");
        }
    }
}
