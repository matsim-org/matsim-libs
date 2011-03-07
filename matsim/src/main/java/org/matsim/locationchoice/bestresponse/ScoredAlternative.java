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
	 * If the scores are identical, additionally use id to sort such that deterministic order is ensured
	 */
	@Override
	public int compareTo(ScoredAlternative o) {
		if (this.score > o.getScore()) return 1;
		else if (this.score < o.getScore()) return -1;
		else return this.alternativeId.compareTo(o.getAlternativeId());
	}
}
