package playground.joel.analysis;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Joel on 11.07.2017.
 */
public class MeanValueAnalysis {
    final int steps;
    final int size;
    final Tensor mi;
    final Tensor pi;

    Tensor W = Tensors.empty();
    Tensor L;

    public MeanValueAnalysis(int stepsIn, Tensor miIn, Tensor piIn) {
        steps = stepsIn;
        mi = miIn;
        pi = piIn;
        GlobalAssert.that(mi.length() == pi.length());
        size = mi.length();
        W = Array.zeros(steps);
        L = Array.zeros(steps);
    }

    public void perform() {
        W.set(Array.zeros(size), 0);
        L.set(Array.zeros(size), 0);
        for (int step = 1 ; step < steps; step++) {
            updateW(step);
            updateL(step);
        }
    }

    private void updateW(int step) {
        Tensor Wt = Tensors.empty();
        for (int i = 0; i < size; i++) {
            // TODO possible to replace x?x:x with InvertUnlessZero.apply if it was static
            Wt.append((L.get(step - 1, i).add(RealScalar.of(1))).multiply( //
                    Scalars.isZero(mi.Get(i)) ? mi.Get(i) : mi.Get(i).invert()));
        }
        W.set(Wt, step);
    }

    private void updateL(int step) {
        Tensor Lt = Tensors.empty();
        // TODO: use Tensor operations instead of for loop
        // Tensor piMulW = W.get(step).pmul(pi);
        Tensor piMulW = Tensors.empty();
        for (int i = 0; i < size; i++) {
            piMulW.append(W.Get(step, i)).multiply(pi.Get(i));
        }
        Scalar norm = Scalars.isZero(Total.of(piMulW).Get()) ? Total.of(piMulW).Get() : Total.of(piMulW).Get().invert();
        for (int i = 0; i < size; i++) {
            Lt.append((RealScalar.of(step).multiply(piMulW.Get(i))).multiply(norm));
        }
        L.set(Lt, step);
    }


    // external getter methods
    public Tensor getW() {
        return W.copy();
    }

    public Tensor getW(int step) {
        return W.get(step).copy();
    }

    public Scalar getW(int step, int node) {
        return W.Get(step, node);
    }

    public Tensor getL() {
        return L.copy();
    }

    public Tensor getL(int step) {
        return L.get(step).copy();
    }

    public Scalar getL(int step, int node) {
        return L.Get(step, node);
    }
}
