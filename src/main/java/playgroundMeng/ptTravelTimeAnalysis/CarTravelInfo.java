package playgroundMeng.ptTravelTimeAnalysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class CarTravelInfo {
	LeastCostPathCalculator.Path carPath;
	double travelTime;
	
	public CarTravelInfo(LeastCostPathCalculator.Path carPath) {
		this.carPath = carPath;
		this.travelTime = carPath.travelTime;
	}
	
	public void setCarPath(LeastCostPathCalculator.Path carPath) {
		this.carPath = carPath;
	}
	public LeastCostPathCalculator.Path getCarPath() {
		return carPath;
	}
	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}
	public double getTravelTime() {
		return travelTime;
	}
	@Override
	public String toString() {
		return "CarTravelInfo [carPath=" + linksIdString(carPath.links) + ", travelTime=" + travelTime + "]";
	}
	
	private String linksIdString(List<Link> links) {
		List<Id<Link>> linkIds = new LinkedList<Id<Link>>();
		for(Link link : links) {
			linkIds.add(link.getId());
		}
		return linkIds.toString();
	}

}
