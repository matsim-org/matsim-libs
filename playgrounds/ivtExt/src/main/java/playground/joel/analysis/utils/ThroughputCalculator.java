/**
 * 
 */
package playground.joel.analysis.utils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.mat.NullSpace;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.tensorUtils.TensorOperations;
import playground.clruch.utils.GlobalAssert;

/** @author Claudio Ruch */
public enum ThroughputCalculator {
    ;

    /** @param pij
     *            row-stochastic matrix where M(i,j) is transition probability from i to j
     * @return relative throughputs normed and positive, if pij == 0, then zero is returned, if the matrix has no
     * steady-state throughput, zero is returned
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
        if(Scalars.isZero(tot)) return Array.zeros(1,size);


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
        
        if(Dimensions.of(tp).get(0) == 0){            
            System.err.println("INFO: no throughput found beause pij has full rank, outputting zero");
            return Array.zeros(1,size);
            
        }

        // ensure positivity of solution
        tp.flatten(-1).forEach(v -> GlobalAssert.that(Scalars.lessEquals(RealScalar.ZERO, (Scalar) v)));
        

        // return normed vector
        return TensorOperations.normToRowStochastic(tp);

    }

}

//// 1) find lines with zero sum and create reduced matrix
// Set<Integer> zeroLines = new HashSet<>();
// Set<Integer> nonzeroLines = new HashSet<>();
// for (int i = 0; i < size; ++i) {
// RealScalar sum = (RealScalar) Total.of(pij.get(i));
// if (sum.number().doubleValue() == 0.0)
// zeroLines.add(i);
// else
// nonzeroLines.add(i);
// }
//
// Tensor tpReduced = TensorOperations.eliminateRowCols(pij, zeroLines);
// int nonZeroSize = nonzeroLines.size();
//
//// 2) calculate throughput for reduced system
// Tensor IminusPki = IdentityMatrix.of(nonzeroLines.size()).subtract(Transpose.of(tpReduced));
// Tensor relativeThroughput = NullSpace.of(IminusPki);
//
//// System.out.println("relativeThroughput = " + Pretty.of(relativeThroughput));
//
//// if there are several solution, there are disjoint subcycles
// Tensor rt = Array.zeros(nonZeroSize);
// for (int i = 0; i < Dimensions.of(relativeThroughput).get(0); ++i) {
// rt = rt.add(relativeThroughput.get(i));
//
// }
//
//// normalize
// rt = Normalize.unlessZero(rt, Norm._1);// TensorOperations.normToRowStochastic(rt);
//
//// 3) build full throughput and return
// Tensor throughput = Array.zeros(size);
// int k = 0;
// for (int i = 0; i < size; ++i) {
// if (nonzeroLines.contains(i)) {
// throughput.set(rt.Get(k), i);
// ++k;
// }
// }
//
// return throughput;
