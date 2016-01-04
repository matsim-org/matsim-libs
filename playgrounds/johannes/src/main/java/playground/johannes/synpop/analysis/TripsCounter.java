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
package playground.johannes.synpop.analysis;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author jillenberger
 */
public class TripsCounter implements ValueProvider<Double, Episode> {

    private Predicate<Segment> predicate;

    public TripsCounter(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Double get(Episode attributable) {
        int count = 0;
        for (Segment leg : attributable.getLegs()) {
            if (predicate == null || predicate.test(leg)) {
                count++;
            }
        }
        return new Double(count);
    }
}
