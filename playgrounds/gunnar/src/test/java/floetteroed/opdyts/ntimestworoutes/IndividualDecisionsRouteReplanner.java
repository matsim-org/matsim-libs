package floetteroed.opdyts.ntimestworoutes;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Random;

import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class IndividualDecisionsRouteReplanner implements TwoRoutesReplanner {

	private final int demand;

	private Double fixedReplanningProbability = 1.0;

	private Double deltaCostAtWhichSwitchingIsCertain = null;

	private final boolean individualChoices;

	private final Random rnd;

	private int q1;

	public IndividualDecisionsRouteReplanner(final int demand,
			final boolean individualChoices, final Random rnd) {
		this.demand = demand;
		this.individualChoices = individualChoices;
		this.rnd = rnd;
		this.setQ1(0.5 * demand);
	}

	public void setFixedReplanningProbability(
			final double fixedReplanningProbability) {
		this.fixedReplanningProbability = fixedReplanningProbability;
		this.deltaCostAtWhichSwitchingIsCertain = null;
	}

	public void setDeltaCostAtWhichSwitchingIsCertain(
			final double deltaCostAtWhichSwitchingIsCertain) {
		this.fixedReplanningProbability = null;
		this.deltaCostAtWhichSwitchingIsCertain = deltaCostAtWhichSwitchingIsCertain;
	}

	@Override
	public void setQ1(final double q1) {
		this.q1 = MathHelpers.round(q1);
	}

	@Override
	public void update(double cost1, double cost2) {
		
		final double replanningProbability;
		if (this.fixedReplanningProbability != null) {
			replanningProbability = this.fixedReplanningProbability;
		} else {
			replanningProbability = max(
					0,
					min(1, abs(cost1 - cost2)
							/ this.deltaCostAtWhichSwitchingIsCertain));
		}
		
		int deltaQ1 = 0;
		if (cost1 > cost2) {
			// route 1 is more expensive
			if (this.individualChoices) {
				for (int n = 0; n < this.q1; n++) {
					if (this.rnd.nextDouble() < replanningProbability) {
						deltaQ1--;
					}
				}
			} else {
				deltaQ1 = -MathHelpers.round(replanningProbability * this.q1);
			}
		} else if (cost1 < cost2) {
			// route 2 is more expensive
			if (this.individualChoices) {
				for (int n = this.q1; n < this.demand; n++) {
					if (this.rnd.nextDouble() < replanningProbability) {
						deltaQ1++;
					}
				}
			} else {
				deltaQ1 = +MathHelpers.round(replanningProbability
						* (this.demand - this.q1));
			}
		}
		this.q1 += deltaQ1;
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
