/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.utilities.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;

/**
 * Linear programming solution for a least absolute deviation minimizing
 * regression estimator. More robust against outliers than usual least squares.
 * 
 * TODO Add dimensionality checks etc.
 * 
 * TODO Move main function into junit test.
 * 
 * @see https://en.wikipedia.org/wiki/Least_absolute_deviations
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LeastAbsoluteDeviations {

	// -------------------- MEMBERS --------------------

	private final List<Vector> xList = new ArrayList<Vector>();

	private final List<Double> yList = new ArrayList<Double>();

	private final List<Double> deltaList = new ArrayList<Double>();

	private Vector lowerBounds = null;

	private Vector upperBounds = null;

	private Vector coeffs = null;

	private Double error = null;

	// -------------------- CONSTRUCTION --------------------

	public LeastAbsoluteDeviations() {
	}

	public void add(final Vector x, final double y, final double delta) {
		this.xList.add(x);
		this.yList.add(y);
		this.deltaList.add(delta);
	}

	public void add(final Vector x, final double y) {
		this.add(x, y, 0.0);
	}

	// -------------------- IMPLEMENTATION --------------------

	public int size() {
		return this.xList.size();
	}

	public Integer xDim() {
		if (this.xList.isEmpty()) {
			return null;
		} else {
			return this.xList.get(0).size();
		}
	}

	public void setLowerBounds(double... bounds) {
		this.lowerBounds = new Vector(bounds);
	}

	public void setUpperBounds(double... bounds) {
		this.upperBounds = new Vector(bounds);
	}

	public void solve() {

		/*
		 * Decision variables in the linear program are
		 * 
		 * [u(1) ... u(N) b(1) ... b(M)]
		 * 
		 * with u being N=this.size() dummies representing absolute errors per
		 * measurement and b being M=this.xDim() actual regression variables.
		 */

		/*
		 * Create objective function.
		 */
		final LinearObjectiveFunction objFct;
		{
			final double[] objFctCoeffs = new double[this.size() + this.xDim()];
			for (int i = 0; i < this.size(); i++) {
				objFctCoeffs[i] = 1.0;
			}
			objFct = new LinearObjectiveFunction(objFctCoeffs, 0.0);
		}

		/*
		 * Create constraints.
		 */
		final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(
				3 * this.size() + (this.lowerBounds == null ? 0 : this.xDim())
						+ (this.upperBounds == null ? 0 : this.xDim()));
		{
			/*
			 * Three constraints per measurement that couple the dummy decision
			 * variables to regression residuals.
			 */
			for (int i = 0; i < this.size(); i++) {
				final double[] constrCoeffs1 = new double[this.size()
						+ this.xDim()];
				final double[] constrCoeffs2 = new double[this.size()
						+ this.xDim()];
				final double[] constrCoeffs3 = new double[this.size()
						+ this.xDim()];
				constrCoeffs1[i] = 1.0;
				constrCoeffs2[i] = 1.0;
				constrCoeffs3[i] = 1.0;
				for (int j = 0; j < this.xDim(); j++) {
					constrCoeffs1[this.size() + j] = +this.xList.get(i).get(j);
					constrCoeffs2[this.size() + j] = -this.xList.get(i).get(j);
				}
				constraints.add(new LinearConstraint(constrCoeffs1,
						Relationship.GEQ, +this.yList.get(i)
								- this.deltaList.get(i)));
				constraints.add(new LinearConstraint(constrCoeffs2,
						Relationship.GEQ, -this.yList.get(i)
								- this.deltaList.get(i)));
				constraints.add(new LinearConstraint(constrCoeffs3,
						Relationship.GEQ, 0.0));
			}

			/*
			 * If applicable, one constraint per lower bound per regression
			 * model coefficient.
			 */
			if (this.lowerBounds != null) {
				for (int i = 0; i < this.xDim(); i++) {
					final double[] constrCoeffs = new double[this.size()
							+ this.xDim()];
					constrCoeffs[this.size() + i] = 1.0;
					constraints.add(new LinearConstraint(constrCoeffs,
							Relationship.GEQ, this.lowerBounds.get(i)));
				}
			}

			/*
			 * If applicable, one constraint per upper bound per regression
			 * model coefficient.
			 */
			if (this.upperBounds != null) {
				for (int i = 0; i < this.xDim(); i++) {
					final double[] constrCoeffs = new double[this.size()
							+ this.xDim()];
					constrCoeffs[this.size() + i] = 1.0;
					constraints.add(new LinearConstraint(constrCoeffs,
							Relationship.LEQ, this.upperBounds.get(i)));
				}
			}
		}

		/*
		 * Solve the linear program and take over the result.
		 */
		final PointValuePair result = (new SimplexSolver()).optimize(objFct,
				new LinearConstraintSet(constraints));
		this.coeffs = new Vector(this.xDim());
		for (int i = 0; i < this.xDim(); i++) {
			this.coeffs.set(i, result.getPoint()[this.size() + i]);
		}
		this.error = result.getValue();
	}

	public Vector getCoefficients() {
		return this.coeffs.copy();
	}

	public Double getError() {
		return this.error;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		final Random rnd = new Random();
		final LeastAbsoluteDeviations lad = new LeastAbsoluteDeviations();
		final Vector coeffs = new Vector(1.0, -2.0, 3.0, -4.0);
		System.out.println(coeffs);
		for (int i = 0; i < 500; i++) {
			final Vector x = Vector.newGaussian(coeffs.size(), rnd);
			lad.add(x, coeffs.innerProd(x) + 0.0 * rnd.nextGaussian());
		}
		lad.solve();
		System.out.println(lad.coeffs);
		System.out.println("... DONE");
	}
}
