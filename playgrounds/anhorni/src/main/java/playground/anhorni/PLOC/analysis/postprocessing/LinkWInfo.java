package playground.anhorni.PLOC.analysis.postprocessing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class LinkWInfo {
	
	private Id id;
	private Coord coord;
	private double [] stdDevsPerHour = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double [] avgVolumesPerHour = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	public LinkWInfo(Id id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	
	public void setStdDev(int hour, double val) {
		this.stdDevsPerHour[hour] = val;
	}
	
	public void setAvgVolume(int hour, double val) {
		this.avgVolumesPerHour[hour] = val;
	}
	
	public double getAvgVolume(int hour) {
		return this.avgVolumesPerHour[hour];
	}
	
	public double getStdDevs(int hour) {
		return this.stdDevsPerHour[hour];
	}

	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public Coord getCoord() {
		return coord;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}

}
