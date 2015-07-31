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

import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

import java.util.List;

/**
 * @author johannes
 */
public class CachedEpisode extends CachedElement implements Episode {

    public CachedEpisode(Episode delegate) {
        super(delegate);
    }

    @Override
    public List<Element> getActivities() {
        return ((Episode)getDelegate()).getActivities();
    }

    @Override
    public List<Element> getLegs() {
        return ((Episode)getDelegate()).getLegs();
    }

    @Override
    public void addActivity(Element activity) {
        ((Episode)getDelegate()).addActivity(activity);
    }

    @Override
    public void addLeg(Element leg) {
        ((Episode)getDelegate()).addLeg(leg);
    }
}
