package opdytsintegration.example.roadpricing;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OptFileSummary {

	// -------------------- MEMBERS --------------------

	private List<Integer> initialTransitionCounts = new ArrayList<>();

	private List<Double> initialEquilibriumGapWeights = new ArrayList<>();

	private List<Double> initialUniformityGapWeights = new ArrayList<>();

	private List<Double> finalObjectiveFunctionValues = new ArrayList<>();

	private List<Integer> addedTransitionCounts = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	OptFileSummary() {
	}

	void add(final Integer initialTransitionCount,
			final Double initialEquilibriumGapWeight,
			final Double initialUniformityGapWeight,
			final Double finalObjectiveFunctionValue,
			final Integer addedTransitionCount) {
		this.initialTransitionCounts.add(initialTransitionCount);
		this.initialEquilibriumGapWeights.add(initialEquilibriumGapWeight);
		this.initialUniformityGapWeights.add(initialUniformityGapWeight);
		this.finalObjectiveFunctionValues.add(finalObjectiveFunctionValue);
		this.addedTransitionCounts.add(addedTransitionCount);
	}

	// -------------------- CONTENT ACCESS --------------------

	public int getStageCnt() {
		return this.initialTransitionCounts.size();
	}

	public List<Integer> getInitialTransitionCounts() {
		return this.initialTransitionCounts;
	}

	public List<Double> getInitialEquilbriumGapWeights() {
		return this.initialEquilibriumGapWeights;
	}

	public List<Double> getInitialUniformityGapWeights() {
		return this.initialUniformityGapWeights;
	}

	public List<Double> getFinalObjectiveFunctionValues() {
		return this.finalObjectiveFunctionValues;
	}

	public List<Integer> getAddedTransitionCounts() {
		return this.addedTransitionCounts;
	}
}
