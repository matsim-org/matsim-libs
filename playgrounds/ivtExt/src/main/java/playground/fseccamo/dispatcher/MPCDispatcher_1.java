/**
 * Dispatcher implementing the linear program from Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.fseccamo.dispatcher;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkLoader;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.netdata.vLinkDataReader;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class MPCDispatcher_1 extends PartitionedDispatcher {
    public final int samplingPeriod;
    // public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    Tensor printVals = Tensors.empty();
    LPVehicleRebalancing lpVehicleRebalancing;

    JavaContainerSocket javaContainerSocket;

    public MPCDispatcher_1( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            Map<VirtualLink, Double> travelTimesIn) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;

        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        samplingPeriod = Integer.parseInt(config.getParams().get("samplingPeriod"));
        // redispatchPeriod = Integer.parseInt(config.getParams().get("redispatchPeriod"));
        // setup linear program
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork, travelTimes);

        try {
            final int m = virtualNetwork.getvLinksCount();
            final int n = virtualNetwork.getvNodesCount();
            // TODO
            javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));
            // ---
            {
                Container container = new Container("init");
                {
                    double[] array = new double[n * m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("E_in", new int[] { n, m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[n * m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("E_out", new int[] { n, m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("P", new int[] { m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("Q", new int[] { m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("T", new int[] { m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[m];
                    DoubleArray doubleArray = new DoubleArray("C", new int[] { m }, array);
                    container.add(doubleArray);
                }
                {
                    double[] array = new double[] { numberOfAVs };
                    GlobalAssert.that(0 < numberOfAVs);
                    DoubleArray doubleArray = new DoubleArray("N_cars", new int[] { 1 }, array);
                    container.add(doubleArray);
                }
                { // normalized per seconds
                    double[] array = new double[m]; // TODO
                    DoubleArray doubleArray = new DoubleArray("lambda", new int[] { m }, array);
                    container.add(doubleArray);
                }
                { // normalized per seconds
                    double[] array = new double[] { 10.0 };
                    DoubleArray doubleArray = new DoubleArray("samplingPeriod", new int[] { 1 }, array);
                    container.add(doubleArray);
                }
                javaContainerSocket.writeContainer(container);
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

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

                { // send
                  // INPUT TO MPC:
                  // 1) CREATE A MAP <VirtualLink, Integer> waitingCustomerpervLink where forall virtual link ij the number of customer requests for that
                  // link c_ij is stored. Take care: if a requests is from a vNode to another one without direct virtualLink then the
                  // path has to be split according to the virtual Network.
                  // 2) <VirtualNode, Integer> availalbeVehiclesperVirutalNode --> use Map<VirtualNode, List<VehicleLinkPair>> availableVehiclesAll=
                  // getVirtualNodeAvailableVehicles();
                  // 3) <VirtualLink, Integer> numberofCustomerCarryingVehicles --> new function needs to be implemented
                  // 4) <VirtualLink, Integer> numberofRebalancingVehicles --> new function needs to implemented

                    Container container = new Container("problem");
                    {
                        /**
                         * number of waiting customers that begin their journey on link_k = (node_i, node_j)
                         */
                        double[] array = new double[m]; // TODO
                        DoubleArray doubleArray = new DoubleArray("waitCustomersPerVLink", new int[] { m }, array);
                        container.add(doubleArray);
                    }
                    {
                        /**
                         * STAY vehicles + vehicles without task inside VirtualNode
                         */
                        // all vehicles except the ones with a customer on board and the ones which are rebalancing
                        Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                        double[] array = new double[n];
                        for (Entry<VirtualNode, List<VehicleLinkPair>> entry : availableVehicles.entrySet())
                            array[entry.getKey().index] = entry.getValue().size();

                        DoubleArray doubleArray = new DoubleArray("availableVehiclesPerVNode", new int[] { n }, array);
                        container.add(doubleArray);
                    }
                    {
                        /**
                         * Vehicles with customers still within node_i traveling on link_k = (node_i, node_j)
                         */
                        double[] array = new double[m]; // TODO
                        DoubleArray doubleArray = new DoubleArray("movingVehiclesWithCustomersPerVLink", new int[] { m }, array);
                        container.add(doubleArray);
                    }
                    {
                        /**
                         * rebalancing vehicles still within node_i traveling on link_k = (node_i, node_j)
                         */
                        double[] array = new double[m]; // TODO
                        DoubleArray doubleArray = new DoubleArray("movingRebalancingVehiclesPerVLink", new int[] { m }, array);
                        container.add(doubleArray);
                    }
                    javaContainerSocket.writeContainer(container);
                }
                // COMPUTE MPC OUTSIDE OF MATSIM:

                { // recv
                  // USE THE RETURN VALUE TO MAKE COMMANDS IN MATSIM:
                  // 1) <VirtualLink, Integer> numberOfCustomerCarryingVehicles: the number of vehicles at VirtualLink ij which should take a request and
                  // transport
                  // it to a virtualNode
                  // for all these u_ij select u_ij customers in vNode i which have any shortest path that with sequence (vN1i, vNj, whatever, ... )
                  // 2) <VirtualLink, Integer> numberofRebalancingVehicles : for every VirtualLink select this number of vehicles vehicle in the fromVNode and
                  // send it
                  // to the to VNode
                  // use existing commands

                    // this waits until a reply has been received:
                    Container container = javaContainerSocket.blocking_getContainer();
                    System.out.println("received: " + container);

                    // container.id == 'solution'
                    {
                        DoubleArray doubleArray = container.get("pickupPerVLink");
                        /**
                         * find closest available cars to customers and pickup
                         */
                    }
                    {
                        DoubleArray doubleArray = container.get("rebalancingPerVLink");
                        /**
                         * find remaining available cars and do rebalance
                         */
                    }

                }
            }
            // all the open requests
            Collection<AVRequest> avrequest = getAVRequests();

            // TAKE CARE: Special case request from _ to same virtual Node.
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
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork.xml");
                System.out.println("" + virtualnetworkFile.getAbsoluteFile());
                virtualNetwork = VirtualNetworkLoader.fromXML(network, virtualnetworkFile);
                travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkFile, virtualNetwork, "Ttime");
            }

            return new MPCDispatcher_1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher,
                    travelTimes);
        }
    }
}
