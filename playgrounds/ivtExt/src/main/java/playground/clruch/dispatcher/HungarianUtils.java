package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.ImmobilizeVehicleDestMatcher;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public enum HungarianUtils {
    ;

    /**
     * @param dispatcher
     *            univeralDispatcher from where call takes place
     * @param supplier
     *            supplier for VehicleLinkPairs (divertable Vehicles)
     * @param requests
     *            open requests
     * @return does assignment according to globalBipartiteMatching and returns a tensor with infoline Information
     */
    public static Tensor globalBipartiteMatching(UniversalDispatcher dispatcher, Supplier<Collection<VehicleLinkPair>> supplier,
            Map<Link, List<AVRequest>> requests) {

        Tensor returnTensor = Tensors.empty(); // contains information for InfoLine

        // generate a list of request links (there can be multiple entries)
        List<Link> requestLinks = requests.values().stream() //
                .flatMap(List::stream).map(AVRequest::getFromLink).collect(Collectors.toList());

        // 1) for every request, immobilize a stay vehicle on the same link if existing and take request out of set
        Collection<VehicleLinkPair> divertableVehicles = supplier.get();
        returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requestLinks.size())); // initial problem size
        new ImmobilizeVehicleDestMatcher().match(divertableVehicles, requestLinks) //
                .entrySet().forEach(dispatcher::setVehicleDiversion);

        divertableVehicles = supplier.get();
        returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requestLinks.size())); // initial problem size

        // TODO: this global assert does not work. Check if it is because some vehicles are not available all the time
        // and thus do not appear in getDivertableVehicles();
        // GlobalAssert.that(returnTensor.get(0,0).subtract(returnTensor.get(1,0)).equals(returnTensor.get(0,1).subtract(returnTensor.get(1,1))));

        // 2) In case divertableVehicles >> requests reduce search space using kd-trees
        Collection<VehicleLinkPair> divertableVehiclesReduced = reduceVehiclesKDTree(requestLinks, divertableVehicles);

        // 3) In case requests >> divertablevehicles reduce the search space using kd-trees
        List<Link> requestLinksReduced = reduceRequestsKDTree(requestLinks, divertableVehicles);

        // 4) compute Euclidean bipartite matching for all vehicles using the Hungarian method
        returnTensor.append(Tensors.vectorInt(divertableVehiclesReduced.size(), requestLinks.size())); // initial problem size
        new HungarBiPartVehicleDestMatcher().match(divertableVehiclesReduced, requestLinksReduced) //
                .entrySet().forEach(dispatcher::setVehicleDiversion);

        // return infoLine
        return returnTensor;
    }

    // class to uniquely distinguish requests
    static class RequestWithID {
        public Link link;
        public int id;

        RequestWithID(Link linkIn, int idIn) {
            link = linkIn;
            id = idIn;
        }
    }

    private static List<Link> reduceRequestsKDTree(List<Link> requestlocs, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (requestlocs.size() < divertableVehicles.size())
            return requestlocs;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = requestlocs.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = requestlocs.size();
        MyTree<RequestWithID> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        int reqIter = 0;
        for (Link link : requestlocs) {
            double d1 = link.getFromNode().getCoord().getX();
            double d2 = link.getFromNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, new RequestWithID(link, reqIter));
            reqIter++;
        }

        // for all divertable vehicles, start nearestNeighborSearch until union is as large as the number of vehicles
        // start with only one request per vehicle
        HashMap<RequestWithID, Link> requestsChosen = new HashMap<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                double[] vehLoc = new double[] { vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX(),
                        vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getY() };
                Cluster<RequestWithID> nearestCluster = KDTree.buildCluster(vehLoc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> requestsChosen.put(v, v.link));
            }
            ++iter;
        } while (requestsChosen.size() < divertableVehicles.size() && iter <= divertableVehicles.size());

        List<Link> toSearchrequestlocs = new LinkedList<>();
        requestsChosen.values().stream().forEach(v -> toSearchrequestlocs.add(v));

        if (toSearchrequestlocs.size() < divertableVehicles.size()) {
            System.out.println("Hoppla");
        }

        return toSearchrequestlocs;
    }

    private static Collection<VehicleLinkPair> reduceVehiclesKDTree(List<Link> requestlocs, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (divertableVehicles.size() < requestlocs.size())
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
            for (Link link : requestlocs) {
                double[] reqloc = new double[] { link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY() };
                Cluster<VehicleLinkPair> nearestCluster = KDTree.buildCluster(reqloc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> vehiclesChosen.add(v));
            }
            ++iter;
        } while (vehiclesChosen.size() < requestlocs.size() && iter <= requestlocs.size());

        return vehiclesChosen;
    }
}
