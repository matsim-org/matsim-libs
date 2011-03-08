package org.matsim.locationchoice.bestresponse;

import org.matsim.api.core.v01.Id;

public class ScoredAlternative implements Comparable<ScoredAlternative> {
	
	private double score;
	private Id alternativeId;
	
	public ScoredAlternative(double score, Id alternativeId) {
		this.score = score;
		this.alternativeId = alternativeId;
	}
	
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public Id getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Id alternativeId) {
		this.alternativeId = alternativeId;
	}

	/* 
	 * Compare keys (double scores). 
	 * If the scores are identical, additionally use the 'alternatives' id's to sort such that deterministic order is ensured
	 */
	@Override
	public int compareTo(ScoredAlternative o) {
		// numerics
		double epsilon = 0.000001;
		
		// reverse order:
		if (Math.abs(this.score - o.getScore()) > epsilon) {
			if (this.score > o.getScore()) return -1;
			else return +1;
		}		
		else {
			return this.alternativeId.compareTo(o.getAlternativeId());
		}
	}
}
