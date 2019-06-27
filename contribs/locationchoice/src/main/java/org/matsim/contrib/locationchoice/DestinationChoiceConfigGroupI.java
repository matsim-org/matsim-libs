package org.matsim.contrib.locationchoice;

public interface DestinationChoiceConfigGroupI{
	String getFlexibleTypes();

	String getCenterNode();

	Double getRadius();

	String getEpsilonScaleFactors();

	double getRestraintFcnFactor();

	double getRestraintFcnExp();
}
