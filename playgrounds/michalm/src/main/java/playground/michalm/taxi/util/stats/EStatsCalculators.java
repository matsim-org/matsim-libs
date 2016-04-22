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

package playground.michalm.taxi.util.stats;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;

import playground.michalm.ev.UnitConversionRatios;
import playground.michalm.taxi.data.*;


public class EStatsCalculators
{
    public static ProfileCalculator<Integer> createDischargedVehiclesCounter(final ETaxiData taxiData)
    {
        return new ProfileCalculator<Integer>() {
            @Override
            public Integer calcCurrentPoint()
            {
                int count = 0;
                for (ETaxi t : taxiData.getETaxis().values()) {
                    if (t.getBattery().getSoc() < 0) {
                        count++;
                    }
                }
                return count;
            }
        };
    }


    public static ProfileCalculator<Double> createMeanSocCalculator(final ETaxiData taxiData)
    {
        return new ProfileCalculator<Double>() {
            @Override
            public Double calcCurrentPoint()
            {
                Mean mean = new Mean();
                for (ETaxi t : taxiData.getETaxis().values()) {
                    mean.increment(t.getBattery().getSoc());
                }
                return mean.getResult() / UnitConversionRatios.J_PER_kWh;//print out in [kWh]
            }
        };
    }
}
