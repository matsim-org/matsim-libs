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
package playground.johannes.studies.matrix2014.sim;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.PersonTask;

/**
 * @author jillenberger
 */
public class CopyPersonAttToLeg implements PersonTask {

    private final String key;

    public CopyPersonAttToLeg(String key) {
        this.key = key;
    }

    @Override
    public void apply(Person person) {
        String value = person.getAttribute(key);
        for(Episode e : person.getEpisodes()) {
            for(Segment leg : e.getLegs()) {
                leg.setAttribute(key, value);
            }
        }
    }
}
