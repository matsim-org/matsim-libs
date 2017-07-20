package playground.joel.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.utils.GlobalAssert;

/**
 * @author Claudio Ruch Performns mean value analysis for a system with i nodes, relative throughputs pii, arrival rates mui
 */
public class MeanValueAnalysis {
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
        GlobalAssert.that(mui.length() == pii.length());
        numNodes = mui.length();
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
        Tensor OnePlusLi = (L.get(agents - 1)).add(Tensors.vector(i_ -> RealScalar.ONE, numNodes));
        W.set(muiInv.pmul(OnePlusLi), agents);
    }

    private void updateL(int agents) {
        Tensor numerator = (W.get(agents).pmul(pii)).multiply(RealScalar.of(agents));
        // Tensor denominator = mui.dot(W.get(agents));
        Tensor denominator = pii.dot(W.get(agents));
        L.set(numerator.multiply((Scalar) InvertUnlessZero.of(denominator)), agents);

    }

    // external getter methods
    public Tensor getW() {
        return W.copy();
    }

    public Tensor getW(int agents) {
        return W.get(agents).copy();
    }

    public Tensor getL() {
        return L.copy();
    }

    public Tensor getL(int agents) {
        return L.get(agents).copy();
    }
}
