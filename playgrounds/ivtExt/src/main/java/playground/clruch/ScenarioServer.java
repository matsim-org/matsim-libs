package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveltimetracker.AVTravelTimeModule;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.joel.analysis.AnalyzeAll;
import playground.joel.analysis.AnalyzeSummary;
import playground.joel.analysis.MinimumFleetSizeCalculator;
import playground.joel.html.DataCollector;
import playground.joel.html.ReportGenerator;
import playground.joel.html.ScenarioParameters;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/**
 * main entry point
 * 
 * only one ScenarioServer can run at one time, since a fixed network port is reserved
 * to serve the simulation status
 * 
 * if you wish to run multiple simulations at the same time use for instance
 * {@link RunAVScenario}
 */
public class ScenarioServer {

    public static ScenarioParameters scenarioParameters;

    public static void main(String[] args) throws MalformedURLException, Exception {

        // BEGIN: CUSTOMIZE -----------------------------------------------
        // set manually depending on the scenario:
        int maxPopulationSize = 1000;

        int minFleetSizeBinSize = 3600;

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
        //Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup, new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();
        MatsimStaticDatabase.initializeSingletonInstance( //
                scenario.getNetwork(), ReferenceFrame.IDENTITY);
        
        
//        // admissible Nodes sebhoerl
//        final Network network = scenario.getNetwork();
//        
//        FileInputStream stream = new FileInputStream(ConfigGroup.getInputFileURL(config.getContext(), "nodes.list").getPath());
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//
//        final Set<Node> permissibleNodes = new HashSet<>();
//        final Set<Link> permissibleLinks = new HashSet<>();
//
//        reader.lines().forEach((String nodeId) -> permissibleNodes.add(network.getNodes().get(Id.createNodeId(nodeId))) );
//        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getOutLinks().values()));
//        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getInLinks().values()));
//        final Set<Link> filteredPermissibleLinks = permissibleLinks.stream().filter((l) -> l.getAllowedModes().contains("car")).collect(Collectors.toSet());
        
        
        TheApocalypse.decimatesThe(population).toNoMoreThan(maxPopulationSize).people();
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule()); // added only to listen to iteration counter
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        controler.addOverridingModule(new AVTravelTimeModule());
        
        
//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                bind(new TypeLiteral<Collection<Link>>() {}).annotatedWith(Names.named("zurich")).toInstance(filteredPermissibleLinks);
//                //AVUtils.registerDispatcherFactory(binder(), "ZurichDispatcher", ZurichDispatcher.ZurichDispatcherFactory.class);
//                AVUtils.registerGeneratorFactory(binder(), "ZurichGenerator", ZurichGenerator.ZurichGeneratorFactory.class);
//
//                addPlanStrategyBinding("ZurichModeChoice").toProvider(ZurichPlanStrategyProvider.class);
//            }
//        });

        controler.run();

        SimulationServer.INSTANCE.stopAccepting(); // close port

        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        if (virtualNetwork != null) {
            minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(scenario.getNetwork(), //
                    population, virtualNetwork, minFleetSizeBinSize);
        }

        AnalyzeSummary analyzeSummary = AnalyzeAll.analyze(args);
        //AnalyzeMarc.analyze(args);

        DataCollector.store(args, controler, minimumFleetSizeCalculator, analyzeSummary, scenarioParameters);

        ReportGenerator.from(args);

    }
}
