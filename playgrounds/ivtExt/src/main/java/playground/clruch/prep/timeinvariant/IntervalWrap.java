/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
enum IntervalWrap {
    ;

    public static Interval of(Interval... intervals) {
        Tensor lbWrap = Tensors.empty();
        Tensor ubWrap = Tensors.empty();

        lbWrap = intervals[0].getLb();
        for (int i = 0; i < intervals.length; ++i) {
            lbWrap = Min.of(lbWrap, intervals[i].getLb());
        }

        ubWrap = intervals[0].getUb();
        for (int i = 0; i < intervals.length; ++i) {
            ubWrap = Max.of(ubWrap, intervals[i].getUb());
        }

        return new Interval(lbWrap, ubWrap);
    }
    
    


}
