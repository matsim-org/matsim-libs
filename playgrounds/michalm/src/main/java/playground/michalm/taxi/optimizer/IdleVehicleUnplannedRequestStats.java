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

package playground.michalm.taxi.optimizer;

import java.io.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;

import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;


public class IdleVehicleUnplannedRequestStats
    implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener
{
    private static final int MAX_TIME = 30 * 60 * 60;
    private static final int STEP = 60;

    private final double[] idleVehs = new double[MAX_TIME];
    private final double[] unplannedReqs = new double[MAX_TIME];

    private final MatsimVrpContext context;
    private final String filename;


    public IdleVehicleUnplannedRequestStats(MatsimVrpContext context, String filename)
    {
        this.context = context;
        this.filename = filename;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        int idx = (int)e.getSimulationTime();
        if (idx < MAX_TIME) {

            int idleVehCount = 0;
            for (Vehicle v : context.getVrpData().getVehicles()) {
                if (TaxiUtils.isIdle(v)) {
                    idleVehCount++;
                }
            }
            idleVehs[idx] = idleVehCount;

            int unplannedReqCount = 0;
            for (Request r : context.getVrpData().getRequests()) {
                if ( ((TaxiRequest)r).getStatus() == TaxiRequestStatus.UNPLANNED) {
                    unplannedReqCount++;
                }
            }
            unplannedReqs[idx] = unplannedReqCount;
        }
    }


    @Override
    public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e)
    {
        try {
            PrintWriter pw = new PrintWriter(filename);

            pw.println("time\tidleVehs\tunplReqs");

            for (int i = STEP; i < idleVehs.length; i += STEP) {
                double avgIV = new Mean().evaluate(idleVehs, i - STEP, STEP);
                double avgUR = new Mean().evaluate(unplannedReqs, i - STEP, STEP);
                pw.println(i + "\t" + avgIV + "\t" + avgUR);
            }

            pw.close();
        }
        catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        }
    }
}
