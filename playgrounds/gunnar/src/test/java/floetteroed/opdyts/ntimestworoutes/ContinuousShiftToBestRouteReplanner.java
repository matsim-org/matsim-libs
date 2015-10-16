package floetteroed.opdyts.ntimestworoutes;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ContinuousShiftToBestRouteReplanner implements TwoRoutesReplanner {

	private final double demand;

	private final double deltaCostAtWhichSwitchingIsCertain;

	private double q1;

	ContinuousShiftToBestRouteReplanner(final double demand,
			final double deltaCostAtWhichSwitchingIsCertain) {
		this.demand = demand;
		this.deltaCostAtWhichSwitchingIsCertain = deltaCostAtWhichSwitchingIsCertain;
		this.setQ1(0.5 * demand);
	}

	@Override
	public void setQ1(final double q1) {
		this.q1 = q1;
	}

	@Override
	public void update(final double c1, final double c2) {
		final double replanningProbability = max(0, min(1, abs(c1 - c2)
				/ this.deltaCostAtWhichSwitchingIsCertain));
		if (c1 < c2) {
			this.q1 += replanningProbability * (this.demand - this.q1);
		} else if (c1 > c2) {
			this.q1 -= replanningProbability * this.q1;
		}
		this.q1 = Math.max(this.q1, 0);
		this.q1 = Math.min(this.q1, this.demand);
	}

	@Override
	public double getRealizedQ1() {
		return this.q1;
	}

	@Override
	public double getRealizedQ2() {
		return this.demand - this.q1;
	}
}
