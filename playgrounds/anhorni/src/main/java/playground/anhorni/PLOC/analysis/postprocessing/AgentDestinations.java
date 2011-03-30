package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

public class AgentDestinations {
	
	private TreeMap<Integer, List<CoordImpl>> destinations = new TreeMap<Integer, List<CoordImpl>>();
	private Id agentId;
	
	public AgentDestinations(Id id) {
		this.agentId = id;
	}
	
	public void addDestination(int activityNumber, Coord coord) {
		if (this.destinations.get(activityNumber) == null) this.destinations.put(activityNumber, new Vector<CoordImpl>());
		this.destinations.get(activityNumber).add((CoordImpl)coord);
	}
	
	public double getAverageDistanceFromCenterPointForAllActivities() {
		double distance = 0.0;
		for (Integer number : this.destinations.keySet()) {
			distance += this.getAverageDistanceFromCenterPoint(number);
		}
		return distance / this.destinations.keySet().size();
	}
	
	public double getAverageDistanceFromCenterPoint(int activityNumber) {
		CoordImpl center = this.getCenterPoint(activityNumber);
		
		double distance = 0.0;
		for (CoordImpl coord : this.destinations.get(activityNumber)) {
			distance += center.calcDistance(coord);
		}
		distance /= this.destinations.get(activityNumber).size();
		return distance;
	}
	
	private CoordImpl getCenterPoint(int activityNumber) {
		double x = 0.0;
		double y = 0.0;
		for (CoordImpl coord : this.destinations.get(activityNumber)) {
			x += coord.getX();
			y += coord.getY();
		}
		x /= this.destinations.get(activityNumber).size();
		y /= this.destinations.get(activityNumber).size();
		return new CoordImpl(x,y);
	}
}
