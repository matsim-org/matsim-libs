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
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import playground.clruch.export.EventFileToProcessingXML;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.prep.TheApocalypse;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/**
 * main entry point
 */
public class ScenarioServer {
    public static void main(String[] args) throws MalformedURLException {
        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();

        SimulationServer.INSTANCE.startAcceptingNonBlocking();

        // set to true in order to make server wait for viewer client:
        SimulationServer.INSTANCE.setWaitForClients(false);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();

        MatsimStaticDatabase.initializeSingletonInstance( //
                scenario.getNetwork(), new IdentityTransformation());

        TheApocalypse.decimatesThe(population).toNoMoreThan(20000).people();

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());

        controler.run();

        SimulationServer.INSTANCE.stopAccepting();

        EventFileToProcessingXML.convert(dir);
    }
}
