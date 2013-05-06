package playground.michalm.vrp.run.online;

import java.io.*;
import java.util.*;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.MatsimArcFactories;
import playground.michalm.vrp.run.VrpConfigUtils;
import playground.michalm.vrp.taxi.*;


public class OnlineDvrpLauncherUtils
{
    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED(24 * 60 * 60), // no eventsFileName
        EVENTS_24_H(24 * 60 * 60), // based on eventsFileName, with 24-hour time interval
        EVENTS_15_MIN(15 * 60); // based on eventsFileName, with 15-minute time interval

        /*package*/final int travelTimeBinSize;
        /*package*/final int numSlots;


        private TravelTimeSource(int travelTimeBinSize)
        {
            this.travelTimeBinSize = travelTimeBinSize;
            this.numSlots = 24 * 60 * 60 / travelTimeBinSize;// to cover 24 hours
        }
    }


    public enum TravelCostSource
    {
        TIME, // travel time
        DISTANCE; // travel distance
    }


    /**
     * Mandatory
     */
    public static Scenario initMatsimData(String netFileName, String plansFileName,
            String taxiCustomersFileName)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        List<String> taxiCustomerIds;
        try {
            taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String id : taxiCustomerIds) {
            Person person = scenario.getPopulation().getPersons().get(scenario.createId(id));
            Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
            leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
        }

        return scenario;
    }


    /**
     * Mandatory
     */
    public static MatsimVrpData initMatsimVrpData(Scenario scenario, TravelTimeSource ttimeSource,
            TravelCostSource tcostSource, String eventsFileName, String depotsFileName)
    {
        int travelTimeBinSize = ttimeSource.travelTimeBinSize;
        int numSlots = ttimeSource.numSlots;

        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);

        TravelTime ttimeCalc;
        TravelDisutility tcostCalc;

        switch (ttimeSource) {
            case FREE_FLOW_SPEED:
                ttimeCalc = new FreeSpeedTravelTime();
                break;

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                ttimeCalc = TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                        scenario);
                break;

            default:
                throw new IllegalArgumentException();
        }

        switch (tcostSource) {
            case DISTANCE:
                tcostCalc = new DistanceAsTravelDisutility();
                break;

            case TIME:
                tcostCalc = new TimeAsTravelDisutility(ttimeCalc);
                break;

            default:
                throw new IllegalArgumentException();
        }

        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(travelTimeBinSize, numSlots);
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, ttimeCalc, tcostCalc,
                timeDiscretizer, false);

        MatsimVrpGraph graph;
        try {
            graph = MatsimVrpGraphCreator.create(network, arcFactory, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        vrpData.setCustomers(new ArrayList<Customer>());
        vrpData.setRequests(new ArrayList<Request>());
        new DepotReader(scenario, vrpData).readFile(depotsFileName);

        return new MatsimVrpData(vrpData, scenario);
    }


    /**
     * Mandatory
     */
    public static QSim initQSim(MatsimVrpData data, TaxiOptimizer optimizer)
    {
        Scenario scenario = data.getScenario();
        EventsManager events = EventsUtils.createEventsManager();
        QSim qSim = new QSim(scenario, events);

        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);

        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim);
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

        TeleportationEngine teleportationEngine = new TeleportationEngine();
        qSim.addMobsimEngine(teleportationEngine);

        TaxiSimEngine taxiSimEngine = new TaxiSimEngine(qSim, data, optimizer);
        qSim.addMobsimEngine(taxiSimEngine);

        qSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        qSim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        return qSim;
    }


    /**
     * Optional
     */
    public static void writeHistograms(LegHistogram legHistogram, String histogramOutDirName)
    {
        new File(histogramOutDirName).mkdir();
        legHistogram.write(histogramOutDirName + "legHistogram_all.txt");
        legHistogram.writeGraphic(histogramOutDirName + "legHistogram_all.png");
        for (String legMode : legHistogram.getLegModes()) {
            legHistogram.writeGraphic(histogramOutDirName + "legHistogram_" + legMode + ".png",
                    legMode);
        }
    }
}
