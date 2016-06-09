package playground.artemc.heterogeneity.routing;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * Created by artemc on 3/2/15.
 */
public class TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper {

		public static class TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProvider implements Provider<TravelDisutilityFactory> {

			private final Scenario scenario;
			private final RoadPricingScheme scheme;

			@Inject
			TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProvider(Scenario scenario, RoadPricingScheme scheme) {
				this.scenario = scenario;
				this.scheme = scheme;
			}

			@Override
			public TravelDisutilityFactory get() {
				final Config config = scenario.getConfig();
				TimeDistanceTollAndHeterogeneityBasedTravelDisutility.Builder travelDisutilityFactory = 
						new TimeDistanceTollAndHeterogeneityBasedTravelDisutility.Builder(scheme, config.planCalcScore());
				travelDisutilityFactory.setSigma(config.plansCalcRoute().getRoutingRandomness());
				return travelDisutilityFactory;
			}
		}
}
