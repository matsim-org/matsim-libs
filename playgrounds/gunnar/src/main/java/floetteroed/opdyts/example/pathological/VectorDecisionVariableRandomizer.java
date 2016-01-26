package floetteroed.opdyts.example.pathological;

import static floetteroed.utilities.math.Vector.diff;
import static floetteroed.utilities.math.Vector.newGaussian;
import static floetteroed.utilities.math.Vector.sum;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VectorDecisionVariableRandomizer implements
		DecisionVariableRandomizer<VectorDecisionVariable> {

	private final int dim;

	private final double sigma;

	private final Random rnd;

	private final LinearSystemSimulator system;

	private final Vector min;

	private final Vector max;

	public VectorDecisionVariableRandomizer(final int dim, final double sigma,
			final Random rnd, final LinearSystemSimulator system,
			final Vector min, final Vector max) {
		this.dim = dim;
		this.sigma = sigma;
		this.rnd = rnd;
		this.system = system;
		this.min = min;
		this.max = max;
	}

	// @Override
	// public VectorDecisionVariable newRandomDecisionVariable() {
	// // TODO returns all-zeros: not random, and should not be
	// return new VectorDecisionVariable(new Vector(this.dim), this.system);
	// }

	@Override
	public Collection<VectorDecisionVariable> newRandomVariations(
			final VectorDecisionVariable decisionVariable) {
		final Vector delta = newGaussian(this.dim, this.rnd);
		delta.mult(this.sigma);

		final Vector u1 = sum(decisionVariable.getU(), delta);
		u1.constrain(this.min, this.max);
		final VectorDecisionVariable result1 = new VectorDecisionVariable(u1,
				this.system);

		final Vector u2 = diff(decisionVariable.getU(), delta);
		u2.constrain(this.min, this.max);
		final VectorDecisionVariable result2 = new VectorDecisionVariable(u2,
				this.system);

		return Arrays.asList(result1, result2);
	}
}
