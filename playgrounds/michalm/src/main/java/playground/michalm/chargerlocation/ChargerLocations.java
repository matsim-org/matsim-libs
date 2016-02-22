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

package playground.michalm.chargerlocation;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.zone.Zone;


public class ChargerLocations
{
    public static ChargerLocation createLocation(long id, double x, double y, double power)
    {
        return new ChargerLocation(Id.create(id, ChargerLocation.class), new Coord(x, y),
                power);
    }


    public static List<ChargerLocation> createLocationsInZones(Iterable<Zone> zones, double power)
    {
        List<ChargerLocation> locations = new ArrayList<>();
        for (Zone z : zones) {
            locations.add(new ChargerLocation(Id.create(z.getId(), ChargerLocation.class),
                    z.getCoord(), power));
        }

        return locations;
    }
}
