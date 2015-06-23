package utils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class QuadraticEquation {

	// CONSTANTS

	private final Double xMin;

	private final Double xMax;

	// CONSTRUCTION

	public QuadraticEquation(final double p, final double q) {
		final double sqrtArg = p * p / 4.0 - q;
		if (sqrtArg < 0.0) {
			this.xMin = null;
			this.xMax = null;
		} else {
			this.xMin = -p / 2.0 - Math.sqrt(sqrtArg);
			this.xMax = -p / 2.0 + Math.sqrt(sqrtArg);
		}
	}

	// GETTERS

	public boolean hasSolution() {
		return (this.xMin != null);
	}

	public double getSmallerSolution() {
		return this.xMin;
	}

	public double getLargerSolution() {
		return this.xMax;
	}

	// CONVENIENCE FUNCTIONS

	public Double boundToSolutionInterval(final double x, final double p,
			final double q) {
		final QuadraticEquation qe = new QuadraticEquation(p, q);
		if (qe.hasSolution()) {
			return Math.max(qe.getSmallerSolution(),
					Math.min(x, qe.getLargerSolution()));
		} else {
			return null;
		}

	}

}
