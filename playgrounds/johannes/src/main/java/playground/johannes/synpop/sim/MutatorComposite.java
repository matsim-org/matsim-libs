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

import org.matsim.contrib.common.collections.ChoiceSet;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class MutatorComposite<T extends Attributable> implements Mutator<T> {

    private final ChoiceSet<Mutator> mutators;

    private Mutator active;

    public MutatorComposite(Random random) {
        mutators = new ChoiceSet<>(random);
    }

    public void addMutator(Mutator mutator) {
        mutators.addOption(mutator);
    }

    @Override
    public List<T> select(List<CachedPerson> persons) {
        active = mutators.randomChoice();
        return active.select(persons);
    }

    @Override
    public boolean modify(List<T> persons) {
        return active.modify(persons);
    }

    @Override
    public void revert(List<T> persons) {
        active.revert(persons);

    }

}
