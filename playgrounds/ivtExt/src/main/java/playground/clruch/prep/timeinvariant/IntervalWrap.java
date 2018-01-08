/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Entrywise;

/** @author Claudio Ruch */
enum IntervalWrap {
    ;

    public static Interval of(Interval... intervals) {
        Tensor lbWrap = Tensors.empty();
        Tensor ubWrap = Tensors.empty();

        lbWrap = intervals[0].getLb();
        for (int i = 0; i < intervals.length; ++i) {
            lbWrap = Entrywise.min().of(lbWrap, intervals[i].getLb());
        }

        ubWrap = intervals[0].getUb();
        for (int i = 0; i < intervals.length; ++i) {
            ubWrap = Entrywise.max().of(ubWrap, intervals[i].getUb());
        }

        return new Interval(lbWrap, ubWrap);
    }

}
