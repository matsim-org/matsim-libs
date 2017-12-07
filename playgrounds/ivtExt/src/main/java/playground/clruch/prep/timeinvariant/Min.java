/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.List;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;

/** @author Claudio Ruch */
enum Min {
    ;

    public static Tensor of(Tensor... tensors) {
        List<Integer> dim = Dimensions.of(tensors[0]);
        for (int i = 0; i < tensors.length; ++i) {
            GlobalAssert.that(Dimensions.of(tensors[i]).equals(dim));
        }
        GlobalAssert.that(Dimensions.of(tensors[0]).size() == 1);

        Tensor min = tensors[0].copy();
        for(int i =0; i<tensors.length;++i){
            min = Min.of2(min,tensors[i]);
        }
        return min;

    }

    /** @param a Tensor of type vector
     * @param b Tensor of type vector
     * @return element-wise minimum */
    private static Tensor of2(Tensor a, Tensor b) {
        GlobalAssert.that(Dimensions.of(a).equals(Dimensions.of(b)));

        Tensor min = Tensors.empty();
        for (int i = 0; i < a.length(); ++i) {
            if (Scalars.lessEquals(a.Get(i), b.Get(i))) {
                min.append(a.Get(i));
            } else {
                min.append(b.Get(i));
            }
        }
        return min;
    }

}
