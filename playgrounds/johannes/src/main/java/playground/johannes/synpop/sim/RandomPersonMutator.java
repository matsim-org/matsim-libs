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

import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class RandomPersonMutator implements Mutator<CachedPerson> {

    private final RandomElementMutator delegate;

    private final Random random;

    private final List<CachedPerson> mutation;

    public RandomPersonMutator(RandomElementMutator delegate, Random random) {
        this.delegate = delegate;
        this.random = random;
        mutation = new ArrayList<>(1);
        mutation.add(null);
    }

    @Override
    public List<CachedPerson> select(List<CachedPerson> population) {
        mutation.set(0, population.get(random.nextInt(population.size())));
        return mutation;
    }

    @Override
    public boolean modify(List<CachedPerson> elements) {
        return delegate.modify(elements.get(0));
    }

    @Override
    public void revert(List<CachedPerson> elements) {
        delegate.revert(elements.get(0));

    }
}
