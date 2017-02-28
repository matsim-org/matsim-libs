package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;


import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.dispatcher.ConsensusDispatcher;
import playground.clruch.dispatcher.LPFeedbackLIPDispatcher;
import playground.clruch.export.EventFileToProcessingXML;
import playground.clruch.netdata.LinkWeights;
import playground.clruch.netdata.VirtualNetworkLoader;
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

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        System.out.println("Population size:" + scenario.getPopulation().getPersons().values().size());


        // Debugging
        File linkWeightFile = new File(dir + "/consensusWeights.xml");
        File virtualnetworkXML = new File(dir + "/virtualNetwork.xml");
        /*
        ConsensusDispatcher.Factory.virtualNetwork = VirtualNetworkLoader.fromXML(scenario.getNetwork(), virtualnetworkXML);
        ConsensusDispatcher.Factory.linkWeights = LinkWeights.fillLinkWeights(linkWeightFile,ConsensusDispatcher.Factory.virtualNetwork);
        */
        LPFeedbackLIPDispatcher.Factory.virtualNetwork = VirtualNetworkLoader.fromXML(scenario.getNetwork(), virtualnetworkXML);
        LPFeedbackLIPDispatcher.Factory.travelTimes = LinkWeights.fillLinkWeights(linkWeightFile, LPFeedbackLIPDispatcher.Factory.virtualNetwork);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());

        controler.run();

        EventFileToProcessingXML.convert(dir);
    }
}

