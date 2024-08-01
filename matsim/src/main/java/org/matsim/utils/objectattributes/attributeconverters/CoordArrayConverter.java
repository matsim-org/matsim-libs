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

package org.matsim.utils.objectattributes.attributeconverters;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Coord;
import org.matsim.utils.objectattributes.AttributeConverter;

public class CoordArrayConverter implements AttributeConverter<Coord[]> {

    // [(X1;Y1),(X2;Y2),...,(Xn;Yn)]

    private static final String DELIMITER_COORD = ";";
    private static final String BRACER_COORD = "()";
    private static final String DELIMITER_ARRAY = ",";
    private static final String BRACER_ARRAY = "[]";

    private static final String BRACER_COORD_BEGIN = BRACER_COORD.substring(0, 1);
    private static final String BRACER_COORD_END = BRACER_COORD.substring(1, 2);
    private static final String BRACER_ARRAY_BEGIN = BRACER_ARRAY.substring(0, 1);
    private static final String BRACER_ARRAY_END = BRACER_ARRAY.substring(1, 2);

    @Override
    public Coord[] convert(String value) {
        value = value.replace(BRACER_ARRAY_BEGIN, "");
        value = value.replace(BRACER_ARRAY_END, "");
        String[] values = value.split(DELIMITER_ARRAY);
        Coord[] result = new Coord[values.length];
        for (int i = 0; i < values.length; i++) {
            String s = values[i].replace(BRACER_COORD_BEGIN, "");
            s = s.replace(BRACER_COORD_END, "");
            String[] sa = s.split(DELIMITER_COORD);
            result[i] = new Coord(Double.parseDouble(sa[0]), Double.parseDouble(sa[1]));
        }
        return result;
    }

    @Override
    public String convertToString(Object o) {
        if (!(o instanceof Coord[])) {
            LogManager.getLogger(getClass()).error("Object is not of type Coord[] " + o.getClass().toString());
            return null;
        }
        Coord[] c = (Coord[]) o;
        StringBuilder result = new StringBuilder();
        result.append(BRACER_ARRAY_BEGIN);
        for (int i = 0; i < c.length; i++) {
            if (i > 0) {
                result.append(DELIMITER_ARRAY);
            }
            result.append(String.format(
                    BRACER_COORD_BEGIN+"%s"+DELIMITER_COORD+"%s"+BRACER_COORD_END,
                    Double.toString(c[i].getX()),
                    Double.toString(c[i].getY())
            ));
        }
        result.append(BRACER_ARRAY_END);
        return result.toString();
    }
}
