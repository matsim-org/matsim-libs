package org.matsim.contrib.analysis.vsp;

public class Range {
	
	private double lowerBound;
	private double upperBound;
	private String label;
	
	public Range(double lowerBound, double upperBound, String label){
		
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.label = label;
		
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public String getLabel() {
		return label;
	}

}
