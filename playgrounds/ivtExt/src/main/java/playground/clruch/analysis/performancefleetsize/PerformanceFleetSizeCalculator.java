/**
 * 
 */
package playground.clruch.analysis.performancefleetsize;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.math.MeanValueAnalysis;

import ch.ethz.idsc.queuey.math.TensorOperations;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.ArrayQ;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.N;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.ScenarioOptions;
import playground.clruch.analysis.AnalyzeAll;

import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveldata.TravelDataUtils;
import playground.joel.analysis.utils.ThroughputCalculator;

/** @author Claudio Ruch */
public class PerformanceFleetSizeCalculator implements Serializable {

    final int timeSteps;
    final int dt;
    final int maxVehicles;
    final int vehicleSteps = 50;
    final double PEAKPERCENTAGE = 0.5;
    final int avSpeed = 1;
    final int vehicleBins;
    final int numVNode;
    final int numVLink;
    private Tensor meanByVehiclesPeak;
    private Tensor meanByVehiclesOffPeak;

    public PerformanceFleetSizeCalculator(VirtualNetwork<Link> virtualNetwork, TravelData travelData, int maxVehicles) throws InterruptedException {
        this.timeSteps = travelData.getNumbertimeSteps();
        this.dt = travelData.getdt();
        this.maxVehicles = maxVehicles;
        this.vehicleBins = this.maxVehicles / TravelDataUtils.greatestNonRestDt(vehicleSteps, this.maxVehicles);
        this.numVNode = virtualNetwork.getvNodesCount();
        this.numVLink = numVNode * numVNode - numVNode;
        GlobalAssert.that(numVLink == virtualNetwork.getvLinksCount());
        meanByVehiclesPeak = Tensors.empty();
        meanByVehiclesOffPeak = Tensors.empty();
        calcAvailab(virtualNetwork, travelData);
    }

    private void calcAvailab(VirtualNetwork<Link> virtualNetwork, TravelData travelData) throws InterruptedException {

        Tensor a = Tensors.empty();

        Set<Integer> offPeakSteps = new HashSet<Integer>();
        for (int i = 0; i < timeSteps; ++i)
            offPeakSteps.add(i);

        Set<Integer> peakSteps = PerformanceFleetSizeUtils.calcPeakSteps(virtualNetwork, travelData, PEAKPERCENTAGE);
        peakSteps.stream().forEach(v -> offPeakSteps.remove(v));

        System.out.println("peak steps: " + peakSteps);
        System.out.println("offpeak steps: " + offPeakSteps);

        Tensor roadServ1 = PerformanceFleetSizeUtils.calcSRRoad(virtualNetwork, avSpeed);

        Tensor atemp = Tensors.empty();
        for (int i = 0; i < timeSteps; ++i) {
            System.out.println("i = " + i);

            // 1) calculate the throughput of the network
            Tensor flowReb = travelData.getAlphaijPSFforTime(i * dt);
            Tensor flowCust = travelData.getlambdaijPSFforTime(i * dt);
            Tensor flowTot = flowCust.add(flowReb);
            // TODO look at flowTot and see if some properties can be asserted.
            Tensor flowTotLarge = flowTot.multiply(RealScalar.of(1000000)).map(Round.FUNCTION);
            Tensor pij = TensorOperations.normToRowStochastic(flowTotLarge);
            Tensor tpStation = ThroughputCalculator.getRelativeThroughputOfi(pij);
            Tensor tp = PerformanceFleetSizeUtils.calctpTot(tpStation, pij);

            // 2) calculate the service rates
            Tensor srStations = Total.of(Transpose.of(flowTot));
            Tensor srTotVehicles = Tensors.empty();
            for (int j = 0; j <= maxVehicles; ++j) {
                Tensor lineupd = srStations.copy();
                Tensor roadServ = roadServ1.multiply(RealScalar.of(j));
                roadServ.flatten(-1).forEach(v -> lineupd.append(v));
                srTotVehicles.append(lineupd);
            }

            // 3) perform mean value analysis and print
            Tensor srTotVehiclesNum = N.of(srTotVehicles);
            Tensor tpNum = N.of(tp);

            int numdisjointSol = Dimensions.of(tpNum).get(0);
            Tensor AvehRef = Array.zeros(maxVehicles + 1, numVNode);
            for (int sol = 0; sol < numdisjointSol; ++sol) {
                Tensor tpNumsol = tpNum.get(sol);
                MeanValueAnalysis mvasol = new MeanValueAnalysis(srTotVehiclesNum, tpNumsol, numVNode, 10);
                Tensor AvehRefSol = Transpose.of(Transpose.of(mvasol.getA()));
                AvehRef = AvehRef.add(AvehRefSol);
            }

            for (int sol = 0; sol < numdisjointSol; ++sol) {
                System.out.println("throughput     = " + tpNum.get(sol).extract(0, numVNode).map(Round._2));
            }
            System.out.println("availabilities = " + AvehRef.get(maxVehicles).map(Round._2));

            // 4) save results
            atemp.append(AvehRef);
            GlobalAssert.that(ArrayQ.of(atemp));
        }
        a = Transpose.of(atemp, 1, 2, 0);
        meanByVehiclesPeak = PerformanceFleetSizeUtils.calcMeanByVehicles(a, peakSteps);
        meanByVehiclesOffPeak = PerformanceFleetSizeUtils.calcMeanByVehicles(a, offPeakSteps);

    }

    public void saveAndPlot(String dataFolderName, File relativeDirectory) {
        try {
            AnalyzeAll.saveFile(meanByVehiclesOffPeak, "availabilitiesOffPeak", dataFolderName);
            AnalyzeAll.saveFile(meanByVehiclesPeak, "availabilitiesPeak", dataFolderName);
            PerformanceFleetSizeUtils.plot(meanByVehiclesOffPeak, meanByVehiclesPeak, relativeDirectory);
        } catch (Exception e) {
            System.out.println("Error saving the availabilities");
            e.printStackTrace(System.out);
        }
    }

    public static void main(String[] args) throws Exception {
        String dataFolderName = args[0]; // e.g. "output/data"
        File relativeDirectory = new File(dataFolderName);

        File workingDirectory = new File("").getCanonicalFile();
        Properties simOptions = ScenarioOptions.load(workingDirectory);
        File configFile = new File(workingDirectory, simOptions.getProperty("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);
        int maxNumberRoboTaxis = (int) (scenario.getPopulation().getPersons().size());
        long startTime = System.currentTimeMillis();
        PerformanceFleetSizeCalculator performcalc = new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberRoboTaxis);
        performcalc.saveAndPlot(dataFolderName, relativeDirectory);
        System.out.println("runtime: " + ((System.currentTimeMillis() - startTime) / 1000.0) + " [s]");
    }

}
