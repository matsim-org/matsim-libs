/**
 * Dispatcher implementing the linear program from Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
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
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class MPCDispatcher_1 extends PartitionedDispatcher {
    public final int rebalancingPeriod;
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    Tensor printVals = Tensors.empty();
    LPVehicleRebalancing lpVehicleRebalancing;

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
        rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
        redispatchPeriod = Integer.parseInt(config.getParams().get("redispatchPeriod"));
        // setup linear program
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork, travelTimes);
    }

    @Override
    public void redispatch(double now) {
        
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());

        
        final long round_now = Math.round(now);
        if(round_now % rebalancingPeriod == 0){            
            // all vehicles except the ones with a customer on board
            Map<VirtualNode, List<VehicleLinkPair>> availableVehiclesAll= getVirtualNodeAvailableVehicles();
            
            // all vehicles except the ones with a customer on board and the ones which are rebalancing
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
            
            // all the open requests
            Collection<AVRequest> avrequest = getAVRequests();
            
            
            
            // INPUT TO MPC:
            // 1) CREATE A MAP  <VirtualLink, Integer> waitingCustomerpervLink where forall virtual link ij the number of customer requests for that
            // link c_ij is stored. Take care: if a requests is from a vNode to another one without direct virtualLink then the 
            // path has to be split according to the virtual Network. 
            // 2) <VirtualNode, Integer> availalbeVehiclesperVirutalNode   --> use Map<VirtualNode, List<VehicleLinkPair>> availableVehiclesAll= getVirtualNodeAvailableVehicles();
            // 3) <VirtualLink, Integer> numberofCustomerCarryingVehicles --> new function needs to be implemented
            // 4) <VirtualLink, Integer> numberofRebalancingVehicles --> new function needs to implemented
            
            // COMPUTE MPC OUTSIDE OF MATSIM:
            
            // USE THE RETURN VALUE TO MAKE COMMANDS IN MATSIM:  
            // 1) <VirtualLink, Integer> numberOfCustomerCarryingVehicles:   the number of vehicles at VirtualLink ij which should take a request and transport
            // it to a virtualNode
            // for all these u_ij select u_ij customers in vNode i which have any shortest path that with sequence (vN1i, vNj, whatever, ... )
            // 2) <VirtualLink, Integer> numberofRebalancingVehicles : for every VirtualLink select this number of vehicles vehicle in the fromVNode and send it to the to VNode
            // use existing commands
            
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

            return new MPCDispatcher_1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork,
                    abstractVirtualNodeDest, abstractVehicleDestMatcher, travelTimes);
        }
    }
}
