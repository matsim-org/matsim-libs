package playground.anhorni.locationchoice.cs.helper;

import org.matsim.basic.v01.Id;

public class TravelTimeBudget {
	
	private Id personId;
	private double travelTimeBudget = 0.0;
	private int subChainIndex;
	
	public TravelTimeBudget(Id personId, double travelTimeBudget, int subChainIndex) {
		this.personId = personId;
		this.travelTimeBudget = travelTimeBudget;
		this.subChainIndex = subChainIndex;
	}
	
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public double getTravelTimeBudget() {
		return travelTimeBudget;
	}
	public void setTravelTimeBudget(double travelTimeBudget) {
		this.travelTimeBudget = travelTimeBudget;
	}
	public int getSubChainIndex() {
		return subChainIndex;
	}
	public void setSubChainIndex(int subChainIndex) {
		this.subChainIndex = subChainIndex;
	}

}
