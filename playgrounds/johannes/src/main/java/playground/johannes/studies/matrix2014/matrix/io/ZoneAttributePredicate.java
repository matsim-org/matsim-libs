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

package playground.johannes.studies.matrix2014.matrix.io;

import org.apache.log4j.Logger;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.Matrix;

/**
 * @author johannes
 */
public class ZoneAttributePredicate implements ODPredicate<String, Double> {

    private static final Logger logger = Logger.getLogger(ZoneAttributePredicate.class);

    private final ZoneCollection zones;

    private final String key;

    private final String value;

    public ZoneAttributePredicate(String key, String value, ZoneCollection zones) {
        this.key = key;
        this.value = value;
        this.zones = zones;
    }

    @Override
    public boolean test(String row, String col, Matrix<String, Double> matrix) {
        Zone zone_i = zones.get(row);
        Zone zone_j = zones.get(col);

        if (zone_i != null && zone_j != null) {
            return (value.equals(zone_i.getAttribute(key)) && value.equals(zone_j.getAttribute(key)));
        } else {
            if (zone_i == null)
                logger.warn(String.format("Zone not found: %s", row));

            if (zone_j == null)
                logger.warn(String.format("Zone not found: %s", col));

            return false;
        }
    }
}
