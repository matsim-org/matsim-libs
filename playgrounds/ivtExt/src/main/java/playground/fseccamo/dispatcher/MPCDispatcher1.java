package playground.fseccamo.dispatcher;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.ExtractPrimitives;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Increment;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.DispatcherUtils;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.dispatcher.utils.PredefinedMatchingMatcher;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * MPC Dispatcher 1 contains the implementation that generated the results
 * in Francesco's Master thesis.
 * 
 * MPC dispatcher requires yalmip running in matlab
 */
public class MPCDispatcher1 extends BaseMpcDispatcher {
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfVehicles;
    private final double[] networkBounds;
    final Map<VirtualNode, Link> centerLink = new HashMap<>();
    private String infoLineExtension = "";

    final JavaContainerSocket javaContainerSocket;

    public MPCDispatcher1( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            Network network, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            Map<VirtualLink, Double> travelTimesIn) {
        super(config, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        numberOfVehicles = (int) generatorConfig.getNumberOfVehicles();

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            final QuadTree<Link> quadTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
            for (Link link : virtualNode.getLinks())
                quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
            Link center = quadTree.getClosest(virtualNode.getCoord().getX(), virtualNode.getCoord().getY());
            centerLink.put(virtualNode, center);
        }

        try {
            javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

            Container container = MpcUtils.getContainerInit(virtualNetwork, samplingPeriod, numberOfVehicles);
            javaContainerSocket.writeContainer(container);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(); // dispatcher will not work if constructor has issues
        }
    }

