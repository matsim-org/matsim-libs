package playgroundMeng.ptTravelTimeAnalysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;

public class PtTraveInfo {
	List<Leg> ptLegs = new LinkedList<Leg>();
	double travelTime;
	
	public PtTraveInfo(List<Leg> ptLegs) {
		this.ptLegs = ptLegs;
		this.travelTime = this.setTravelTime(ptLegs);
	}
	private double setTravelTime(List<Leg> ptLegs) {
		double time = 0;
		for(Leg leg: ptLegs) {
			time = time + leg.getTravelTime();
		}
		return time;
	}
	public double getTravelTime() {
		return travelTime;
	}
	public void setPtLegs(List<Leg> ptLegs) {
		this.ptLegs = ptLegs;
	}
	public List<Leg> getPtLegs() {
		return ptLegs;
	}
	
	private String legsToString() {
		String legString = null;
		for (Leg leg : this.ptLegs) {
			legString = legString + leg +" ";
	      }
		return legString;
	}
	@Override
	public String toString() {
		return "PTTraveInfo [ptLegs=" + this.legsToString() + ", travelTime=" + travelTime + "]";
	}

}
