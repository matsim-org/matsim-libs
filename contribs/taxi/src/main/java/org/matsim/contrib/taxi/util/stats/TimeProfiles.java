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

package org.matsim.contrib.taxi.util.stats;

import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;


public class TimeProfiles
{
    public static ProfileCalculator<String> combineProfileCalculators(
            final ProfileCalculator<?>... calculators)
    {
        return new ProfileCalculator<String>() {
            @Override
            public String calcCurrentPoint()
            {
                String s = "";
                for (ProfileCalculator<?> pc : calculators) {
                    s += pc.calcCurrentPoint() + "\t";
                }
                return s;
            }
        };
    }


    public static String combineValues(Object... values)
    {
        String s = "";
        for (Object v : values) {
            s += v.toString() + "\t";
        }
        return s;
    }
}
