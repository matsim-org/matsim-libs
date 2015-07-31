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

import java.util.List;

/**
 * @author johannes
 */
public class CachedPerson extends CachedElement implements Person {

    public CachedPerson(Person delegate) {
        super(delegate);
    }

    @Override
    public List<Episode> getPlans() {
        return ((Person)getDelegate()).getPlans();
    }

    @Override
    public void addPlan(Episode episode) {
        ((Person)getDelegate()).addPlan(episode);
    }
}
