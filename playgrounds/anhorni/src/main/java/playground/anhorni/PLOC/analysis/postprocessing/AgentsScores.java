package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class AgentsScores {
	
	private final static Logger log = Logger.getLogger(AgentsScores.class);
	
	private Id agentId;
	private List<Double> scores = new Vector<Double>();
	
	public AgentsScores(Id agentId) {
		this.agentId = agentId;
	}
	
	public void addScore(double score) {
		this.scores.add(score);
	}
	
	public double getAverageScore() {
		double sum = 0.0;
		for (double score : this.scores) {
			sum += score;
		}
		return sum / this.scores.size();
	}
	
	public double getStandardDeviationScore_S() {
		double sumQuadraticDeviations = 0.0;
		double averageScore = this.getAverageScore();
		for (double score : this.scores) {
			sumQuadraticDeviations += Math.pow(score - averageScore, 2.0);
		}
		double stdev = Math.sqrt(sumQuadraticDeviations / (this.scores.size() -1));
		if (Double.isNaN(stdev)) {
			log.error("avg score: " + averageScore + " /scores size: " + this.scores.size() + " /sumQuadraticDeviations " + sumQuadraticDeviations);
		}
		return stdev;
	}

	public Id getAgentId() {
		return agentId;
	}

	public void setAgentId(Id agentId) {
		this.agentId = agentId;
	}
}
