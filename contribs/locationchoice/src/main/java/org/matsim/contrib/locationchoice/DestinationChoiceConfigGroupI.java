package org.matsim.contrib.locationchoice;

public interface DestinationChoiceConfigGroupI{
	String getFlexibleTypes();

	double getScaleFactor();

	void setScaleFactor( double scaleFactor );

	String getCenterNode();

	Double getRadius();

	String getEpsilonScaleFactors();

	double getRestraintFcnFactor();

	double getRestraintFcnExp();
}
