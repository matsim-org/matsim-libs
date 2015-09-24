/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.sim;

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class MarkovEngine {

    private final List<CachedPerson> simPopulation;

    private final Hamiltonian hamiltonian;

    private final Mutator mutator;

    private final Random random;

    private MarkovEngineListener listener;

    public MarkovEngine(Collection<? extends Person> population, Hamiltonian hamiltonian, Mutator mutator, Random random) {
        this.hamiltonian = hamiltonian;
        this.mutator = mutator;
        this.random = random;

        this.simPopulation = new ArrayList<>(population.size());
        for (Person p : population) {
            CachedPerson cp = new CachedPerson(p);
            this.simPopulation.add(cp);
        }

        listener = new DefaultListener();
    }


    public void setListener(MarkovEngineListener listener) {
        this.listener = listener;
    }

    public void run(long iterations) {
        for (long i = 0; i < iterations; i++) {
            /*
			 * select person
			 */
            List<? extends Attributable> mutations = mutator.select(simPopulation);
			/*
			 * evaluate
			 */
            double H_before = hamiltonian.evaluate(simPopulation);

            boolean accepted = false;
            if (mutator.modify(mutations)) {
				/*
				 * evaluate
				 */
                double H_after = hamiltonian.evaluate(simPopulation);

                double p = 1 / (1 + Math.exp(H_after - H_before));

                if (p >= random.nextDouble()) {
                    accepted = true;
                } else {
                    mutator.revert(mutations);

                }
            }

            listener.afterStep(MarkovEngine.this.simPopulation, mutations, accepted);
        }
    }

    private static class DefaultListener implements MarkovEngineListener {

        @Override
        public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {

        }
    }
}
