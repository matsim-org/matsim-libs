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
package playground.johannes.synpop.matrix;

import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

import java.util.Set;

/**
 * @author jillenberger
 */
public class ODMatrixOperations {

    public static NumericMatrix aggregate(NumericMatrix m, ZoneCollection zones, String key) {
        NumericMatrix newM = new NumericMatrix();

        Set<String> keys = m.keys();
        for (String i : keys) {
            for (String j : keys) {
                Double val = m.get(i, j);
                if (val != null) {
                    Zone zone_i = zones.get(i);
                    Zone zone_j = zones.get(j);

                    if (zone_i != null && zone_j != null) {
                        String att_i = zone_i.getAttribute(key);
                        String att_j = zone_j.getAttribute(key);

                        newM.add(att_i, att_j, val);
                    }
                }
            }
        }

        return newM;
    }
}
