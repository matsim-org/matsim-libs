package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;

public class FacilityLoad {
	
	private IdImpl facilityId;
	private Coord coord;
	private double load0 = 0.0;
	private double load1 = 0.0;
	
	public IdImpl getFacilityId() {
		return facilityId;
	}
	public void setFacilityId(IdImpl facilityId) {
		this.facilityId = facilityId;
	}
	public double getLoad0() {
		return load0;
	}
	public void setLoad0(double load0) {
		this.load0 = load0;
	}
	public double getLoad1() {
		return load1;
	}
	public void setLoad1(double load1) {
		this.load1 = load1;
	}

	public Coord getCoord() {
		return coord;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
	public double getLoadDiffRel() {
		if (load0 > 0.0) {
			return (load1 - load0) / load0;
		}
		else {
			if (load1 > 0) {
				return 1.0;
			}
			else {
				return 0.0;
			}
		}
		
	}
}
