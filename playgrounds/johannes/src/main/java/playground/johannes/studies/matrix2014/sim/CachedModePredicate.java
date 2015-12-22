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

import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.sim.data.CachedSegment;

/**
 * @author jillenberger
 */
public class CachedModePredicate implements Predicate<CachedSegment> {

    private static Object dataKey = new Object();

    private final String key;

    private final String value;

    public CachedModePredicate(String key, String value) {
        this.key = key;
        this.value = value;
    }
    @Override
    public boolean test(CachedSegment cachedSegment) {
        Boolean isMode = (Boolean) cachedSegment.getData(dataKey);
        if(isMode == null) {
            isMode = value.equals(cachedSegment.getAttribute(key));
            cachedSegment.setData(dataKey, isMode);
        }
        return isMode;
    }
}
