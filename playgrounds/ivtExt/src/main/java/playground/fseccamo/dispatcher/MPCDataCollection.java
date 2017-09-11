/**
 * 
 */
package playground.fseccamo.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.Primitives;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Increment;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.core.VehicleOnVirtualLinkCalculator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
/* package */ class MPCDataCollection {

    private final int m;
    private final int n;
    private final VirtualNetwork<Link> virtualNetwork;
    private final Collection<AVRequest> unassignedRequests;
    private final List<RoboTaxi> styRoboTaxis;
    private final List<RoboTaxi> d2cRoboTaxis;
    private final List<RoboTaxi> dwcRoboTaxis;
    private final List<RoboTaxi> rebRoboTaxis;

    /**
     * 
     */
    public MPCDataCollection(VirtualNetwork<Link> virtualNetwork, Collection<AVRequest> unassignedRequests, //
            List<RoboTaxi> styRoboTaxis, List<RoboTaxi> d2cRoboTaxis, List<RoboTaxi> dwcRoboTaxis, List<RoboTaxi> rebRoboTaxis) {

        m = virtualNetwork.getvLinksCount();
        n = virtualNetwork.getvNodesCount();
        this.virtualNetwork = virtualNetwork;
        this.unassignedRequests = unassignedRequests;
        this.styRoboTaxis = styRoboTaxis;
        this.d2cRoboTaxis = d2cRoboTaxis;
        this.dwcRoboTaxis = dwcRoboTaxis;
        this.rebRoboTaxis = rebRoboTaxis;
    }

    /* package */public Container collectData(double now, Map<RoboTaxi, AVRequest> pickupPairs, MPCDispatcher mpcDispatcher) {

        GlobalAssert.that(styRoboTaxis.size() + d2cRoboTaxis.size() + dwcRoboTaxis.size() + rebRoboTaxis.size() == mpcDispatcher.numberOfVehicles);
        Container container = new Container(String.format("problem@%06d", Math.round(now)));
        addWaitInformation(container, mpcDispatcher, now);
        addVehicleInformation(container, mpcDispatcher, pickupPairs);
        return container;
    }

    private void addWaitInformation(Container container, MPCDispatcher mpcDispatcher, double now) {
        /** number of waiting customers that begin their journey on link_k = (node_i, node_j)
         * also: max waiting time in seconds of customers that begin their journey on link_k = (node_i, node_j) */
        Tensor waitCustomersPerVLink = Array.zeros(m + n); // +n accounts for self loop
        Tensor maxWaitingTimePerVLink = Array.zeros(m + n); // +n accounts for self loop
        for (AVRequest avRequest : unassignedRequests) { // requests that haven't received a dispatch yet
            waitCustomersPerVLink.set(Increment.ONE, mpcDispatcher.requestVectorIndexMap.get(avRequest));
            double waitTime = now - avRequest.getSubmissionTime();
            GlobalAssert.that(0 <= waitTime);
            maxWaitingTimePerVLink.set(Max.function(DoubleScalar.of(waitTime)), mpcDispatcher.requestVectorIndexMap.get(avRequest)); // TODO is this correct
        }
        {
            double[] array = Primitives.toArrayDouble(waitCustomersPerVLink);
            DoubleArray doubleArray = new DoubleArray("waitCustomersPerVLink", new int[] { array.length }, array);
            container.add(doubleArray);
            System.out.println("waitCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
        }
        {
            double[] array = Primitives.toArrayDouble(maxWaitingTimePerVLink);
            DoubleArray doubleArray = new DoubleArray("maxWaitingTimePerVLink", new int[] { array.length }, array);
            container.add(doubleArray);
            System.out.println("maxWaitingTimePerVLink=" + Tensors.vectorDouble(array) //
                    .flatten(0).map(Scalar.class::cast).reduce(Max::of).get());
        }

    }
    
    
    private void addVehicleInformation(Container container,MPCDispatcher mpcDispatcher,Map<RoboTaxi, AVRequest> pickupPairs){


        Scalar vehicleTotal = RealScalar.ZERO;
        Set<RoboTaxi> accountedVehicles = new HashSet<>();
        { // done
            /** STAY vehicles + vehicles without task inside VirtualNode */
            // all vehicles except the ones with a customer on board and the ones which
            // are rebalancing
            // Map<VirtualNode, List<RoboTaxi>> availableVehicles = getDivertableNotRebalancingNotPickupVehicles();
            Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles = virtualNetwork.createVNodeTypeMap();
            for (RoboTaxi robotaxi : styRoboTaxis) {
                VirtualNode vnode = virtualNetwork.getVirtualNode(robotaxi.getDivertableLocation());
                availableVehicles.get(vnode).add(robotaxi);
            }

            availableVehicles.values().stream().flatMap(List::stream).forEach(accountedVehicles::add);
            double[] array = new double[n];
            for (Entry<VirtualNode<Link>, List<RoboTaxi>> entry : availableVehicles.entrySet())
                array[entry.getKey().getIndex()] = entry.getValue().size(); // could use tensor notation
            DoubleArray doubleArray = new DoubleArray("availableVehiclesPerVNode", new int[] { array.length }, array);
            container.add(doubleArray);
            vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
            System.out.println("availableVehiclesPerVNode=" + Total.of(Tensors.vectorDouble(array)));
        }
        { // done
            /** rebalancing vehicles still within node_i traveling on link_k = (node_i, node_j) */
            accountedVehicles.addAll(rebRoboTaxis);
            final Tensor vector = VehicleOnVirtualLinkCalculator.countVehiclesPerVLink(rebRoboTaxis, virtualNetwork);
            double[] array = Primitives.toArrayDouble(vector);
            DoubleArray doubleArray = new DoubleArray("movingRebalancingVehiclesPerVLink", new int[] { array.length }, array);
            container.add(doubleArray);
            vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
            System.out.println("movingRebalancingVehiclesPerVLink=" + Total.of(Tensors.vectorDouble(array)));
        }
        { // done
            /** Vehicles with customers still within node_i traveling on link_k = (node_i, node_j) */
            // List<RoboTaxi> map = getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER);
            final Tensor vector = VehicleOnVirtualLinkCalculator.countVehiclesPerVLink(dwcRoboTaxis, virtualNetwork);
            accountedVehicles.addAll(dwcRoboTaxis);
            {
                // vehicles on pickup drive appear here
                for (RoboTaxi robotaxi : d2cRoboTaxis) {

                    // for (Entry<AVRequest, RoboTaxi> entry : getMatchings().entrySet()) {
                    AVRequest avRequest = pickupPairs.get(robotaxi);
                    GlobalAssert.that(avRequest != null);

                    if (!accountedVehicles.contains(robotaxi)) {
                        // request
                        int index = -1;
                        if (mpcDispatcher.requestVectorIndexMap.containsKey(avRequest))
                            index = mpcDispatcher.requestVectorIndexMap.get(avRequest);
                        else {
                            index = m + virtualNetwork.getVirtualNode(avRequest.getFromLink()).getIndex();
                            new RuntimeException("map should provide request info").printStackTrace();
                        }
                        vector.set(Increment.ONE, index);
                        accountedVehicles.add(robotaxi);
                    }
                }
            }
            double[] array = Primitives.toArrayDouble(vector);
            DoubleArray doubleArray = new DoubleArray("movingVehiclesWithCustomersPerVLink", new int[] { array.length }, array);
            container.add(doubleArray);
            vehicleTotal = vehicleTotal.add(Total.of(Tensors.vectorDouble(array)));
            System.out.println("movingVehiclesWithCustomersPerVLink=" + Total.of(Tensors.vectorDouble(array)));
        }

        if (!Chop._10.close(vehicleTotal, RealScalar.of(mpcDispatcher.numberOfVehicles))) {
            new RuntimeException("#vehiclesTotal=" + vehicleTotal).printStackTrace();
        }
        
        GlobalAssert.that(mpcDispatcher.numberOfVehicles == accountedVehicles.size());
    }
    

}
