/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.utils.objectattributes;/*
 * created by jbischoff, 22.08.2018
 */

import org.apache.log4j.Logger;

public class DoubleArrayConverter implements AttributeConverter<double[]> {

    private static String DELIMITER = ",";


    @Override
    public double[] convert(String value) {
        String[] values = value.split(DELIMITER);
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Double.parseDouble(values[i]);
        }
        return result;
    }

    @Override
    public String convertToString(Object o) {
        if (!(o instanceof double[])) {
            Logger.getLogger(getClass()).error("Object is not of type double[] " + o.getClass().toString());
            return null;
        }
        double[] s = (double[]) o;
        String result = "";
        for (int i = 0; i < s.length; i++) {
            result = result + Double.toString(s[i]);
            if (i < s.length - 1) {
                result = result + DELIMITER;
            }
        }
        return result;
    }
}
