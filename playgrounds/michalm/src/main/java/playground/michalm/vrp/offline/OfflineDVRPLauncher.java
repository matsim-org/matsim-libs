package playground.michalm.vrp.offline;

import java.io.*;
import java.util.*;

import org.jfree.chart.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.scenario.*;
import org.matsim.core.utils.misc.*;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.*;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.customer.*;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.file.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Route;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.simulator.*;
import playground.michalm.vrp.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.supply.*;


public class OfflineDVRPLauncher
{
    // means: ArcTravelTimes and ArcTravelCosts are averaged for optimization
    private static boolean AVG_TRAFFIC_MODE = false;// default: false

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
            AVG_TRAFFIC_MODE = false;
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
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C101.txt";
            // vrpDynamicFileName = "C101_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C102.txt";
            // vrpDynamicFileName = "C102_scen.txt";

            vrpArcTimesFileName = vrpDirName + "arc_times.txt";
            vrpArcCostsFileName = vrpDirName + "arc_costs.txt";
            vrpArcPathsFileName = vrpDirName + "arc_paths.txt";
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

        Config config = ConfigUtils.loadConfig(cfgFileName);
        Scenario scenario = ScenarioUtils.createScenario(config);
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(config.network().getInputFile());

        // Controler controler = new Controler(new String[] { cfgFileName });
        // controler.setOverwriteFiles(true);

        // to have TravelTimeCalculatorWithBuffer instead of TravelTimeCalculator use:
        // controler.setTravelTimeCalculatorFactory(new TravelTimeCalculatorWithBufferFactory());

        VRPData vrpData = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                new MATSimVertexImpl.Builder(scenario));

        Queue<CustomerAction> caQueue = null;

        if (STATIC_MODE) {
            caQueue = new PriorityQueue<CustomerAction>(1, CustomerAction.TIME_COMPARATOR);
        }
        else {
            caQueue = LacknerReader.parseDynamicFile(vrpDirName, vrpDynamicFileName, vrpData);
        }

        MATSimVRPData data = new MATSimVRPData(vrpData, scenario);

        ShortestPathsFinder spf = new ShortestPathsFinder(data);

        if (VRP_OUT_FILES) {
            spf.readShortestPaths(vrpArcTimesFileName, vrpArcCostsFileName, vrpArcPathsFileName);
        }
        else {
            spf.readShortestPaths(vrpArcTimesFileName, vrpArcCostsFileName, null);
        }

        spf.upadateVRPArcTimesAndCosts();

        // ================================================== BELOW: only for comparison reasons...

        VRPGraph graph = vrpData.getVrpGraph();

        InterpolatedArcCost[][] simulatedArcCosts = null;
        InterpolatedArcTime[][] simulatedArcTimes = null;

        if (AVG_TRAFFIC_MODE) {
            simulatedArcCosts = (InterpolatedArcCost[][])graph.getCosts();
            simulatedArcTimes = (InterpolatedArcTime[][])graph.getTimes();

            graph.setCosts(ConstantArcCost.averageInterpolatedArcCosts(simulatedArcCosts));
            graph.setTimes(ConstantArcTime.averageInterpolatedArcTimes(simulatedArcTimes));

            System.err.println("RUNNING WITH AVERAGED ArcTimes/Costs");
        }

        // ================================================== ABOVE: only for comparison reasons...

        // now can run the optimizer or simulated optimizer...
        Simulator simulator = new Simulator(vrpData, vrpDirName, algParamsFileName, caQueue,
                24 * 60 * 60);

        // simulator.addListener(new ConsoleSimulationListener());

        String vrpOutDirName = vrpDirName + "\\output";
        new File(vrpOutDirName).mkdir();

        if (VRP_OUT_FILES) {
            simulator.addListener(new ChartFileSimulationListener(new ChartCreator() {
                public JFreeChart createChart(VRPData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            simulator.addListener(new ChartFileSimulationListener(new ChartCreator() {
                public JFreeChart createChart(VRPData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        simulator.simulate();

        if (VRP_OUT_FILES) {
            new Routes2QGIS(data.getVrpData().routes, data, vrpOutDirName + "\\route_").write();

            Population popul = scenario.getPopulation();
            PopulationFactory pf = popul.getFactory();

            // generate output plans (plans.xml)
            for (Route rt : data.getVrpData().routes) {
                Person person = pf.createPerson(scenario.createId("vrpDriver_" + rt.id));

                VRPRoutePlan plan = new VRPRoutePlan(new PersonImpl(scenario.createId(Integer
                        .toString(rt.id))), rt, data);

                person.addPlan(plan);
                scenario.getPopulation().addPerson(person);
            }

            new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                    .writeV4(vrpOutDirName + "\\vrpDriverPlans.xml");
        }

        ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        // ================================================== BELOW: only for comparison reasons...

        if (AVG_TRAFFIC_MODE) {
            graph.setCosts(simulatedArcCosts);
            graph.setTimes(simulatedArcTimes);

            new ScheduleUpdater(vrpData).updateSchedule();
            vrpData.evaluateVRP();

            // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(vrpData));
            ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(vrpData));

            System.out.println("################################################");
            DebugPrint.printDynamicData(vrpData);
        }

        // ================================================== ABOVE: only for comparison reasons...

        // TODO fix bug in Link Time Estimations ??

    }
}
