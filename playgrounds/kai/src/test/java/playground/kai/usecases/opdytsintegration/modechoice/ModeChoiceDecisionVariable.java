package playground.kai.usecases.opdytsintegration.modechoice;

import floetteroed.opdyts.DecisionVariable;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceDecisionVariable implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	private final double betaPay;

	private final double betaAlloc;

	// -------------------- CONSTRUCTION --------------------

	public ModeChoiceDecisionVariable(final double betaPay, final double betaAlloc) {
		this.betaPay = betaPay;
		this.betaAlloc = betaAlloc;
	}

	// -------------------- GETTERS --------------------

	public double betaPay() {
		return this.betaPay;
	}

	public double betaAlloc() {
		return this.betaAlloc;
	}

	// --------------- IMPLEMENTATION OF MATSimDecisionVariable ---------------

	@Override public void implementInSimulation() {
		throw new RuntimeException("not implemented") ;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return "(betaPay = " + this.betaPay + ", betaAlloc = " + this.betaAlloc
				+ ")";
	}

}
