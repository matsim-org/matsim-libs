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

package playground.michalm.taxi.util.stats;

import java.io.*;

import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;

import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.scheduler.TaxiSchedulerUtils;
import playground.michalm.taxi.util.TaxicabUtils;


public class IdleVehicleUnplannedRequestStats
    implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener
{
    private static final int MAX_TIME = 30 * 60 * 60;
    private static final int STEP = 60;

    private final double[] idleVehs = new double[MAX_TIME / STEP];
    private final double[] unplannedReqs = new double[MAX_TIME / STEP];

    private final TaxiOptimizerConfiguration optimConfig;
    private final String file;


    public IdleVehicleUnplannedRequestStats(TaxiOptimizerConfiguration optimConfig, String file)
    {
        this.optimConfig = optimConfig;
        this.file = file;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        int idx = (int)e.getSimulationTime();
        if (idx < MAX_TIME) {
            TaxiData taxiData = (TaxiData)optimConfig.context.getVrpData();

            idleVehs[idx / STEP] += TaxicabUtils.countVehicles(taxiData.getVehicles(),
                    TaxiSchedulerUtils.createIsIdle(optimConfig.scheduler));

            unplannedReqs[idx / STEP] += TaxiRequests.countRequestsWithStatus(
                    taxiData.getTaxiRequests(), TaxiRequestStatus.UNPLANNED);
        }
    }


    @Override
    public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e)
    {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("time\tidleVehs\tunplReqs");

            for (int i = 0; i < idleVehs.length; i++) {
                pw.println(i + "\t" + idleVehs[i] / STEP + "\t" + unplannedReqs[i] / STEP);
            }
        }
        catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }
    }
}
