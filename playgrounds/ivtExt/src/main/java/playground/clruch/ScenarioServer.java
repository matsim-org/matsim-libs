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

import playground.joel.analysis.AnalyzeAll;
//import playground.clruch.demo.AnalyzeAll;
import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.prep.TheApocalypse;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/**
 * main entry point
 */
public class ScenarioServer {
    public static void main(String[] args) throws MalformedURLException, Exception {
        File configFile = new File(args[0]);

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();

        // set to true in order to make server wait for at least 1 client, for instance viewer client
        SimulationServer.INSTANCE.setWaitForClients(false);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();

        MatsimStaticDatabase.initializeSingletonInstance( //
                scenario.getNetwork(), ReferenceFrame.IDENTITY);



        TheApocalypse.decimatesThe(population).toNoMoreThan(10000).people();

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule()); // added only to listen to iteration counter
        controler.run();

        SimulationServer.INSTANCE.stopAccepting();

        // EventFileToProcessingXML.convert(dir);
        AnalyzeAll.analyze(args);
    }
}
