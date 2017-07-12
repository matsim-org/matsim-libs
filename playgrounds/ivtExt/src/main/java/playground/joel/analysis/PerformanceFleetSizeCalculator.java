package playground.joel.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.TensorMap;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.LinearSolve;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
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
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;

import java.io.File;
import java.util.Collections;


/**
 * Created by Joel on 11.07.2017.
 */
public class PerformanceFleetSizeCalculator {
    final Network network;
    final Population population;
    final VirtualNetwork virtualNetwork;
    final TravelData tData;
    int size;
    final int numberTimeSteps;
    final int dt;

    public static void main(String[] args) throws Exception {
        // for test purpose only
        int samples = 30;

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        TheApocalypse.decimatesThe(population).toNoMoreThan(100).people();

        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = new PerformanceFleetSizeCalculator(network, //
                population, 40, 108000 / samples);

        Tensor Availabilities = performanceFleetSizeCalculator.calculateAvailabilities(10);
        System.out.println("A " + Dimensions.of(Availabilities) + " = " + Availabilities);

    }

    public PerformanceFleetSizeCalculator(Network networkIn, Population populationIn, int numVirtualNodes, int dtIn) {
        population = populationIn;

        // create a network containing only car nodes
        System.out.println("number of links in original network: " + networkIn.getLinks().size());
        final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(networkIn);
        network = NetworkUtils.createNetwork();
        filter.filter(network, Collections.singleton("car"));
        System.out.println("number of links in car network: " + network.getLinks().size());

        // create virtualNetwork based on input network
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, true);

        int dayduration = 108000;
        // ensure that dayduration / timeInterval is integer value
        dt = TravelDataUtils.greatestNonRestDt(dtIn, dayduration);
        numberTimeSteps = dayduration / dt;

        tData = new TravelData(virtualNetwork, network, population, dt);

    }

    public Tensor calculateAvailabilities(int numVehicles) {
        int vehicleSteps = 10;
        Tensor A = Tensors.empty(); // Tensor of all availabilities(timestep, vehiclestep)
        for (int k = 0; k < numberTimeSteps; ++k) {

            // TODO: need scaling with dt?
            Tensor alphaij = tData.normToRowStochastic(tData.getAlphaijforTime(k * dt)).multiply(RealScalar.of(dt));
            Tensor lambdai = tData.getLambdaforTime(k * dt).multiply(RealScalar.of(dt));
            size = alphaij.length();

            // calculate pii, relative throuputs
            Tensor pii = getPii(lambdai, alphaij, k * dt);

            // calculate availabilities
            Tensor Ak = Tensors.empty();
            Tensor mi = lambdai.add(getPsii(alphaij));
            MeanValueAnalysis MVA = new MeanValueAnalysis(numVehicles, mi, pii);
            MVA.perform();
            for (int vehicleStep = 0; vehicleStep < vehicleSteps; vehicleStep++) {
                // availabilities at all nodes at a certain level of vehicles
                Tensor Ami = (MVA.getL(numVehicles*vehicleStep/vehicleSteps). //
                        pmul(MVA.getW(numVehicles*vehicleStep/vehicleSteps))).pmul(lambdai);
                Ak.append(Mean.of(Ami));
            }
            A.append(Ak);
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
            if (Scalars.isZero(Psii.Get(i).add(lambdai.Get(i)))) Pi.append(RealScalar.ZERO);
            else Pi.append(Psii.Get(i).divide(Psii.Get(i).add(lambdai.Get(i))));

        // calculate pijtilde
        Tensor pTilde = Tensors.empty();
        for (int i = 0; i < size; i++) {
            Tensor pisub = Tensors.empty();
            for (int j = 0; j < size; j++) {
                pisub.append( (alphaij.Get(i,j).multiply(Pi.Get(i))).add( //
                        tData.getpijforTime(time, i, j).multiply(RealScalar.of(1).subtract(Pi.Get(i)))) );
            }
            pTilde.append(pisub);
        }
        return pTilde;
    }

    private Tensor getPii(Tensor lambdai, Tensor alphaij, int time) {
        Tensor Ptilde = getPtilde(lambdai, alphaij, time);
        // a * x = x ---> (a - 1) * x = 0 ---> m * x = b
        return LinearSolve.any(Ptilde.subtract(IdentityMatrix.of(size)), Array.zeros(size));
        // return LinearSolve.of(Ptilde.subtract(IdentityMatrix.of(size)), Array.zeros(size)); // Tensor Runtime Exception
        // return NullSpace.usingSvd(Ptilde.subtract(IdentityMatrix.of(size))); // -> cast exception
        // return NullSpace.of(Ptilde.subtract(IdentityMatrix.of(size))); // -> {}, empty
    }

}
