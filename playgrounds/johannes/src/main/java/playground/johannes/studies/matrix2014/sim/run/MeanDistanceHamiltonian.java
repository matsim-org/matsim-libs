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

import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.studies.matrix2014.sim.CopyPersonAttToLeg;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.BivariatMean;
import playground.johannes.synpop.sim.HamiltonianLogger;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.HashSet;
import java.util.Set;

/**
 * @author jillenberger
 */
public class MeanDistanceHamiltonian {

    public static final String MODULE_NAME = "meanDistanceHamiltonian";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Copy the lau2 class attribute from the person element to the corresponding leg elements.
         */
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), engine.getRefPersons());
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), engine.getSimPersons());
        /*
        Get the legs.
         */
        Set<Attributable> refLegs = getCarLegs(engine.getRefPersons(), engine.getLegPredicate());
        Set<Attributable> simLegs = getCarLegs(engine.getSimPersons(), engine.getLegPredicate());
        /*
        Build and add the hamiltonian.
         */
        Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());

        BivariatMean hamiltonian = new BivariatMean(
                refLegs,
                simLegs,
                MiDKeys.PERSON_LAU2_CLASS,
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
                "meanDistanceLAU2",
                engine.getIOContext().getRoot()));
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
