/**
 * 
 */
package playground.joel.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.Pretty;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;
import playground.joel.analysis.utils.ThroughputCalculator;

/** @author Claudio Ruch */
public class PerformanceFleetSizeCalculatorClean {

    final VirtualNetwork virtualNetwork;
    final TravelData travelData;
    final int timeSteps;
    final int dt;
    final int maxVehicles;
    final int vehicleSteps = 20;
    final int vehicleBins;
    final int numVNode;
    final int numVLink;

    public static void main(String[] args) throws Exception {
        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);
        PerformanceFleetSizeCalculatorClean performcalc = new PerformanceFleetSizeCalculatorClean(virtualNetwork, travelData, 2000);
        performcalc.calcAvailab();
    }

    public PerformanceFleetSizeCalculatorClean(VirtualNetwork virtualNetwork, TravelData travelData, int maxVehicles) {
        this.virtualNetwork = virtualNetwork;
        this.travelData = travelData;
        this.timeSteps = this.travelData.getNumbertimeSteps();
        this.dt = this.travelData.getdt();
        this.maxVehicles = maxVehicles;
        this.vehicleBins = this.maxVehicles / TravelDataUtils.greatestNonRestDt(vehicleSteps, this.maxVehicles);
        this.numVNode = virtualNetwork.getvNodesCount();
        this.numVLink = numVNode * numVNode - numVNode;
        GlobalAssert.that(numVLink == virtualNetwork.getvLinksCount());
    }

    void calcAvailab() throws InterruptedException {

        for (int i = 0; i < timeSteps; ++i) {
            
            Tensor pij = travelData.getpijPSFforTime(i * dt);
            System.out.println("pij = " + Pretty.of(pij));

            Tensor tp = ThroughputCalculator.getRelativeThroughputOfi(travelData.getpijPSFforTime(i * dt));
            System.out.println("tp = " + Pretty.of(tp));
        }

        // Tensor srVeh = Tensors.empty();
        // Tensor tp = Tensors.empty();
        // MeanValueAnalysis mva = new MeanValueAnalysis(srVeh, tp,
        // virtualNetwork.getvNodesCount());

    }

}
