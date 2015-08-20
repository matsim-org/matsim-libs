/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.sim.data;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 */
public class CachedPerson extends CachedElement implements Person {

    private final List<Episode> episodes;

    public CachedPerson(Person delegate) {
        super(delegate);
        episodes = new ArrayList<>(delegate.getEpisodes().size());
        for(Episode episode : delegate.getEpisodes()) {
            CachedEpisode cachedEpisode = new CachedEpisode(episode);
            episodes.add(cachedEpisode);
        }
    }

    @Override
    public String getId() {
        return ((Person)getDelegate()).getId();
    }

    @Override
    public List<? extends Episode> getEpisodes() {
        return episodes;
//        return ((Person)getDelegate()).getEpisodes();
    }

    @Override
    public void addEpisode(Episode episode) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
//        ((Person)getDelegate()).addEpisode(episode);
    }
}
