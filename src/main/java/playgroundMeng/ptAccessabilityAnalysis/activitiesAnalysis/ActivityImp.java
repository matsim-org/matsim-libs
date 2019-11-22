package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import org.matsim.api.core.v01.Coord;

public class ActivityImp  {
	String type;
	Coord coord;
	Double startTime;
	
	public ActivityImp(String type, Coord coord, double startTime) {
		this.type = type;
		this.coord = coord;
		this.startTime = startTime;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "ActivityImp [type=" + type + ", coord=" + coord + ", startTime=" + startTime + "]";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Double getStartTime() {
		return startTime;
	}

	public void setStartTime(Double startTime) {
		this.startTime = startTime;
	}
	
}
