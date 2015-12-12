package gunnar.ihop2.roadpricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO More a general-purpose utility.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OrderedValuesRandomizer {

	// -------------------- INTERFACE DEFINITION --------------------

	public static interface ValueRandomizer {
		public double newValue();

		public double newValue(double oldValue);
	}

	// -------------------- MEMBERS --------------------

	private final ValueRandomizer valueRandomizer;

	// -------------------- CONSTRUCTION --------------------

	public OrderedValuesRandomizer(final ValueRandomizer valueRandomizer) {
		this.valueRandomizer = valueRandomizer;
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<Double> newRandomized(final int size) {
		List<Double> result;
		do {
			result = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				result.add(this.valueRandomizer.newValue());
			}
		} while (result.size() != size);
		Collections.sort(result);
		return result;
	}

	public List<Double> newRandomized(final List<Double> values) {
		List<Double> result;
		do {
			result = new ArrayList<>(values.size());
			for (Double value : values) {
				result.add(this.valueRandomizer.newValue(value));
			}
		} while (result.size() != values.size());
		Collections.sort(result);
		return result;
	}
}
