package playground.artemc.heterogeneity;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.pricing.LinkOccupancyAnalyzerModule;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by artemc on 19/1/15.
 */
public class BasicControler {

    private static String input;
    private static String output;

    public static void main(String[] args){

        input = args[0];

        if (args.length > 1) {
            output = args[1];
        }

        BasicControler runner = new BasicControler();
        runner.run();
    }

    private void run() {

        Controler controler = null;
        Scenario scenario = initSampleScenario();
        System.setProperty("matsim.preferLocalDtds", "true");
        controler = new Controler(scenario);
        controler.setModules(new ControlerDefaultsModule(), new LinkOccupancyAnalyzerModule());
        controler.setOverwriteFiles(true);
        controler.run();
    }

    private static Scenario initSampleScenario() {

        Config config = ConfigUtils.loadConfig(input + "config.xml");
        config.network().setInputFile(input + "network.xml");

        boolean isPopulationZipped = new File(input + "population.xml.gz").isFile();
        if (isPopulationZipped) {
            config.plans().setInputFile(input + "population.xml.gz");
        } else {
            config.plans().setInputFile(input + "population.xml");
        }

        config.transit().setTransitScheduleFile(input + "transitSchedule.xml");
        config.transit().setVehiclesFile(input + "vehicles.xml");

        if (output != null) {
            config.controler().setOutputDirectory(output);
        }

        //config.controler().setLastIteration(10);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        return scenario;
    }


    private static class Initializer implements StartupListener {

        @Override
        public void notifyStartup(StartupEvent event) {

            Controler controler = event.getControler();

            // create a plot containing the mean travel times
            Set<String> transportModes = new HashSet<String>();
            transportModes.add(TransportMode.car);
            transportModes.add(TransportMode.pt);
            transportModes.add(TransportMode.walk);
        }
    }

}
