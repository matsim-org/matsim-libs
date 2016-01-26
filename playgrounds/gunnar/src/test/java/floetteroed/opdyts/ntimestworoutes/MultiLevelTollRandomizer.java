package floetteroed.opdyts.ntimestworoutes;

import java.util.List;
import java.util.Random;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.math.Vector;

public class MultiLevelTollRandomizer implements DecisionVariableRandomizer {

	// -------------------- CONSTANTS --------------------

	private final NTimesTwoRoutesSimulator simulator;

	private final int roadCnt;

	final double deltaToll;

	final int tollBinCnt;

	private final double mutationProbability;

	private final int maxDeltaBin;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	MultiLevelTollRandomizer(final NTimesTwoRoutesSimulator simulator,
			final int roadCnt, final double deltaToll, final int tollBinCnt,
			final double mutationProbability, final int maxDeltaBin,
			final Random rnd) {
		this.simulator = simulator;
		this.roadCnt = roadCnt;
		this.deltaToll = deltaToll;
		this.tollBinCnt = tollBinCnt;
		this.mutationProbability = mutationProbability;
		this.maxDeltaBin = maxDeltaBin;
		this.rnd = rnd;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	private NTimesTwoRoutesDecisionVariable extremeToll() {
		final Vector tollVector = new Vector(this.roadCnt);
		for (int i = 0; i < this.roadCnt; i += 2) {
			tollVector.set(i, this.deltaToll * (this.tollBinCnt - 1));
		}
		return new NTimesTwoRoutesDecisionVariable(this.simulator, tollVector);
	}

	public DecisionVariable newRandomDecisionVariable() {
		final Vector tollVector = new Vector(this.roadCnt);
		for (int i = 0; i < this.roadCnt; i++) {
			tollVector.set(i,
					this.deltaToll * this.rnd.nextInt(this.tollBinCnt));
		}
		return new NTimesTwoRoutesDecisionVariable(this.simulator, tollVector);
		// return extremeToll();
	}

	@Override
	public List<DecisionVariable> newRandomVariations(final DecisionVariable parent) {
		return null;
//		final Vector childTolls = ((NTimesTwoRoutesDecisionVariable) parent)
//				.getTolls().copy();
//		for (int i = 0; i < this.roadCnt; i++) {
//			if (this.rnd.nextDouble() < this.mutationProbability) {
//				double val = childTolls.get(i);
//				if (this.rnd.nextBoolean()) {
//					// val += this.deltaToll;
//					val += (1 + this.rnd.nextInt(this.maxDeltaBin))
//							+ this.deltaToll;
//				} else {
//					// val -= this.deltaToll;
//					val -= (1 + this.rnd.nextInt(this.maxDeltaBin))
//							+ this.deltaToll;
//				}
//				val = Math.max(0,
//						Math.min((this.tollBinCnt - 1) * this.deltaToll, val));
//				childTolls.set(i, val);
//			}
//		}
//		return new NTimesTwoRoutesDecisionVariable(this.simulator, childTolls);
//		// return extremeToll();
	}
}
