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

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityCalculator;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.simulator.DeterministicSimulator;
import pl.poznan.put.vrp.dynamic.simulator.customer.CustomerAction;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.TravelTimeCalculators;
import playground.michalm.vrp.data.network.shortestpath.MatsimArcFactories;
import playground.michalm.vrp.driver.VrpSchedulePlan;
import playground.michalm.vrp.run.VrpConfigUtils;
import playground.michalm.vrp.run.online.AlgorithmConfig.AlgorithmType;
import playground.michalm.vrp.taxi.TaxiModeDepartureHandler;


public class SingleIterOfflineDvrpLauncher
{
    private String dirName;
    private String netFileName;
    private String plansFileName;
    private String depotsFileName;
    private String reqIdToVehIdFileName;
    private boolean vrpOutFiles;
    private String vrpOutDirName;

    private boolean travelTimesFromEvents;
    private String eventsFileName;

    private Scenario scenario;
    private MatsimVrpData data;

    private AlgorithmType algorithmType;
    private TaxiOptimizationPolicy optimizationPolicy;
    private TaxiOptimizerFactory optimizerFactory;

    private TravelTime ttimeCalc;
    private TravelDisutility tcostCalc;


    private void processArgs()
    {
        dirName = "D:\\PP-rad\\taxi\\mielec-nowe-OD\\";
        netFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        depotsFileName = dirName + "depots.xml";
        reqIdToVehIdFileName = dirName + "reqIdToVehId";

        travelTimesFromEvents = true;
        eventsFileName = "d:\\PP-rad\\taxi\\orig-mielec-nowe-OD\\output\\std\\ITERS\\it.10\\10.events.xml.gz";

        algorithmType = AlgorithmType.ONE_TIME_SCHEDULING;
        // algorithmType = AlgorithmType.RE_ASSIGNMENT;
        optimizationPolicy = TaxiOptimizationPolicy.ALWAYS;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "\\vrp_output";
        new File(vrpOutDirName).mkdir();
    }


    private void prepareMatsimData()
        throws IOException
    {
        scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        ttimeCalc = travelTimesFromEvents ? TravelTimeCalculators.createTravelTimeFromEvents(
                eventsFileName, scenario) : new FreeSpeedTravelTime();
        tcostCalc = new OnlyTimeDependentTravelDisutilityCalculator(ttimeCalc);

    }


    private void handleTaxiModeDepartures()
    {
        MatsimVrpGraph vrpGraph = data.getMatsimVrpGraph();
        List<Customer> customers = data.getVrpData().getCustomers();
        List<Request> requests = data.getVrpData().getRequests();

        List<Person> reorderedPersons = new ArrayList<Person>(scenario.getPopulation().getPersons()
                .values());

        // order the requests as they would appear in MATSim
        Collections.sort(reorderedPersons, new Comparator<Person>() {
            @Override
            public int compare(Person p1, Person p2)
            {
                int t1 = (int) ((Activity)p1.getSelectedPlan().getPlanElements().get(0))
                        .getEndTime();
                int t2 = (int) ((Activity)p2.getSelectedPlan().getPlanElements().get(0))
                        .getEndTime();

                if (t1 != t2) {
                    return t1 - t2;
                }

                // reverse order
                return -p1.getId().compareTo(p2.getId());
            };
        });

        for (Person p : reorderedPersons) {
            List<PlanElement> planElements = p.getSelectedPlan().getPlanElements();

            if ( ((Leg)planElements.get(1)).getMode() != TaxiModeDepartureHandler.TAXI_MODE) {
                continue;
            }

            Activity beginAct = (Activity)planElements.get(0);
            Activity endAct = (Activity)planElements.get(2);

            MatsimVertex fromVertex = vrpGraph.getVertex(beginAct.getLinkId());
            MatsimVertex toVertex = vrpGraph.getVertex(endAct.getLinkId());
            double now = beginAct.getEndTime();

            int id = requests.size();

            Customer customer = new CustomerImpl(id, p.getId().toString(), fromVertex);// TODO

            int duration = 120; // approx. 120 s for entering the taxi
            int t0 = (int)now;
            int t1 = t0 + 0; // hardcoded values!
            Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, 1, duration,
                    t0, t1, false);

            request.deactivate(t0);

            customers.add(customer);
            requests.add(request);
        }
    }


    private void initMatsimVrpData()
        throws IOException
    {
        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = TimeDiscretizer.TD_24H_BY_15MIN;
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, ttimeCalc, tcostCalc,
                timeDiscretizer, false);
        MatsimVrpGraph graph = MatsimVrpGraphCreator.create(network, arcFactory, true);

        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        vrpData.setCustomers(new ArrayList<Customer>());
        vrpData.setRequests(new ArrayList<Request>());
        new DepotReader(scenario, vrpData).readFile(depotsFileName);
        handleTaxiModeDepartures();// creates Requests

        data = new MatsimVrpData(vrpData, scenario);
    }


    private void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmType) {
            case ONE_TIME_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithoutReassignment
                        .createFactory(optimizationPolicy);
                break;

            case RE_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithReassignment.createFactory(optimizationPolicy);
                break;

            default:
                throw new IllegalStateException();
        }
    }


    private void runSim()
        throws IOException
    {
        TaxiOptimizer optimizer = optimizerFactory.create(data.getVrpData());

        DeterministicSimulator simulator = new DeterministicSimulator(data.getVrpData(),
                24 * 60 * 60, optimizer, new Comparator<CustomerAction>() {
                    @Override
                    public int compare(CustomerAction ca1, CustomerAction ca2)
                    {
                        // order the requests as they would appear in MATSim
                        return ca1.getRequest().getId() - ca2.getRequest().getId();
                    }
                });

        // simulator.addListener(new ConsoleSimulationListener());

        String vrpOutDirName = dirName + "\\output";
        new File(vrpOutDirName).mkdir();

        if (vrpOutFiles) {
            optimizer.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
                public JFreeChart createChart(VrpData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            optimizer.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
                public JFreeChart createChart(VrpData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        simulator.simulate();
    }


    private void generateVrpOutput()
        throws IOException
    {
        System.out.println(new TaxiEvaluator().evaluateVrp(data.getVrpData()).toString());

        if (vrpOutFiles) {
            List<Vehicle> vehicles = data.getVrpData().getVehicles();

            new Schedules2GIS(vehicles, data, vrpOutDirName + "\\route_").write();

            // PopulationReader popReader = new MatsimPopulationReader(scenario).readFile(dirName +
            // );

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
                    .writeFileV4(vrpOutDirName + "\\vrpDriverPlans.xml");
        }

        ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        // ============================== for PreAssignment ==============================

        File reqIdToVehIdFile = new File(reqIdToVehIdFileName);
        Writer w = new BufferedWriter(new FileWriter(reqIdToVehIdFile));

        for (Request req : data.getVrpData().getRequests()) {
            w.write(req.getServeTask().getSchedule().getVehicle().getId() + "\n");
        }

        w.close();
    }


    public static void main(String... args)
        throws IOException
    {
        new SingleIterOfflineDvrpLauncher().go();
    }


    private void go()
        throws IOException
    {
        processArgs();
        prepareMatsimData();
        initMatsimVrpData();
        initOptimizerFactory();
        runSim();
        generateVrpOutput();
    }
}
