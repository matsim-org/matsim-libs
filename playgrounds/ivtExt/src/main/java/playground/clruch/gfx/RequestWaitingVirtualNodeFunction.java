// code by jph
package playground.clruch.gfx;

import java.util.function.Function;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Median;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;

/**
 * mean request waiting time
 */
public class RequestWaitingVirtualNodeFunction extends AbstractVirtualNodeFunction {

    private final Function<Tensor, Scalar> function;

    public RequestWaitingVirtualNodeFunction( //
            MatsimStaticDatabase db, VirtualNetwork virtualNetwork, Function<Tensor, Scalar> function) {
        super(db, virtualNetwork);
        this.function = function;
    }

    @Override
    public Tensor evaluate(SimulationObject ref) {
        Tensor collect = Tensors.vector(i -> Tensors.empty(), virtualNetwork.getvNodesCount());
        for (RequestContainer rc : ref.requests) {
            double duration = ref.now - rc.submissionTime;
            Link linkAnte = db.getOsmLink(rc.fromLinkIndex).link;
            // Link linkPost = db.getOsmLink(rc.toLinkIndex).link;
            VirtualNode vn = virtualNetwork.getVirtualNode(linkAnte);
            collect.set(s -> s.append(DoubleScalar.of(duration)), vn.getIndex());
        }
        return Tensors.vector(i -> function.apply(collect.get(i)), virtualNetwork.getvNodesCount());
    }

    /************************************************************/

    // THESE FUNCTIONS SHOULD BE EXTRACTED IF THEY TURN OUT TO BE USEFUL
    // -----------------------------------------------------------------
    public static Scalar meanOrZero(Tensor vector) {
        if (vector.length() == 0)
            return RealScalar.ZERO;
        return Mean.of(vector).Get();
    }

    public static Scalar medianOrZero(Tensor vector) {
        if (vector.length() == 0)
            return RealScalar.ZERO;
        return Median.of(vector).Get();
    }

    public static Scalar maxOrZero(Tensor vector) {
        return vector.flatten(0) //
                .map(Scalar.class::cast) //
                .reduce(Max::of).orElse(RealScalar.ZERO);
    }
}
