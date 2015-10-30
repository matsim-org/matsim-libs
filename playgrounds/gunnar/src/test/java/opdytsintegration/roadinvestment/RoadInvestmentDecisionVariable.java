package opdytsintegration.roadinvestment;

import java.util.Map;


import floetteroed.opdyts.DecisionVariable;
import org.matsim.api.core.v01.network.Link;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadInvestmentDecisionVariable implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	private final double valueForMoneyFactor = 2.0;

	private final double betaPay;

	private final double betaAlloc;

	private final Map<Link, Double> link2freespeed;

	private final Map<Link, Double> link2capacity;

	// -------------------- CONSTRUCTION --------------------

	public RoadInvestmentDecisionVariable(final double betaPay,
			final double betaAlloc, final Map<Link, Double> link2freespeed,
			final Map<Link, Double> link2capacity) {
		this.betaPay = betaPay;
		this.betaAlloc = betaAlloc;
		this.link2freespeed = link2freespeed;
		this.link2capacity = link2capacity;
	}

	// -------------------- GETTERS --------------------

	public double betaPay() {
		return this.betaPay;
	}

	public double betaAlloc() {
		return this.betaAlloc;
	}

	// --------------- IMPLEMENTATION OF MATSimDecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		for (Link link : this.link2capacity.keySet()) {
			link.setCapacity((1.0 + this.betaAlloc() * this.betaPay()
					* this.valueForMoneyFactor)
					* this.link2capacity.get(link));
			link.setFreespeed((1.0 + (1.0 - this.betaAlloc()) * this.betaPay()
					* this.valueForMoneyFactor)
					* this.link2freespeed.get(link));
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return "(betaPay = " + this.betaPay + ", betaAlloc = " + this.betaAlloc
				+ ")";
	}

}
