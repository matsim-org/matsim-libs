package floetteroed.utilities.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LatinHypercubeSampling {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private List<Double> minList = new ArrayList<>();

	private List<Double> maxList = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LatinHypercubeSampling(final Random rnd) {
		this.rnd = rnd;
	}

	public void addDimension(final double minValue, final double maxValue) {
		this.minList.add(minValue);
		this.maxList.add(maxValue);
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * 
	 * @param sampleAndIntervalCnt
	 *            number of samples, which is identical to the interval
	 *            discretization along each dimension
	 * 
	 * @return an array double[sample][i] where sample indexes the individual
	 *         draws and dim is then the vector index within each draw
	 */
	public double[][] draw(final int sampleAndIntervalCnt) {

		final double[][] result = new double[sampleAndIntervalCnt][sampleAndIntervalCnt];

		for (int dim = 0; dim < this.minList.size(); dim++) {

			/*
			 * Extract the smallest value (min) and the interval size
			 * (intervalSize) along the current dimension dim.
			 */
			final double minValue = this.minList.get(dim);
			final double intervalSize = (this.maxList.get(dim) - minValue) / sampleAndIntervalCnt;

			/*
			 * Split the value range along the current dimension dim into
			 * sampleAndIntervalCnt equally large intervals and draw one value
			 * uniformly from each interval.
			 */
			final List<Double> valuesAlongDimension = new ArrayList<>(sampleAndIntervalCnt);
			for (int interval = 0; interval < sampleAndIntervalCnt; interval++) {
				valuesAlongDimension.add(minValue + intervalSize * (interval + this.rnd.nextDouble()));
			}

			/*
			 * Allocate one randomly selected (hence the shuffle) value along
			 * the given dimension to each sample.
			 */
			Collections.shuffle(valuesAlongDimension);
			for (int sample = 0; sample < sampleAndIntervalCnt; sample++) {
				result[sample][dim] = valuesAlongDimension.get(sample);
			}

		}
		return result;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		
		Random rnd = new Random();
		LatinHypercubeSampling sampler = new LatinHypercubeSampling(rnd);
		sampler.addDimension(0, 1);
		sampler.addDimension(0, 1);
		
		int sampleSize = 10;
		double[][] result = sampler.draw(sampleSize);
		for (int sample = 0; sample < sampleSize; sample++) {
			// System.out.println(rnd.nextDouble() + "\t" + rnd.nextDouble());
			System.out.println(result[sample][0] + "\t" + result[sample][1]);
		}		

	}
}
