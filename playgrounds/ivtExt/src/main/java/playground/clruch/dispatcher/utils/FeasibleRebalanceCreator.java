// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.sca.Floor;
import playground.clruch.dispatcher.core.RoboTaxi;

public enum FeasibleRebalanceCreator {
    ;
    /** @param rebalanceInput
     * @param availableVehicles
     * @return returns a scaled rebalanceInput which is feasible considering the available number of
     *         vehicles */
    public static Tensor returnFeasibleRebalance(Tensor rebalanceInput, Map<VirtualNode<Link>, //
            List<RoboTaxi>> availableVehicles) {

        GlobalAssert.that(Dimensions.of(rebalanceInput).get(0) == Dimensions.of(rebalanceInput).get(1));
        GlobalAssert.that(Dimensions.of(rebalanceInput).get(0) == availableVehicles.size());
        GlobalAssert.that(!rebalanceInput.flatten(-1).filter(v -> Scalars.lessThan((Scalar) v, RealScalar.ZERO)).findAny().isPresent());

        Tensor feasibleRebalance = rebalanceInput.copy();
        for (int i = 0; i < Dimensions.of(rebalanceInput).get(0); ++i) {
            // count number of outgoing vehicles per vNode
            double outgoingNmrvNode = 0.0;
            Tensor outgoingVehicles = rebalanceInput.get(i);
            for (int j = 0; j < Dimensions.of(rebalanceInput).get(0); ++j) {
                outgoingNmrvNode = outgoingNmrvNode + (Integer) (outgoingVehicles.Get(j)).number();
            }
            int outgoingVeh = (int) outgoingNmrvNode;
            int finalI = i;
            int availableVehvNode = availableVehicles //
                    .get(availableVehicles.keySet().stream().filter(v -> v.getIndex() == finalI).findAny().get()).size();
            // if number of outoing vehicles too small, reduce proportionally
            if (availableVehvNode < outgoingVeh) {
                long shrinkingFactor = ((long) availableVehvNode / ((long) outgoingVeh));
                Tensor newRow = Floor.of(rebalanceInput.get(i).multiply(RealScalar.of(shrinkingFactor)));
                feasibleRebalance.set(newRow, i);
            }
        }
        return feasibleRebalance;
    }

}
