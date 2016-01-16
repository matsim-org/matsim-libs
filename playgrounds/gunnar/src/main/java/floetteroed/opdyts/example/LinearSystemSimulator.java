package floetteroed.opdyts.example;

import java.util.Random;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearSystemSimulator implements Simulator<VectorDecisionVariable> {

	private final Matrix _A;

	private final Matrix _B;

	private final double sigmaEps;

	private final Random rnd;

	private Vector x = null;

	private Vector u = null;

	public LinearSystemSimulator(final Matrix _A, final Matrix _B,
			final double sigmaEps, final Random rnd) {
		this._A = _A.copy();
		this._B = _B.copy();
		this.sigmaEps = sigmaEps;
		this.rnd = rnd;
		// the initial stationary point is all-zeros:
		this.x = new Vector(this._A.columnSize());
		this.u = new Vector(this._B.columnSize());
	}

	void setState(final Vector x) {
		this.x = x.copy();
	}

	void setDecisionVariable(final Vector u) {
		this.u = u.copy();
	}

	@Override
	public SimulatorState run(
			final TrajectorySampler<VectorDecisionVariable> evaluator) {
		return this.run(evaluator, null);
	}

	@Override
	public SimulatorState run(
			final TrajectorySampler<VectorDecisionVariable> evaluator,
			final SimulatorState initialState) {
		if (initialState != null) {
			this.x = ((VectorState) initialState).getX().copy();
		}
		evaluator.initialize();
		do {
			this.x = this._A.timesVectorFromRight(this.x);
			this.x.add(this._B.timesVectorFromRight(this.u));
			final Vector eps = Vector.newGaussian(this.x.size(), this.rnd);
			this.x.add(eps, this.sigmaEps);
			evaluator.afterIteration(new VectorState(this.x, this));
		} while (!evaluator.foundSolution());
		return new VectorState(this.x, this);
	}
}
