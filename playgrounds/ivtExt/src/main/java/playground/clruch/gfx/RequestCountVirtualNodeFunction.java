package playground.clruch.gfx;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Increment;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;

/**
 * count requests
 */
class RequestCountVirtualNodeFunction extends AbstractVirtualNodeFunction {
    public RequestCountVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        super(db, virtualNetwork);
    }

    @Override
    public Tensor evaluate(SimulationObject ref) {
        Tensor count = Array.zeros(virtualNetwork.getvNodesCount());
        for (RequestContainer rc : ref.requests) {
            int linkIndex = rc.fromLinkIndex;
            Link link = db.getOsmLink(linkIndex).link;
            VirtualNode vn = virtualNetwork.getVirtualNode(link);
            count.set(Increment.ONE, vn.index);
        }
        return count;
    }
}
