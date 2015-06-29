/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.berlin.converters;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.Id;

import playground.michalm.zone.Zone;


/**
 * @author jbischoff
 */
public class LORUtil
{
    public static boolean isInBerlin(String zone)
    {
        if (zone.length() != 8) {
            throw new IllegalStateException();
        }

        return !zone.startsWith("120");
    }


    public static Id<Zone> createZoneId(String id)
    {
        String _8digitZoneId = StringUtils.leftPad(id, 8, '0');//some ids lack leading 0's
        return Id.create(_8digitZoneId, Zone.class);
    }
}
