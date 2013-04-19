/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.run.online;

import java.io.IOException;


public class KaiSingleIterOnlineDvrpLauncher
{
    private static void processArgs(SingleIterOnlineDvrpLauncher launcher, String dir)
    {
        launcher.dirName = dir + "\\";
        launcher.netFileName = launcher.dirName + "network.xml";
        launcher.plansFileName = launcher.dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

        launcher.reqIdToVehIdFileName = launcher.dirName + "reqIdToVehId";

        launcher.depotsFileName = launcher.dirName + "depots-5_taxis-15.xml";
        launcher.taxiCustomersFileName = launcher.dirName + "taxiCustomers_1_pc.txt";

        launcher.eventsFileName = launcher.dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

        launcher.algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

        launcher.otfVis = true;

        launcher.vrpOutFiles = !true;
        launcher.vrpOutDirName = launcher.dirName + "vrp_output";
    }


    public static void main(String... args)
        throws IOException
    {
        String dir = "D:\\PP-rad\\taxi\\mielec\\";
        SingleIterOnlineDvrpLauncher launcher = new SingleIterOnlineDvrpLauncher();
        processArgs(launcher, dir);

        launcher.prepareMatsimData();
        launcher.go();
        launcher.generateOutput();
    }
}
