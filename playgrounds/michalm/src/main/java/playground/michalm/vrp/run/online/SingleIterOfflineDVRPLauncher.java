package playground.michalm.vrp.run.online;

import java.io.*;
import java.util.*;

import org.jfree.chart.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.*;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.*;
import org.matsim.core.trafficmonitoring.*;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.optimizer.listener.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer.*;
import pl.poznan.put.vrp.dynamic.simulator.*;
import pl.poznan.put.vrp.dynamic.simulator.customer.*;
import playground.michalm.util.gis.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.file.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.sparse.*;
import playground.michalm.vrp.driver.*;


public class SingleIterOfflineDVRPLauncher
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
    private MATSimVRPData data;

    private AlgorithmType algorithmType;
    private TaxiOptimizerFactory optimizerFactory;

    private PersonalizableTravelTime ttimeCalc;
    private PersonalizableTravelDisutility tcostCalc;


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
        //algorithmType = AlgorithmType.RE_ASSIGNMENT;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "\\vrp_output";
        new File(vrpOutDirName).mkdir();
    }


    private void prepareMATSimData()
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
        MATSimVRPGraph vrpGraph = data.getVrpGraph();
        List<Customer> customers = data.getVrpData().getCustomers();
        List<Request> requests = data.getVrpData().getRequests();

        List<Person> reorderedPersons = new ArrayList<Person>(scenario.getPopulation().getPersons()
                .values());

        // order the requests as they would appear in MATSim
        Collections.sort(reorderedPersons, new Comparator<Person>() {
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

            MATSimVertex fromVertex = vrpGraph.getVertex(beginAct.getLinkId());
            MATSimVertex toVertex = vrpGraph.getVertex(endAct.getLinkId());
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


    private void initMATSimVRPData()
        throws IOException
    {
        data = MATSimVRPDataCreator.create(scenario);
        new DepotReader(scenario, data).readFile(depotsFileName);

        handleTaxiModeDepartures();// creates Requests

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data);
        sspf.findShortestPaths(ttimeCalc, tcostCalc, router);
        sspf.upadateVRPArcTimesAndCosts();
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
                public JFreeChart createChart(VRPData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            optimizer.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                public JFreeChart createChart(VRPData data)
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
        System.out.println(new TaxiEvaluator().evaluateVRP(data.getVrpData()).toString());

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

                VRPSchedulePlan plan = new VRPSchedulePlan(v, data);

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
        new SingleIterOfflineDVRPLauncher().go();
    }


    private void go()
        throws IOException
    {
        processArgs();
        prepareMATSimData();
        initMATSimVRPData();
        initOptimizerFactory();
        runSim();
        generateVrpOutput();
    }
}
