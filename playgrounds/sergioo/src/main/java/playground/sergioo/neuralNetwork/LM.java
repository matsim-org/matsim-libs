package playground.sergioo.neuralNetwork;

public class LM {

	/**
	 * calculate the current sum-squared-error
	 * (Chi-squared is the distribution of squared Gaussian errors,
	 * thus the name)
	 */
	/*static double chiSquared(double[][] x, double[] a, double[] y, double[] s, LMfunc f)
	{
		int npts = y.length;
		double sum = 0.;

		for( int i = 0; i < npts; i++ ) {
			double d = y[i] - f.val(x[i], a);
			d = d / s[i];
			sum = sum + (d*d);
		}

		return sum;
	} //chiSquared*/


	/**
	 * Minimize E = sum {(y[k] - f(x[k],a)) / s[k]}^2
	 * The individual errors are optionally scaled by s[k].
	 * Note that LMfunc implements the value and gradient of f(x,a),
	 * NOT the value and gradient of E with respect to a!
	 * 
	 * @param x array of domain points, each may be multidimensional
	 * @param y corresponding array of values
	 * @param a the parameters/state of the model
	 * @param vary false to indicate the corresponding a[k] is to be held fixed
	 * @param s2 sigma^2 for point i
	 * @param lambda blend between steepest descent (lambda high) and
	 *	jump to bottom of quadratic (lambda zero).
	 * 	Start with 0.001.
	 * @param termepsilon termination accuracy (0.01)
	 * @param maxiter	stop and return after this many iterations if not done
	 * @param verbose	set to zero (no prints), 1, 2
	 *
	 * @return the new lambda for future iterations.
	 *  Can use this and maxiter to interleave the LM descent with some other
	 *  task, setting maxiter to something small.
	 */
	/*public static double solve(double[][] x, double[] a, double[] y, double[] s,
			boolean[] vary, LMfunc f,
			double lambda, double termepsilon, int maxiter,
			int verbose)
					throws Exception
	{
		int npts = y.length;
		int nparm = a.length;
		assert s.length == npts;
		assert x.length == npts;
		if (verbose > 0) {
			System.out.print("solve x["+x.length+"]["+x[0].length+"]" );
			System.out.print(" a["+a.length+"]");
			System.out.println(" y["+y.length+"]");
		}

		double e0 = chiSquared(x, a, y, s, f);
		//double lambda = 0.001;
		boolean done = false;

		// g = gradient, H = hessian, d = step to minimum
		// H d = -g, solve for d
		double[][] H = new double[nparm][nparm];
		double[] g = new double[nparm];
		//double[] d = new double[nparm];

		double[] oos2 = new double[s.length];
		for( int i = 0; i < npts; i++ )  oos2[i] = 1./(s[i]*s[i]);

		int iter = 0;
		int term = 0;	// termination count test

		do {
			++iter;

			// hessian approximation
			for( int r = 0; r < nparm; r++ ) {
				for( int c = 0; c < nparm; c++ ) {
					for( int i = 0; i < npts; i++ ) {
						if (i == 0) H[r][c] = 0.;
						double[] xi = x[i];
						H[r][c] += (oos2[i] * f.grad(xi, a, r) * f.grad(xi, a, c));
					}  //npts
				} //c
			} //r

			// boost diagonal towards gradient descent
			for( int r = 0; r < nparm; r++ )
				H[r][r] *= (1. + lambda);

			// gradient
			for( int r = 0; r < nparm; r++ ) {
				for( int i = 0; i < npts; i++ ) {
					if (i == 0) g[r] = 0.;
					double[] xi = x[i];
					g[r] += (oos2[i] * (y[i]-f.val(xi,a)) * f.grad(xi, a, r));
				}
			} //npts

			// scale (for consistency with NR, not necessary)
			if (false) {
				for( int r = 0; r < nparm; r++ ) {
					g[r] = -0.5 * g[r];
					for( int c = 0; c < nparm; c++ ) {
						H[r][c] *= 0.5;
					}
				}
			}

			// solve H d = -g, evaluate error at new location
			//double[] d = DoubleMatrix.solve(H, g);
			double[] d = (new Matrix(H)).lu().solve(new Matrix(g, nparm)).getRowPackedCopy();
			//double[] na = DoubleVector.add(a, d);
			double[] na = (new Matrix(a, nparm)).plus(new Matrix(d, nparm)).getRowPackedCopy();
			double e1 = chiSquared(x, na, y, s, f);

			if (verbose > 0) {
				System.out.println("\n\niteration "+iter+" lambda = "+lambda);
				System.out.print("a = ");
				(new Matrix(a, nparm)).print(10, 2);
				if (verbose > 1) {
					System.out.print("H = "); 
					(new Matrix(H)).print(10, 2);
					System.out.print("g = "); 
					(new Matrix(g, nparm)).print(10, 2);
					System.out.print("d = "); 
					(new Matrix(d, nparm)).print(10, 2);
				}
				System.out.print("e0 = " + e0 + ": ");
				System.out.print("moved from ");
				(new Matrix(a, nparm)).print(10, 2);
				System.out.print("e1 = " + e1 + ": ");
				if (e1 < e0) {
					System.out.print("to ");
					(new Matrix(na, nparm)).print(10, 2);
				}
				else {
					System.out.println("move rejected");
				}
			}

			// termination test (slightly different than NR)
			if (Math.abs(e1-e0) > termepsilon) {
				term = 0;
			}
			else {
				term++;
				if (term == 4) {
					System.out.println("terminating after " + iter + " iterations");
					done = true;
				}
			}
			if (iter >= maxiter) done = true;

			// in the C++ version, found that changing this to e1 >= e0
			// was not a good idea.  See comment there.
			//
			if (e1 > e0 || Double.isNaN(e1)) { // new location worse than before
				lambda *= 10.;
			}
			else {		// new location better, accept new parameters
				lambda *= 0.1;
				e0 = e1;
				// simply assigning a = na will not get results copied back to caller
				for( int i = 0; i < nparm; i++ ) {
					if (vary[i]) a[i] = na[i];
				}
			}

		} while(!done);

		return lambda;
	} //solve*/

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
