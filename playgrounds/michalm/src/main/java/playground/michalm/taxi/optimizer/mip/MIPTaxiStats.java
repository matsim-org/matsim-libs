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

package playground.michalm.taxi.optimizer.mip;

import java.io.PrintWriter;

import org.matsim.contrib.dvrp.data.VrpData;

import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;


public class MIPTaxiStats
{
    //temporarily... not the cleanest design...
    public static MIPTaxiStats currentStats;

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
        assertNull(initial);
        initial = calcTaxiStats();
    }


    void calcSolved()
    {
        assertNull(solved);
        solved = calcTaxiStats();
    }


    public void calcSimulated()
    {
        assertNull(simulated);
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
        pw.println("initial\t" + initial.toString());
        pw.println("solved\t" + solved.toString());
        pw.println("simulated\t" + simulated.toString());
    }


    private void assertNull(TaxiStats stats)
    {
        if (stats != null) {
            throw new IllegalStateException("Already set..");
        }
    }


    private TaxiStats calcTaxiStats()
    {
        return new TaxiStatsCalculator().calculateStats(data);
    }
}
