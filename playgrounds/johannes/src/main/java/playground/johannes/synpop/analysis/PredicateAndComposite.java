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

package playground.johannes.synpop.analysis;

import org.matsim.contrib.common.collections.Composite;
import playground.johannes.synpop.data.Attributable;

/**
 * @author johannes
 */
public class PredicateAndComposite<T extends Attributable> extends Composite<Predicate<T>> implements Predicate<T> {

    public static <T extends Attributable> PredicateAndComposite<T> create(Predicate<T>... predicates) {
        PredicateAndComposite<T> composite = new PredicateAndComposite();
        for (Predicate p : predicates) {
            /*
            For convenience, allow null-predicates in constructor.
             */
            if(p != null) composite.addComponent(p);
        }
        return composite;
    }

    @Override
    public boolean test(T attributable) {
        for (Predicate<T> p : components) {
            if (!p.test(attributable)) {
                return false;
            }
        }

        return true;
    }
}
