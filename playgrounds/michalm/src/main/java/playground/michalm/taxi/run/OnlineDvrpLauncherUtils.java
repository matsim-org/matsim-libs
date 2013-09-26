package playground.michalm.taxi.run;

import java.io.*;
import java.util.*;

import javax.swing.SwingUtilities;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.file.DepotReader;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.contrib.dvrp.data.network.router.*;
import org.matsim.contrib.dvrp.data.network.shortestpath.MatsimArcFactories;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.*;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import pl.poznan.put.vrp.dynamic.util.TimeDiscretizer;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.taxi.*;
import playground.michalm.taxi.optimizer.TaxiOptimizer;


public class OnlineDvrpLauncherUtils
{
    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED("FF", 24 * 60 * 60), // no eventsFileName
        EVENTS_24_H("24H", 24 * 60 * 60), // based on eventsFileName, with 24-hour time interval
        EVENTS_15_MIN("15M", 15 * 60); // based on eventsFileName, with 15-minute time interval

        /*package*/final String shortcut;
        /*package*/final int travelTimeBinSize;
        /*package*/final int numSlots;


        private TravelTimeSource(String shortcut, int travelTimeBinSize)
        {
            this.shortcut = shortcut;
            this.travelTimeBinSize = travelTimeBinSize;
            this.numSlots = 24 * 60 * 60 / travelTimeBinSize;// to cover 24 hours
        }
    }


    public enum TravelDisutilitySource
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
        
// replacing the above fore loop by the code below will remove	 the non-taxicab passengers from the simulation.
// was, for example, useful for a "freight-like" demo.
//        Collection<Id> normalPersons = new ArrayList<Id>() ;
//        for ( Entry<Id, ? extends Person> entry : scenario.getPopulation().getPersons().entrySet() ) {
//        	Person person = entry.getValue() ;
//        	if ( taxiCustomerIds.contains( person.getId().toString() ) ) {
//              Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
//              leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
//        	} else {
//        		normalPersons.add( person.getId() ) ;
//        	}
//        }
//        System.err.println( " population size before deletion: " + scenario.getPopulation().getPersons().size() );
//        for ( Id id : normalPersons ) {
//        	scenario.getPopulation().getPersons().remove(id) ;
//        }
//        System.err.println( " population size after deletion: " + scenario.getPopulation().getPersons().size() );
        
        return scenario;
    }


    /**
     * Mandatory
     */
    public static MatsimVrpData initMatsimVrpData(Scenario scenario, TravelTimeCalculator travelTimeCalculator,
            TravelTimeSource ttimeSource, TravelDisutilitySource tdisSource, String eventsFileName,
            String depotsFileName)
    {
        TravelTime travelTime;
        if (travelTimeCalculator == null) {
            switch (ttimeSource) {
                case FREE_FLOW_SPEED:
                    travelTime = new FreeSpeedTravelTime();
                    break;

                case EVENTS_15_MIN:
                case EVENTS_24_H:
                    scenario.getConfig().travelTimeCalculator()
                            .setTraveltimeBinSize(ttimeSource.travelTimeBinSize);
                    travelTime = TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                            scenario);
                    break;

                default:
                    throw new IllegalArgumentException();
            }
        }
        else {
            travelTime = travelTimeCalculator.getLinkTravelTimes();
        }

        TravelDisutility travelDisutility;
        switch (tdisSource) {
            case DISTANCE:
                travelDisutility = new DistanceAsTravelDisutility();
                break;

            case TIME:
                travelDisutility = new TimeAsTravelDisutility(travelTime);
                break;

            default:
                throw new IllegalArgumentException();
        }

        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(ttimeSource.travelTimeBinSize,
                ttimeSource.numSlots);
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, travelTime,
                travelDisutility, timeDiscretizer, false);

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
    public static QSim initQSim(MatsimVrpData data, TaxiOptimizer optimizer,
            boolean onlineVehicleTracker)
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
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine, onlineVehicleTracker));
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
