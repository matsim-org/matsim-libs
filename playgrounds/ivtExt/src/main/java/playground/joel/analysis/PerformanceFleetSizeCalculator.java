package playground.joel.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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

import ch.ethz.idsc.tensor.DecimalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;

/**
 * Claudio on July 17, 2017
 * 
 * Procedure is according to publication "Control of Robotic Mobility-On-Demand Systems: a Queueing-Theoretical Perspective" by Rick Zhang and Marco
 * Pavone, page 5 B. Computation of performance metrics
 * 
 * TODO: Note: numerical instabilities for very low arrival rates observed, where availabilities do not approach 1 indepenntly of the number of cars
 * added to the system. Therfore only timesteps are considered when more than 0.5 % of the peak arrivals happen. This is reasonable because e.g. in
 * Sioux falls with 45 min timestep the maximum number of arrivals is ~ 21,654, if only 0.5%of this, i.e. 100 arrive in 45 mins any reasonable fleet
 * for peak times will be able to serve with availability 1.
 * 
 */
public class PerformanceFleetSizeCalculator {
    final Network network;
    final VirtualNetwork virtualNetwork;
    final TravelData tData;
    final int numberTimeSteps;
    final int dt;
    final int dayduration = 108000;
    final int numVehicles;
    final int vehicleSteps;
    final int vehicleBins;
    final int numberRoads;
    final int numberStations;
    final double MINPERCENTAGE = 0.005;
    final double PEAKPERCENTAGE = 0.5;

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
                travelDataLoad, 5000, 10);
        Tensor Availabilities = performanceFleetSizeCalculator.calculateAvailabilities();
    }

    public PerformanceFleetSizeCalculator(Network networkIn, VirtualNetwork virtualNetworkIn, TravelData travelDataIn, int numVehiclesMaxIn,
            int vehicleStepsIn) {
        network = networkIn;
        virtualNetwork = virtualNetworkIn;
        tData = travelDataIn;
        numberTimeSteps = tData.getNumbertimeSteps();
        GlobalAssert.that(dayduration % numberTimeSteps == 0);
        dt = dayduration / numberTimeSteps;
        numVehicles = numVehiclesMaxIn;
        vehicleSteps = vehicleStepsIn;
        vehicleBins = numVehicles / TravelDataUtils.greatestNonRestDt(vehicleSteps, numVehicles);
        numberStations = virtualNetwork.getvNodesCount();
        numberRoads = numberStations * numberStations - numberStations;
        GlobalAssert.that(numberRoads == virtualNetwork.getvLinksCount());
    }

    public static Function<Scalar, Scalar> NICE = s -> Round.toMultipleOf(DecimalScalar.of(new BigDecimal("0.0001"))).apply(s);

    public Tensor calculateAvailabilities() throws InterruptedException {

        // find relevant time steps when more than MINPERCENTAGEARRIVAL of maximum arrivals happen
        List<Double> arrivalNumbers = new ArrayList<>();
        for (int k = 0; k < numberTimeSteps; ++k) {
            Tensor lambdaii = tData.getLambdaPSFforTime(k * dt);
            Tensor totalArrivalRate = Total.of(lambdaii);
            RealScalar arrivals = (RealScalar) totalArrivalRate.multiply(RealScalar.of(dt));
            arrivalNumbers.add(arrivals.number().doubleValue());
        }

        double maxArrival = Collections.max(arrivalNumbers);
        Set<Integer> relevantTimeSteps = new HashSet<Integer>();
        Set<Integer> irrelevantSteps = new HashSet<Integer>();
        Set<Integer> offpeakSteps = new HashSet<Integer>();
        Set<Integer> peakSteps = new HashSet<Integer>();

        for (int k = 0; k < numberTimeSteps; ++k) {
            if (arrivalNumbers.get(k) > MINPERCENTAGE * maxArrival) {
                relevantTimeSteps.add(k);
                if (arrivalNumbers.get(k) > PEAKPERCENTAGE * maxArrival) {
                    peakSteps.add(k);

                } else {
                    offpeakSteps.add(k);
                }
            } else {
                irrelevantSteps.add(k);
            }
        }

        System.out.println("peak steps : " + peakSteps);
        System.out.println("offpeak steps : " + offpeakSteps);
        System.out.println("not considered steps: " + irrelevantSteps);

        // Tensor of all availabilities(virtualnode, timestep, vehiclestep)
        // only the results for station virtual nodes are saved, not for road nodes
        Tensor a = Array.zeros(numberRoads, numberTimeSteps, vehicleBins + 1);

        // iterate through timesteps to compute availabilities
        for (int k : relevantTimeSteps) {

            // Step 1: Calculate the customer flows cf (i->j)
            Tensor lambdaii = tData.getLambdaPSFforTime(k * dt);
            Tensor pij = tData.getpijPSFforTime(k * dt);

            Tensor flowCust = Tensors.empty();
            for (int i = 0; i < Dimensions.of(lambdaii).get(0); ++i) {
                Tensor row = pij.get(i);
                Tensor rowUpdated = row.multiply(lambdaii.Get(i));
                flowCust.append(rowUpdated);
            }

            // Step 2: calculate the serviceRates as a function of the number of vehicles, first
            // service rates for stations, then for roads
            Tensor flowReb = tData.getAlphaijPSFforTime(k * dt);

            Tensor flowTot = flowCust.add(flowReb);
            Tensor pijTot = tData.normToRowStochastic(flowTot);

            Tensor serviceRateStations = Tensors.empty();
            for (int i = 0; i < Dimensions.of(lambdaii).get(0); ++i) {
                Tensor rebarrivali = Total.of(flowReb.get(i));
                serviceRateStations.append(rebarrivali.add(lambdaii.Get(i)));
            }

            Tensor serviceRateRoads = Array.zeros(numberStations, numberStations);
            for (int i = 0; i < numberStations; ++i) {
                for (int j = 0; j < numberStations; ++j) {
                    VirtualNode from = virtualNetwork.getVirtualNode(i);
                    VirtualNode to = virtualNetwork.getVirtualNode(j);
                    Scalar serviceRate = RealScalar.ZERO;
                    if (i != j) {
                        VirtualLink vLink = virtualNetwork.getVirtualLink(from, to);
                        serviceRate = RealScalar.of(1.0 / vLink.getTtime());
                    }
                    serviceRateRoads.set(serviceRate, i, j);
                }
            }

            Tensor serviceRateRoadsVector = Tensors.empty();

            for (int i = 0; i < Dimensions.of(serviceRateRoads).get(0); ++i) {
                for (int j = 0; j < Dimensions.of(serviceRateRoads).get(1); ++j) {
                    if (i != j) {
                        serviceRateRoadsVector.append(serviceRateRoads.Get(i, j));
                    }
                }
            }

            Tensor serviceRatesPerVehicles = Tensors.empty();

            for (int i = 0; i <= numVehicles; ++i) {
                Tensor lineupd = serviceRateStations.copy();
                Tensor roadServ = serviceRateRoadsVector.multiply(RealScalar.of(i));
                roadServ.flatten(-1).forEach(v -> lineupd.append(v));
                serviceRatesPerVehicles.append(lineupd);
            }

            // Step 3: calculate the throughput
            Tensor throughputStations = PerformanceFleetSizeCalculator.getRelativeThroughputOfi(pijTot);
            Tensor throughputRoads = Array.zeros(numberStations, numberStations);
            for (int i = 0; i < numberStations; ++i) {
                for (int j = 0; j < numberStations; ++j) {
                    Tensor tpij = (RealScalar) pijTot.Get(i, j).multiply(throughputStations.Get(i));
                    throughputRoads.set(tpij, i, j);
                }
            }

            Tensor throughPut = throughputStations.copy();

            for (int i = 0; i < Dimensions.of(throughputRoads).get(0); ++i) {
                for (int j = 0; j < Dimensions.of(throughputRoads).get(1); ++j) {
                    if (i != j) {
                        throughPut.append(throughputRoads.Get(i, j));
                    }
                }
            }

            // Step 4: conduct mean value analysis
            MeanValueAnalysis mva = new MeanValueAnalysis(numVehicles, serviceRatesPerVehicles, throughPut);

            // Step 5: compute availabilities
            Tensor Ak = Tensors.empty();

            for (int vehBin = 0; vehBin <= vehicleBins; ++vehBin) {
                Tensor Lveh = mva.getL(vehBin * vehicleSteps);
                Tensor Wveh = mva.getW(vehBin * vehicleSteps);

                // availabilities at timestep k with v vehicles for every virtualNode
                Tensor Aveh = (Lveh.pmul(InvertUnlessZero.of(Wveh))).pmul(InvertUnlessZero.of(serviceRatesPerVehicles.get(vehBin)));

                for (int i = 0; i < numberStations; ++i) {
                    a.set(Aveh.Get(i), i, k, vehBin);
                }
            }
        }

        // based on the a matrix calculate overall availabilities as a function of the number of vehicles
        Tensor meanByVehicles = calcMeanByVehicles(a, offpeakSteps);
        Tensor meanByVehiclesPeak = calcMeanByVehicles(a, peakSteps);

        try {
            AnalyzeAll.saveFile(a, "availabilitiesFull");
            AnalyzeAll.saveFile(meanByVehicles, "availabilitiesOffPeak");
            AnalyzeAll.saveFile(meanByVehiclesPeak, "availabilitiesPeak");
            plot(meanByVehicles, meanByVehiclesPeak, vehicleSteps);
        } catch (Exception e) {
            System.out.println("Error saving the availabilities");
        }

        return meanByVehicles;
    }

    Tensor calcMeanByVehicles(Tensor a, Set<Integer> steps) {
        Tensor meanByVehicles = Array.zeros(vehicleBins + 1);
        int numElem = 0;
        for (int i = 0; i < numberStations; ++i) {
            for (int j : steps) {
                Tensor aByVehicle = a.get(i, j, Tensor.ALL);
                if (!(Norm.ofVector(aByVehicle, 2).number().doubleValue() == 0.0)) {
                    meanByVehicles = meanByVehicles.add(aByVehicle);
                    ++numElem;
                }
            }
        }

        meanByVehicles = meanByVehicles.multiply(RealScalar.of(1.0 / numElem));
        return meanByVehicles;

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
        Tensor relativeThroughput = Chop._10.of(NullSpace.usingSvd(IminusPki)); // chopped to avoid numerical errors

        // if no solution found, return empty Tensor.
        if (Dimensions.of(relativeThroughput).get(0) == 0) {
            return Array.zeros(size);
        } else {
            // if there are multiple solutions, take the feasible one, i.e. all entries same sign
            int selectedSolution = -1;
            boolean isNegativeScaled = false;

            System.out.println("multiple throughput solutions: selecting first with equal signs");

            for (int i = 0; i < Dimensions.of(relativeThroughput).get(0); ++i) {
                int positiveSigns = 0;
                int negativeSigns = 0;
                int zeroSigns = 0;
                for (int j = 0; j < Dimensions.of(relativeThroughput).get(1); ++j) {
                    if (relativeThroughput.Get(i, j).number().doubleValue() > 0.0)
                        ++positiveSigns;
                    if (relativeThroughput.Get(i, j).number().doubleValue() < 0.0)
                        ++negativeSigns;
                    if (relativeThroughput.Get(i, j).number().doubleValue() == 0.0)
                        ++zeroSigns;
                }
                GlobalAssert.that(positiveSigns + negativeSigns + zeroSigns == size);
                if (positiveSigns + zeroSigns == size || negativeSigns + zeroSigns == size) {
                    selectedSolution = i;
                    if (negativeSigns > 0) {
                        isNegativeScaled = true;
                    }
                    break;
                }
            }
            if(selectedSolution == -1){
                System.out.println("no solution found");
                System.out.println(transitionProb);
                
                GlobalAssert.that(false);                
            }
            
            


            Tensor selectedRelativeThroughput = relativeThroughput.get(selectedSolution);

            // make sure relative throughput positive in case solver returns negative throughput
            if (isNegativeScaled) {
                selectedRelativeThroughput = selectedRelativeThroughput.multiply(RealScalar.of(-1));
            }

            return selectedRelativeThroughput;

        }
    }

    private void plot(Tensor values, Tensor valuesPeak, int vehicleSteps) throws Exception {
        XYSeries series = new XYSeries("Availability");
        for (int i = 0; i < Dimensions.of(values).get(0); ++i) {
            series.add(i * vehicleSteps, values.Get(i).number().doubleValue());
        }
        XYSeries seriesPeak = new XYSeries("Availability Peak");
        for (int i = 0; i < Dimensions.of(values).get(0); ++i) {
            seriesPeak.add(i * vehicleSteps, valuesPeak.Get(i).number().doubleValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(seriesPeak);

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
        DiagramCreator.savePlot(AnalyzeAll.RELATIVE_DIRECTORY, "availbilitiesByNumberVehicles", timechart, width, height);
    }

}
