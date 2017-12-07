/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;

/** @author Claudio Ruch */
class Interval {
    private final Tensor lb;
    private final Tensor ub;
    private final int n; // dimension

    public Interval(Tensor lb, Tensor ub) {
        GlobalAssert.that(Dimensions.of(ub).size() == 1);
        n = Dimensions.of(ub).get(0);
        GlobalAssert.that(Dimensions.of(ub).equals(Dimensions.of(lb)));
        this.ub = ub;
        this.lb = lb;
    }

    public boolean contains(Tensor p) {
        GlobalAssert.that(Dimensions.of(p).equals(Dimensions.of(ub)));
        boolean isContained = true;
        for (int i = 0; i < p.length(); ++i) {
            if (Scalars.lessThan(p.Get(i), lb.Get(i))) {
                isContained = false;
                break;
            }
            if (Scalars.lessThan(ub.Get(i), p.Get(i))) {
                isContained = false;
                break;
            }
        }
        return isContained;
    }

    public int getDim() {
        return n;
    }

    public Tensor getLength() {
        Tensor length = Tensors.empty();
        for (int i = 0; i < lb.length(); ++i) {
            Scalar li = ub.Get(i).subtract(lb.Get(i)); // TODO simplify
            if (Scalars.lessThan(li, RealScalar.ZERO)) {
                length.append(RealScalar.ZERO);
            } else {
                length.append(li);
            }
        }
        return length;
    }

    @Override
    public String toString() {
        String pVal = "";
        for (int i = 0; i < lb.length(); ++i) {
            pVal = pVal + ("x" + i + ":  [" + lb.Get(i) + "," + ub.Get(i) + "]\n");
        }

        return pVal;
    }

    /* package */ Tensor getLb() {
        return lb.copy();
    }

    /* package */ Tensor getUb() {
        return ub.copy();
    }

    @Override
    public boolean equals(Object intervalIn) {
        Interval interval = (Interval) intervalIn;
        boolean lbsame = interval.lb.equals(lb);
        boolean ubsame = interval.ub.equals(ub);
        return lbsame && ubsame;
    }

    public boolean contains(Interval interval) {
        return contains(interval.lb) && contains(interval.ub);
    }

}
