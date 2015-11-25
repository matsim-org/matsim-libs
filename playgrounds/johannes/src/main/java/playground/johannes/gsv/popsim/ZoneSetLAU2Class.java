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

import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneData;
import playground.johannes.synpop.source.mid2008.MiDKeys;

/**
 * @author johannes
 */
public class ZoneSetLAU2Class {

    private static final Discretizer categories;

    static {
        double[] borders = new double[5];
        borders[0] = 5000;
        borders[1] = 20000;
        borders[2] = 50000;
        borders[3] = 100000;
        borders[4] = 500000;

        categories = new FixedBordersDiscretizer(borders);
    }

    public void apply(ZoneCollection zones) {
        for(Zone zone : zones.getZones()) {
            String inhabitantsVal = zone.getAttribute(ZoneData.POPULATION_KEY);
            if(inhabitantsVal != null) {
                double inhabitants = Double.parseDouble(inhabitantsVal);
                String category = inhabitants2Class(inhabitants);
                zone.setAttribute(MiDKeys.PERSON_LAU2_CLASS, category);
            }
        }
    }

    public static String inhabitants2Class(double inhabitants) {
        int idx = categories.index(inhabitants); //TODO: synchronize with PersonMunicipalityClassHandler
        return String.valueOf(idx);
    }
}
