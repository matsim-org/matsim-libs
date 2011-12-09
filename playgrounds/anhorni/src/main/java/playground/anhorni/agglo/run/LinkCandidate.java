package playground.anhorni.agglo.run;

import org.matsim.api.core.v01.Id;

public class LinkCandidate {
	private Id linkId;
	private int potentialCustomersCount = 0;
	private double competitorsPower = 0;

	public LinkCandidate(Id id) {
		this.linkId = id;
	}

	public Id getLinkId() {
		return linkId;
	}

	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}

	public int getPotentialCustomersCount() {
		return potentialCustomersCount;
	}

	public void setPotentialCustomersCount(int potentialCustomersCount) {
		this.potentialCustomersCount = potentialCustomersCount;
	}

	public double getCompetitorsCount() {
		return competitorsPower;
	}

	public void setCompetitorsCount(double competitorsCount) {
		this.competitorsPower = competitorsCount;
	}
	
	public void increasePotentialCustomersCount(int inc) {
		this.potentialCustomersCount += inc;
	}
	
	public void increaseCompetitorsPower(double inc) {
		this.competitorsPower += inc;
	}
	
	public double getPotential() {
		return this.potentialCustomersCount / (this.competitorsPower + 1);
	}
}
