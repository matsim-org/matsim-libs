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

package playground.johannes.gsv.popsim.analysis;

import playground.johannes.synpop.data.Attributable;

/**
 * @author johannes
 */
public class AttributeProvider<A extends Attributable> implements ValueProvider<String, A> {

    private final String key;

    public AttributeProvider(String key) {
        this.key = key;
    }

    @Override
    public String get(A attributable) {
        return attributable.getAttribute(key);
    }
}
