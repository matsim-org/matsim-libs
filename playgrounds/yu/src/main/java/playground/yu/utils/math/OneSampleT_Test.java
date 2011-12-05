/**
 * 
 */
package playground.yu.utils.math;

import java.util.List;

/**
 * implementation of one-sample t-test testing the null hypothesis {@link http
 * ://en.wikipedia.org/wiki/Student%27s_t-test#One-sample_t-test}, calculates t
 * value
 * 
 * @author yu
 * 
 */
public class OneSampleT_Test {
	private static int N;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String paramFilename, outputFilename;
		int width;
		if (args.length != 3) {
			// paramFilename = "test/input/bln2pct/baseSyn3PureParams.log";
			paramFilename = "test/output/2car1ptRoutes/pc2params/outputTravPt-6constPt-3/pureParams.log";
			outputFilename = "test/output/2car1ptRoutes/pc2params/outputTravPt-6constPt-3/pureParams500Windows.log";
			width = 500;
		} else {
			paramFilename = args[0];
			outputFilename = args[1];
			width = Integer.parseInt(args[2]);
		}

		// TODO System.out.println("One-sample t-test\tt-value:\t" + new
		// OneSampleT_Test().gettValue());
	}

	private final double tValue;

	// TODO output degree of freedom

	public OneSampleT_Test(List<Double> sample, double mu) {
		N = sample.size();
		System.out.println("One-sample t-test\tdegree of freedom:\t" + (N - 1));
		double avgX = SimpleStatistics.average(sample);
		double sampleStandardDeviation = SimpleStatistics
		.sampleStandardDeviation(sample);
		tValue = Math.sqrt(N) * (avgX - mu) / sampleStandardDeviation;
	}

	public double gettValue() {
		return tValue;
	}
}
