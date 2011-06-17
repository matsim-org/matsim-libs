package vrp.algorithms.ruinAndRecreate;

/**
 * Collector of algo-information. Can be listened to.
 * 
 * @author stefan schroeder
 *
 */

public class RuinAndRecreateEvent {
	
	private int currentMutation;
	
	private double tentativeSolution;
	
	private double currentResult;
	
	private double threshold;
	
	private boolean solutionAccepted;

	public RuinAndRecreateEvent(int currentMutation, double tentativeSolution,
			double currentResult, double currentThreshold, boolean solutionAccepted) {
		super();
		this.currentMutation = currentMutation;
		this.tentativeSolution = tentativeSolution;
		this.threshold = currentThreshold;
		this.solutionAccepted = solutionAccepted;
		this.currentResult = currentResult;
	}

	public double getCurrentResult() {
		return currentResult;
	}

	public int getCurrentMutation() {
		return currentMutation;
	}

	public double getTentativeSolution() {
		return tentativeSolution;
	}

	public double getThreshold() {
		return threshold;
	}

	public boolean isSolutionAccepted() {
		return solutionAccepted;
	}
	
	@Override
	public String toString() {
		return "[currentMutation=" + currentMutation + "][tentativeSolution=" + tentativeSolution + "][currentSolution=" + currentResult + "][currentThreshold=" + threshold + "][isAccepted=" + solutionAccepted + "]";
	}

}
