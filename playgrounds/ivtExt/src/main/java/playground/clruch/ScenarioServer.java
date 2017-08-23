package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.analysis.AnalyzeAll;
import playground.clruch.analysis.AnalyzeSummary;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeGet;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.html.DataCollector;
import playground.clruch.html.ReportGenerator;
import playground.clruch.html.ScenarioParameters;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.net.StorageUtils;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveltimetracker.AVTravelTimeModule;
import playground.clruch.trb18.traveltime.reloading.TravelTimeReader;
import playground.clruch.trb18.traveltime.reloading.WriteTravelTimesModule;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/** main entry point
 * 
 * only one ScenarioServer can run at one time, since a fixed network port is reserved to serve the
 * simulation status
 * 
 * if you wish to run multiple simulations at the same time use for instance
 * {@link RunAVScenario} */
public class ScenarioServer {

    public static ScenarioParameters scenarioParameters;

    public static void main(String[] args) throws MalformedURLException, Exception {

        // BEGIN: CUSTOMIZE -----------------------------------------------
        // set to true in order to make server wait for at least 1 client, for instance viewer
        // client
        boolean waitForClients = false;

        // END: CUSTOMIZE -------------------------------------------------

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        // load MATSim configs
        File configFile = new File(args[0]);
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup, new BlackListedTimeAllocationMutatorConfigGroup());

        // extract data for scenarioParameters
        scenarioParameters = new ScenarioParameters(config);

        // load scenario for simulation
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SWITZERLAND);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule());
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        controler.addOverridingModule(new AVTravelTimeModule());
        controler.addOverridingModule(new WriteTravelTimesModule());

        // directories for saving results
        StorageUtils.OUTPUT = new File(config.controler().getOutputDirectory());
        StorageUtils.DIRECTORY = new File(StorageUtils.OUTPUT, "simobj");

        // run simulation
        controler.run();

        // close port for visualization
        SimulationServer.INSTANCE.stopAccepting();

        // perform analysis of results
        AnalyzeSummary analyzeSummary = AnalyzeAll.analyze(args);
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = null;

        int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);

        if (virtualNetwork != null) {
            minimumFleetSizeCalculator = MinimumFleetSizeGet.readDefault();
            performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);
        }

        DataCollector.store(args, controler, minimumFleetSizeCalculator, performanceFleetSizeCalculator, //
                analyzeSummary, scenarioParameters);

        // generate report  
        ReportGenerator.from(args);

    }
}
