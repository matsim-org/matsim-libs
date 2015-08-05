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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class LegKeyValuePredicate implements LegPredicate {

    private final String key;

    private final String value;

    public LegKeyValuePredicate(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean test(Segment leg) {
        return value.equalsIgnoreCase(leg.getAttribute(key));
    }
}
