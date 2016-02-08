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

import gnu.trove.list.array.TDoubleArrayList;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.UnivariatFrequency;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jillenberger
 */
public class GeoDistanceLAU2Hamiltonian {

    public static final String MODULE_NAME = "geoDistanceHamiltonian";

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

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            /*
            Get the elements the hamiltonian runs on.
            */
            Predicate<Segment> predicate = PredicateAndComposite.create(
                    engine.getLegPredicate(),
                    new LegPersonAttributePredicate(
                            MiDKeys.PERSON_LAU2_CLASS,
                            String.valueOf(classIdx)));

            Set<Attributable> refLegs = getCarLegs(engine.getRefPersons(), predicate);
            Set<Attributable> simLegs = getCarLegs(engine.getSimPersons(), predicate);
            /*
            Create and add the hamiltonian.
            */
            UnivariatFrequency hamiltonian = new UnivariatFrequency(
                    refLegs,
                    simLegs,
                    CommonKeys.LEG_GEO_DISTANCE,
                    discretizer,
                    engine.getUseWeights());

            hamiltonian.setPredicate(predicate);

            AnnealingHamiltonian annealingHamiltonian = AnnealingHamiltonianConfigurator.configure(
                    hamiltonian,
                    configGroup);
            engine.getHamiltonian().addComponent(annealingHamiltonian);
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
                    predicate,
                    String.format("%s.lau%s", engine.getLegPredicateName(), classIdx),
                    writer);
            engine.getHamiltonianAnalyzers().addComponent(analyzer);
            /*
            Add a hamiltonian logger.
            */
            engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                    engine.getLoggingInterval(),
                    String.format("%s.lau%s", CommonKeys.LEG_GEO_DISTANCE, classIdx),
                    engine.getIOContext().getRoot()));
        }

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
