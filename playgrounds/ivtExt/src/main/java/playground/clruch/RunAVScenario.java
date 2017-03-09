package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.export.EventFileToProcessingXML;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/**
 * main entry point
 */
public class RunAVScenario {
    public static void main(String[] args) throws MalformedURLException {
        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        System.out.println("Population size:" + scenario.getPopulation().getPersons().values().size());



        // TODO clean up this input for both ConsensusDispatcherDFR and for LinearDispatcher

        // Debugging
        File linkWeightFile = new File(dir, "consensusWeights.xml");
        File virtualnetworkXML = new File(dir, "virtualNetwork.xml");

        /*
        ConsensusDispatcherDFR.Factory.virtualNetwork = VirtualNetworkLoader.fromXML(scenario.getNetwork(), virtualnetworkXML);
        ConsensusDispatcherDFR.Factory.travelTimes = LinkWeights.fillLinkWeights(linkWeightFile, ConsensusDispatcherDFR.Factory.virtualNetwork);

        LPFeedbackLIPDispatcher.Factory.virtualNetwork = VirtualNetworkLoader.fromXML(scenario.getNetwork(), virtualnetworkXML);
        LPFeedbackLIPDispatcher.Factory.travelTimes = LinkWeights.fillLinkWeights(linkWeightFile, LPFeedbackLIPDispatcher.Factory.virtualNetwork);
        */

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());

        controler.run();

        EventFileToProcessingXML.convert(dir);
    }
}
