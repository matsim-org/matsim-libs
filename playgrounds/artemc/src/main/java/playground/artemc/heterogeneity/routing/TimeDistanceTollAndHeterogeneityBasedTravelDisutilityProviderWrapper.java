package playground.artemc.heterogeneity.routing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;

import javax.inject.Inject;
import javax.inject.Provider;

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
				RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
				TimeDistanceTollAndHeterogeneityBasedTravelDisutility.Builder travelDisutilityFactory = 
						new TimeDistanceTollAndHeterogeneityBasedTravelDisutility.Builder(scheme, scenario.getConfig().planCalcScore());
				travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
				return travelDisutilityFactory;
			}
		}
}
