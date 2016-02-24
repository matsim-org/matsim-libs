package floetteroed.opdyts.ntimestworoutes;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesObjectiveFunction implements VectorBasedObjectiveFunction {

	private Vector externalities;
	
	NTimesTwoRoutesObjectiveFunction(final Vector externalities) {
		this.externalities = externalities.copy();
	}

	private double q(final int i, final Vector state) {
		return state.get(i);
	}

	private double tt(final int i, final Vector state) {
		return state.get(i + this.externalities.size());
	}

	// TODO NEW
	@Override
	public double value(final Vector state) {
		double result = 0;
		for (int i = 0; i < this.externalities.size(); i++) {
			result += q(i, state) * (tt(i, state) + this.externalities.get(i));
		}
		return result;
	}

	// TODO NEW
	@Override
	public Vector gradient(final Vector state) {
		final Vector result = new Vector(state.size());
		for (int i = 0; i < this.externalities.size(); i++) {
			result.set(i, tt(i, state) + this.externalities.get(i));
			result.set(i + this.externalities.size(), q(i, state));
		}
		return result;
	}
}
