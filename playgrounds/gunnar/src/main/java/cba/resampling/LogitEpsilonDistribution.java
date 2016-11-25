package cba.resampling;

import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.util.FastMath;

/**
 * Gumbel distribution with zero mean and configurable scale.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LogitEpsilonDistribution implements EpsilonDistribution {

	private final static double GAMMA = FastMath.PI / (2 * FastMath.E);

	private final GumbelDistribution gumbel;

	public LogitEpsilonDistribution(final double scale) {
		this.gumbel = new GumbelDistribution(-GAMMA * scale, scale);
	}

	@Override
	public double nextEpsilon() {
		return this.gumbel.sample();
	}
	
	public static void main(String[] args) {
		final LogitEpsilonDistribution distr = new LogitEpsilonDistribution(1.0);
		System.out.println("mean: " + distr.gumbel.getNumericalMean());
		System.out.println("variance: " + distr.gumbel.getNumericalVariance());
	}
}
