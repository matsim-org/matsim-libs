package floetteroed.opdyts.ntimestworoutes;

import floetteroed.utilities.math.Vector;

public class NTimesTwoRoutesObjectiveFunction_exact implements
		VectorBasedObjectiveFunction {

	private Vector externalities;

	private final double capacity;

	NTimesTwoRoutesObjectiveFunction_exact(final Vector externalities,
			final double capacity) {
		this.externalities = externalities.copy();
		this.capacity = capacity;
	}

	// TODO NEW
	@Override
	public double value(final Vector state) {
		double result = 0;
		for (int i = 0; i < state.size(); i++) {
			result += state.get(i)
					* (Math.pow(state.get(i) / this.capacity, 2.0) + this.externalities
							.get(i));
		}
		return result;
	}

	// TODO NEW
	@Override
	public Vector gradient(final Vector state) {
		final Vector result = new Vector(state.size());
		for (int i = 0; i < state.size(); i++) {
			result.set(i, 3.0 * Math.pow(state.get(i) / this.capacity, 2.0)
					+ this.externalities.get(i));
		}
		return result;
	}
}
