package playground.joel.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;

/**
 * Created by Joel on 11.07.2017. Debugged, finished and overall reworked by Claudio on July 17, 2017
 * 
 * Procedure is according to publication "Control of Robotic Mobility-On-Demand Systems: a Queueing-Theoretical Perspective" by Rick Zhang and Marco
 * Pavone, page 5 B. Computation of performance metrics
 */
public class PerformanceFleetSizeCalculator {
    final Network network;
    final VirtualNetwork virtualNetwork;
    final TravelData tData;
    final int numberTimeSteps;
    final int dt;
    final int dayduration = 108000;
    int size;
    final int numVehicles;
    final int vehicleSteps;


    public static void main(String[] args) throws Exception {


        // load needed information
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        final File virtualnetworkDir = new File("virtualNetwork");
        VirtualNetwork virtualNetworkLoad = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelDataLoad = TravelDataIO.fromByte(network, virtualNetworkLoad, new File(virtualnetworkDir, "travelData"));

        // call the performancefleetsizecalculator and calculate the availabilities
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(network, virtualNetworkLoad,
                travelDataLoad, travelDataLoad.getNumbertimeSteps(), 5000,50);
        Tensor Availabilities = performanceFleetSizeCalculator.calculateAvailabilities();
    }

    public PerformanceFleetSizeCalculator(Network networkIn, VirtualNetwork virtualNetworkIn, TravelData travelDataIn, int numberTimeStepsIn, int numVehiclesMaxIn, int vehicleStepsIn) {
        network = networkIn;
        virtualNetwork = virtualNetworkIn;
        tData = travelDataIn;
        numberTimeSteps = numberTimeStepsIn;
        GlobalAssert.that(dayduration%numberTimeStepsIn ==0);
        dt = dayduration/numberTimeStepsIn;
        size = virtualNetwork.getvNodesCount();
        numVehicles = numVehiclesMaxIn;
        vehicleSteps = vehicleStepsIn;
    }

    public Tensor calculateAvailabilities() throws InterruptedException {
        int vehicleBins = numVehicles/TravelDataUtils.greatestNonRestDt(vehicleSteps, numVehicles);
        System.out.println("number of vehicle  bins = " + vehicleBins);
        
        

        // Tensor of all availabilities(virtualnode, timestep, vehiclestep)
        Tensor A = Array.zeros(virtualNetwork.getvNodesCount(), numberTimeSteps, vehicleBins+1);

        // iterate through timesteps to compute availabilities
        for (int k = 0; k < numberTimeSteps; ++k) {
            // Step 1: Calculate the customer flows cf (i->j)
            Tensor lambdaii = tData.getLambdaPSFforTime(k * dt);
            Tensor pij = tData.getpijPSFforTime(k * dt);

            Tensor flowCust = Tensors.empty();
            for (int i = 0; i < Dimensions.of(lambdaii).get(0); ++i) {
                Tensor row = pij.get(i);
                Tensor rowUpdated = row.multiply(lambdaii.Get(i));
                flowCust.append(rowUpdated);
            }

            // Step 2: calculate the total flow and transition probabilities
            // for customer and rebalancing flows, calculate the overall arrival rates
            Tensor flowReb = tData.getAlphaijPSFforTime(k * dt);
            Tensor flowTot = flowCust.add(flowReb);
            Tensor pijTot = tData.normToRowStochastic(flowTot);

            Tensor arrivalTot = Tensors.empty();
            for (int i = 0; i < Dimensions.of(lambdaii).get(0); ++i) {
                Tensor rebarrivali = Total.of(flowReb.get(i));
                arrivalTot.append(rebarrivali.add(lambdaii.Get(i)));
            }

            // Step 3: calculate the throughput
            Tensor throughput = PerformanceFleetSizeCalculator.getRelativeThroughputOfi(pijTot);

            // Step 4: conduct mean value analysis
            System.out.println("starting mean value analysis");
            MeanValueAnalysis mva = new MeanValueAnalysis(numVehicles, arrivalTot, throughput);
            System.out.println("ended mean value analysis");

            // Step 5: compute availabilities
            Tensor Ak = Tensors.empty();
            for (int vehBin = 0; vehBin <= vehicleBins; ++vehBin) {
                Tensor Lveh = mva.getL(vehBin*vehicleSteps);
                Tensor Wveh = mva.getW(vehBin*vehicleSteps);

                // availabilities at timestep k with v vehicles for every virtualNode
                Tensor Aveh = (Lveh.pmul(InvertUnlessZero.of(Wveh))).pmul(InvertUnlessZero.of(arrivalTot));

                for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                    A.set(Aveh.Get(i), i, k, vehBin);
                }
            }
        }

