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

package org.matsim.contrib.taxi.util.stats;

import java.io.PrintWriter;
import java.util.*;

import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.utils.io.IOUtils;


public class StatsCollector<T>
    implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener
{
    public interface StatsCalculator<S>
    {
        S calculateStat();
    }


    private final StatsCalculator<T> calculator;
    private final List<T> stats = new ArrayList<>();
    private final int step;
    private final String name;
    private final String file;


    public StatsCollector(StatsCalculator<T> calculator, int step, String name)
    {
        this(calculator, step, name, null);
    }


    public StatsCollector(StatsCalculator<T> calculator, int step, String name, String file)
    {
        this.calculator = calculator;
        this.step = step;
        this.name = name;
        this.file = file;
    }


    public List<T> getStats()
    {
        return stats;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (e.getSimulationTime() % step == 0) {
            stats.add(calculator.calculateStat());
        }
    }


    @Override
    public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e)
    {
        PrintWriter pw = file != null ? new PrintWriter(IOUtils.getBufferedWriter(file))
                : new PrintWriter(System.out);

        pw.println("time\t" + name);

        for (int i = 0; i < stats.size(); i++) {
            pw.println(i * step + "\t" + stats.get(i));
        }
        
        pw.close();
    }
}
