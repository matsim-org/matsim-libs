package floetteroed.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Discretizer {

	// -------------------- CONSTRUCTION --------------------

	private Discretizer() {
		// do not instantiate
	}

	// -------------------- IMPLEMENTATION --------------------

	static public List<Double> interpolateOrder0(final List<Double> times_s,
			final List<Double> values, final double startTime_s,
			final double binSize_s, final int binCnt) {
		return interpolate(times_s, values, startTime_s, binSize_s, binCnt,
				false);
	}

	static public List<Double> interpolateOrder1(final List<Double> times_s,
			final List<Double> values, final double startTime_s,
			final double binSize_s, final int binCnt) {
		return interpolate(times_s, values, startTime_s, binSize_s, binCnt,
				true);
	}

	static private List<Double> interpolate(final List<Double> times_s,
			final List<Double> values, final double startTime_s,
			final double binSize_s, final int binCnt,
			final boolean interpolatePoints) {
		if (times_s == null || times_s.size() == 0) {
			throw new IllegalArgumentException("list of times is null or empty");
		}
		if (values == null || values.size() == 0) {
			throw new IllegalArgumentException(
					"list of values is null or empty");
		}
		if (times_s.size() != values.size()) {
			throw new IllegalArgumentException(
					"list sizes of times and values do not match: "
							+ times_s.size() + " vs. " + values.size());
		}
		final List<Double> result = new ArrayList<Double>(binCnt);
		int nextIndex = 0;
		for (int bin = 0; bin < binCnt; bin++) {
			/*
			 * (1) compute the time for which to interpolate a value (use an
			 * outer loop with integer indices to avoid floating point error
			 * accumulation)
			 */
			final double currentTime_s = startTime_s + bin * binSize_s;
			/*
			 * (2) try to find the data point right after the current
			 * interpolation time
			 */
			while (times_s.get(nextIndex) < currentTime_s
					&& nextIndex < times_s.size() - 1) {
				nextIndex++;
			}
			/*
			 * (3) compute and store the interpolated value
			 */
			if (times_s.size() == 1 || nextIndex == 0
					|| times_s.get(nextIndex) < currentTime_s) {
				/*
				 * (3.A) we have just one interpolation point or we are before
				 * or after all interpolation points
				 */
				result.add(values.get(nextIndex));
			} else {
				/*
				 * (3.B) we are within the interpolation points
				 */
				final double w;
				if (interpolatePoints) {
					w = (times_s.get(nextIndex) - currentTime_s)
							/ (times_s.get(nextIndex) - times_s
									.get(nextIndex - 1));
				} else {
					w = 1.0;
				}
				result.add(w * values.get(nextIndex - 1) + (1.0 - w)
						* values.get(nextIndex));
			}
		}
		return result;
	}
}
