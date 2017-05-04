package playground.clruch.gfx;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Plus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;

/**
 * count vehicles
 */
class RequestCountVirtualNodeFunction {
    final MatsimStaticDatabase db;
    final VirtualNetwork virtualNetwork;

    public RequestCountVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        this.db = db;
        this.virtualNetwork = virtualNetwork;
    }

    Tensor evaluate(SimulationObject ref) {
        Tensor count = Array.zeros(virtualNetwork.getvNodesCount());
        for (RequestContainer rc : ref.requests) {
            int linkIndex = rc.fromLinkIndex;
            Link link = db.getOsmLink(linkIndex).link;
            VirtualNode vn = virtualNetwork.getVirtualNode(link);
            count.set(Plus.ONE, vn.index);
        }
        return count;
    }
}
