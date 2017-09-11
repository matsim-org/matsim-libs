// code by jph
package playground.clruch.gfx;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.sca.Increment;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

/**
 * count vehicles
 */
/* package */ class VehicleCountVirtualNodeFunction extends AbstractVirtualNodeFunction {
    public VehicleCountVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        super(db, virtualNetwork);
    }

    @Override
    public Tensor evaluate(SimulationObject ref) {
        Tensor count = Array.zeros(virtualNetwork.getvNodesCount());
        for (VehicleContainer vc : ref.vehicles) {
            int linkIndex = vc.linkIndex;
            Link link = db.getOsmLink(linkIndex).link;
            VirtualNode vn = virtualNetwork.getVirtualNode(link);
            count.set(Increment.ONE, vn.getIndex());
        }
        return count;
    }
}
