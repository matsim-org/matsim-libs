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

	/* positive – this object is greater than o
	   zero – this object equals to o
	   negative – this object is less than o
	   */
	@Override
	public int compareTo(ScoredAlternative o) {
		if (this.score > o.score) return 1;
		else if (this.score < o.score) return -1;
		else return 0;
	}
}
