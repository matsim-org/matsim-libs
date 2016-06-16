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

package playground.michalm.ev;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;

import playground.michalm.ev.data.*;


public class EvTimeProfiles
{
    public static ProfileCalculator<Integer> createDischargedVehiclesCounter(final EvData evData)
    {
        return new ProfileCalculator<Integer>() {
            @Override
            public Integer calcCurrentPoint()
            {
                int count = 0;
                for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
                    if (ev.getBattery().getSoc() < 0) {
                        count++;
                    }
                }
                return count;
            }
        };
    }


    public static ProfileCalculator<Double> createMeanSocCalculator(final EvData evData)
    {
        return new ProfileCalculator<Double>() {
            @Override
            public Double calcCurrentPoint()
            {
                Mean mean = new Mean();
                for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
                    mean.increment(ev.getBattery().getSoc());
                }
                return mean.getResult() / UnitConversionRatios.J_PER_kWh;//print out in [kWh]
            }
        };
    }
}
