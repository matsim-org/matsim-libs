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

package org.matsim.contrib.taxi.util.stats;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;


public class LeastCostPathCalculatorCacheStats
{
    private final SummaryStatistics hitStats = new SummaryStatistics();
    private final SummaryStatistics missStats = new SummaryStatistics();


    public void updateStats(LeastCostPathCalculatorWithCache calculatorWithCache)
    {
        hitStats.addValue(calculatorWithCache.getCacheStats().getHits());
        missStats.addValue(calculatorWithCache.getCacheStats().getMisses());
    }


    public static final String HEADER = "cfg\tHits\tMisses";


    public void printStats(PrintWriter pw, String id)
    {
        pw.printf("%10s\t%f\t%f\n", id, hitStats.getMean(), missStats.getMean());
    }


    public void clearStats()
    {
        hitStats.clear();
        missStats.clear();
    }
}
