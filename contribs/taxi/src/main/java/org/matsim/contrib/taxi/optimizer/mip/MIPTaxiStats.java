/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.mip;

import java.io.PrintWriter;

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.taxi.util.stats.*;


public class MIPTaxiStats
{
    //temporarily... not the cleanest design...
    //    public static MIPTaxiStats currentStats;

    private final VrpData data;

    private TaxiStats initial;
    private TaxiStats solved;
    private TaxiStats simulated;


    MIPTaxiStats(VrpData data)
    {
        this.data = data;
    }


    void calcInitial()
    {
        initial = calcTaxiStats();
    }


    void calcSolved()
    {
        solved = calcTaxiStats();
    }


    public void calcSimulated()
    {
        simulated = calcTaxiStats();
    }


    public TaxiStats getInitial()
    {
        return initial;
    }


    public TaxiStats getSolved()
    {
        return solved;
    }


    public TaxiStats getSimulated()
    {
        return simulated;
    }


    public void print(PrintWriter pw)
    {
        pw.println("state\t" + TaxiStats.HEADER);
        pw.println("initial\t" + statsToString(initial));
        pw.println("solved\t" + statsToString(solved));
        pw.println("simulated\t" + statsToString(simulated));
    }


    private String statsToString(TaxiStats stats)
    {
        return stats == null ? "---" : stats.toString();
    }


    private TaxiStats calcTaxiStats()
    {
        return new TaxiStatsCalculator(data.getVehicles().values()).getStats();
    }
}
