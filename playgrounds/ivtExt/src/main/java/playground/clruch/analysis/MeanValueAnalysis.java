package playground.clruch.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.sca.Increment;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.utils.GlobalAssert;

/** @author Claudio Ruch Performns mean value analysis for a system with i nodes, relative
 *         throughputs pii, arrival rates mui */

public class MeanValueAnalysis {

    final int maxAgents;
    final int numNodes;
    final Tensor mui;
    final Tensor pii;
    final Tensor muiInv;
    final int numStation;

    Tensor W;
    Tensor L;
    final Tensor A = Tensors.empty();

    public MeanValueAnalysis(Tensor muiIn, Tensor piiIn, int numStation) {
        maxAgents = Dimensions.of(muiIn).get(0) - 1;
        numNodes = Dimensions.of(piiIn).get(0);
        mui = muiIn.copy();
        pii = piiIn.copy();
        this.numStation = numStation;
        muiInv = InvertUnlessZero.of(mui);
        GlobalAssert.that(Dimensions.of(mui).get(1).equals(Dimensions.of(pii).get(0)));

        W = Array.zeros(maxAgents + 1, numNodes);
        L = Array.zeros(maxAgents + 1, numNodes);
        perform();

    }

    private void perform() {
        // L_i(0) = 0 was initialized in the constructor
        // W_i(0) = 0 was initialized in the constructor
        for (int agents = 1; agents <= maxAgents; ++agents) {
            updateW(agents);
            updateL(agents);

        }
        calcAvailabilities();
    }

    private void updateW(int agents) {
        Tensor OnePlusLi = L.get(agents - 1).map(Increment.ONE);
        W.set(muiInv.get(agents).pmul(OnePlusLi), agents);
    }

    private void updateL(int agents) {
        Tensor numerator = W.get(agents).pmul(pii).multiply(RealScalar.of(agents));
        Tensor denominator = pii.dot(W.get(agents));
        L.set(numerator.multiply((Scalar) InvertUnlessZero.of(denominator)), agents);

    }

    private void calcAvailabilities() {
        for (int i = 0; i <= maxAgents; ++i) {
            Tensor Li = getL(i);
            Tensor Wi = getW(i);
            A.append((Li.pmul(InvertUnlessZero.of(Wi))).pmul(InvertUnlessZero.of(mui.get(i))));
        }
    }

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

    public Tensor getA() {
        return A.copy();
    }

    public Tensor getA(int agents) {
        return A.get(agents);
    }

}
