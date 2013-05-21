package playground.dziemke.potsdam.analysis.disaggregated.revised;

import org.matsim.api.core.v01.Id;

public class DoubleId {

	double time	;
	Id id;
	
	public DoubleId (double time, Id id ){
		this.time = time;
		this.id = id;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "DoubleId [time=" + time + ", id=" + id + "]";
	}
	
}
