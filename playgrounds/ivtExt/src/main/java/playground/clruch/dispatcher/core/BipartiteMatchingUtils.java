package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
// TODO move this out of core
public enum BipartiteMatchingUtils {
    ;

    public static Tensor executePickup(UniversalDispatcher dispatcher, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            dispatcher.setRoboTaxiPickup(entry.getKey(), entry.getValue());
        }
        return infoLine;
    }

    public static Tensor executeRebalance(RebalancingDispatcher dispatcher, Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests) {
        Tensor infoLine = Tensors.empty();
        Map<RoboTaxi, AVRequest> gbpMatch = globalBipartiteMatching(roboTaxis, requests, infoLine);
        for (Entry<RoboTaxi, AVRequest> entry : gbpMatch.entrySet()) {
            dispatcher.setRoboTaxiRebalance(entry.getKey(), entry.getValue().getFromLink());
        }
        return infoLine;
    }

    private static Map<RoboTaxi, AVRequest> globalBipartiteMatching(Collection<RoboTaxi> roboTaxis, Collection<AVRequest> requests, Tensor infoLine) {

        // save initial problemsize
        infoLine.append(Tensors.vectorInt(roboTaxis.size(), requests.size()));

        // 1) In case roboTaxis >> requests reduce search space using kd-trees
        Collection<RoboTaxi> roboTaxisReduced = reduceVehiclesKDTree(requests, roboTaxis);

        // 2) In case requests >> roboTaxis reduce the search space using kd-trees
        Collection<AVRequest> requestsReduced = reduceRequestsKDTree(requests, roboTaxis);

        // 3) compute Euclidean bipartite matching for all vehicles using the Hungarian method and
        // set new pickup commands
        infoLine.append(Tensors.vectorInt(roboTaxisReduced.size(), requestsReduced.size())); // initial problem  size
        
        return ((new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction())).matchAVRequest(roboTaxisReduced, requestsReduced)); //

    }

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
