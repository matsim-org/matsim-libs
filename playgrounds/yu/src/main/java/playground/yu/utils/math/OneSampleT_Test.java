/**
 * 
 */
package playground.yu.utils.math;

import java.util.List;

import playground.yu.utils.io.PCParameterReader;

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
		// int width;
		int window;
		double[] mus = new double[2];

		if (args.length < 4) {
			// paramFilename = "test/input/bln2pct/baseSyn3PureParams.log";
			paramFilename = "test/input/bln2pct/run1536/base.log";
			// outputFilename =
			// "test/output/2car1ptRoutes/pc2params/outputTravPt-6constPt-3/pureParams500Windows.log";
			// width = 500;
			window=50;
			mus[0] = -3;
			mus[1] = 0;
		} else {
			paramFilename = args[0];
			// outputFilename = args[1];
			// width = Integer.parseInt(args[2]);
			window=Integer.parseInt(args[1]);
			mus[0] = Double.parseDouble(args[2]);
			mus[1] = Double.parseDouble(args[3]);
		}

		PCParameterReader paramReader = new PCParameterReader();
		paramReader.readFile(paramFilename);

		int i = 0;
		for (String paramName : paramReader.getParameterNames()) {
			if (i < 2) {
				double mu = mus[i];
				System.out.println("One-sample t-test\tmu:\t" + mu);
				System.out.println("One-sample t-test\tt-value:\t"
						+ new OneSampleT_Test(paramReader
								.getParameter(paramName),window, mu).gettValue());
			}
			i++;
		}
	}

	private final double tValue;

	// TODO output degree of freedom

	public OneSampleT_Test(List<Double> sample, int window, double mu) {
		sample = sample.subList(sample.size() - window, sample.size());
		N = sample.size();
		System.out.println("One-sample t-test\tdegree of freedom:\t" + (N - 1));
		double avgX = SimpleStatistics.average(sample);
		System.out.println("One-sample t-test\taverage x:\t" + avgX);
		double sampleStandardDeviation = SimpleStatistics
				.sampleStandardDeviation(sample);
		System.out
		.println("One-sample t-test\ts (sample standard deviation):\t"
				+ sampleStandardDeviation);
		tValue = Math.sqrt(N) * (avgX - mu) / sampleStandardDeviation;
	}

	public double gettValue() {
		return tValue;
	}
}