        // based on A calculate overall availabilities as a function of the number of vehicles
        System.out.println(Pretty.of(A));
        Tensor ATimeVehMean = Mean.of(A);
        Tensor ATimeVehMeanOnlyWithCustomer = Tensors.empty();

        for (int i = 0; i < numberTimeSteps; ++i) {
            Tensor row = ATimeVehMean.get(i);
            if (!(Norm.ofVector(row, 2).number().doubleValue() == 0.0)) {
                ATimeVehMeanOnlyWithCustomer.append(row);
            }
        }


        Tensor meanAvailabilityVehicle = Mean.of(ATimeVehMeanOnlyWithCustomer);

        try {
            AnalyzeAll.saveFile(meanAvailabilityVehicle, "availabilities");
            plot(meanAvailabilityVehicle, vehicleSteps);
        } catch (Exception e) {
            System.out.println("Error saving the availabilities");
        }

        return meanAvailabilityVehicle;
    }


    /**
     * 
     * @param transitionProb
     *            row-stochastic matrix where M(i,j) is transition probability from i to j
     * @return relative throughputs normed and positive
     * @throws InterruptedException
     */
    public static Tensor getRelativeThroughputOfi(Tensor transitionProb) throws InterruptedException {
        int size = Dimensions.of(transitionProb).get(0);
        Tensor IminusPki = IdentityMatrix.of(size).subtract(Transpose.of(transitionProb));
        Tensor relativeThroughput = NullSpace.usingSvd(IminusPki);

        // if no solution found, return empty Tensor.
        if (Dimensions.of(relativeThroughput).get(0) == 0) {
            return Array.zeros(size);
        } else {
            // make sure relative throughput positive in case solver returns negative throughput
            if (((RealScalar) relativeThroughput.get(0, 0)).number().doubleValue() < 0.0) {
                relativeThroughput = relativeThroughput.multiply(RealScalar.of(-1));
            }

            return relativeThroughput.get(0);

        }
    }

    private void plot(Tensor values, int vehicleSteps) throws Exception {
        XYSeries series = new XYSeries("Availability");
        for (int i = 0; i < Dimensions.of(values).get(0); ++i) {
            series.add(i * vehicleSteps, values.Get(i).number().doubleValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart timechart = ChartFactory.createXYLineChart("Vehicle Availability", "Number of Vehicles", //
                "Availability", dataset);

        // range and colors of the background/grid
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        // line thickness
        for (int k = 0; k < values.length(); k++) {
            timechart.getXYPlot().getRenderer().setSeriesStroke(k, new BasicStroke(2.0f));
        }

        // set text fonts
        timechart.getTitle().setFont(DiagramCreator.titleFont);
        timechart.getXYPlot().getDomainAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getRangeAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getDomainAxis().setTickLabelFont(DiagramCreator.tickFont);
        timechart.getXYPlot().getRangeAxis().setTickLabelFont(DiagramCreator.tickFont);

        // save plot as png
        int width = 1000; // Width of the image
        int height = 750; // Height of the image
        DiagramCreator.savePlot(AnalyzeAll.RELATIVE_DIRECTORY, "availabilitiesByTime", timechart, width, height);
    }

}

