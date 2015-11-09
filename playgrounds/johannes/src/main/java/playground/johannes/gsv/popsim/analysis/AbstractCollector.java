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
package playground.johannes.gsv.popsim.analysis;

import playground.johannes.synpop.data.Attributable;

/**
 * @author jillenberger
 */
public abstract class AbstractCollector<T, A extends Attributable> implements Collector<T> {

    protected Predicate<A> predicate;

    protected final ValueProvider<T, A> provider;

    public AbstractCollector(ValueProvider<T, A> provider) {
        this.provider = provider;
    }

    public void setPredicate(Predicate<A> predicate) {
        this.predicate = predicate;
    }

    public Predicate<A> getPredicate() {
        return predicate;
    }

}
