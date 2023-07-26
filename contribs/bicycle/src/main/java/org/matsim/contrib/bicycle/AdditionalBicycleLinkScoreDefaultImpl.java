package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;

public final class AdditionalBicycleLinkScoreDefaultImpl implements AdditionalBicycleLinkScore {

	private final double marginalUtilityOfInfrastructure_m;
	private final double userDefinedNetworkAttributeDefaultValue;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_m_100m;
	private final double marginalUtilityOfUserDefinedNetworkAttribute_m;
	private final String nameOfUserDefinedNetworkAttribute;
	@Inject AdditionalBicycleLinkScoreDefaultImpl( Scenario scenario ) {
		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), BicycleConfigGroup.class );
		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_m_100m = bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m();
		this.marginalUtilityOfUserDefinedNetworkAttribute_m = bicycleConfigGroup.getMarginalUtilityOfUserDefinedNetworkAttribute_m();
		this.nameOfUserDefinedNetworkAttribute = bicycleConfigGroup.getUserDefinedNetworkAttributeName();
		this.userDefinedNetworkAttributeDefaultValue = bicycleConfigGroup.getUserDefinedNetworkAttributeDefaultValue();

	}
	@Override public double computeLinkBasedScore( Link link ){
		String surface = (String) link.getAttributes().getAttribute(BicycleUtils.SURFACE );
		String type = (String) link.getAttributes().getAttribute("type" );
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleUtils.CYCLEWAY );

		double distance = link.getLength();

		double comfortFactor = BicycleUtils.getComfortFactor(surface );
		double comfortScore = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance;

		double infrastructureFactor = BicycleUtils.getInfrastructureFactor(type, cyclewaytype );
		double infrastructureScore = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance;

		double gradient = BicycleUtils.getGradient( link );
		double gradientScore = marginalUtilityOfGradient_m_100m * gradient * distance;

		String userDefinedNetworkAttributeString;
		double userDefinedNetworkAttributeScore = 0.;
		if ( nameOfUserDefinedNetworkAttribute != null) {
			userDefinedNetworkAttributeString = BicycleUtils.getUserDefinedNetworkAttribute( link, nameOfUserDefinedNetworkAttribute );
			double userDefinedNetworkAttributeFactor = BicycleUtils.getUserDefinedNetworkAttributeFactor(userDefinedNetworkAttributeString, userDefinedNetworkAttributeDefaultValue );
			userDefinedNetworkAttributeScore = marginalUtilityOfUserDefinedNetworkAttribute_m * (1. - userDefinedNetworkAttributeFactor) * distance;
		}

		return (infrastructureScore + comfortScore + gradientScore + userDefinedNetworkAttributeScore);
	}
}

