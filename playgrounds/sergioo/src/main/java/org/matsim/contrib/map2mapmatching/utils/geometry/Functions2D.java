package org.matsim.contrib.map2mapmatching.utils.geometry;

public class Functions2D {

	//Static Methods
	
	public static double getAnglesDifference(double angleA, double angleB) {
		double difference = Math.abs(angleA-angleB); 
		return difference>Math.PI ? 2*Math.PI-difference : difference;
	}
	
}
