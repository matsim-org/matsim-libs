package floetteroed.opdyts.ntimestworoutes;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesDecisionVariableRandomizer implements
		DecisionVariableRandomizer {

	// -------------------- CONSTANTS --------------------

	private final NTimesTwoRoutesSimulator simulator;

	private final int tollCnt;

	private final int roadCnt;

	private final double tollLevel;

	private final double mutationProbability;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	NTimesTwoRoutesDecisionVariableRandomizer(
			final NTimesTwoRoutesSimulator simulator, final int tollCnt,
			final int roadCnt, final double tollLevel,
			final double mutationProbability, final Random rnd) {
		this.simulator = simulator;
		this.tollCnt = tollCnt;
		this.roadCnt = roadCnt;
		this.tollLevel = tollLevel;
		this.mutationProbability = mutationProbability;
		this.rnd = rnd;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	private DecisionVariable newDecisionVariableFromTollLocations(
			final Set<Integer> locations) {
		final Vector tollVector = new Vector(this.roadCnt);
		for (Integer location : locations) {
			tollVector.set(location, this.tollLevel);
		}
		return new NTimesTwoRoutesDecisionVariable(this.simulator, tollVector);
	}

	@Override
	public DecisionVariable newRandomDecisionVariable() {
		final Set<Integer> tollLocations = new TreeSet<Integer>();
		// for (int i = 0; i < this.tollCnt; i++) {
		// tollLocations.add(this.roadCnt - 1 - i);
		// }
		while (tollLocations.size() < this.tollCnt) {
			tollLocations.add(new Integer(this.rnd.nextInt(this.roadCnt)));
		}
		return this.newDecisionVariableFromTollLocations(tollLocations);
	}

	@Override
	public DecisionVariable newRandomVariation(final DecisionVariable parent) {

		final Vector parentTolls = ((NTimesTwoRoutesDecisionVariable) parent)
				.getTolls();

		final Set<Integer> parentLocations = new TreeSet<Integer>();
		for (int i = 0; i < this.roadCnt; i++) {
			if (parentTolls.get(i) != 0.0) {
				parentLocations.add(i);
			}
		}
		if (parentLocations.size() != this.tollCnt) {
			throw new RuntimeException("toll vector " + parentTolls
					+ " does not have " + this.tollCnt + " non-zero entries");
		}

		final Set<Integer> childLocations = new TreeSet<Integer>();
		while (childLocations.size() != this.tollCnt) {
			childLocations.clear();
			for (Integer parentLocation : parentLocations) {
				if (this.rnd.nextDouble() < this.mutationProbability) {
					childLocations.add(new Integer(this.rnd
							.nextInt(this.roadCnt)));
				} else {
					childLocations.add(parentLocation);
				}
			}
		}

		return newDecisionVariableFromTollLocations(childLocations);
	}
}