    @Override
    public void redispatch(double now) {

        final long round_now = Math.round(now);
        if (round_now % samplingPeriod != 0) {

            // PART 0: match vehicles at a customer link only if the request has received a pickup order

            Map<AVVehicle, Link> stayVehiclesAtLinks = DispatcherUtils.vehicleMapper(getStayVehicles());
            // match all matched av/request pairs which are at same link

            final int pendingSize = getAVRequests().size();
            final int matchingSize = getMatchings().size();

            new PredefinedMatchingMatcher(this::setAcceptRequest).matchPredefined(getMatchings(), stayVehiclesAtLinks);

            final int delta = pendingSize - getAVRequests().size();

            // at this point, the same number of requests should have disappeared as matchings
            GlobalAssert.that(delta == matchingSize - getMatchings().size());

        } else {

            final StringBuilder stringBuilder = new StringBuilder();

            manageIncomingRequests(now);

            {
                final int m = virtualNetwork.getvLinksCount();
                final int n = virtualNetwork.getvNodesCount();

                System.out.println("open requests: " + getAVRequests().size() + "  in the process of pickup: " + getMatchings().size());

                { // build and send problem description to MATLAB as input to MPC
                    Container container = new Container(String.format("problem@%06d", Math.round(now)));
                    { // done
                        /**
                         * number of waiting customers that begin their journey on link_k = (node_i, node_j)
                         * also:
                         * max waiting time in seconds of customers that begin their journey on link_k = (node_i, node_j)
                         */
                        Tensor waitCustomersPerVLink = Array.zeros(m + n); // +n accounts for self loop
                        Tensor maxWaitingTimePerVLink = Array.zeros(m + n); // +n accounts for self loop
                        for (AVRequest avRequest : getAVRequestsUnserved()) { // requests that haven't received a dispatch yet
                            MpcRequest mpcRequest = mpcRequestsMap.get(avRequest); // must not be null
                            waitCustomersPerVLink.set(Increment.ONE, mpcRequest.vectorIndex);
                            double waitTime = now - mpcRequest.avRequest.getSubmissionTime();
                            GlobalAssert.that(0 <= waitTime);
                            maxWaitingTimePerVLink.set(Max.function(DoubleScalar.of(waitTime)), mpcRequest.vectorIndex);
                        }
                        {
                            double[] array = ExtractPrimitives.toArrayDouble(waitCustomersPerVLink);
                            DoubleArray doubleArray = new DoubleArray("waitCustomersPerVLink", new int[] { array.length }, array);
                            container.add(doubleArray);
                            System.out.println("waitCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
                        }
                        {
                            double[] array = ExtractPrimitives.toArrayDouble(maxWaitingTimePerVLink);
                            DoubleArray doubleArray = new DoubleArray("maxWaitingTimePerVLink", new int[] { array.length }, array);
                            container.add(doubleArray);
                            System.out.println("maxWaitingTimePerVLink=" + Tensors.vectorDouble(array) //
                                    .flatten(0).map(Scalar.class::cast).reduce(Max::of).get());
                        }
                    }
                    Scalar vehicleTotal = RealScalar.ZERO;
                    Set<AVVehicle> accountedVehicles = new HashSet<>();
                    { // done
                        /**
                         * STAY vehicles + vehicles without task inside VirtualNode
                         */
                        // all vehicles except the ones with a customer on board and the ones which are rebalancing
                        Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getDivertableNotRebalancingNotPickupVehicles();
                        availableVehicles.values().stream().flatMap(List::stream).map(vlp -> vlp.avVehicle) //
                                .forEach(accountedVehicles::add);
                        double[] array = new double[n];
                        for (Entry<VirtualNode, List<VehicleLinkPair>> entry : availableVehicles.entrySet())
                            array[entry.getKey().index] = entry.getValue().size(); // could use tensor notation

                        DoubleArray doubleArray = new DoubleArray("availableVehiclesPerVNode", new int[] { array.length }, array);
                        container.add(doubleArray);
                        vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
                        System.out.println("availableVehiclesPerVNode=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    { // done
                        /**
                         * rebalancing vehicles still within node_i traveling on link_k = (node_i, node_j)
                         */
                        Map<AVVehicle, Link> map = getRebalancingVehicles();
                        accountedVehicles.addAll(map.keySet());
                        final Tensor vector = countVehiclesPerVLink(map);
                        double[] array = ExtractPrimitives.toArrayDouble(vector);
                        DoubleArray doubleArray = new DoubleArray("movingRebalancingVehiclesPerVLink", new int[] { array.length }, array);
                        container.add(doubleArray);
                        vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
                        System.out.println("movingRebalancingVehiclesPerVLink=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    { // done
                        /**
                         * Vehicles with customers still within node_i traveling on link_k = (node_i, node_j)
                         */
                        Map<AVVehicle, Link> map = getVehiclesWithCustomer();
                        final Tensor vector = countVehiclesPerVLink(map);
                        accountedVehicles.addAll(map.keySet());
                        {
                            // vehicles on pickup drive appear here
                            for (Entry<AVRequest, AVVehicle> entry : getMatchings().entrySet()) {
                                AVRequest avRequest = entry.getKey();
                                AVVehicle avVehicle = entry.getValue();
                                if (!accountedVehicles.contains(avVehicle)) {
                                    // request
                                    int index = -1;
                                    if (mpcRequestsMap.containsKey(avRequest))
                                        index = mpcRequestsMap.get(avRequest).vectorIndex;
                                    else {
                                        index = m + virtualNetwork.getVirtualNode(avRequest.getFromLink()).index;
                                        new RuntimeException("map should provide request info").printStackTrace();
                                    }
                                    vector.set(Increment.ONE, index);
                                    accountedVehicles.add(avVehicle);
                                }
                            }
                        }
                        double[] array = ExtractPrimitives.toArrayDouble(vector);
                        DoubleArray doubleArray = new DoubleArray("movingVehiclesWithCustomersPerVLink", new int[] { array.length }, array);
                        container.add(doubleArray);
                        vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
                        System.out.println("movingVehiclesWithCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    
                    if (Scalars.nonZero(Chop.of(vehicleTotal.subtract(RealScalar.of(numberOfVehicles))))) {
                        new RuntimeException("#vehiclesTotal=" + vehicleTotal).printStackTrace();
                    }
                    if (numberOfVehicles != accountedVehicles.size())
                        new RuntimeException("#2nd check ==> numberOfVehicles != " + accountedVehicles.size()).printStackTrace();
                    javaContainerSocket.writeContainer(container);
                }
                // COMPUTE MPC OUTSIDE OF MATSIM:

                { // recv

                    // USE THE RETURN VALUE TO MAKE COMMANDS IN MATSIM:
                    // 1) <VirtualLink, Integer> pickupPerVLink: the number of vehicles at VirtualLink ij which should take a request and
                    // transport the person to a virtualNode
                    // for all these u_ij select u_ij customers in vNode i which have any shortest path that with sequence (vN1i, vNj, , ... )

                    // 2) <VirtualLink, Integer> rebalancingPerVLink : for every VirtualLink select this number of vehicles vehicle in the fromVNode and
                    // send it to the to VNode / use existing commands

                    // this waits until a reply has been received:
                    Container container = javaContainerSocket.blocking_getContainer();
                    System.out.println("received: " + container);

                    // container.id == 'solution'
                    Tensor requestVector = null;
                    Tensor rebalanceVector = null;
                    {
                        DoubleArray doubleArray = container.get("pickupPerVLink");
                        requestVector = Round.of(Tensors.vectorDouble(doubleArray.value)); // integer values = # person
                        GlobalAssert.that(requestVector.length() == m + n);
                    }
                    {
                        DoubleArray doubleArray = container.get("rebalancingPerVLink");
                        rebalanceVector = Round.of(Tensors.vectorDouble(doubleArray.value));
                        GlobalAssert.that(rebalanceVector.length() == m + n);

                        {
                            for (int vl = 0; vl < m; vl += 2) {
                                Scalar d1 = rebalanceVector.Get(vl + 0);
                                Scalar d2 = rebalanceVector.Get(vl + 1);

                                if (Scalars.nonZero(d1.multiply(d2))) {
                                    System.out.println("double rebalance");
                                    System.out.print("" + virtualNetwork.getVirtualLink(vl + 0).getFrom().index);
                                    System.out.println(" -> " + virtualNetwork.getVirtualLink(vl + 0).getTo().index);
                                    System.out.print("" + virtualNetwork.getVirtualLink(vl + 1).getFrom().index);
                                    System.out.println("-> " + virtualNetwork.getVirtualLink(vl + 1).getTo().index);
                                    System.out.println(d1 + " " + d2);

                                    Scalar surplus = Min.of(d1, d2);

                                    rebalanceVector.set(s -> s.subtract(surplus), vl + 0);
                                    rebalanceVector.set(s -> s.subtract(surplus), vl + 1);

                                    {
                                        Scalar c1 = rebalanceVector.Get(vl + 0);
                                        Scalar c2 = rebalanceVector.Get(vl + 1);

                                        System.out.println(c1 + " " + c2);
                                    }
                                }

                            }
                        }
                    }
                    final int totalRebalanceDesired = Total.of(rebalanceVector).Get().number().intValue();
                    final int totalPickupDesired = Total.of(requestVector).Get().number().intValue();
                    stringBuilder.append(String.format("%2dp, %2dr ", totalPickupDesired, totalRebalanceDesired));
                    // System.out.println("pickupPerVLink : TOTAL " + totalPickupDesired);
                    // System.out.println("rebalancingPerVLink: TOTAL " + totalRebalanceDesired);

                    stringBuilder.append(" effective: ");

                    {
                        int totalPickupEffective = 0;

                        final NavigableMap<Integer, List<MpcRequest>> virtualLinkRequestsMap = new TreeMap<>(mpcRequestsMap.values().stream() //
                                .collect(Collectors.groupingBy(mpcRequest -> mpcRequest.vectorIndex)));

                        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
                            // ---
                            final VirtualNode vnFrom = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            final Map<VirtualNode, List<AVVehicle>> availableVehicles = getVirtualNodeStayVehicles();

                            final List<AVVehicle> cars = availableVehicles.get(vnFrom); // find cars
                            cars.removeAll(getAVVehicleInMatching());
                            final int pickupOffset = 0; // <- should be 0 !!! only 1 for testing
                            final int desiredPickup = requestVector.Get(vectorIndex).number().intValue() + pickupOffset;
                            // int pickupPerNode = 0;
                            if (0 < desiredPickup) {
                                // if (pickupOffset < desiredPickup)
                                // System.out.println(String.format("vl=%3d cars=%3d pick=%3d ", //
                                // vectorIndex, cars.size(), desiredPickup));
                                // handle requests
                                final List<MpcRequest> requests = virtualLinkRequestsMap.containsKey(vectorIndex) ? //
                                        virtualLinkRequestsMap.get(vectorIndex) : Collections.emptyList();
                                Collections.sort(requests, MpcRequestComparator.INSTANCE);

                                { // assert that requests are sorted, oldest requests are served first
                                    double last = 0;
                                    for (MpcRequest mpcRequest : requests) {
                                        GlobalAssert.that(last <= mpcRequest.avRequest.getSubmissionTime());
                                        last = mpcRequest.avRequest.getSubmissionTime();
                                    }
                                }

                                int min = Math.min(Math.min(desiredPickup, requests.size()), cars.size());
                                for (int count = 0; count < min; ++count) {

                                    final MpcRequest mpcRequest = requests.get(count);

                                    final QuadTree<AVVehicle> unassignedVehiclesTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

                                    for (AVVehicle avVehicle : cars) {
                                        Link link = getVehicleLocation(avVehicle);
                                        unassignedVehiclesTree.put(link.getCoord().getX(), link.getCoord().getY(), avVehicle);
                                    }

                                    AVVehicle avVehicle = unassignedVehiclesTree.getClosest( //
                                            mpcRequest.avRequest.getFromLink().getCoord().getX(), //
                                            mpcRequest.avRequest.getFromLink().getCoord().getY());
                                    GlobalAssert.that(!getAVVehicleInMatching().contains(avVehicle));
                                    {
                                        boolean removed = cars.remove(avVehicle);
                                        GlobalAssert.that(removed);
                                    }
                                    Link pickupLocation = mpcRequest.avRequest.getFromLink(); // where the customer is waiting right now
                                    setStayVehicleDiversion(avVehicle, pickupLocation); // send car to customer
                                    ++totalPickupEffective;
                                    // ++pickupPerNode;
                                    getMatchings().put(mpcRequest.avRequest, avVehicle);
                                }

                            }
                            // if (pickupPerNode != desiredPickup)
                            // new RuntimeException("pickup inconsistent:" + pickupPerNode + " != " + desiredPickup).printStackTrace();

                        }
                        if (totalPickupEffective != totalPickupDesired)
                            System.out.println(" !!! pickup delta: " + totalPickupEffective + " < " + totalPickupDesired);

                        stringBuilder.append(String.format("%2dp, ", totalPickupEffective));
                    }

                    {
                        int totalRebalanceEffective = 0;
                        int selfRebalanceEffective = 0;
                        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
                            final VirtualNode vnFrom = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            final VirtualNode vnTo = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getTo() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            final Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getDivertableNotRebalancingNotPickupVehicles();

                            List<Link> candidateLinks = new ArrayList<>();
                            for (AVRequest avRequest : getAVRequestsUnserved()) {
                                Link link = avRequest.getFromLink();
                                if (vnTo.getLinks().contains(link))
                                    candidateLinks.add(link);
                            }
                            if (candidateLinks.isEmpty())
                                candidateLinks.addAll(vnTo.getLinks());

                            // ---
                            if (availableVehicles.containsKey(vnFrom)) {
                                final List<VehicleLinkPair> cars = availableVehicles.get(vnFrom); // find cars
                                final int desiredRebalance = rebalanceVector.Get(vectorIndex).number().intValue();
                                @SuppressWarnings("unused")
                                int pickupPerNode = 0;
                                if (0 < desiredRebalance) {
                                    String infoString = vnFrom.equals(vnTo) ? "DEST==ORIG" : "";
                                    if (vnFrom.equals(vnTo))
                                        System.out.println(String.format("vl=%3d  cars=%3d  reb=%3d  %s", vectorIndex, cars.size(), desiredRebalance, infoString));
                                    Random random = new Random();
                                    int min = Math.min(desiredRebalance, cars.size());
                                    for (int count = 0; count < min; ++count) {
                                        VehicleLinkPair vehicleLinkPair = cars.get(0);
                                        cars.remove(0);
                                        Link rebalanceDest =
                                                // centerLink.get(vnTo);
                                                new ArrayList<>( //
                                                        // vnTo.getLinks() //
                                                        candidateLinks //
                                                ).get(random.nextInt(candidateLinks.size()));
                                        setVehicleRebalance(vehicleLinkPair, rebalanceDest); // send car to adjacent virtual node
                                        ++totalRebalanceEffective;
                                        if (vnFrom.equals(vnTo))
                                            ++selfRebalanceEffective;
                                        ++pickupPerNode;
                                    }
                                }
                                // if (pickupPerNode != desiredRebalance)
                                // new RuntimeException("rebalance inconsistent:" + pickupPerNode + " != " + desiredRebalance).printStackTrace();
                            } else {
                                System.out.println("no available vehicles inside vnode " + vectorIndex + " " + vnFrom.index);
                            }
                        }
                        if (totalRebalanceEffective != totalRebalanceDesired)
                            System.out.println(" !!! rebalance delta: " + totalRebalanceEffective + " < " + totalRebalanceDesired);

                        stringBuilder.append(String.format("%2dr (self=%d)", totalRebalanceEffective, selfRebalanceEffective));
                    }

                }
            }
            infoLineExtension = stringBuilder.toString();
        }
    }

    @Override
    public String getInfoLine() {
        return String.format("%s %s", super.getInfoLine(), infoLineExtension);
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        private Network network;

        public static VirtualNetwork virtualNetwork;
        public static Map<VirtualLink, Double> travelTimes;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
            virtualNetwork = VirtualNetworkGet.readDefault(network);

            return new MPCDispatcher1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, network, abstractVirtualNodeDest, abstractVehicleDestMatcher,
                    travelTimes);
        }
    }
}
