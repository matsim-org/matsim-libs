package playground.toronto.analysis.tripchains;

import org.matsim.api.core.v01.Id;

public class InTransitVehicleComponent implements TripComponent {
	private double startTime;
	private double endTime;
	private Id transitLineId;
	private Id transitRouteId;
	
	public InTransitVehicleComponent(double startTime){
		this.startTime = startTime;
	}
	
	public void setRouteInfo(Id lineId, Id routeId){
		this.transitLineId = lineId;
		this.transitRouteId = routeId;
	}
	
	public String getFullRouteName(){
		return "Line:" + this.transitLineId.toString() + " Branch:" + this.transitRouteId.toString();
	}
	
	@Override
	public int compareTo(TripComponent o) {
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
	public boolean overlaps(TripComponent t) {
		return (t.getEndtime() > this.startTime && t.getEndtime() < this.endTime) ||
				(t.getStartTime() > this.startTime && t.getStartTime() < this.endTime);
	}
	@Override
	public void finishComponent(double endTime) {
		this.endTime = endTime;		
	}
}
