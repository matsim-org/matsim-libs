package playground.anhorni.surprice;

import org.matsim.analysis.ScoreStats;
import org.matsim.core.controler.Controler.TerminationCriterion;

public class TerminationCriterionScoreBased implements TerminationCriterion {
	
	private double stoppingCriterionVal = 0.1;
	final private static int INDEX_BEST = 1;
	private ScoreStats scoreStats;
	
	public TerminationCriterionScoreBased(double stoppingCriterionVal, ScoreStats scoreStats) {
		this.stoppingCriterionVal = stoppingCriterionVal;
		this.scoreStats = scoreStats;
	}

	@Override
	public boolean continueIterations(int iteration) {
		double prevBestScore = 0.0;
		if (iteration >= 1) {
			prevBestScore = scoreStats.getHistory()[INDEX_BEST][iteration - 1];
		}
		double bestScore = scoreStats.getHistory()[INDEX_BEST][iteration];
		
		if (Math.abs((bestScore - prevBestScore) / prevBestScore) < this.stoppingCriterionVal) {
			return false;
		}
		else {
			return true;
		}
	}
}
