package playground.artemc.pricing;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingTravelDisutilityFactory;

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
			final Config config = scenario.getConfig();
			RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), scheme, config.planCalcScore().getMarginalUtilityOfMoney());
			travelDisutilityFactory.setSigma(config.plansCalcRoute().getRoutingRandomness());
			return travelDisutilityFactory;
		}

	}
}

