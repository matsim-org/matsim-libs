package playground.fseccamo.dispatcher;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** MPC Dispatcher 1 contains the implementation that generated the results
 * in Francesco's Master thesis.
 * 
 * MPC dispatcher requires yalmip running in matlab */
public class MPCDispatcher1 extends BaseMpcDispatcher {
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfVehicles;
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
            Map<VirtualLink, Double> travelTimesIn) {
        super(config, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        travelTimes = travelTimesIn;
        numberOfVehicles = (int) generatorConfig.getNumberOfVehicles();
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
        if (round_now % samplingPeriod == 0) {
            final StringBuilder stringBuilder = new StringBuilder();
            manageIncomingRequests(now);
            GlobalAssert.that(getAVRequests().size() >= getRoboTaxiSubset(AVStatus.DRIVETOCUSTMER).size());
            System.out.println("open requests: " + getAVRequests().size() + "  in the process of pickup: " + getRoboTaxiSubset(AVStatus.DRIVETOCUSTMER).size());

            // A) BUILD AND SEND PROBLEM DESCRIPTION TO MATLAB AS INPUT TO MPC
            Container containerSend = MPCDataCollection.collectData(this, now, virtualNetwork, //
                    rt -> getRoboTaxiPickupRequest(rt), getUnassignedAVRequests(), //
                    getRoboTaxiSubset(AVStatus.STAY), getRoboTaxiSubset(AVStatus.DRIVETOCUSTMER), getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER),
                    getRoboTaxiSubset(AVStatus.REBALANCEDRIVE));
            javaContainerSocket.writeContainer(containerSend);

            // B) COMPUTE MPC OUTSIDE OF MATSIM

            // C) RECEIVE RETURN VALUE TO MAKE COMMANDS IN MATSIM
            // this waits until a reply has been received:
            Container container = javaContainerSocket.blocking_getContainer();
            System.out.println("received: " + container);


            performPickup(stringBuilder, container, virtualNetwork);
            performRebalance(stringBuilder, container, virtualNetwork);

            infoLineExtension = stringBuilder.toString();
        }
    }

    private void performPickup(StringBuilder stringBuilder, Container container, VirtualNetwork virtualNetwork) {

        // 1) <VirtualLink, Integer> pickupPerVLink: the number of vehicles at
        // VirtualLink ij which should take a request and
        // transport the person to a virtualNode
        // for all these u_ij select u_ij customers in vNode i which have any shortest
        // path that with sequence (vN1i, vNj, , ... )

        final int m = virtualNetwork.getvLinksCount();
        final int n = virtualNetwork.getvNodesCount();

        // container.id == 'solution'
        Tensor requestVector = null;

        DoubleArray doubleArray = container.get("pickupPerVLink");
        requestVector = Round.of(Tensors.vectorDouble(doubleArray.value)); // integer values = # person
        GlobalAssert.that(requestVector.length() == m + n);

        final int totalPickupDesired = Total.of(requestVector).Get().number().intValue();
        stringBuilder.append(String.format("%2dp", totalPickupDesired));

        int totalPickupEffective = 0;

        final NavigableMap<Integer, List<MpcRequest>> virtualLinkRequestsMap = new TreeMap<>(mpcRequestsMap.values().stream() //
                .collect(Collectors.groupingBy(mpcRequest -> mpcRequest.vectorIndex)));

        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
            // ---
            final VirtualNode vnFrom = vectorIndex < m ? //
                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
            final Map<VirtualNode, List<RoboTaxi>> availableVehicles = getVirtualNodeStayVehicles();

            final List<RoboTaxi> cars = availableVehicles.get(vnFrom); // find cars
            cars.removeAll(getRoboTaxiSubset(AVStatus.DRIVETOCUSTMER));
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

                { // assert that requests are sorted, oldest requests are served
                  // first
                    double last = 0;
                    for (MpcRequest mpcRequest : requests) {
                        GlobalAssert.that(last <= mpcRequest.avRequest.getSubmissionTime());
                        last = mpcRequest.avRequest.getSubmissionTime();
                    }
                }

                int min = Math.min(Math.min(desiredPickup, requests.size()), cars.size());

                // MPC1 code:
                // ==========================
                // Map<RoboTaxi, AVRequest> pickupAssignments = new HashMap<>();
                // totalPickupEffective += MPCAuxiliary.cellMatchingMPCOption1(min,
                // requests, networkBounds, cars, this, pickupAssignments);
                // for (Entry<RoboTaxi, AVRequest> entry :
                // pickupAssignments.entrySet()) {
                // setRoboTaxiPickup(entry.getKey(), entry.getValue());
                // }

                // MPC2 code:
                // ==========================
                Map<RoboTaxi, AVRequest> pickupAssignments = new HashMap<>();
                totalPickupEffective += MPCAuxiliary.cellMatchingMPCOption2(min, requests, cars, this, pickupAssignments,
                        new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction()));
                for (Entry<RoboTaxi, AVRequest> entry : pickupAssignments.entrySet()) {
                    setRoboTaxiPickup(entry.getKey(), entry.getValue());
                }
            }
        }

        if (totalPickupEffective != totalPickupDesired)
            System.out.println(" !!! pickup delta: " + totalPickupEffective + " < " + totalPickupDesired);

        stringBuilder.append(" effective: ");
        stringBuilder.append(String.format("%2dp, ", totalPickupEffective));
    }

    private void performRebalance(StringBuilder stringBuilder, Container container, VirtualNetwork virtualNetwork) {
        
        // 2) <VirtualLink, Integer> rebalancingPerVLink : for every VirtualLink select
        // this number of vehicles vehicle in the fromVNode and
        // send it to the to VNode / use existing commands


        final int m = virtualNetwork.getvLinksCount();
        final int n = virtualNetwork.getvNodesCount();

        Tensor rebalanceVector = null;

        DoubleArray doubleArray = container.get("rebalancingPerVLink");
        rebalanceVector = Round.of(Tensors.vectorDouble(doubleArray.value));
        GlobalAssert.that(rebalanceVector.length() == m + n);

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
            }
        }

        final int totalRebalanceDesired = Total.of(rebalanceVector).Get().number().intValue();
        stringBuilder.append(String.format("%2dr ", totalRebalanceDesired));

        int totalRebalanceEffective = 0;
        int selfRebalanceEffective = 0;
        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
            final VirtualNode vnFrom = vectorIndex < m ? //
                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
            final VirtualNode vnTo = vectorIndex < m ? //
                    virtualNetwork.getVirtualLink(vectorIndex).getTo() : virtualNetwork.getVirtualNode(vectorIndex - m);
            final Map<VirtualNode, List<RoboTaxi>> availableVehicles = getDivertableNotRebalancingNotPickupVehicles();

            List<Link> candidateLinks = new ArrayList<>();
            for (AVRequest avRequest : getUnassignedAVRequests()) {
                Link link = avRequest.getFromLink();
                if (vnTo.getLinks().contains(link))
                    candidateLinks.add(link);
            }
            if (candidateLinks.isEmpty())
                candidateLinks.addAll(vnTo.getLinks());

            // ---
            if (availableVehicles.containsKey(vnFrom)) {
                final List<RoboTaxi> cars = availableVehicles.get(vnFrom); // find
                                                                           // cars
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
                        RoboTaxi robotaxi = cars.get(0);
                        cars.remove(0);
                        Link rebalanceDest =
                                // centerLink.get(vnTo);
                                new ArrayList<>( //
                                        // vnTo.getLinks() //
                                        candidateLinks //
                                ).get(random.nextInt(candidateLinks.size()));
                        setRoboTaxiRebalance(robotaxi, rebalanceDest); // send car
                                                                       // to
                                                                       // adjacent
                                                                       // virtual
                                                                       // node
                        ++totalRebalanceEffective;
                        if (vnFrom.equals(vnTo))
                            ++selfRebalanceEffective;
                        ++pickupPerNode;
                    }
                }
                // if (pickupPerNode != desiredRebalance)
                // new RuntimeException("rebalance inconsistent:" + pickupPerNode +
                // " != " + desiredRebalance).printStackTrace();
            } else {
                System.out.println("no available vehicles inside vnode " + vectorIndex + " " + vnFrom.index);
            }
        }
        if (totalRebalanceEffective != totalRebalanceDesired)
            System.out.println(" !!! rebalance delta: " + totalRebalanceEffective + " < " + totalRebalanceDesired);

        stringBuilder.append(" effective: ");
        stringBuilder.append(String.format("%2dr (self=%d)", totalRebalanceEffective, selfRebalanceEffective));

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

            virtualNetwork = VirtualNetworkGet.readDefault(network);

            return new MPCDispatcher1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, network, travelTimes);
        }
    }
}
