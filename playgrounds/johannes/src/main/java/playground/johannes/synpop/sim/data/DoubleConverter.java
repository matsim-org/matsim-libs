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

package playground.johannes.synpop.sim.data;

/**
 * @author johannes
 */
public class DoubleConverter implements Converter {

    private static DoubleConverter converter;

    public static DoubleConverter getInstance() {
        if(converter == null) converter = new DoubleConverter();
        return converter;
    }

    @Override
    public Object toObject(String value) {
        return new Double(value);
    }

    @Override
    public String toString(Object value) {
        return String.valueOf(value);
    }
}
