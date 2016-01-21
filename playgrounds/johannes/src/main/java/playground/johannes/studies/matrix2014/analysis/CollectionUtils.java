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
package playground.johannes.studies.matrix2014.analysis;

import javax.management.RuntimeErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jillenberger
 */
public class CollectionUtils {

    public static List<double[]> toNativeArray(List<Double> values1, List<Double> values2) {
        if(values1.size() != values2.size()) {
            throw new RuntimeException("Values and weights have to have equal length.");
        }

        double[] nativeValues1 = new double[values1.size()];
        double[] nativeValues2 = new double[values2.size()];

        int idx = 0;
        for(int i = 0; i < values1.size(); i++) {
            if(values1.get(i) != null && values2.get(i) != null) {
                nativeValues1[idx] = values1.get(i);
                nativeValues2[idx] = values2.get(i);
                idx++;
            }
        }

        if(idx < values1.size()) {
            nativeValues1 = Arrays.copyOf(nativeValues1, idx);
            nativeValues2 = Arrays.copyOf(nativeValues2, idx);
        }

        List<double[]> list = new ArrayList<>(2);
        list.add(nativeValues1);
        list.add(nativeValues2);

        return list;
    }
}
