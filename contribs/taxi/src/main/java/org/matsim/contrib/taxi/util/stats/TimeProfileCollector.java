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

import java.util.*;

import org.matsim.contrib.util.*;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.utils.io.IOUtils;


public class TimeProfileCollector
    implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener
{
    public interface ProfileCalculator
    {
        String[] getHeader();


        String[] calcValues();
    }


    private final ProfileCalculator calculator;
    private final List<String[]> timeProfile = new ArrayList<>();
    private final int interval;
    private final String outputFile;
    private final MatsimServices matsimServices;


    public TimeProfileCollector(ProfileCalculator calculator, int interval, String outputFile,
            MatsimServices matsimServices)
    {
        this.calculator = calculator;
        this.interval = interval;
        this.outputFile = outputFile;
        this.matsimServices = matsimServices;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        if (e.getSimulationTime() % interval == 0) {
            timeProfile.add(calculator.calcValues());
        }
    }


    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e)
    {
        String file = matsimServices.getControlerIO()
                .getIterationFilename(matsimServices.getIterationNumber(), outputFile);

        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            writer.writeNext(calculator.getHeader());
            for (int i = 0; i < timeProfile.size(); i++) {
                writer.writeNext(new CSVLineBuilder().add( (i * interval) + "")
                        .addAll(timeProfile.get(i)).build());
            }
        }
    }
}
