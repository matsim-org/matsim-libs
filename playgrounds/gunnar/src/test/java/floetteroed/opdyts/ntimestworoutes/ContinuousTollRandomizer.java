package floetteroed.opdyts.ntimestworoutes;

import java.util.List;
import java.util.Random;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ContinuousTollRandomizer implements DecisionVariableRandomizer {

	// -------------------- CONSTANTS --------------------

	private final NTimesTwoRoutesSimulator simulator;

	private final int roadCnt;

	final double sigmaToll;

	final double maxToll;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	ContinuousTollRandomizer(final NTimesTwoRoutesSimulator simulator,
			final int roadCnt, final double sigmaToll, final double maxToll,
			final Random rnd) {
		this.simulator = simulator;
		this.roadCnt = roadCnt;
		this.sigmaToll = sigmaToll;
		this.maxToll = maxToll;
		this.rnd = rnd;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	public DecisionVariable newRandomDecisionVariable() {
		final Vector tollVector = new Vector(this.roadCnt);
		for (int i = 0; i < this.roadCnt; i++) {
			tollVector.set(i, this.maxToll * this.rnd.nextDouble());
		}
		return new NTimesTwoRoutesDecisionVariable(this.simulator, tollVector);
	}

	@Override
	public List<DecisionVariable> newRandomVariations(final DecisionVariable parent) {
		return null;
//		final Vector childTolls = ((NTimesTwoRoutesDecisionVariable) parent)
//				.getTolls().copy();
//		for (int i = 0; i < this.roadCnt; i++) {
//			double newVal = childTolls.get(i) + this.sigmaToll
//					* this.rnd.nextGaussian();
//			if (newVal > this.maxToll) {
//				newVal = this.maxToll;
//			} else if (newVal < 0.0) {
//				newVal = 0.0;
//			}
//			childTolls.set(i, newVal);
//		}
//		return new NTimesTwoRoutesDecisionVariable(this.simulator, childTolls);
	}
}
