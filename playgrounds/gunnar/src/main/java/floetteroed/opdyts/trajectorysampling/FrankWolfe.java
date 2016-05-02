package floetteroed.opdyts.trajectorysampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.log4j.Logger;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FrankWolfe {

	// -------------------- MEMBERS --------------------

	private final MultivariateFunction objectiveFunction;

	private final MultivariateVectorFunction gradientFunction;

	private final LineSearch lineSearch;

	// private final int dim;

	private final int maxIts = 1000;

	private final double eps;

	private Double value = null;

	private double[] point = null;
	
	// -------------------- CONSTRUCTION --------------------

	public FrankWolfe(final MultivariateFunction objectiveFunction,
			final MultivariateVectorFunction gradientFunction,
			final LineSearch lineSearch,
			// final int dim,
			final double eps) {
		this.objectiveFunction = objectiveFunction;
		this.gradientFunction = gradientFunction;
		this.lineSearch = lineSearch;
		// this.dim = dim;
		this.eps = eps;
	}

	// -------------------- INTERNALS --------------------

	public static double innerProd(final double[] a, final double[] b) {
		double result = 0;
		for (int i = 0; i < a.length; i++) {
			result += a[i] * b[i];
		}
		return result;
	}

	private double[] sum(final double[] a, final double weightA,
			final double[] b, final double weightB) {
		final double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			result[i] = weightA * a[i] + weightB * b[i];
		}
		return result;
	}

	private double maxAbs(final double[] v) {
		double result = 0;
		for (int i = 0; i < v.length; i++) {
			result = Math.max(result, Math.abs(v[i]));
		}
		return result;
	}

	private double sum(final double[] v) {
		double result = 0;
		for (double val : v) {
			result += val;
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double[] getPoint() {
		return this.point;
	}

	public double getValue() {
		return this.value;
	}

	public void run(final double[] initialPoint) {

		int it = 0;

		this.point = new double[initialPoint.length];
		System.arraycopy(initialPoint, 0, this.point, 0, initialPoint.length);
		// Arrays.fill(this.point, 1.0 / this.dim);
		this.value = this.objectiveFunction.value(this.point);
		double residual = 0.0;

		do {
			it++;

			/*-
			 * Construct linear objective function.
			 * 
			 * Q(x) = Q(x0) + grad * (x - x0)
			 *      = grad * x + [Q(x0) - grad * x0] 
			 * 
			 */
			final double[] grad = this.gradientFunction.value(this.point);
			final LinearObjectiveFunction linearObjFctApprox = new LinearObjectiveFunction(
					grad, this.value - innerProd(grad, this.point));

			/*
			 * Construct constraints.
			 */
			final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(
					initialPoint.length + 1);
			// non-negative
			for (int i = 0; i < initialPoint.length; i++) {
				final double[] coeffs = new double[initialPoint.length];
				coeffs[i] = 1.0;
				constraints.add(new LinearConstraint(coeffs, Relationship.GEQ,
						0.0));
			}
			// sum is one
			final double coeffs[] = new double[initialPoint.length];
			Arrays.fill(coeffs, 1.0);
			constraints.add(new LinearConstraint(coeffs, Relationship.EQ, 1.0));

			/*
			 * Solve the problem.
			 */
			final PointValuePair result = (new SimplexSolver()).optimize(
					linearObjFctApprox, new LinearConstraintSet(constraints));

			/*
			 * Update solution point.
			 */
			if (lineSearch == null) {
				final double innoWeight = 1.0 / it;
				final double[] newPoint = this.sum(result.getPoint(),
						innoWeight, this.point, 1.0 - innoWeight);
				final double[] dir = this.sum(newPoint, 1.0, this.point, -1.0);
				// Logger.getLogger(this.getClass().getName()).info(
				// "dir=" + (new Vector(dir)));
				// residual = this.maxAbs(dir);
				this.point = newPoint;
				this.value = this.objectiveFunction.value(this.point);
			} else {
				final double[] dir = this.sum(result.getPoint(), 1.0,
						this.point, -1.0);
				double eta = this.lineSearch.stepLength(this.point, dir);
				// Logger.getLogger(this.getClass().getName()).info(
				// "eta(orig)=" + eta);
				eta = Math.max(0, Math.min(1.0, eta));
				// Logger.getLogger(this.getClass().getName()).info(
				// "dir=" + (new Vector(dir)));
				// Logger.getLogger(this.getClass().getName()).info(
				// "eta(constr)=" + eta);
				// residual = Math.abs(eta) * Math.sqrt(innerProd(dir, dir)); //
				// Math.abs(eta) * this.maxAbs(dir);
				this.point = this.sum(this.point, 1.0, dir, eta);
				this.value = this.objectiveFunction.value(this.point);
			}

			residual = Math.abs(linearObjFctApprox.value(this.point)
					- this.value);

//			Logger.getLogger(this.getClass().getName()).info(
//					"it " + it + ": val = " + this.value + ", residual="
//							+ residual);

		} while ((residual > this.eps) && (it < this.maxIts));
	}

	public static interface LineSearch {
		public double stepLength(double[] point, double[] dir);
	}
}
