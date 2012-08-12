package playground.toronto.analysis.tripchains;

import org.matsim.api.core.v01.Id;

public class WaitForTransitComponent implements TripChainComponent{
	private double startTime;
	private double endTime;
	private Id stopId;		
	
	public WaitForTransitComponent(double startTime){
		this.startTime = startTime;
	}

	@Override
	public int compareTo(TripChainComponent o) {
		if (this.overlaps(o)) return 0;
		else if (o.getStartTime() < this.startTime) return 1;
		else if (o.getStartTime() > this.startTime) return -1;
		else return 0;
	}
	@Override
	public double getStartTime() {
		return this.startTime;
	}
	@Override
	public double getEndtime() {
		return this.endTime;
	}
	@Override
	public boolean overlaps(TripChainComponent t) {
		return (t.getEndtime() > this.startTime && t.getEndtime() < this.endTime) ||
				(t.getStartTime() > this.startTime && t.getStartTime() < this.endTime);
	}


	@Override
	public void finishComponent(double endTime) {
		this.endTime = endTime;
	}
}
