package playground.singapore.springcalibration.run;

import java.util.ArrayList;
import java.util.Arrays;

import org.matsim.api.core.v01.Coord;

public class TaxiWaitingTime {
	
	private ArrayList<Double> waitingTime = new ArrayList<Double>(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
	private Coord centroid;
	
	public double getWaitingTime(int hour) {
		return this.waitingTime.get(hour);
	}
	public void setWaitingTime(int hour, double waitingTime) {
		this.waitingTime.set(hour, waitingTime);
	}
	public Coord getCentroid() {
		return centroid;
	}
	public void setCentroid(Coord centroid) {
		this.centroid = centroid;
	}

}
