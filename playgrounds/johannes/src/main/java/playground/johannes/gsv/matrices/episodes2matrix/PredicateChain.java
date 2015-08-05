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

package playground.johannes.gsv.matrices.episodes2matrix;

import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class PredicateChain implements LegPredicate {

    private PredicateChain next;

    private LegPredicate current;

    public PredicateChain(PredicateChain next) {
        this.next = next;
    }

    private void setCurrent(LegPredicate current) {
        this.current = current;
    }

    @Override
    public boolean test(Segment leg) {
        if(current.test(leg)) {
            if(next == null) return true;
            else return next.test(leg);
        } else {
            return false;
        }
    }
}
