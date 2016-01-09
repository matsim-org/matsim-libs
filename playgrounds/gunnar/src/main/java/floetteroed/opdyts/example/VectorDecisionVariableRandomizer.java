package floetteroed.opdyts.example;

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

	public VectorDecisionVariableRandomizer(final int dim, final double sigma,
			final Random rnd, final LinearSystemSimulator system) {
		this.dim = dim;
		this.sigma = sigma;
		this.rnd = rnd;
		this.system = system;
	}

	@Override
	public VectorDecisionVariable newRandomDecisionVariable() {
		// TODO not random, and should not be
		return new VectorDecisionVariable(new Vector(this.dim), this.system);
	}

	@Override
	public Collection<VectorDecisionVariable> newRandomVariations(
			final VectorDecisionVariable decisionVariable) {
		final Vector delta = newGaussian(this.dim, this.rnd);
		delta.mult(this.sigma);
		final VectorDecisionVariable result1 = new VectorDecisionVariable(sum(
				decisionVariable.getVector(), delta), this.system);
		final VectorDecisionVariable result2 = new VectorDecisionVariable(diff(
				decisionVariable.getVector(), delta), this.system);
		return Arrays.asList(result1, result2);
	}
}
