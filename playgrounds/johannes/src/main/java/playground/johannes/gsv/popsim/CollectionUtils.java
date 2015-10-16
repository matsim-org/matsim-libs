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

package playground.johannes.gsv.popsim;

import java.util.Arrays;
import java.util.List;

/**
 * @author johannes
 */
public class CollectionUtils {

    public static double[] toNativeArray(List<Double> values) {
        return toNativeArray(values, true, true, true);
    }

    public static double[] toNativeArray(List<Double> values, boolean ignoreNull, boolean ignoreNAN, boolean
            ignoreInf) {
        double[] nativeVals = new double[values.size()];
        int cnt = 0;

        for(Double val : values) {
            if(!ignoreNull || val != null) {

                if(val == null) val = 0.0;

                if(!ignoreNAN || !val.isNaN()) {

                    if(!ignoreInf || !val.isInfinite()) {
                        nativeVals[cnt] = val;
                        cnt++;
                    }
                }
            }
        }

        if(cnt < values.size()) {
            nativeVals = Arrays.copyOf(nativeVals, cnt);
        }

        return nativeVals;
    }
}
