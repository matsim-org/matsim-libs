package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import ch.ethz.idsc.owly.data.nd.NdCluster;
import ch.ethz.idsc.owly.data.nd.NdDistanceInterface;
import ch.ethz.idsc.owly.data.nd.NdTreeMap;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public enum BipartiteMatchingUtils {
    ;

    public static Tensor executePickup(BiConsumer<RoboTaxi, AVRequest> setFunction, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests, //
            DistanceFunction distanceFunction, Network network) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, distanceFunction, network, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            setFunction.accept(entry.getKey(), entry.getValue());
        }
        return infoLine;
    }

    public static Tensor executeRebalance(BiConsumer<RoboTaxi, Link> setFunction, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests, //
            DistanceFunction distanceFunction, Network network) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, distanceFunction, network, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            setFunction.accept(entry.getKey(), entry.getValue().getFromLink());
        }
        return infoLine;
    }

    private static Map<RoboTaxi, AVRequest> globalBipartiteMatching(Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests, //
            DistanceFunction distanceFunction, Network network, Tensor infoLine) {

        // save initial problemsize
        infoLine.append(Tensors.vectorInt(roboTaxis.size(), requests.size()));

        // 1) In case roboTaxis >> requests reduce search space using kd-trees
        Collection<RoboTaxi> roboTaxisReduced = reduceRoboTaxis(requests, roboTaxis, network);

        // 2) In case requests >> roboTaxis reduce the search space using kd-trees
        Collection<AVRequest> requestsReduced = reduceRequests(requests, roboTaxis, network);

        // 3) compute Euclidean bipartite matching for all vehicles using the Hungarian method and set new pickup commands
        infoLine.append(Tensors.vectorInt(roboTaxisReduced.size(), requestsReduced.size()));

        return ((new HungarBiPartVehicleDestMatcher(//
                distanceFunction)).matchAVRequest(roboTaxisReduced, requestsReduced));

    }

    private static Collection<AVRequest> reduceRequests(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis, Network network) {
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
            ndTree.add(EuclideanLocation.of(avRequest), avRequest);
        }

        // for all roboTaxis vehicles, start nearestNeighborSearch until union is as large as the
        // number of vehicles start with only one request per vehicle
        Collection<AVRequest> requestsChosen = new HashSet<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (RoboTaxi roboTaxi : roboTaxis) {
                Tensor center = EuclideanLocation.of(roboTaxi);
                NdCluster<AVRequest> nearestCluster = ndTree.buildCluster(center, iter, NdDistanceInterface.EUCLIDEAN);
                nearestCluster.stream().forEach(ndentry -> requestsChosen.add(ndentry.value));
            }
            ++iter;
        } while (requestsChosen.size() < roboTaxis.size() && iter <= roboTaxis.size());

        return requestsChosen;

    }

    private static Collection<RoboTaxi> reduceRoboTaxis(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis, //
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
            ndTree.add(EuclideanLocation.of(robotaxi), robotaxi);
        }

        // for all robotaxis, start nearestNeighborSearch until union is as large as the number of requests
        // start with only one vehicle per request
        Set<RoboTaxi> vehiclesChosen = new HashSet<>(); // note: must be HashSet to avoid duplicate elements.
        int roboTaxiPerRequest = 1;
        do {
            vehiclesChosen.clear();
            for (AVRequest avRequest : requests) {
                Tensor center = EuclideanLocation.of(avRequest);
                NdCluster<RoboTaxi> nearestCluster = ndTree.buildCluster(center, roboTaxiPerRequest, NdDistanceInterface.EUCLIDEAN);
                nearestCluster.stream().forEach(ndentry -> vehiclesChosen.add(ndentry.value));
            }
            ++roboTaxiPerRequest;
        } while (vehiclesChosen.size() < requests.size() && roboTaxiPerRequest <= requests.size());

        return vehiclesChosen;
    }

    private static Tensor lowerBoudnsOf(Network network) {
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        return Tensors.vectorDouble(bounds[0], bounds[1]);
    }

    private static Tensor upperBoudnsOf(Network network) {

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
