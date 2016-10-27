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
import org.matsim.contrib.util.CSVLineBuilder;


public class TimeProfiles
{
    public static ProfileCalculator combineProfileCalculators(
            final ProfileCalculator... calculators)
    {
        CSVLineBuilder builder = new CSVLineBuilder();
        for (ProfileCalculator pc : calculators) {
            builder.addAll(pc.getHeader());
        }
        final String[] header = builder.build();

        return new ProfileCalculator() {
            @Override
            public String[] getHeader()
            {
                return header;
            }


            @Override
            public String[] calcValues()
            {
                CSVLineBuilder builder = new CSVLineBuilder();
                for (ProfileCalculator pc : calculators) {
                    builder.addAll(pc.calcValues());
                }
                return builder.build();
            }
        };
    }


    public static String[] combineValues(Object... values)
    {
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = values[i] + "";
        }
        return strings;
    }


    public static abstract class SingleValueProfileCalculator
        implements ProfileCalculator
    {
        private final String[] headerArray;


        public SingleValueProfileCalculator(String header)
        {
            this.headerArray = new String[] { header };
        }


        @Override
        public String[] getHeader()
        {
            return headerArray;
        }


        @Override
        public String[] calcValues()
        {
            return new String[] { calcValue() };
        }


        public abstract String calcValue();
    }


    public static abstract class MultiValueProfileCalculator
        implements ProfileCalculator
    {
        private final String[] headerArray;


        public MultiValueProfileCalculator(String... header)
        {
            this.headerArray = header;
        }


        @Override
        public String[] getHeader()
        {
            return headerArray;
        }
    }
}
