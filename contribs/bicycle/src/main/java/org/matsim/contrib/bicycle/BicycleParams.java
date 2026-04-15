package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

public interface BicycleParams {
	public double getComfortFactor(String surface);


	// legacy: just here to make the preceived-safety tests work
	@Deprecated(forRemoval = true)
	default double getInfrastructureFactor(String type, String infrastructureValue) {
		return getInfrastructureFactor(
			type,
			infrastructureValue,
			BicycleUtils.CYCLEWAY
		);
	}

	double getInfrastructureFactor(String type, String infrastructureValue, String infrastructureAttribute);

	double getGradient_pct(Link link);

	double computeSurfaceFactor(Link link);

	// James Woodcock reported some values from Corine Staves (Jul' 2025).
}
