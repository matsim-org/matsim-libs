package floetteroed.utilities.searchrepeater;

import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchRepeater<D> {

	// -------------------- CONSTANTS --------------------

	private final int maxTrials;

	private final int maxFailures;

	private Double objectiveFunctionValue = null;

	private D decisionVariable = null;

	private int trials;

	private int failures;

	private boolean minimize = true;

	// -------------------- CONSTRUCTION --------------------

	public SearchRepeater(final int maxTrials, final int maxFailures) {
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setMinimize() {
		this.minimize = true;
	}

	public void setMaximize() {
		this.minimize = false;
	}

	public boolean getMinimize() {
		return this.minimize;
	}

	public boolean getMaximize() {
		return (!this.minimize);
	}

	// -------------------- IMPLEMENTATION --------------------

	private boolean isImprovement(final double newObjFctVal) {
		return ((this.getMinimize() && (newObjFctVal < this.objectiveFunctionValue))
				|| (this.getMaximize() && (newObjFctVal > this.objectiveFunctionValue)));
	}

	public void run(final SearchAlgorithm<? extends D> algo) {

		if (this.minimize) {
			this.objectiveFunctionValue = Double.POSITIVE_INFINITY;
		} else {
			this.objectiveFunctionValue = Double.NEGATIVE_INFINITY;
		}

		this.decisionVariable = null;
		this.trials = 0;
		this.failures = 0;

		while ((trials < this.maxTrials) && (failures < this.maxFailures)) {

			algo.run();
			this.trials++;

			final Double newObjFctVal = algo.getObjectiveFunctionValue();
			// if (newObjFctVal < this.objectiveFunctionValue) {
			if (this.isImprovement(newObjFctVal)) {
				this.objectiveFunctionValue = newObjFctVal;
				this.decisionVariable = algo.getDecisionVariable();
				this.failures = 0;
			} else {
				this.failures++;
			}
		}
	}

	public Double getObjectiveFunctionValue() {
		return this.objectiveFunctionValue;
	}

	public D getDecisionVariable() {
		return this.decisionVariable;
	}

	public int getTrials() {
		return this.trials;
	}

	public int getFailures() {
		return this.failures;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final int maxTrials = 1;
		final int maxFailures = 10;
		final SearchRepeater<Integer> repeater = new SearchRepeater<>(maxTrials, maxFailures);
		repeater.run(new SearchAlgorithm<Integer>() {
			private final Random rnd = new Random();
			private Double objVal = null;
			private Integer decVar = null;

			@Override
			public void run() {
				this.decVar = rnd.nextInt(1000);
				this.objVal = (double) this.decVar * this.decVar;
				System.out.println("decVar, objVal  =\t" + this.decVar + "\t" + this.objVal);
			}

			@Override
			public Double getObjectiveFunctionValue() {
				return this.objVal;
			}

			@Override
			public Integer getDecisionVariable() {
				return this.decVar;
			}
		});
		System.out.println("----------");
		System.out.println(
				"decVar, objVal  =\t" + repeater.getDecisionVariable() + "\t" + repeater.getObjectiveFunctionValue());
		System.out.println("total trials    =\t" + repeater.getTrials());
		System.out.println("recent failures =\t" + repeater.getFailures());
	}

}
