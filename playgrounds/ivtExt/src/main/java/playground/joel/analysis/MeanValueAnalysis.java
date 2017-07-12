package playground.joel.analysis;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Joel on 11.07.2017.
 */
public class MeanValueAnalysis {
    final int maxTokens;
    final int size;
    final Tensor mi;
    final Tensor pi;

    Tensor W = Tensors.empty();
    Tensor L;

    public MeanValueAnalysis(int maxTokensIn, Tensor miIn, Tensor piIn) {
        maxTokens = maxTokensIn;
        mi = miIn;
        pi = piIn;
        GlobalAssert.that(mi.length() == pi.length());
        size = mi.length();
        W = Array.zeros(maxTokens, size);
        L = Array.zeros(maxTokens, size);
    }

    public void perform() {
        // L_i(0) = 0 was initialized in the constructor
        for (int tokens = 1; tokens < maxTokens; tokens++) {
            updateW(tokens);
            updateL(tokens);
        }
    }

    private void updateW(int tokens) {
        Tensor Wt = L.get(tokens - 1).add(Array.of(e -> RealScalar.of(1), size)).pmul(mi.map(InvertUnlessZero.function));
        W.set(Wt, tokens);
        System.out.println("Wt " + Dimensions.of(Wt)+ " = " + Dimensions.of(Wt));
        System.out.println("W " + Dimensions.of(W)+ " = " + Dimensions.of(W));
    }

    private void updateL(int tokens) {
        Tensor piMulW = W.get(tokens).pmul(pi);
        Scalar norm = Total.of(piMulW).map(InvertUnlessZero.function).Get();
        Tensor Lt = piMulW.multiply(RealScalar.of(tokens)).multiply(norm);
        L.set(Lt, tokens);
        System.out.println("Lt " + Dimensions.of(Lt)+ " = " + Dimensions.of(Lt));
        System.out.println("L " + Dimensions.of(L)+ " = " + Dimensions.of(L));
    }


    // external getter methods
    public Tensor getW() {
        return W.copy();
    }

    public Tensor getW(int tokens) {
        return W.get(tokens).copy();
    }

    public Scalar getW(int tokens, int node) {
        return W.Get(tokens, node);
    }

    public Tensor getL() {
        return L.copy();
    }

    public Tensor getL(int tokens) {
        return L.get(tokens).copy();
    }

    public Scalar getL(int tokens, int node) {
        return L.Get(tokens, node);
    }
}
