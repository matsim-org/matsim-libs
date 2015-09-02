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

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.michalm.zone.Zone;


public class ChargingStations
{
    public static ChargingStation createStation(long id, double x, double y, double power)
    {
        return new ChargingStation(Id.create(id, ChargingStation.class), new CoordImpl(x, y),
                power);
    }


    public static List<ChargingStation> createStationsInZones(Iterable<Zone> zones, double power)
    {
        List<ChargingStation> stations = new ArrayList<>();
        for (Zone z : zones) {
            stations.add(new ChargingStation(Id.create(z.getId(), ChargingStation.class),
                    z.getCoord(), power));
        }

        return stations;
    }

}
