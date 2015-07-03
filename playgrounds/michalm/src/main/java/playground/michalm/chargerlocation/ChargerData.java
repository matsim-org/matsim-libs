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


public class ChargerData
{
    public final List<ChargingStation> stations;
    public final double totalPower;
    public final double powerToEnergy;


    public ChargerData(List<ChargingStation> stations, double powerToEnergy)
    {
        this.stations = stations;
        this.powerToEnergy = powerToEnergy;

        double powerSum = 0;
        for (ChargingStation s : stations) {
            powerSum += s.getPower();
        }

        totalPower = powerSum;

        Collections.sort(stations, new Comparator<ChargingStation>() {
            @Override
            public int compare(ChargingStation o1, ChargingStation o2)
            {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }
}
