package floetteroed.opdyts.convergencecriteria;

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
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FindContrativeCenter {

	public FindContrativeCenter() {
		// TODO Auto-generated constructor stub
	}

	public static double[] filter(final double[] y) {

		/*
		 * The data is [y(1) ... y(K)].
		 * 
		 * The decision variables are
		 * 
		 * [r(1) ... r(K), x(1) ... x(K), d(1) ... d(K-1)]
		 * 
		 * with
		 * 
		 * x(i) the filtered values,
		 * 
		 * r(i) = |y(i) - x(i)|,
		 * 
		 * d(i) = |x(i+1) - x(i)|.
		 * 
		 * The objective function is
		 * 
		 * [1 ... 1, 0 ... 0, 0 ... 0]' [r(1) ... r(K), x(1) ... x(K), d(1) ...
		 * d(K-1)]
		 */
		final LinearObjectiveFunction objFct;
		{
			final double[] objFctCoeffs = new double[3 * y.length - 1];
			for (int i = 0; i < y.length; i++) {
				objFctCoeffs[i] = 1.0;
			}
			objFct = new LinearObjectiveFunction(objFctCoeffs, 0.0);
		}

		final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

		/*
		 * Two constraints per residual.
		 */
		for (int i = 0; i < y.length; i++) {

			// r(i) >= y(i) - x(i) <=> r(i) + x(i) >= y(i)
			final double[] constrCoeffs1 = new double[3 * y.length - 1];
			constrCoeffs1[i] = 1.0; // r(i)
			constrCoeffs1[i + y.length] = +1.0; // + x(i)
			constraints.add(new LinearConstraint(constrCoeffs1,
					Relationship.GEQ, +y[i]));

			// r(i) >= -(y(i) - x(i)) <=> r(i) - x(i) >= -y(i)
			final double[] constrCoeffs2 = new double[3 * y.length - 1];
			constrCoeffs2[i] = 1.0; // r(i)
			constrCoeffs2[i + y.length] = -1.0; // - x(i)
			constraints.add(new LinearConstraint(constrCoeffs2,
					Relationship.GEQ, -y[i]));
		}

		/*
		 * Two constraints per difference.
		 */
		for (int i = 0; i < y.length - 1; i++) {

			// d(i) >= +(x(i+1) - x(i)) <=> d(i) - x(i+1) + x(i) >= 0
			final double[] constrCoeffs1 = new double[3 * y.length - 1];
			constrCoeffs1[2 * y.length] = 1.0; // d(i)
			constrCoeffs1[y.length + i + 1] = -1.0; // -x(i+1)
			constrCoeffs1[y.length + i] = 1.0; // +x(i)
			constraints.add(new LinearConstraint(constrCoeffs1,
					Relationship.GEQ, 0.0));

			// d(i) >= -(x(i+1) - x(i)) <=> d(i) + x(i+1) - x(i) >= 0
			final double[] constrCoeffs2 = new double[3 * y.length - 1];
			constrCoeffs2[2 * y.length + i] = 1.0; // d(i)
			constrCoeffs2[y.length + i + 1] = +1.0; // +x(i+1)
			constrCoeffs2[y.length + i] = -1.0; // -x(i)
			constraints.add(new LinearConstraint(constrCoeffs2,
					Relationship.GEQ, 0.0));
		}

		/*
		 * And the difference shall go down:
		 */
		for (int i = 0; i < y.length - 2; i++) {
			// d(i+1) <= d(i) <=> -d(i) + d(i+1) <= 0
			final double[] constrCoeffs = new double[3 * y.length - 1];
			constrCoeffs[2 * y.length + i] = -1.0;
			constrCoeffs[2 * y.length + i + 1] = +1.0;
			constraints.add(new LinearConstraint(constrCoeffs,
					Relationship.LEQ, 0.0));
		}
		
		/*
		 * Run the solver.
		 */
		final PointValuePair result = (new SimplexSolver()).optimize(objFct,
				new LinearConstraintSet(constraints));

		final double[] x = new double[y.length];
		System.arraycopy(result.getPoint(), y.length, x, 0, y.length);
		return x;		
	}
	
	public static void main(String[] args) {
		
		final Random rnd = new Random();
		final int maxIts = 100;
		final double[] y = new double[maxIts];
		for (int i = 0; i < maxIts; i++) {
			y[i] = Math.exp(-i) + rnd.nextGaussian();
		}
		
		final double[] x = filter(y);
		
		for (int i = 0; i < maxIts; i++) {
			System.out.println(y[i] + "\t" + x[i]);
		}
		
	}

}
