package playground.fseccamo.dispatcher;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Plus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

abstract class BaseMpcDispatcher extends PartitionedDispatcher {

    BaseMpcDispatcher( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
    }

    protected Tensor countVehiclesPerVLink(Map<AVVehicle, Link> map) {
        final Tensor vector = Array.zeros(virtualNetwork.getvLinksCount());
        for (Entry<AVVehicle, Link> entry : map.entrySet()) {
            final AVVehicle avVehicle = entry.getKey();
            final Link current = entry.getValue();
            Task task = avVehicle.getSchedule().getCurrentTask();
            // TODO
            AVDriveTask driveTask = (AVDriveTask) task;
            VrpPath vrpPath = driveTask.getPath();
            boolean fused = false;
            VirtualNode fromIn = null;
            VirtualNode toIn = null;

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
                            // TODO there will be a guarantee that this is non null
                            vector.set(Plus.ONE, virtualLink.index);
                            break;
                        }
                    }
                }
            }
            if (toIn == null) {
                System.out.println("failed to find dest.");
            }
        }
        return vector;
    }

}
