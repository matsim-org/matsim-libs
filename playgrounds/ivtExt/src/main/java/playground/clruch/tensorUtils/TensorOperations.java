/**
 * 
 */
package playground.clruch.tensorUtils;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.alg.TensorMap;
import ch.ethz.idsc.tensor.red.Norm;

/** @author Claudio Ruch */
public enum TensorOperations {
    ;

    /** @param T
     *            tensor which will be normed for row-stochasticity */
    public static Tensor normToRowStochastic(Tensor Tin) {

        return TensorMap.of(v -> Normalize.unlessZero(v, Norm._1), Tin, 1);
    }

}
