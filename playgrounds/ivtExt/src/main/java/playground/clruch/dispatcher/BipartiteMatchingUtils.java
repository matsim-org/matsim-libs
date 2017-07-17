package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class BipartiteMatchingUtils {

    Map<VehicleLinkPair, AVRequest> gbpMatch = null;

    /**
     * @param dispatcher
     *            univeralDispatcher from where call takes place
     * @param supplier
     *            supplier for VehicleLinkPairs (divertable Vehicles)
     * @param requests
     *            open requests
     * @return does assignment according to globalBipartiteMatching and returns a tensor with infoline Information
     */
    public Tensor globalBipartiteMatching(Supplier<Collection<VehicleLinkPair>> supplier, Collection<AVRequest> requests) {

        Tensor returnTensor = Tensors.empty(); // contains information for InfoLine
        Collection<VehicleLinkPair> divertableVehicles = supplier.get(); // save initial problem size
        returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requests.size()));

        // 1) In case divertableVehicles >> requests reduce search space using kd-trees
        Collection<VehicleLinkPair> divertableVehiclesReduced = reduceVehiclesKDTree(requests, divertableVehicles);

        // 2) In case requests >> divertablevehicles reduce the search space using kd-trees
        Collection<AVRequest> requestsReduced = reduceRequestsKDTree(requests, divertableVehicles);

        // 3) compute Euclidean bipartite matching for all vehicles using the Hungarian method and set new pickup commands
        returnTensor.append(Tensors.vectorInt(divertableVehiclesReduced.size(), requestsReduced.size())); // initial problem size

        gbpMatch = (new HungarBiPartVehicleDestMatcher()).matchAVRequest(divertableVehiclesReduced, requestsReduced); //

        // return infoLine
        return returnTensor;
    }

    public void executePickup(UniversalDispatcher dispatcher) {
        for (Entry<VehicleLinkPair, AVRequest> entry : gbpMatch.entrySet()) {
            AVVehicle av = entry.getKey().avVehicle;
            AVRequest avRequest = entry.getValue();
            dispatcher.setVehiclePickup(av, avRequest);
        }
    }

    public void executeRebalance(RebalancingDispatcher dispatcher) {
        for (Entry<VehicleLinkPair, AVRequest> entry : gbpMatch.entrySet()) {
            AVVehicle av = entry.getKey().avVehicle;
            AVRequest avRequest = entry.getValue();
            dispatcher.setVehicleRebalance(av, avRequest.getFromLink());
        }
    }

    private Collection<AVRequest> reduceRequestsKDTree(Collection<AVRequest> requests, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (requests.size() < divertableVehicles.size())
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

        // for all divertable vehicles, start nearestNeighborSearch until union is as large as the number of vehicles
        // start with only one request per vehicle
        Collection<AVRequest> requestsChosen = new HashSet<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                double[] vehLoc = new double[] { vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX(),
                        vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getY() };
                Cluster<AVRequest> nearestCluster = KDTree.buildCluster(vehLoc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> requestsChosen.add(v));
            }
            ++iter;
        } while (requestsChosen.size() < divertableVehicles.size() && iter <= divertableVehicles.size());

        return requestsChosen;

    }

    private Collection<VehicleLinkPair> reduceVehiclesKDTree(Collection<AVRequest> requests, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (divertableVehicles.size() < requests.size())
            return divertableVehicles;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = divertableVehicles.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = divertableVehicles.size();
        MyTree<VehicleLinkPair> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        // int vehIter = 0; // not read
        for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
            double d1 = vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX();
            double d2 = vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, vehicleLinkPair);
            // vehIter++;
        }

        // for all requests, start nearestNeighborSearch until union is as large as the number of requests
        // start with only one vehicle per request
        HashSet<VehicleLinkPair> vehiclesChosen = new HashSet(); // note: must be HashSet to avoid duplicate elements.
        int iter = 1;
        do {
            vehiclesChosen.clear();
            for (AVRequest avRequest : requests) {
                Link link = avRequest.getFromLink();
                double[] reqloc = new double[] { link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY() };
                Cluster<VehicleLinkPair> nearestCluster = KDTree.buildCluster(reqloc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> vehiclesChosen.add(v));
            }
            ++iter;
        } while (vehiclesChosen.size() < requests.size() && iter <= requests.size());

        return vehiclesChosen;
    }
}
