/**
 * 
 */
package playground.clruch.dispatcher.utils;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Norm;

/** @author Claudio Ruch */
public enum PlaneEuclideanDistance {
    ;
    /** vector 2-norm */
    public static final double of(Tensor d1, Tensor d2) {
        return Norm._2.ofVector(d1.subtract(d2)).number().doubleValue();
    }

}
