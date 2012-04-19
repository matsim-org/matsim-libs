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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;

import pl.poznan.put.util.jfreechart.ChartUtils;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.ChartCreator;
import pl.poznan.put.vrp.dynamic.chart.RouteChartUtils;
import pl.poznan.put.vrp.dynamic.chart.ScheduleChartUtils;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.model.CustomerImpl;
import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.model.RequestImpl;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.FixedSizeVrpGraph;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer.AlgorithmType;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerWithReassignment;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerWithoutReassignment;
import pl.poznan.put.vrp.dynamic.simulator.DeterministicSimulator;
import pl.poznan.put.vrp.dynamic.simulator.customer.CustomerAction;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.MatsimVrpDataCreator;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.MatsimVrpGraph;
import playground.michalm.vrp.data.network.router.TravelTimeCalculators;
import playground.michalm.vrp.data.network.shortestpath.sparse.SparseShortestPathArc;
import playground.michalm.vrp.data.network.shortestpath.sparse.SparseShortestPathFinder;
import playground.michalm.vrp.driver.VrpSchedulePlan;


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
    private TaxiOptimizerFactory optimizerFactory;

    private PersonalizableTravelTime ttimeCalc;
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

        algorithmType = AlgorithmType.NO_RE_ASSIGNMENT;
        // algorithmType = AlgorithmType.RE_ASSIGNMENT;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "\\vrp_output";
        new File(vrpOutDirName).mkdir();
    }


    private void prepareMatsimData()
        throws IOException
    {
        Config config = ConfigUtils.createConfig();
        scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        ttimeCalc = travelTimesFromEvents ? TravelTimeCalculators.createTravelTimeFromEvents(
                eventsFileName, scenario) : new FreeSpeedTravelTimeCalculator();
        tcostCalc = new OnlyTimeDependentTravelDisutilityCalculator(ttimeCalc);

    }


    private void handleTaxiModeDepartures()
    {
        MatsimVrpGraph vrpGraph = data.getVrpGraph();
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

            if ( ((Leg)planElements.get(1)).getMode() != "taxi") {
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
        data = MatsimVrpDataCreator.create(scenario);
        new DepotReader(scenario, data).readFile(depotsFileName);

        handleTaxiModeDepartures();// creates Requests

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data);
        SparseShortestPathArc[][] arcs = sspf.findShortestPaths(ttimeCalc, tcostCalc, router);
        ((FixedSizeVrpGraph)data.getVrpGraph()).setArcs(arcs);
    }


    private void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmType) {
            case NO_RE_ASSIGNMENT:
                optimizerFactory = TaxiOptimizerWithoutReassignment.FACTORY;
                break;

            case RE_ASSIGNMENT:
                optimizerFactory = TaxiOptimizerWithReassignment.FACTORY;
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
