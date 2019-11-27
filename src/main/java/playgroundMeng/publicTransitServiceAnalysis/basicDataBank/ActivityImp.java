package playgroundMeng.publicTransitServiceAnalysis.basicDataBank;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class ActivityImp {
	String type;
	Coord coord;
	Link link;
	Double time;

	public ActivityImp(String type, Coord coord, double startTime) {
		this.type = type;
		this.coord = coord;
		this.time = startTime;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "ActivityImp [type=" + type + ", coord=" + coord + ", startTime=" + time + "]";
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

	public Double getTime() {
		return time;
	}

	public void setTime(Double startTime) {
		this.time = startTime;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Link getLink() {
		return link;
	}

}
