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

import java.io.*;
import java.util.List;

import com.opencsv.CSVReader;


public class HourlyTaxiStatsReader
{
    private final List<String[]> content;


    public HourlyTaxiStatsReader(String file)
    {
        try (CSVReader reader = new CSVReader(new FileReader(file), '\t')) {
            content = reader.readAll();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public int getHours()
    {
        return content.size() - 2;
    }


    public double getMeanWaitTime(int hour)
    {
        return getValue(hour, 1);
    }


    public double getP95WaitTime(int hour)
    {
        return getValue(hour, 8);
    }


    public double getMeanEmptyRatio(int hour)
    {
        return getValue(hour, 12);
    }


    private double getValue(int hour, int col)
    {
        return Double.valueOf(content.get(hour + 2)[col]);
    }
}
