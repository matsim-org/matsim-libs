package playground.jjoubert.digicoreNew.containers;

import java.util.GregorianCalendar;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class DigicoreActivity implements BasicLocation {
		
	private Id id;
	private Coord coord;
	private GregorianCalendar startTime;
	private GregorianCalendar endTime;
	
	public DigicoreActivity(Id id) {
		this.id = id;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	public GregorianCalendar getStartTime() {
		return startTime;
	}
	
	public GregorianCalendar getEndTime() {
		return endTime;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public void setStartTime(GregorianCalendar startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(GregorianCalendar endTime) {
		this.endTime = endTime;
	}
	
	
}
