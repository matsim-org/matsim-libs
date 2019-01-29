package org.matsim.contrib.analysis.vsp.qgis;

public class Range {

	private double lowerBound;
    boolean lowerBoundSet;
    private double upperBound;
    boolean upperBoundSet;
    private String label;

	public Range(double lowerBound, double upperBound, String label){
		this.setLowerBound(lowerBound);
		this.setUpperBound(upperBound);
		this.label = label;
	}

    public Range(String label){
        this.label = label;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
        this.lowerBoundSet = true;
    }

    public double getLowerBound() {
		return lowerBound;
	}

    public boolean isLowerBoundSet() {
        return lowerBoundSet;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
        this.upperBoundSet = true;
    }

    public double getUpperBound() {
		return upperBound;
	}

    public boolean isUpperBoundSet() {
        return upperBoundSet;
    }

    public String getLabel() {
		return label;
	}

}
