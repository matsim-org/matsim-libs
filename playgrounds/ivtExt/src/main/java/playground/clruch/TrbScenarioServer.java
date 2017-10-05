package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import playground.clruch.analysis.AnalyzeAll;
import playground.clruch.analysis.AnalyzeSummary;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeGet;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeGet;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.html.DataCollector;
import playground.clruch.html.ReportGenerator;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.net.StorageUtils;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveltimetracker.AVTravelTimeModule;
import playground.clruch.trb18.TRBModule;
import playground.clruch.trb18.scenario.TRBScenarioConfig;
import playground.clruch.trb18.traveltime.reloading.TravelTimeReader;
import playground.clruch.trb18.traveltime.reloading.WriteTravelTimesModule;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/** main entry point
 * 
 * only one ScenarioServer can run at one time, since a fixed network port is reserved to serve the simulation status
 * 
 * if you wish to run multiple simulations at the same time use for instance {@link RunAVScenario} */
public class TrbScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {

        // BEGIN: CUSTOMIZE -----------------------------------------------
        // set to true in order to make server wait for at least 1 client, for instance viewer client
        boolean waitForClients = false;

        // END: CUSTOMIZE -------------------------------------------------

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup, new BlackListedTimeAllocationMutatorConfigGroup());
        String outputdirectory = config.controler().getOutputDirectory();
        System.out.println("outputdirectory = " + outputdirectory);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();

        Network network = scenario.getNetwork();

        Network reducedNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(reducedNetwork).readFile(new TRBScenarioConfig().filteredNetworkOutputPath);

        MatsimStaticDatabase.initializeSingletonInstance( //

                network, ReferenceFrame.SIOUXFALLS);

        for (String type : new String[] { "home", "shop", "leisure", "escort_kids", "escort_other", "work", "education", "remote_work", "remote_home" }) {
            for (int i = 0; i <= 20; i++) {
                ActivityParams params = new ActivityParams();
                params.setActivityType(type + "_" + i);
                params.setScoringThisActivityAtAll(false);
                config.planCalcScore().addActivityParams(params);
            }
        }

        Controler controler = new Controler(scenario);

        File inputFile = new File("travelTimes.txt.gz");

        if (inputFile.exists()) {
            TravelTimeReader travelTimeReader = new TravelTimeReader(300.0, 3600.0 * 30.0);
            controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(travelTimeReader.readTravelTimes(inputFile), 0.05));
        } else {
            controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        }

        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule()); // added only to listen to iteration counter
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        controler.addOverridingModule(new AVTravelTimeModule());
        controler.addOverridingModule(new TRBModule(reducedNetwork));
        controler.addOverridingModule(new WriteTravelTimesModule());

        // controler.addOverridingModule(new AbstractModule() {
        // @Override
        // public void install() {
        // bind(new TypeLiteral<Collection<Link>>() {}).annotatedWith(Names.named("zurich")).toInstance(filteredPermissibleLinks);
        // //AVUtils.registerDispatcherFactory(binder(), "ZurichDispatcher", ZurichDispatcher.ZurichDispatcherFactory.class);
        // AVUtils.registerGeneratorFactory(binder(), "ZurichGenerator", ZurichGenerator.ZurichGeneratorFactory.class);
        //
        // addPlanStrategyBinding("ZurichModeChoice").toProvider(ZurichPlanStrategyProvider.class);
        // }
        // });

        StorageUtils.OUTPUT = new File(config.controler().getOutputDirectory());
        StorageUtils.DIRECTORY = new File(StorageUtils.OUTPUT, "simobj");

        controler.run();

        SimulationServer.INSTANCE.stopAccepting(); // close port

        AnalyzeSummary analyzeSummary = AnalyzeAll.analyze(new File(args[0]), outputdirectory);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
        int vehicleSteps = Math.max(10, maxNumberVehiclesPerformanceCalculator / 400);

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = null;
        TravelData travelData = null;
        if (virtualNetwork != null) {
            minimumFleetSizeCalculator = MinimumFleetSizeGet.readDefault();
            performanceFleetSizeCalculator = PerformanceFleetSizeGet.readDefault();
            if (performanceFleetSizeCalculator != null) {
                String dataFolderName = outputdirectory + "/data";
                File relativeDirectory = new File(dataFolderName);
                performanceFleetSizeCalculator.saveAndPlot(dataFolderName, relativeDirectory);
            }
            travelData = TravelDataGet.readDefault(virtualNetwork);
        }

        DataCollector datacollector = new DataCollector(new File(args[0]), outputdirectory, controler, //
                minimumFleetSizeCalculator, analyzeSummary, network, population, travelData);

        // generate report
        ReportGenerator.from(new File(args[0]), outputdirectory);

    }
}
