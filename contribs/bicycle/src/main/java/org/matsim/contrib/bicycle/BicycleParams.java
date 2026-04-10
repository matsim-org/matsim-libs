package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

public interface BicycleParams {
	public double getComfortFactor(String surface);

	//double getInfrastructureFactor(String type, String cyclewaytype); // bicycleInfraType
	//double getInfrastructureFactor(String type, String cyclewayType, String bicycleInfraType,
	//							   BicycleInfrastructureMode infrastructureMode);
	double getInfrastructureFactor(String type, String infrastructureValue, String infrastructureAttribute);

	double getGradient_pct(Link link);

	double computeSurfaceFactor(Link link);

	// James Woodcock reported some values from Corine Staves (Jul' 2025).
}
