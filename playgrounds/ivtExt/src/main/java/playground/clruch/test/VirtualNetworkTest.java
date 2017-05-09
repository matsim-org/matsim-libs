package playground.clruch.test;

import java.io.File;
import java.util.Map;

import org.gnu.glpk.GLPKConstants;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.vLinkDataReader;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.config.AVConfigReader;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

public class VirtualNetworkTest {

    public static void main(String[] args) {
        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        AVConfig avConfig = new AVConfig();
        AVConfigReader reader = new AVConfigReader(avConfig);

        System.out.println(configFile.getParent());
        reader.readFile(configFile.getParent() + "/av.xml");

        File virtualnetworkXML = null;
        for (AVOperatorConfig oc : avConfig.getOperatorConfigs()) {
            AVDispatcherConfig dc = oc.getDispatcherConfig();
            AVGeneratorConfig gc = oc.getGeneratorConfig();
            virtualnetworkXML = new File(dc.getParams().get("virtualNetworkFile"));
        }

        System.out.println("" + virtualnetworkXML.getAbsoluteFile());
        VirtualNetwork virtualNetwork = VirtualNetworkIO.fromXML(scenario.getNetwork(), virtualnetworkXML);
        Map<VirtualLink, Double> travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkXML, virtualNetwork, "Ttime");

        int iter = 10;

        // Solving the LP with deleting
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iter; ++i) {
            LPVehicleRebalancing lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
            Tensor rhs = Array.zeros(virtualNetwork.getvNodesCount());
            Tensor rebalanceCount2 = lpVehicleRebalancing.solveUpdatedLP(rhs,GLPKConstants.GLP_LO);
            lpVehicleRebalancing.closeLP();
        }
        long estimatedTimeNewSetup = System.currentTimeMillis() - startTime;

        // Solving the LP without deleting
        startTime = System.currentTimeMillis();
        LPVehicleRebalancing lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        for (int i = 0; i < iter * 10000; ++i) {
            Tensor rhs = Array.zeros(virtualNetwork.getvNodesCount());
            Tensor rebalanceCount2 = lpVehicleRebalancing.solveUpdatedLP(rhs,GLPKConstants.GLP_LO);
        }
        lpVehicleRebalancing.closeLP();
        long estimatedTimeRHS = System.currentTimeMillis() - startTime;

        // Results
        System.out.println("Time with repeated setup: " + estimatedTimeNewSetup);
        System.out.println("Time with single setup: " + estimatedTimeRHS);
        System.out.println("Saved time: " + (1.0 - (double) estimatedTimeRHS / (double) estimatedTimeNewSetup) * 100 + "%");

    }
}
