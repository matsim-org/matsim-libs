/**
 * 
 */
package playground.clruch.tensorUtils;

import java.util.Set;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.alg.TensorMap;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Norm;
import playground.clruch.utils.GlobalAssert;

/**
 * @author Claudio Ruch
 *
 */
public enum TensorOperations {
    ;

    /**
     * @param T
     *            tensor which will be normed for row-stochasticity
     */
    public static Tensor normToRowStochastic(Tensor Tin) {
        
        return TensorMap.of(v->Normalize.unlessZero(v,Norm._1), Tin, 1);
    }
    
    /**
     * 
     * @param t square tensor
     * @param r set of lines/cols that should be reduced
     * @return tensor without lines / cols in r
     */
    public static Tensor eliminateRowCols(Tensor t, Set<Integer> r) {
        GlobalAssert.that(Dimensions.of(t).get(0) == Dimensions.of(t).get(1)); // only square matrices
        GlobalAssert.that(Dimensions.of(t).get(0) >= r.size()); // only number of elements less or equal than size can be reduced.

        // eliminate rows
        Tensor rowred = Tensors.empty();
        for (int i = 0; i < Dimensions.of(t).get(0); ++i) {
            if (!r.contains(i)) {
                rowred.append(t.get(i));
            }
        }

        // eliminate cols
        Tensor red = Tensors.empty();
        for (int i = 0; i < Dimensions.of(t).get(0); ++i) {
            if (!r.contains(i)) {
                red.append((Transpose.of(rowred)).get(i));
            }
        }
        return Transpose.of(red);

    }

}
