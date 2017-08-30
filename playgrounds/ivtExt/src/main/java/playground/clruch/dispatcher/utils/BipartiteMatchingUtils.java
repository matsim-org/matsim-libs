package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.owly.data.nd.NdCluster;
import ch.ethz.idsc.owly.data.nd.NdDistanceInterface;
import ch.ethz.idsc.owly.data.nd.NdTreeMap;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public enum BipartiteMatchingUtils {
    ;

    public static Tensor executePickup(BiConsumer<RoboTaxi, AVRequest> setFunction, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            setFunction.accept(entry.getKey(), entry.getValue());
        }
        return infoLine;
    }

    public static Tensor executeRebalance(BiConsumer<RoboTaxi, Link> setFunction, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            setFunction.accept(entry.getKey(), entry.getValue().getFromLink());
        }
        return infoLine;
    }

    private static Map<RoboTaxi, AVRequest> globalBipartiteMatching(Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests, Tensor infoLine) {

        // save initial problemsize
        infoLine.append(Tensors.vectorInt(roboTaxis.size(), requests.size()));

        // NEW TRY
        // 1) In case roboTaxis >> requests reduce search space using kd-trees
        Collection<RoboTaxi> roboTaxisReduced = reduceRoboTaxis(requests, roboTaxis);
        //
        // // 2) In case requests >> roboTaxis reduce the search space using kd-trees
        // Collection<AVRequest> requestsReduced = reduceRequestsKDTree(requests, roboTaxis);

        // 3) compute Euclidean bipartite matching for all vehicles using the Hungarian method and set new pickup commands
        infoLine.append(Tensors.vectorInt(roboTaxisReduced.size(), requests.size()));

        return ((new HungarBiPartVehicleDestMatcher(//
                new EuclideanDistanceFunction())).matchAVRequest(roboTaxis, requests));

        // // OLD IMPLEMENTATION
        // // 1) In case roboTaxis >> requests reduce search space using kd-trees
        // Collection<RoboTaxi> roboTaxisReduced = reduceVehiclesKDTree(requests, roboTaxis);
        //
        // // 2) In case requests >> roboTaxis reduce the search space using kd-trees
        // Collection<AVRequest> requestsReduced = reduceRequestsKDTree(requests, roboTaxis);
        //
        // // 3) compute Euclidean bipartite matching for all vehicles using the Hungarian method and
        // // set new pickup commands
        // infoLine.append(Tensors.vectorInt(roboTaxisReduced.size(), requestsReduced.size())); // initial problem size
        //
        // return ((new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction())).matchAVRequest(roboTaxisReduced, requestsReduced)); //

    }

    private static Collection<RoboTaxi> reduceRoboTaxis(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis) {
        // for less requests than cars, don't do anything
        if (roboTaxis.size() < requests.size() || roboTaxis.size() < 10)
            return roboTaxis;

        // otherwise create Quadtree and return minimum amount of RoboTaxis
        // Build the ND tree

        Tensor lbounds = findBoundsRT(requests, roboTaxis, -1);
        Tensor ubounds = findBoundsRT(requests, roboTaxis, 1);
        NdTreeMap<RoboTaxi> ndTree = new NdTreeMap<>(lbounds, ubounds, 10, roboTaxis.size());

        // add roboTaxis to ND Tree
        for (RoboTaxi robotaxi : roboTaxis) {
            double d1 = robotaxi.getDivertableLocation().getToNode().getCoord().getX();
            double d2 = robotaxi.getDivertableLocation().getToNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            ndTree.add(Tensors.vectorDouble(d1, d2), robotaxi);
        }

        // for all robotaxis, start nearestNeighborSearch until union is as large as the number of requests
        // start with only one vehicle per request
        HashSet<RoboTaxi> vehiclesChosen = new HashSet(); // note: must be HashSet to avoid duplicate elements.
        int roboTaxiPerRequest = 1;
        do {
            vehiclesChosen.clear();
            for (AVRequest avRequest : requests) {
                Link link = avRequest.getFromLink();
                Tensor center = Tensors.vectorDouble(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
                NdCluster<RoboTaxi> nearestCluster = ndTree.buildCluster(center, roboTaxiPerRequest, NdDistanceInterface.EUCLIDEAN);
                nearestCluster.stream().forEach(ndentry -> vehiclesChosen.add(ndentry.value));
            }
            ++roboTaxiPerRequest;
        } while (vehiclesChosen.size() < requests.size() && roboTaxiPerRequest <= requests.size());

        return vehiclesChosen;
    }

    private static Tensor findBoundsRT(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis, int lowhigh) {
        GlobalAssert.that(lowhigh != 0);
        GlobalAssert.that(roboTaxis.size() > 0);

        if (lowhigh < 0) { // find lower bounds
            double minX = roboTaxis.stream().mapToDouble(r -> r.getDivertableLocation().getToNode().getCoord().getX()).min().getAsDouble();
            double minY = roboTaxis.stream().mapToDouble(r -> r.getDivertableLocation().getToNode().getCoord().getY()).min().getAsDouble();

            OptionalDouble minXReq = requests.stream().mapToDouble(r -> r.getFromLink().getFromNode().getCoord().getX()).min();
            if (minXReq.isPresent())
                minX = Math.min(minX, minXReq.getAsDouble());

            OptionalDouble minYReq = requests.stream().mapToDouble(r -> r.getFromLink().getFromNode().getCoord().getY()).min();
            if (minYReq.isPresent())
                minY = Math.min(minY, minYReq.getAsDouble());

            return Tensors.vectorDouble(minX, minY);

        } else {
            double maxX = roboTaxis.stream().mapToDouble(r -> r.getDivertableLocation().getToNode().getCoord().getX()).max().getAsDouble();
            double maxY = roboTaxis.stream().mapToDouble(r -> r.getDivertableLocation().getToNode().getCoord().getY()).max().getAsDouble();

            OptionalDouble maxXReq = requests.stream().mapToDouble(r -> r.getFromLink().getFromNode().getCoord().getX()).max();
            if (maxXReq.isPresent())
                maxX = Math.max(maxX, maxXReq.getAsDouble());

            OptionalDouble maxYReq = requests.stream().mapToDouble(r -> r.getFromLink().getFromNode().getCoord().getY()).max();
            if (maxYReq.isPresent())
                maxY = Math.max(maxY, maxYReq.getAsDouble());

            return Tensors.vectorDouble(maxX, maxY);
        }
    }

    @Deprecated
    private static Collection<AVRequest> reduceRequestsKDTree(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis) {
        // for less requests than cars, don't do anything
        if (requests.size() < roboTaxis.size())
            return requests;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = requests.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = requests.size();
        MyTree<AVRequest> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        int reqIter = 0;
        for (AVRequest avRequest : requests) {
            Link link = avRequest.getFromLink();
            double d1 = link.getFromNode().getCoord().getX();
            double d2 = link.getFromNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, avRequest);
            reqIter++;
        }

        // for all roboTaxis vehicles, start nearestNeighborSearch until union is as large as the
        // number of vehicles
        // start with only one request per vehicle
        Collection<AVRequest> requestsChosen = new HashSet<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (RoboTaxi roboTaxi : roboTaxis) {
                double[] vehLoc = new double[] { roboTaxi.getDivertableLocation().getToNode().getCoord().getX(),
                        roboTaxi.getDivertableLocation().getToNode().getCoord().getY() };
                Cluster<AVRequest> nearestCluster = KDTree.buildCluster(vehLoc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> requestsChosen.add(v));
            }
            ++iter;
        } while (requestsChosen.size() < roboTaxis.size() && iter <= roboTaxis.size());

        return requestsChosen;

    }

    @Deprecated
    private static Collection<RoboTaxi> reduceVehiclesKDTree(Collection<AVRequest> requests, Collection<RoboTaxi> roboTaxis) {
        // for less requests than cars, don't do anything
        if (roboTaxis.size() < requests.size())
            return roboTaxis;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = roboTaxis.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = roboTaxis.size();
        MyTree<RoboTaxi> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        // int vehIter = 0; // not read
        for (RoboTaxi roboTaxi : roboTaxis) {
            double d1 = roboTaxi.getDivertableLocation().getToNode().getCoord().getX();
            double d2 = roboTaxi.getDivertableLocation().getToNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, roboTaxi);
            // vehIter++;
        }

        // for all requests, start nearestNeighborSearch until union is as large as the number of
        // requests
        // start with only one vehicle per request
        HashSet<RoboTaxi> vehiclesChosen = new HashSet(); // note: must be HashSet to avoid
                                                          // duplicate elements.
        int iter = 1;
        do {
            vehiclesChosen.clear();
            for (AVRequest avRequest : requests) {
                Link link = avRequest.getFromLink();
                double[] reqloc = new double[] { link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY() };
                Cluster<RoboTaxi> nearestCluster = KDTree.buildCluster(reqloc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> vehiclesChosen.add(v));
            }
            ++iter;
        } while (vehiclesChosen.size() < requests.size() && iter <= requests.size());

        return vehiclesChosen;
    }

}
