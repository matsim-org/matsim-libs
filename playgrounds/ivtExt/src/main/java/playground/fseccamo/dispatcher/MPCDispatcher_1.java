package playground.fseccamo.dispatcher;

import java.io.File;
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
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.ExtractPrimitives;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.red.KroneckerDelta;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Increment;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.router.InstantPathFactory;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * Dispatcher implementing the linear program from Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * Implemented by Claudio Ruch on 2017, 02, 25
 */
public class MPCDispatcher_1 extends BaseMpcDispatcher {
    public final int samplingPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfVehicles;
    private int total_rebalanceCount = 0;
    Tensor printVals = Tensors.empty();
    final InstantPathFactory instantPathFactory;

    JavaContainerSocket javaContainerSocket;

    public MPCDispatcher_1( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            Map<VirtualLink, Double> travelTimesIn) {
        super(config, travelTime, parallelLeastCostPathCalculator, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.instantPathFactory = new InstantPathFactory(parallelLeastCostPathCalculator, travelTime);
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        numberOfVehicles = (int) generatorConfig.getNumberOfVehicles();
        samplingPeriod = Integer.parseInt(config.getParams().get("samplingPeriod")); // period between calls to MPC

        try {
            final int n = virtualNetwork.getvNodesCount();
            final int m = virtualNetwork.getvLinksCount();
            javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));
            // ---
            {
                Container container = new Container("init");
                { // directed graph incidence matrix
                    Tensor matrix = Tensors.matrix((i, j) -> KroneckerDelta.of(virtualNetwork.getVirtualLink(j).getTo().index, i), n, m);
                    double[] array = ExtractPrimitives.toArrayDouble(Transpose.of(matrix));
                    DoubleArray doubleArray = new DoubleArray("E_in", new int[] { n, m }, array);
                    container.add(doubleArray);
                }
                {
                    Tensor matrix = Tensors.matrix((i, j) -> KroneckerDelta.of(virtualNetwork.getVirtualLink(j).getFrom().index, i), n, m);
                    double[] array = ExtractPrimitives.toArrayDouble(Transpose.of(matrix));
                    DoubleArray doubleArray = new DoubleArray("E_out", new int[] { n, m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[] { samplingPeriod };
                    DoubleArray doubleArray = new DoubleArray("Ts", new int[] { 1 }, array);
                    container.add(doubleArray);
                }
                {
                    Tensor matrix = Tensors.vector(i -> Tensors.vector( //
                            virtualNetwork.getVirtualNode(i).getCoord().getX(), //
                            virtualNetwork.getVirtualNode(i).getCoord().getY() //
                    ), n);
                    // System.out.println(Pretty.of(matrix));
                    double[] array = ExtractPrimitives.toArrayDouble(Transpose.of(matrix));
                    DoubleArray doubleArray = new DoubleArray("voronoiCenter", new int[] { n, 2 }, array);
                    container.add(doubleArray);
                }
                {
                    Tensor matrix = Import.of(getRequestScheduleFileGlobal());
                    double[] array = ExtractPrimitives.toArrayDouble(Transpose.of(matrix));
                    DoubleArray doubleArray = new DoubleArray("requestSchedule", new int[] { matrix.length(), 3 }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[] { numberOfVehicles };
                    GlobalAssert.that(0 < numberOfVehicles);
                    DoubleArray doubleArray = new DoubleArray("N_cars", new int[] { 1 }, array);
                    container.add(doubleArray);
                }
                javaContainerSocket.writeContainer(container);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(); // dispatcher will not work if constructor has issues
        }
    }

    // TODO remove served requests from map to save memory (but will not influence functionality)
    final Map<AVRequest, MpcRequest> mpcRequestsMap = new HashMap<>();

    @Override
    public void redispatch(double now) {

        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());

        final long round_now = Math.round(now);
        if (round_now % samplingPeriod == 0) {

            {
                final int m = virtualNetwork.getvLinksCount();
                final int n = virtualNetwork.getvNodesCount();

                System.out.println("open requests: " + getAVRequests().size());

                { // send
                  // INPUT TO MPC:
                  // 1) CREATE A MAP <VirtualLink, Integer> waitingCustomerpervLink where forall virtual link ij the number of customer requests for that
                  // link c_ij is stored. Take care: if a requests is from a vNode to another one without direct virtualLink then the
                  // path has to be split according to the virtual Network.
                  // 2) <VirtualNode, Integer> availalbeVehiclesperVirutalNode --> use Map<VirtualNode, List<VehicleLinkPair>> availableVehiclesAll=
                  // getVirtualNodeAvailableVehicles();
                  // 3) <VirtualLink, Integer> numberofCustomerCarryingVehicles --> new function needs to be implemented
                  // 4) <VirtualLink, Integer> numberofRebalancingVehicles --> new function needs to implemented

                    Container container = new Container(String.format("problem@%06d", Math.round(now)));
                    { // done
                        /**
                         * number of waiting customers that begin their journey on link_k = (node_i, node_j)
                         */
                        for (AVRequest avRequest : getAVRequests()) // all current requests
                            if (!mpcRequestsMap.containsKey(avRequest)) { // if request has been seen/computed before
                                // check if origin and dest are from same virtualNode
                                final VirtualNode vnFrom = virtualNetwork.getVirtualNode(avRequest.getFromLink());
                                final VirtualNode vnTo = virtualNetwork.getVirtualNode(avRequest.getToLink());
                                GlobalAssert.that(vnFrom.equals(vnTo) == (vnFrom.index == vnTo.index));
                                if (vnFrom.equals(vnTo)) {
                                    mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, m, vnFrom));
                                } else {
                                    VirtualNode fromIn = null;
                                    VrpPath vrpPath = instantPathFactory.getVrpPathWithTravelData( //
                                            avRequest.getFromLink(), avRequest.getToLink(), now); // TODO perhaps add expected waitTime
                                    for (Link link : vrpPath) {
                                        final VirtualNode toIn = virtualNetwork.getVirtualNode(link);
                                        if (fromIn == null)
                                            fromIn = toIn;
                                        else //
                                        if (fromIn != toIn) { // found adjacent node
                                            VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
                                            mpcRequestsMap.put(avRequest, new MpcRequest(avRequest, virtualLink));
                                            break;
                                        }
                                    }
                                }
                            }

                        Tensor waitCustomersPerVLink = Array.zeros(m + n); // +n accounts for self loop
                        for (AVRequest avRequest : getAVRequests()) // all current requests
                            waitCustomersPerVLink.set(Increment.ONE, mpcRequestsMap.get(avRequest).vectorIndex);

                        double[] array = ExtractPrimitives.toArrayDouble(waitCustomersPerVLink);
                        DoubleArray doubleArray = new DoubleArray("waitCustomersPerVLink", new int[] { array.length }, array);
                        container.add(doubleArray);
                        System.out.println("waitCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    { // done
                        /**
                         * STAY vehicles + vehicles without task inside VirtualNode
                         */
                        // all vehicles except the ones with a customer on board and the ones which are rebalancing
                        Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                        double[] array = new double[n];
                        for (Entry<VirtualNode, List<VehicleLinkPair>> entry : availableVehicles.entrySet())
                            array[entry.getKey().index] = entry.getValue().size(); // could use tensor notation

                        DoubleArray doubleArray = new DoubleArray("availableVehiclesPerVNode", new int[] { array.length }, array);
                        container.add(doubleArray);
                        System.out.println("availableVehiclesPerVNode=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    { // done
                        /**
                         * Vehicles with customers still within node_i traveling on link_k = (node_i, node_j)
                         */
                        final Tensor vector = countVehiclesPerVLink(getVehiclesWithCustomer());
                        double[] array = ExtractPrimitives.toArrayDouble(vector);
                        DoubleArray doubleArray = new DoubleArray("movingVehiclesWithCustomersPerVLink", new int[] { array.length }, array);
                        container.add(doubleArray);
                        System.out.println("movingVehiclesWithCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
                    }
                    { // done
                        /**
                         * rebalancing vehicles still within node_i traveling on link_k = (node_i, node_j)
                         */
                        final Tensor vector = countVehiclesPerVLink(getRebalancingVehicles());
                        double[] array = ExtractPrimitives.toArrayDouble(vector);
                        DoubleArray doubleArray = new DoubleArray("movingRebalancingVehiclesPerVLink", new int[] { array.length }, array);
                        container.add(doubleArray);
                        System.out.println("movingRebalancingVehiclesPerVLink=" + Total.of(Tensors.vectorDouble(array)));

                    }
                    javaContainerSocket.writeContainer(container);
                }
                // COMPUTE MPC OUTSIDE OF MATSIM:

                { // recv
                  // USE THE RETURN VALUE TO MAKE COMMANDS IN MATSIM:
                  // 1) <VirtualLink, Integer> pickupPerVLink: the number of vehicles at VirtualLink ij which should take a request and
                  // transport the person to a virtualNode
                  // for all these u_ij select u_ij customers in vNode i which have any shortest path that with sequence (vN1i, vNj, whatever, ... )

                    // 2) <VirtualLink, Integer> rebalancingPerVLink : for every VirtualLink select this number of vehicles vehicle in the fromVNode and
                    // send it to the to VNode / use existing commands

                    // this waits until a reply has been received:
                    Container container = javaContainerSocket.blocking_getContainer();
                    System.out.println("received: " + container);

                    // container.id == 'solution'
                    // TODO perhaps consider requests and rebalancing simultaneously instead of consecutively
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
                    }
                    final int totalRebalanceDesired = Total.of(rebalanceVector).Get().number().intValue();
                    final int totalPickupDesired = Total.of(requestVector).Get().number().intValue();
                    System.out.println("pickupPerVLink     : TOTAL " + totalPickupDesired);
                    System.out.println("rebalancingPerVLink: TOTAL " + totalRebalanceDesired);

                    {
                        int totalRebalanceEffective = 0;
                        final Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
                            final VirtualNode vnFrom = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            final VirtualNode vnTo = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getTo() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            // ---
                            if (availableVehicles.containsKey(vnFrom)) {
                                final List<VehicleLinkPair> cars = availableVehicles.get(vnFrom); // find cars
                                final int desiredRebalance = rebalanceVector.Get(vectorIndex).number().intValue();
                                if (0 < desiredRebalance) {
                                    String infoString = vnFrom.equals(vnTo) ? "DEST==ORIG" : "";
                                    System.out.println(String.format("vl=%2d  cars=%d  reb=%3d  %s", vectorIndex, cars.size(), desiredRebalance, infoString));
                                    Random random = new Random();
                                    int min = Math.min(desiredRebalance, cars.size());
                                    for (int count = 0; count < min; ++count) {
                                        VehicleLinkPair vehicleLinkPair = cars.get(0);
                                        cars.remove(0);
                                        // TODO choose better link
                                        Link rebalanceDest = new ArrayList<>( //
                                                vnTo.getLinks()).get(random.nextInt(vnTo.getLinks().size()));
                                        setVehicleRebalance(vehicleLinkPair, rebalanceDest); // send car to adjacent virtual node
                                        ++totalRebalanceEffective;
                                    }
                                }
                            } else {
                                System.out.println("no available vehicles inside vnode " + vectorIndex + " " + vnFrom.index);
                            }
                        }
                        if (totalRebalanceEffective != totalRebalanceDesired)
                            System.out.println(" !!! rebalance delta: " + totalRebalanceEffective + " < " + totalRebalanceDesired);
                    }

                    {
                        int totalPickupEffective = 0;
                        final Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                        final NavigableMap<Integer, List<MpcRequest>> virtualLinkRequestsMap = new TreeMap<>(mpcRequestsMap.values().stream() //
                                .collect(Collectors.groupingBy(mpcRequest -> mpcRequest.vectorIndex)));
                        for (int vectorIndex = 0; vectorIndex < m + n; ++vectorIndex) {
                            // ---
                            final VirtualNode vnFrom = vectorIndex < m ? //
                                    virtualNetwork.getVirtualLink(vectorIndex).getFrom() : virtualNetwork.getVirtualNode(vectorIndex - m);
                            if (availableVehicles.containsKey(vnFrom)) {
                                final List<VehicleLinkPair> cars = availableVehicles.get(vnFrom); // find cars
                                final int desiredPickup = requestVector.Get(vectorIndex).number().intValue();
                                if (0 < desiredPickup) {
                                    System.out.println(String.format("vl=%2d  cars=%d  pick=%3d  ", //
                                            vectorIndex, cars.size(), desiredPickup));
                                    { // handle requests
                                        final List<MpcRequest> requests = virtualLinkRequestsMap.containsKey(vectorIndex) ? //
                                                virtualLinkRequestsMap.get(vectorIndex) : Collections.emptyList();

                                        int min = Math.min(Math.min(desiredPickup, requests.size()), cars.size());
                                        for (int count = 0; count < min; ++count) {
                                            VehicleLinkPair vehicleLinkPair = cars.get(0);
                                            cars.remove(0);
                                            MpcRequest mpcRequest = requests.get(count);
                                            Link pickupLocation = mpcRequest.avRequest.getFromLink(); // where the customer is waiting right now
                                            System.out.println("set diversion for pickup");
                                            setVehicleDiversion(vehicleLinkPair, pickupLocation); // send car to customer
                                            ++totalPickupEffective;
                                        }
                                    }
                                }
                            } else {
                                System.out.println("no available vehicles inside vnode " + vectorIndex + " " + vnFrom.index);
                            }
                        }
                        if (totalPickupEffective != totalPickupDesired)
                            System.out.println(" !!! rebalance delta: " + totalPickupEffective + " < " + totalPickupDesired);

                    }
                }
            }
        }
    }

    @Override
    public String getInfoLine() {
        return String.format("%s RV=%s H=%s", //
                super.getInfoLine(), //
                total_rebalanceCount, //
                printVals.toString() //
        );
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

            final File virtualnetworkDir = new File(config.getParams().get("virtualNetworkDirectory"));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            {
                // TODO
                // final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork.xml");
                // System.out.println("" + virtualnetworkFile.getAbsoluteFile());
                // virtualNetwork = VirtualNetworkIO.fromXML(network, virtualnetworkFile);
                virtualNetwork = VirtualNetworkGet.readDefault(network);
                // travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkFile, virtualNetwork, "Ttime");
            }

            return new MPCDispatcher_1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher,
                    travelTimes);
        }
    }
}
