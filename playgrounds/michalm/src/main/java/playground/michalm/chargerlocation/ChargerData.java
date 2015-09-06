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
    public final List<ChargerLocation> locations;
    public final double totalPower;
    public final double powerToEnergy;


    public ChargerData(List<ChargerLocation> locations, double powerToEnergy)
    {
        this.locations = locations;
        this.powerToEnergy = powerToEnergy;

        double powerSum = 0;
        for (ChargerLocation l : locations) {
            powerSum += l.getPower();
        }

        totalPower = powerSum;

        Collections.sort(locations, new Comparator<ChargerLocation>() {
            @Override
            public int compare(ChargerLocation l1, ChargerLocation l2)
            {
                return l1.getId().compareTo(l2.getId());
            }
        });
    }
}
