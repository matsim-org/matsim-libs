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

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveltimetracker.AVTravelTimeModule;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.joel.analysis.AnalyzeAll;
import playground.joel.analysis.AnalyzeSummary;
import playground.joel.analysis.MinimumFleetSizeCalculator;
import playground.joel.analysis.PerformanceFleetSizeCalculator;
import playground.joel.html.DataCollector;
import playground.joel.html.ReportGenerator;
import playground.joel.html.ScenarioParameters;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/**
 * main entry point
 * 
 * only one ScenarioServer can run at one time, since a fixed network port is reserved to serve the simulation status
 * 
 * if you wish to run multiple simulations at the same time use for instance {@link RunAVScenario}
 */
public class ScenarioServer {

    public static ScenarioParameters scenarioParameters;

    public static void main(String[] args) throws MalformedURLException, Exception {

        // BEGIN: CUSTOMIZE -----------------------------------------------
        // set to true in order to make server wait for at least 1 client, for instance viewer client
        boolean waitForClients = false;

        // END: CUSTOMIZE -------------------------------------------------

        scenarioParameters = new ScenarioParameters();

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());

        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();
        
        Network network = scenario.getNetwork();

//        Network reducedNetwork = NetworkUtils.createNetwork();
//        new MatsimNetworkReader(reducedNetwork).readFile(new TRBScenarioConfig().filteredNetworkOutputPath);
        
        

        
        
        MatsimStaticDatabase.initializeSingletonInstance( //

                network, ReferenceFrame.SIOUXFALLS);
        
        
        
        for (String type : new String[] { "home", "shop", "leisure", "escort_kids", "escort_other", "work", "education", "remote_work",
                "remote_home" }) {
            for (int i = 0; i <= 20; i++) {
                ActivityParams params = new ActivityParams();
                params.setActivityType(type + "_" + i);
                params.setScoringThisActivityAtAll(false);
                config.planCalcScore().addActivityParams(params);
            }
        }

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule()); // added only to listen to iteration counter
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        controler.addOverridingModule(new AVTravelTimeModule());

        controler.run();

        SimulationServer.INSTANCE.stopAccepting(); // close port

        AnalyzeSummary analyzeSummary = AnalyzeAll.analyze(args);
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(network, virtualNetwork);
        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = null;
        int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
        int vehicleSteps = Math.max(10, maxNumberVehiclesPerformanceCalculator / 400);

        if (virtualNetwork != null) {
            minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network,population,virtualNetwork,travelData);
            performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(network, virtualNetwork, travelData,
                    maxNumberVehiclesPerformanceCalculator, vehicleSteps);
        }

        DataCollector.store(args, controler, minimumFleetSizeCalculator, performanceFleetSizeCalculator, //
                analyzeSummary, scenarioParameters);

        ReportGenerator.from(args);

    }
}
