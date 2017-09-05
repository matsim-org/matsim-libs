/**
 * 
 */
package playground.joel.analysis.utils;

import ch.ethz.idsc.queuey.math.TensorOperations;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;

/** @author Claudio Ruch */
public enum ThroughputCalculator {
    ;

    /** @param pij
     *            row-stochastic matrix where M(i,j) is transition probability from i to j
     * @return relative throughputs normed and positive, if pij == 0, then zero is returned, if the
     *         matrix has no
     *         steady-state throughput, zero is returned
     * @throws InterruptedException */

    public static Tensor getRelativeThroughputOfi(Tensor pijInput) throws InterruptedException {
        Tensor pij = pijInput.copy();
        int size = Dimensions.of(pij).get(0);
        GlobalAssert.that(size == Dimensions.of(pij).get(1)); // only square matrices

        // transform to rational form
        pij = pij.multiply(RealScalar.of(1000000)).map(Round.FUNCTION);
        pij = TensorOperations.normToRowStochastic(pij);

        // ensure positivity of input
        pij.flatten(-1).forEach(v -> GlobalAssert.that(Scalars.lessEquals(RealScalar.ZERO, (Scalar) v)));

        // if pij == ZERO, return zero throughput
        Scalar tot = (Scalar) Total.of(Total.of(pij));
        if (Scalars.isZero(tot))
            return Array.zeros(1, size);

        // ensure input is row-stochastic
        Scalar totalofRows = (Scalar) Total.of(Total.of(pij));
        try {
            GlobalAssert.that(totalofRows.number().intValue() == size);
        } catch (Exception e) {
            System.err.println("INFO: input of throughputCalculator not row stochastic.");
        }

        // calculate throughput
        Tensor IminusPij = IdentityMatrix.of(size).subtract(Transpose.of(pij));
        Tensor tp = NullSpace.of(IminusPij);

        if (Dimensions.of(tp).get(0) == 0) {
            System.err.println("INFO: no throughput found beause pij has full rank, outputting zero");
            return Array.zeros(1, size);

        }

        // Asser that nodes without probability to leave from (sum_j pij = 0) have zero throughput
        for (int i = 0; i < size; ++i) {
            Scalar sumpij = (Scalar) Total.of(pij.get(i));
            if (Scalars.isZero(sumpij)) {
                Scalar tpi = (Scalar) Total.of((Transpose.of(tp)).get(i));
                GlobalAssert.that(Scalars.isZero(tpi));
            }
        }

        // ensure positivity of solution
        tp.flatten(-1).forEach(v -> GlobalAssert.that(Scalars.lessEquals(RealScalar.ZERO, (Scalar) v)));

        // return normed vector
        return TensorOperations.normToRowStochastic(tp);

    }
}
