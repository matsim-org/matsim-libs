/**
 * 
 */
package playground.clruch.dispatcher.core;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.Task;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Increment;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;

/** @author Claudio Ruch */
public enum VehicleOnVirtualLinkCalculator {
    ;

    public static Tensor countVehiclesPerVLink(List<RoboTaxi> rebalancingRoboTaxis, VirtualNetwork virtualNetwork) {
        final int m = virtualNetwork.getvLinksCount();
        final Tensor vector = Array.zeros(m + virtualNetwork.getvNodesCount()); // self
                                                                                // loops

        for (RoboTaxi robotaxi : rebalancingRoboTaxis) {
            final Link current = robotaxi.getCurrentDriveDestination();
            Task task = robotaxi.getSchedule().getCurrentTask();
            int vli = -1;
            if (task instanceof AVPickupTask) {
                List<? extends Task> list = robotaxi.getSchedule().getTasks();
                int taskIndex = list.indexOf(task); //
                task = list.get(taskIndex + 1); // immediately try next
                                                // condition with task as
                                                // AVDriveTask
            } // <- do not put "else" here
            if (task instanceof AVDriveTask) {
                vli = getVirtualLinkOfVehicle((AVDriveTask) task, current, virtualNetwork);
                if (vli == -1 ) {
                    // if no transition between virtual nodes is detected, ...
                    // then the vehicle is considered to remain within current
                    // virtual node
                    VirtualNode vnode = virtualNetwork.getVirtualNode(current);
                    vector.set(Increment.ONE, m + vnode.getIndex()); // self loop
                } else {
                    vector.set(Increment.ONE, vli);
                }
            }
            if (task instanceof AVDropoffTask) {
                // consider the vehicle on the self loop of current virtual node
                VirtualNode vnode = virtualNetwork.getVirtualNode(current);
                vector.set(Increment.ONE, m + vnode.getIndex()); // self loop
            }
        }
        return vector;
    }

    /** @param driveTask
     * @param current
     * @return virtual link index on which vehicle of drive task is traversing
     *         on, or NOLINKFOUND if such link cannot be identified */
    public static int getVirtualLinkOfVehicle(AVDriveTask driveTask, Link current, VirtualNetwork virtualNetwork) {
        return getNextVirtualLinkOnPath(driveTask.getPath(), current, virtualNetwork);
    }

    public static int getNextVirtualLinkOnPath(VrpPath vrpPath, Link current, VirtualNetwork virtualNetwork) {
        final int NOLINKFOUND = -1;
        VirtualNode fromIn = null;
        VirtualNode toIn = null;
        boolean fused = false;
        for (Link link : vrpPath) {
            fused |= link == current;
            if (fused) {
                if (fromIn == null)
                    fromIn = virtualNetwork.getVirtualNode(link);
                else {
                    VirtualNode candidate = virtualNetwork.getVirtualNode(link);
                    if (fromIn != candidate) {
                        toIn = candidate;
                        VirtualLink virtualLink = virtualNetwork.getVirtualLink(fromIn, toIn);
                        return virtualLink.getIndex();
                    }
                }
            }
        }
        // we can reach this point if vehicle is in last virtual node of path
        // System.out.println("failed to find virtual link of transition.");
        return NOLINKFOUND;
    }

}
