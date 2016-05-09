/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.sim.util;

import gnu.trove.map.TDoubleDoubleMap;
import org.matsim.contrib.common.stats.Discretizer;

import java.util.Arrays;

/**
 * @author jillenberger
 */
public class DynamicArrayBuilder {

    public static DynamicDoubleArray build(TDoubleDoubleMap hist, Discretizer discretizer) {
        double[] keys = hist.keys();
        Arrays.sort(keys);

        DynamicDoubleArray arr = new DynamicDoubleArray(keys.length, 0);
        for(double key : keys) {
            int idx = discretizer.index(key);
            double val = hist.get(key);
            arr.set(idx, val);
        }

        return arr;
    }
}
