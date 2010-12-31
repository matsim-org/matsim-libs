package playground.anhorni.LEGO.miniscenario.run.scoring;

import java.util.Vector;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class DestinationChoicePreviousScoreComputer implements IterationEndsListener{
	private double meanScore = 0.0;
	private double varianceScore = 0.0;
	private Vector<Double> scoresTT = new Vector<Double>();
	
	public void notifyIterationEnds(IterationEndsEvent event){				
		this.meanScore = this.computeMeanScore();
		this.varianceScore = this.computeVarianceScores(meanScore);
		this.scoresTT.clear();
	}
	
	public synchronized void addValue(double score) {
		this.scoresTT.add(score);
	}
	
	private double computeVarianceScores(double mean) {			
		double variance = 0.0;
		for (Double val : this.scoresTT) {
			variance += Math.pow(val - mean, 2.0);
		}
		return variance;
	}
	
	private double computeMeanScore() {
		double score = 0.0;
		for (Double val : this.scoresTT) {
			score += val;
		}
		return score / this.scoresTT.size();
	}

	public synchronized double getMeanScore() {
		return meanScore;
	}

	public synchronized double getVarianceScore() {
		return varianceScore;
	}
}
