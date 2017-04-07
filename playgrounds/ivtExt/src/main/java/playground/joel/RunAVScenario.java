package playground.joel;

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

import playground.clruch.export.EventFileToProcessingXML;
import playground.clruch.prep.TheApocalypse;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

import playground.joel.data.EventFileToDataXML;


/**
 * main entry point
 */
public class RunAVScenario {
    public static void main(String[] args) throws MalformedURLException {
        File configFile = new File("C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_03_22_Sioux_Hungarian_check1av/av_config.xml");
        final File dir = configFile.getParentFile();

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        final Population population = scenario.getPopulation();

        TheApocalypse.decimatesThe(population).toNoMoreThan(5000).people();

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());

        controler.run();

        EventFileToProcessingXML.convert(dir);
        EventFileToDataXML.convert(dir);
    }
}

