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

import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.sim.data.CachedElement;

/**
 * @author johannes
 */
public class AttributeChangeListenerComposite extends Composite<AttributeChangeListener> implements AttributeChangeListener {

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        for(int i = 0; i < components.size(); i++) {
            components.get(i).onChange(dataKey, oldValue, newValue, element);
        }
    }
}
