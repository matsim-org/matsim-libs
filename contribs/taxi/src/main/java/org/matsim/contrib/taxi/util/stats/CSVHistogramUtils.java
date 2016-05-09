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

import java.text.DecimalFormat;


public class CSVHistogramUtils
{
    private static final DecimalFormat df = new DecimalFormat("#.##");


    public static String[] createBinsLine(Histogram histogram, double scaling)
    {
        CSVLineBuilder lineBuilder = new CSVLineBuilder();
        addBinsToBuilder(lineBuilder, histogram, scaling);
        return lineBuilder.build();
    }


    public static void addBinsToBuilder(CSVLineBuilder lineBuilder, Histogram histogram, double scaling)
    {
        for (int i = 0; i < histogram.getBinCount(); i++) {
            lineBuilder.add(df.format(i * histogram.getBinSize() * scaling) + "+");
        }
    }


    public static String[] createValuesLine(Histogram histogram)
    {
        CSVLineBuilder lineBuilder = new CSVLineBuilder();
        addValuesToBuilder(lineBuilder, histogram);
        return lineBuilder.build();
    }


    public static void addValuesToBuilder(CSVLineBuilder lineBuilder, Histogram histogram)
    {
        for (int v : histogram.getValues()) {
            lineBuilder.add(v + "");
        }
    }
}
