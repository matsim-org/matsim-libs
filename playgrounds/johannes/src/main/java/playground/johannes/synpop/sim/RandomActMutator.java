/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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

import playground.johannes.synpop.sim.data.CachedEpisode;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.CachedSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author jillenberger
 */
public class RandomActMutator implements Mutator<CachedSegment> {

    private final RandomElementMutator delegate;

    private final Random random;

    private final List<CachedSegment> mutation;

    public RandomActMutator(RandomElementMutator delegate, Random random) {
        this.delegate = delegate;
        this.random = random;

        mutation = new ArrayList<>(1);
        mutation.add(null);
    }

    @Override
    public List<CachedSegment> select(List<CachedPerson> population) {
        CachedPerson p = population.get(random.nextInt(population.size()));
        CachedEpisode e = (CachedEpisode) p.getEpisodes().get(0); //TODO: Or better random episode?
        CachedSegment s = (CachedSegment) e.getActivities().get(random.nextInt(e.getActivities().size()));

        mutation.set(0, s);

        return mutation;
    }

    @Override
    public boolean modify(List<CachedSegment> elements) {
        return delegate.modify(mutation.get(0));
    }

    @Override
    public void revert(List<CachedSegment> elements) {
        delegate.revert(mutation.get(0));
    }
}
