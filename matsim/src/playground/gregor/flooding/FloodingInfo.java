package playground.gregor.flooding;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class FloodingInfo {

	
	private Coordinate c;
	private List<Double> stages;
	private double floodingTime;
	
	
	public FloodingInfo(Coordinate c, List<Double> floodingSeries, double time) {
		this.c = c;
		this.stages = floodingSeries;
		this.floodingTime = time;
	}

	public double getFloodingTime() {
		return this.floodingTime;			
		
	}
	
	public Coordinate getCoordinate() {
		return this.c;
	}
	
	public List<Double> getFloodingSeries() {
		return this.stages;
	}
	
}
