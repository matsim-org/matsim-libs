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

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.file.LacknerReader;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.optimizer.VrpOptimizer;
import pl.poznan.put.vrp.dynamic.optimizer.evolutionary.*;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.simulator.DeterministicSimulator;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.MatsimVertexImpl;
import playground.michalm.vrp.data.network.shortestpath.FullDiscreteMatsimArcIO;
import playground.michalm.vrp.driver.VrpSchedulePlan;
import playground.michalm.vrp.run.VrpConfigUtils;


public class OfflineDvrpLauncher
{
    // means: all requests are known a priori (in advance/static)
    private static boolean STATIC_MODE = false;// default: false

    // schedules/routes PNG files, routes SHP files
    private static boolean VRP_OUT_FILES = true;// default: true


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
        String vrpDynamicFileName;
        String algParamsFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            STATIC_MODE = false;
            VRP_OUT_FILES = true;

            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            cfgFileName = dirName + "config-verB.xml";
            vrpDirName = dirName + "dvrp\\";
            vrpStaticFileName = "A101.txt";
            vrpDynamicFileName = "A101_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "A102.txt";
            // vrpDynamicFileName = "A102_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // cfgFileName = dirName + "config-verBB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C101.txt";
            // vrpDynamicFileName = "C101_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C102.txt";
            // vrpDynamicFileName = "C102_scen.txt";

            vrpArcTimesFileName = vrpDirName + "arc_times.txt.gz";
            vrpArcCostsFileName = vrpDirName + "arc_costs.txt.gz";
            vrpArcPathsFileName = vrpDirName + "arc_paths.txt.gz";
            algParamsFileName = "algorithm.txt";
        }
        else if (args.length == 9) {
            dirName = args[0];
            cfgFileName = dirName + args[1];
            vrpDirName = dirName + args[2];
            vrpStaticFileName = args[3];
            vrpArcTimesFileName = vrpDirName + args[4];
            vrpArcCostsFileName = vrpDirName + args[5];
            vrpArcPathsFileName = vrpDirName + args[6];
            vrpDynamicFileName = args[7];
            algParamsFileName = args[8];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Config config = VrpConfigUtils.loadConfig(cfgFileName);
        Scenario scenario = ScenarioUtils.createScenario(config);

        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(config.network().getInputFile());

        // Controler controler = new Controler(new String[] { cfgFileName });
        // controler.setOverwriteFiles(true);

        // to have TravelTimeCalculatorWithBuffer instead of TravelTimeCalculator use:
        // controler.setTravelTimeCalculatorFactory(new TravelTimeCalculatorWithBufferFactory());

        VrpData vrpData = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                MatsimVertexImpl.createFromXYBuilder(scenario));

        if (!STATIC_MODE) {
            LacknerReader.parseDynamicFile(vrpDirName, vrpDynamicFileName, vrpData);
        }

        MatsimVrpData data = new MatsimVrpData(vrpData, scenario);
        TimeDiscretizer timeDiscretizer = TimeDiscretizer.TD_24H_BY_15MIN;

        if (VRP_OUT_FILES) {
            FullDiscreteMatsimArcIO.readShortestPaths(timeDiscretizer, data, vrpArcTimesFileName,
                    vrpArcCostsFileName, vrpArcPathsFileName);
        }
        else {
            FullDiscreteMatsimArcIO.readShortestPaths(timeDiscretizer, data, vrpArcTimesFileName,
                    vrpArcCostsFileName, null);
        }

        // now can run the optimizer or simulated optimizer...

        VrpOptimizer optimizer = new EvolutionaryVrpOptimizer(new AlgorithmParams(new File(dirName
                + "\\" + algParamsFileName)), data.getVrpData());

        //FIXME
        //ups.... currently unsupported (therefore null) :-/
        DeterministicSimulator simulator = new DeterministicSimulator(vrpData, 24 * 60 * 60,
                null);

        String vrpOutDirName = vrpDirName + "\\output";
        new File(vrpOutDirName).mkdir();

        if (VRP_OUT_FILES) {
            optimizer.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                public JFreeChart createChart(VrpData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            optimizer.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                public JFreeChart createChart(VrpData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        simulator.simulate();

        if (VRP_OUT_FILES) {
            List<Vehicle> vehicles = data.getVrpData().getVehicles();

            new Schedules2GIS(vehicles, data).write(vrpOutDirName);

            Population popul = scenario.getPopulation();
            PopulationFactory pf = popul.getFactory();

            // generate output plans (plans.xml)
            for (Vehicle v : vehicles) {
                Person person = pf.createPerson(scenario.createId("vrpDriver_" + v.getId()));

                VrpSchedulePlan plan = new VrpSchedulePlan(v, data);

                person.addPlan(plan);
                scenario.getPopulation().addPerson(person);
            }

            new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                    .writeV4(vrpOutDirName + "\\vrpDriverPlans.xml");
        }

        ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
        System.out.println("X");
    }
}
