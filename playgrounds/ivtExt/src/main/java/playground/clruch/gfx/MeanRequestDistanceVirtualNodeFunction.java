// code by jph
package playground.clruch.gfx;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;

/**
 * mean request distance
 */
/* package */ class MeanRequestDistanceVirtualNodeFunction extends AbstractVirtualNodeFunction {

    public MeanRequestDistanceVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        super(db, virtualNetwork);
    }

    @Override
    public Tensor evaluate(SimulationObject ref) {
        Tensor collect = Tensors.vector(i -> Tensors.empty(), virtualNetwork.getvNodesCount());
        for (RequestContainer rc : ref.requests) {
            Link linkAnte = db.getOsmLink(rc.fromLinkIndex).link;
            Link linkPost = db.getOsmLink(rc.toLinkIndex).link;
            double distance = Math.hypot( //
                    linkAnte.getCoord().getX() - linkPost.getCoord().getX(), //
                    linkAnte.getCoord().getY() - linkPost.getCoord().getY());
            VirtualNode vn = virtualNetwork.getVirtualNode(linkAnte);
            collect.set(s -> s.append(DoubleScalar.of(distance)), vn.getIndex());
        }
        return Tensors.vector(i -> meanOrZero(collect.get(i)), virtualNetwork.getvNodesCount());
    }

    private static Scalar meanOrZero(Tensor vector) {
        if (vector.length() == 0)
            return RealScalar.ZERO;
        return Mean.of(vector).Get();
    }
}
