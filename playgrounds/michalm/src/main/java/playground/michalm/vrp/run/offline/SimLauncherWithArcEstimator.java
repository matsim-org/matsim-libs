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

package playground.michalm.vrp.run.offline;

import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.*;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.file.LacknerReader;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.TimeAsTravelDisutility;
import playground.michalm.vrp.data.network.shortestpath.*;
import playground.michalm.vrp.data.network.shortestpath.full.*;


public class SimLauncherWithArcEstimator
{
    public static void main(String... args)
        throws IOException
    {
        String dirName;
        String cfgFileName;
        String vrpDirName;
        String vrpStaticFileName;
        String vrpArcTimesFileName;
        String vrpArcCostsFileName;
        String vrpArcPathsFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            cfgFileName = dirName + "config-verB.xml";
            vrpDirName = dirName + "dvrp\\";
            vrpStaticFileName = "A101.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "A102.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // cfgFileName = dirName + "config-verBB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C101.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C102.txt";

            vrpArcTimesFileName = vrpDirName + "arc_times.txt";
            vrpArcCostsFileName = vrpDirName + "arc_costs.txt";
            vrpArcPathsFileName = vrpDirName + "arc_paths.txt";
        }
        else if (args.length == 7) {
            dirName = args[0];
            cfgFileName = dirName + args[1];
            vrpDirName = dirName + args[2];
            vrpStaticFileName = args[3];
            vrpArcTimesFileName = vrpDirName + args[4];
            vrpArcCostsFileName = vrpDirName + args[5];
            vrpArcPathsFileName = vrpDirName + args[6];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Controler controler = new Controler(new String[] { cfgFileName });
        controler.setOverwriteFiles(true);
        controler.run();

        Scenario scenario = controler.getScenario();
        VrpData vrpData = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                MatsimVertexImpl.createFromXYBuilder(scenario));
        MatsimVrpData data = new MatsimVrpData(vrpData, scenario);

        TravelTime travelTime = controler.getTravelTimeCalculator();
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

        LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory()
                .createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
        ShortestPathCalculator shortestPathCalculator = new ShortestPathCalculator(router,
                travelTime, travelDisutility);

        FixedSizeMatsimVrpGraph graph = (FixedSizeMatsimVrpGraph)data.getMatsimVrpGraph();
        ArcFactory arcFactory = new FullMatsimArc.FullMatsimArcFactory(shortestPathCalculator,
                TimeDiscretizer.TD_24H_BY_15MIN);
        graph.initArcs(arcFactory);

        FullMatsimArcIO.writeShortestPaths(graph, vrpArcTimesFileName, vrpArcCostsFileName,
                vrpArcPathsFileName);
    }
}
