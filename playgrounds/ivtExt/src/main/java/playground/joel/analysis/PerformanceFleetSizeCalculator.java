package playground.joel.analysis;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.TensorMap;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.LinearSolve;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;

import java.awt.*;
import java.io.File;
import java.util.Collections;

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

    public static void main(String[] args) throws Exception {
        // number of timesteps in day
        int samples = 10;
        
        
        // load needed information
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        VirtualNetwork virtualNetworkLoad  = VirtualNetworkGet.readDefault(scenario.getNetwork());
        final File virtualnetworkDir = new File("virtualNetwork");
        TravelData travelDataLoad = TravelDataIO.fromByte(network, virtualNetworkLoad, new File(virtualnetworkDir, "travelData"));

        
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(network, virtualNetworkLoad, travelDataLoad, 108000/samples);

        Tensor Availabilities = performanceFleetSizeCalculator.calculateAvailabilities();
        System.out.println("A " + Dimensions.of(Availabilities) + " = ");
        System.out.println(Pretty.of(Availabilities));

        // OLD MAIN:
        // int samples = 10;
        //
        // DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        // dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        //
        // File configFile = new File(args[0]);
        // Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
        // new BlackListedTimeAllocationMutatorConfigGroup());
        // Scenario scenario = ScenarioUtils.loadScenario(config);
        // Network network = scenario.getNetwork();
        // Population population = scenario.getPopulation();
        // TheApocalypse.decimatesThe(population).toNoMoreThan(1000).people();
        //
        // PerformanceFleetSizeCalculator performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(network, //
        // population, 10, 108000 / samples);
        //
        // Tensor Availabilities = performanceFleetSizeCalculator.calculateAvailabilities();
        // System.out.println("A " + Dimensions.of(Availabilities) + " = ");
        // System.out.println(Pretty.of(Availabilities));
    }



    public PerformanceFleetSizeCalculator(Network networkIn, VirtualNetwork virtualNetworkIn, TravelData travelDataIn, int dtIn) {
        network = networkIn;
        virtualNetwork = virtualNetworkIn;
        tData = travelDataIn;
        // ensure that dayduration / timeInterval is integer value
        dt = TravelDataUtils.greatestNonRestDt(dtIn, dayduration);
        numberTimeSteps = dayduration / dt;
    }

    public Tensor calculateAvailabilities() {
        int numVehicles = 100;
        int vehicleSteps = 10;
        Tensor A = Tensors.empty(); // Tensor of all availabilities(timestep, vehiclestep)
        for (int k = 0; k < numberTimeSteps; ++k) {

            // TODO: need scaling with dt?
            Tensor betaij = tData.normToRowStochastic(tData.getAlphaijforTime(k * dt)).multiply(RealScalar.of(dt));
            System.out.println("betaij " + Dimensions.of(betaij) + " = ");
            System.out.println(Pretty.of(betaij));
            Tensor lambdai = tData.getLambdaforTime(k * dt).multiply(RealScalar.of(dt));
            System.out.println("lambdai " + Dimensions.of(lambdai) + " = ");
            System.out.println(Pretty.of(lambdai));
            size = betaij.length();

            // calculate pii, relative throuputs
            Tensor pii = getPii(lambdai, betaij, k * dt);

            // calculate availabilities
            Tensor Ak = Tensors.empty();
            Tensor mi = lambdai.add(getPsii(betaij));
            MeanValueAnalysis MVA = new MeanValueAnalysis(numVehicles, mi, pii);
            MVA.perform();
            // TODO: replace loop with vector operations
            for (int vehicleStep = 0; vehicleStep < vehicleSteps; vehicleStep++) {
                // availabilities at all nodes at a certain level of vehicles
                Tensor Ami = ((MVA.getW(numVehicles * vehicleStep / vehicleSteps).pmul(lambdai)) //
                        .map(InvertUnlessZero.function)) //
                                .pmul(MVA.getL(numVehicles * vehicleStep / vehicleSteps));
                Ak.append(Mean.of(Ami));
            }
            A.append(Ak);
        }
        try {
            AnalyzeAll.saveFile(A, "availabilities");
            plot(A, numVehicles, vehicleSteps);
        } catch (Exception e) {
            System.out.println("Error saving the availabilities");
        }
        return A;
    }

    private Tensor getPsii(Tensor alphaij) {
        Tensor Psii = TensorMap.of(Total::of, alphaij, 1);
        return Psii;
    }

    private Tensor getPtilde(Tensor lambdai, Tensor alphaij, int time) {
        Tensor Psii = getPsii(alphaij);
        GlobalAssert.that(lambdai.length() == Psii.length());
        // calculate pi
        Tensor Pi = Tensors.empty();
        for (int i = 0; i < lambdai.length(); i++)
            if (Scalars.isZero(Psii.Get(i).add(lambdai.Get(i))))
                Pi.append(RealScalar.ZERO);
            else
                Pi.append(Psii.Get(i).divide(Psii.Get(i).add(lambdai.Get(i))));

        // calculate pijtilde
        Tensor pTilde = Tensors.empty();
        for (int i = 0; i < size; i++) {
            Tensor pisub = Tensors.empty();
            for (int j = 0; j < size; j++) {
                pisub.append((alphaij.Get(i, j).multiply(Pi.Get(i))).add( //
                        tData.getpijforTime(time, i, j).multiply(RealScalar.of(1).subtract(Pi.Get(i)))));
            }
            pTilde.append(pisub);
        }
        return pTilde;
    }

    private Tensor getPii(Tensor lambdai, Tensor alphaij, int time) {
        Tensor Ptilde = getPtilde(lambdai, alphaij, time);
        // TODO: find actual null space
        // a * x = x ---> (a - 1) * x = 0 ---> m * x = b
        return LinearSolve.any(Ptilde.subtract(IdentityMatrix.of(size)), Array.zeros(size));
        // return LinearSolve.of(Ptilde.subtract(IdentityMatrix.of(size)), Array.zeros(size)); // Tensor Runtime Exception
        // return NullSpace.usingSvd(Ptilde.subtract(IdentityMatrix.of(size))); // -> cast exception
        // return NullSpace.of(Ptilde.subtract(IdentityMatrix.of(size))); // -> {}, empty
    }

    private void plot(Tensor values, int numVehicles, int vehicleSteps) throws Exception {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int v = 0; v < values.get(0).length(); v++) {
            final TimeSeries series = new TimeSeries(numVehicles * v / vehicleSteps + " vehicles");
            for (int t = 0; t < values.length(); t++) {
                Second daytime = DiagramCreator.toTime(t * 108000 / values.length());
                series.add(daytime, values.get(t).Get(v).number().doubleValue());
            }
            dataset.addSeries(series);
        }

        JFreeChart timechart = ChartFactory.createTimeSeriesChart("Vehicle Availability", "Time", //
                "Availability", dataset, true, false, false);

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

    
    
    
    @Deprecated   // use other constructor that loads directly from file 
    public PerformanceFleetSizeCalculator(Network networkIn, Population populationIn, int numVirtualNodes, int dtIn) {
        Population population = populationIn;

        // create a network containing only car nodes
        System.out.println("number of links in original network: " + networkIn.getLinks().size());
        final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(networkIn);
        network = NetworkUtils.createNetwork();
        filter.filter(network, Collections.singleton("car"));
        System.out.println("number of links in car network: " + network.getLinks().size());

        // create virtualNetwork based on input network
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, true);

        // ensure that dayduration / timeInterval is integer value
        dt = TravelDataUtils.greatestNonRestDt(dtIn, dayduration);
        numberTimeSteps = dayduration / dt;

        tData = new TravelData(virtualNetwork, network, population, dt);

    }
    
    
}
