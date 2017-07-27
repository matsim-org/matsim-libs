/**
 * 
 */
package playground.joel.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.N;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.tensorUtils.TensorOperations;
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
    final int vehicleSteps = 50;
    final double PEAKPERCENTAGE = 0.7;
    final int avSpeed = 33;
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

        Set<Integer> offPeakSteps = new HashSet();
        for (int i = 0; i < timeSteps; ++i)
            offPeakSteps.add(i);
        
        Set<Integer> peakSteps = PerformanceFleetSizeUtils.calcPeakSteps(virtualNetwork, travelData, PEAKPERCENTAGE);
        peakSteps.stream().forEach(v->offPeakSteps.remove(v));
        
        System.out.println("peak steps: " + peakSteps);
        System.out.println("offpeak steps: " + offPeakSteps);
        
        

        Tensor a = Array.zeros(numVNode, timeSteps, vehicleBins + 1);
        Tensor atemp = Array.zeros(timeSteps, vehicleBins + 1, numVNode); // Array.zeros(numberTimeSteps,
        for (int i = 24; i < timeSteps; ++i) {
            System.out.println("i = "+ i);
            // 1) calculate the throughput of the network
            Tensor flowReb = travelData.getAlphaijPSFforTime(i * dt);
            Tensor flowCust = travelData.getlambdaijPSFforTime(i * dt);
            Tensor flowTot = flowCust.add(flowReb);
            // TODO look at flowTot and see if some properties can be asserted.
            Tensor flowTotLarge = flowTot.multiply(RealScalar.of(1000000)).map(Round.FUNCTION);
            Tensor pij = TensorOperations.normToRowStochastic(flowTotLarge);
            Tensor tpStation = ThroughputCalculator.getRelativeThroughputOfi(pij);
            System.out.println("tpStation = " + tpStation.map(Round._2));
            Tensor tpStationTot = ThroughputCalculator.combineDisjoint(tpStation);
            
            System.out.println("tpStationTot = " + tpStationTot.map(Round._2));
            Tensor tp = PerformanceFleetSizeUtils.calctpTot(tpStationTot, pij);
            System.out.println("tp = " + tp.map(Round._2));

            // 2) calculate the service rates
            Tensor srStations = Total.of(Transpose.of(flowTot));
            Tensor srTotVehicles = Tensors.empty();
            for (int j = 0; j <= maxVehicles; ++j) {
                Tensor lineupd = srStations.copy();
                Tensor roadServ = PerformanceFleetSizeUtils.calcSRRoad(j, virtualNetwork, avSpeed);
                roadServ.flatten(-1).forEach(v -> lineupd.append(v));
                srTotVehicles.append(lineupd);
            }
            // srTotVehicles = srTotVehicles.multiply(RealScalar.of(1000000)).map(Round.FUNCTION);
            // srTotVehicles = srTotVehicles.multiply(RealScalar.of(1000000).invert());

            // 3) perform mean value analysis
            Tensor srTotVehiclesNum = N.of(srTotVehicles);
            Tensor tpNum = N.of(tp);
            MeanValueAnalysis mva = new MeanValueAnalysis(srTotVehiclesNum, tpNum, numVNode);

            System.out.println("tps = " + tpStationTot.get(0).map(Round._2));
            System.out.println("as_ = " + mva.getA(maxVehicles).extract(0, numVNode).map(Round._2));

            // 4) save results
            Tensor mat = Tensors.empty();
            for (int vehBin = 0; vehBin <= vehicleBins; ++vehBin) {
                Tensor AvehRef = mva.getA(vehBin * vehicleSteps);
                mat.append(AvehRef.extract(0, numVNode));
            }
            atemp.set(mat, i);
        }
        a = Transpose.of(atemp, 1, 2, 0);
        Tensor meanByVehiclesPeak = PerformanceFleetSizeUtils.calcMeanByVehicles(a, peakSteps);
        Tensor meanByVehiclesOffPeak = PerformanceFleetSizeUtils.calcMeanByVehicles(a, offPeakSteps);

        try {
            AnalyzeAll.saveFile(a, "availabilitiesFull");
            AnalyzeAll.saveFile(meanByVehiclesOffPeak, "availabilitiesOffPeak");
            AnalyzeAll.saveFile(meanByVehiclesPeak, "availabilitiesPeak");
            PerformanceFleetSizeUtils.plot(meanByVehiclesOffPeak, meanByVehiclesPeak, vehicleSteps);
        } catch (Exception e) {
            System.out.println("Error saving the availabilities");
            e.printStackTrace(System.out);
        }
    }

}
