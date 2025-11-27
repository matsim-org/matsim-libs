package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

public final class AdditionalBicycleLinkScoreDefaultImpl implements AdditionalBicycleLinkScore {

	@Inject
	private BicycleParams bicycleParams;
	private final double marginalUtilityOfInfrastructure_m;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_pct_m;
	@Inject AdditionalBicycleLinkScoreDefaultImpl( Scenario scenario ) {
		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), BicycleConfigGroup.class );
		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_pct_m = bicycleConfigGroup.getMarginalUtilityOfGradient_pct_m();

	}
	@Override public double computeLinkBasedScore( Link link ){
		String surface = BicycleUtils.getSurface(link);
		String type = NetworkUtils.getType( link );
		String cyclewaytype = BicycleUtils.getCyclewaytype( link );

		double distance_m = link.getLength();

		double comfortFactor = bicycleParams.getComfortFactor(surface );
		double comfortScore = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance_m;

		double infrastructureFactor = bicycleParams.getInfrastructureFactor(type, cyclewaytype );
		double infrastructureScore = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance_m;

		double gradient_pct = bicycleParams.getGradient_pct( link );
		double gradientScore = marginalUtilityOfGradient_pct_m * gradient_pct * distance_m;

		return (infrastructureScore + comfortScore + gradientScore /*+ userDefinedNetworkAttributeScore*/ );
	}
}

