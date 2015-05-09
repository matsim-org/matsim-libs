package playground.artemc.pricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingTravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by artemc on 3/2/15.
 */
public class TravelDisutilityIncludingTollProviderWrapper {

	public static class TravelDisutilityWithPricingAndHeterogeneityProvider implements Provider<TravelDisutilityFactory> {

		private final Scenario scenario;
		private final RoadPricingScheme scheme;

		@Inject
		TravelDisutilityWithPricingAndHeterogeneityProvider(Scenario scenario, RoadPricingScheme scheme) {
			this.scenario = scenario;
			this.scheme = scheme;
		}

		@Override
		public TravelDisutilityFactory get() {
			RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
			RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), scheme, scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
			travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
			return travelDisutilityFactory;
		}

	}
}

