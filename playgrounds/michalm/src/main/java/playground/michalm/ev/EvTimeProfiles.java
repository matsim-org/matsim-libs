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

import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.taxi.util.stats.TimeProfiles;

import playground.michalm.ev.data.*;


public class EvTimeProfiles
{
    public static ProfileCalculator createUnderchargedVehiclesCounter(final EvData evData,
            final double relativeSoc)
    {
        return new TimeProfiles.SingleValueProfileCalculator("undercharged") {
            @Override
            public String calcValue()
            {
                int count = 0;
                for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
                    if (ev.getBattery().getSoc() < relativeSoc * ev.getBattery().getCapacity()) {
                        count++;
                    }
                }
                return count + "";
            }
        };
    }


    public static ProfileCalculator createMeanSocCalculator(final EvData evData)
    {
        return new TimeProfiles.SingleValueProfileCalculator("meanSOC") {
            @Override
            public String calcValue()
            {
                Mean mean = new Mean();
                for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
                    mean.increment(ev.getBattery().getSoc());
                }
                return (mean.getResult() / UnitConversionRatios.J_PER_kWh) + "";//print out in [kWh]
            }
        };
    }


    public static ProfileCalculator createIndividualSocCalculator(final EvData evData)
    {
        List<String> ids = new ArrayList<>(evData.getElectricVehicles().size());
        for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
            ids.add(ev.getId() + "");
        }
        String[] header = ids.toArray(new String[ids.size()]);

        return new TimeProfiles.MultiValueProfileCalculator(header) {
            @Override
            public String[] calcValues()
            {
                List<String> vals = new ArrayList<>(evData.getElectricVehicles().size());
                for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
                    vals.add( (ev.getBattery().getSoc() / UnitConversionRatios.J_PER_kWh) + "");
                }
                return vals.toArray(new String[vals.size()]);
            }
        };
    }
}
