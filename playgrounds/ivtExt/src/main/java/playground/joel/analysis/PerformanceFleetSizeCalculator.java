package playground.joel.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.LinearSolve;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.utils.GlobalAssert;

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
        // size = numVirtualNodes; for some reason not equal to the length of alphaij etc.

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
                Tensor Ami = (MVA.getL(vehicleStep).pmul(MVA.getW(vehicleStep))).pmul(lambdai);
                Ak.append(Mean.of(Ami));
            }
            A.append(Ak);
            System.out.println("Availability at time: " + k*dt + " = " + Ak);
        }
        return A;
    }

    private Tensor getPsii(Tensor alphaij) {
        Tensor Psii = Tensors.empty();
        // GlobalAssert.that(alphaij.length() == size); for some reason not true
        for (int j = 0; j < size; j++) {
            Psii.append(Total.of(alphaij.get(j)));
        }
        return Psii;
    }

    private Tensor getPtilde(Tensor lambdai, Tensor alphaij, int time) {
        Tensor Psii = getPsii(alphaij);
        GlobalAssert.that(lambdai.length() == Psii.length());
        // calculate pi
        Tensor Pi = Tensors.empty();
        for (int i = 0; i < lambdai.length(); i++)
            Pi.append(Psii.Get(i).divide(Psii.Get(i).add(lambdai.Get(i))));

        // calculate pij
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
    }

}
