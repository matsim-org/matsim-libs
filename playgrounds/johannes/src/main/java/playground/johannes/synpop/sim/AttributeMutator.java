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

package playground.johannes.synpop.sim;

import playground.johannes.synpop.sim.data.CachedElement;

/**
 * @author johannes
 */
public class AttributeMutator implements RandomElementMutator {

    private final Object dataKey;

    private final AttributeChangeListener listener;

    private final ValueGenerator generator;

    private Object oldValue;

    public AttributeMutator(Object dataKey, ValueGenerator generator, AttributeChangeListener listener) {
        this.dataKey = dataKey;
        this.listener = listener;
        this.generator = generator;
    }

    @Override
    public boolean modify(CachedElement element) {
        oldValue = element.getData(dataKey);
        Object newValue = generator.newValue(element);

        if(newValue != null) {
            element.setData(dataKey, newValue);

            if (listener != null) listener.onChange(dataKey, oldValue, newValue, element);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void revert(CachedElement element) {
        Object newValue = element.getData(dataKey);
        element.setData(dataKey, oldValue);

        if (listener != null) listener.onChange(dataKey, newValue, oldValue, element);

    }
}
