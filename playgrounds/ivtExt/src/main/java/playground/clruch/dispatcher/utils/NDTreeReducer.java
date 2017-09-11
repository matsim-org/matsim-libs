/**
 * 
 */
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import ch.ethz.idsc.owly.data.nd.NdCluster;
import ch.ethz.idsc.owly.data.nd.NdDistanceInterface;
import ch.ethz.idsc.owly.data.nd.NdTreeMap;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
public enum NDTreeReducer {
    ;
    /* package */ static Collection<AVRequest> reduceRequests(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis, Network network) {

        // for less requests than cars, don't do anything
        if (requests.size() < roboTaxis.size() || requests.size() < 10)
            return requests;

        // otherwise create Quadtree and return minimum amount of RoboTaxis
        // Build the ND tree
        Tensor lbounds = lowerBoudnsOf(network);
        Tensor ubounds = upperBoudnsOf(network);
        NdTreeMap<AVRequest> ndTree = new NdTreeMap<>(lbounds, ubounds, 10, 24);

        // add uniquely identifiable requests to KD tree
        for (AVRequest avRequest : requests) {
            ndTree.add(PlaneLocation.of(avRequest), avRequest);
        }

        // for all roboTaxis vehicles, start nearestNeighborSearch until union is as large as the
        // number of vehicles start with only one request per vehicle
        Collection<AVRequest> requestsChosen = new HashSet<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (RoboTaxi roboTaxi : roboTaxis) {
                Tensor center = PlaneLocation.of(roboTaxi);
                NdCluster<AVRequest> nearestCluster = ndTree.buildCluster(center, iter, NdDistanceInterface.EUCLIDEAN);
                nearestCluster.stream().forEach(ndentry -> requestsChosen.add(ndentry.value));
            }
            ++iter;
        } while (requestsChosen.size() < roboTaxis.size() && iter <= roboTaxis.size());

        return requestsChosen;

    }

    /* package */ static Collection<RoboTaxi> reduceRoboTaxis(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis, //
            Network network) {
        // for less requests than cars, don't do anything
        if (roboTaxis.size() < requests.size() || roboTaxis.size() < 10)
            return roboTaxis;

        // otherwise create Quadtree and return minimum amount of RoboTaxis
        // Build the ND tree
        Tensor lbounds = lowerBoudnsOf(network);
        Tensor ubounds = upperBoudnsOf(network);
        NdTreeMap<RoboTaxi> ndTree = new NdTreeMap<>(lbounds, ubounds, 10, 24);

        // add roboTaxis to ND Tree
        for (RoboTaxi robotaxi : roboTaxis) {
            ndTree.add(PlaneLocation.of(robotaxi), robotaxi);
        }

        // for all robotaxis, start nearestNeighborSearch until union is as large as the number of requests
        // start with only one vehicle per request
        Set<RoboTaxi> vehiclesChosen = new HashSet<>(); // note: must be HashSet to avoid duplicate elements.
        int roboTaxiPerRequest = 1;
        do {
            vehiclesChosen.clear();
            for (AVRequest avRequest : requests) {
                Tensor center = PlaneLocation.of(avRequest);
                NdCluster<RoboTaxi> nearestCluster = ndTree.buildCluster(center, roboTaxiPerRequest, NdDistanceInterface.EUCLIDEAN);
                nearestCluster.stream().forEach(ndentry -> vehiclesChosen.add(ndentry.value));
            }
            ++roboTaxiPerRequest;
        } while (vehiclesChosen.size() < requests.size() && roboTaxiPerRequest <= requests.size());

        return vehiclesChosen;
    }

    public static Tensor lowerBoudnsOf(Network network) {
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        return Tensors.vectorDouble(bounds[0], bounds[1]);
    }

    public static Tensor upperBoudnsOf(Network network) {

        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        Tensor lbounds = Tensors.vectorDouble(bounds[0], bounds[1]);
        Tensor ubounds = Tensors.vectorDouble(bounds[2], bounds[3]);
        Tensor diff = ubounds.subtract(lbounds);
        double dx = diff.Get(0).number().doubleValue();
        double dy = diff.Get(1).number().doubleValue();
        GlobalAssert.that(dx > 0);
        GlobalAssert.that(dy > 0);
        double dmax = Math.max(dx, dy);
        return (lbounds.add(Tensors.vectorDouble(dmax, dmax)));
    }

}
