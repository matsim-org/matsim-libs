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
		return BicycleUtilityUtils.computeLinkBasedScore( link, marginalUtilityOfComfort_m, marginalUtilityOfInfrastructure_m,
		marginalUtilityOfGradient_m_100m, marginalUtilityOfUserDefinedNetworkAttribute_m, nameOfUserDefinedNetworkAttribute,
		userDefinedNetworkAttributeDefaultValue );
	}
}

