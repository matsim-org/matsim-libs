package floetteroed.opdyts.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LogFileSummary {

	// -------------------- MEMBERS --------------------

	private List<Integer> totalTransitionCounts = new ArrayList<>();

	private List<Double> bestObjectiveFunctionValues = new ArrayList<>();

	private List<Double> equilibriumGapWeights = new ArrayList<>();

	private List<Double> uniformityGapWeights = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	LogFileSummary() {
	}

	void add(final int totalTransitionCount,
			final double bestObjectiveFunctionValue,
			final double equilibriumGapWeight, final double uniformityGapWeight) {
		this.totalTransitionCounts.add(totalTransitionCount);
		this.bestObjectiveFunctionValues.add(bestObjectiveFunctionValue);
		this.equilibriumGapWeights.add(equilibriumGapWeight);
		this.uniformityGapWeights.add(uniformityGapWeight);
	}

	// -------------------- CONTENT ACCESS --------------------

	public int getStageCnt() {
		return this.totalTransitionCounts.size();
	}
	
	public List<Integer> getTotalTransitionCounts() {
		return this.totalTransitionCounts;
	}
	
	public List<Double> getBestObjectiveFunctionValues() {
		return this.bestObjectiveFunctionValues;
	}
	
	public List<Double> getEquilbriumGapWeights() {
		return this.equilibriumGapWeights;
	}
	
	public List<Double> getUniformityGapWeights() {
		return this.uniformityGapWeights;
	}
	
}

