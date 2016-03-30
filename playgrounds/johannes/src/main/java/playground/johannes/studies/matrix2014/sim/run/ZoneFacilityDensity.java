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

package playground.johannes.studies.matrix2014.sim.run;

import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

/**
 * @author johannes
 */
public class ZoneFacilityDensity {

    public static final String FACILITY_DENSITY_KEY = "facility_density";

    public void apply(ZoneCollection zones) {
        for(Zone zone : zones.getZones()) {
            String val = zone.getAttribute(ZoneFacilityCount.FACILITY_COUNT_KEY);
            if(val != null) {
                int count = Integer.parseInt(val);
                double rho = count / zone.getGeometry().getArea();
                zone.setAttribute(FACILITY_DENSITY_KEY, String.valueOf(rho));
            }
        }
    }
}
