/**
 * 
 */
package playground.joel.analysis.utils;

import java.util.HashSet;
import java.util.Set;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.tensorUtils.TensorOperations;
import playground.clruch.utils.GlobalAssert;

/**
 * @author Claudio Ruch
 *
 */
public enum ThroughputCalculator {
    ;

    /**
     * 
     * @param tp
     *            row-stochastic matrix where M(i,j) is transition probability from i to j
     * @return relative throughputs normed and positive
     * @throws InterruptedException
     */

    public static Tensor getRelativeThroughputOfi(Tensor tp) throws InterruptedException {
        int size = Dimensions.of(tp).get(0);
        GlobalAssert.that(size == Dimensions.of(tp).get(1)); // only square matrices

        // 1) find lines with zero sum and create reduced matrix
        Set<Integer> zeroLines = new HashSet<>();
        Set<Integer> nonzeroLines = new HashSet<>();
        for (int i = 0; i < size; ++i) {
            RealScalar sum = (RealScalar) Total.of(tp.get(i));
            if (sum.number().doubleValue() == 0.0)
                zeroLines.add(i);
            else
                nonzeroLines.add(i);
        }

        Tensor tpReduced = TensorOperations.eliminateRowCols(tp, zeroLines);
        int nonZeroSize = nonzeroLines.size();

        // 2) calculate throughput for reduced system
        Tensor IminusPki = IdentityMatrix.of(nonzeroLines.size()).subtract(Transpose.of(tpReduced));
        Tensor relativeThroughput = NullSpace.of(IminusPki);

        // if there are several solution, there are disjoint subcycles
        Tensor rt = Array.zeros(nonZeroSize);
        for (int i = 0; i < Dimensions.of(relativeThroughput).get(0); ++i) {
            rt = rt.add(relativeThroughput.get(i));

        }

        // normalize
        rt = Normalize.unlessZero(rt, Norm._1);// TensorOperations.normToRowStochastic(rt);

        // 3) build full throughput and return
        Tensor throughput = Array.zeros(size);
        int k = 0;
        for (int i = 0; i < size; ++i) {
            if (nonzeroLines.contains(i)) {
                throughput.set(rt.Get(k), i);
                ++k;
            }
        }
        
        return throughput;

    }

}
