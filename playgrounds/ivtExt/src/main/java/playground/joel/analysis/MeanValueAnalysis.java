package playground.joel.analysis;

import java.math.BigDecimal;
import java.util.function.Function;

import ch.ethz.idsc.tensor.DecimalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import ch.ethz.idsc.tensor.sca.Round;
import playground.clruch.utils.GlobalAssert;

/**
 * @author Claudio Ruch Performns mean value analysis for a system with i nodes, relative throughputs pii, arrival rates mui
 */



public class MeanValueAnalysis {
    
    public static Function<Scalar, Scalar> NICE = s -> Round.toMultipleOf(DecimalScalar.of(new BigDecimal("0.0001"))).apply(s);
    
    final int maxAgents;
    final int numNodes;
    final Tensor mui;
    final Tensor pii;
    final Tensor muiInv;

    Tensor W;
    Tensor L;

    public MeanValueAnalysis(int maxAgentsIn, Tensor muiIn, Tensor piiIn) {
        maxAgents = maxAgentsIn;
        mui = muiIn;
        pii = piiIn;
        muiInv = InvertUnlessZero.of(mui);
        GlobalAssert.that(Dimensions.of(mui).get(1).equals(Dimensions.of(pii).get(0)));
        numNodes = Dimensions.of(pii).get(0);
        W = Tensors.matrix((i, j) -> RealScalar.ZERO, maxAgentsIn + 1, numNodes);
        L = Tensors.matrix((i, j) -> RealScalar.ZERO, maxAgentsIn + 1, numNodes);        
        perform();
        
        
        
        
    }

    private void perform() {
        // L_i(0) = 0 was initialized in the constructor
        // W_i(0) = 0 was initialized in the constructor
        for (int agents = 1; agents <= maxAgents; ++agents) {
            updateW(agents);
            updateL(agents);
        }
    }

    private void updateW(int agents) {
        Tensor OnePlusLi = L.get(agents - 1).add(Tensors.vector(i_ -> RealScalar.ONE, numNodes));
        W.set(muiInv.get(agents).pmul(OnePlusLi), agents);
    }

    private void updateL(int agents) {
        Tensor numerator = W.get(agents).pmul(pii).multiply(RealScalar.of(agents));
        Tensor denominator = pii.dot(W.get(agents));
        L.set(numerator.multiply((Scalar) InvertUnlessZero.of(denominator)), agents);

    }

    // external getter methods
    public Tensor getW() {
        return W.copy();
    }

    public Tensor getW(int agents) {
        return W.get(agents);
    }

    public Tensor getL() {
        return L.copy();
    }

    public Tensor getL(int agents) {
        return L.get(agents);
    }
}
