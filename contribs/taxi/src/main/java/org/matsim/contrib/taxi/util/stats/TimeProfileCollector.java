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

import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.utils.io.IOUtils;


public class TimeProfileCollector<T>
    implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener
{
    public interface ProfileCalculator<S>
    {
        S calcCurrentPoint();
    }


    private final ProfileCalculator<T> calculator;
    private final List<T> timeProfile = new ArrayList<>();
    private final int interval;
    private final String header;
    private final MatsimServices matsimServices;


    public TimeProfileCollector(ProfileCalculator<T> calculator, int interval, String header,
            MatsimServices matsimServices)
    {
        this.calculator = calculator;
        this.interval = interval;
        this.header = header;
        this.matsimServices = matsimServices;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (e.getSimulationTime() % interval == 0) {
            timeProfile.add(calculator.calcCurrentPoint());
        }
    }


    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e)
    {
        PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(matsimServices.getControlerIO()
                .getIterationFilename(matsimServices.getIterationNumber(), "taxi_time_profiles.txt")));

        pw.println("time\t" + header);

        for (int i = 0; i < timeProfile.size(); i++) {
            pw.println(i * interval + "\t" + timeProfile.get(i));
        }

        pw.close();
    }
}
