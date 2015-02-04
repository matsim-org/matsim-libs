package playground.artemc.heterogeneityWithToll;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;
import playground.artemc.heterogeneity.IncomeHeterogeneity;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by artemc on 3/2/15.
 */
public class TravelDisutilityTollAndIncomeHeterogeneityProviderWrapper {

		public static class TravelDisutilityWithPricingAndHeterogeneityProvider implements Provider<TravelDisutilityFactory> {

			private final Scenario scenario;
			private final RoadPricingScheme scheme;
			private final IncomeHeterogeneity incomeHeterogeneity;

			@Inject
			TravelDisutilityWithPricingAndHeterogeneityProvider(Scenario scenario, RoadPricingScheme scheme, IncomeHeterogeneity incomeHeterogeneity) {
				this.scenario = scenario;
				this.scheme = scheme;
				this.incomeHeterogeneity = incomeHeterogeneity;
			}

			@Override
			public TravelDisutilityFactory get() {
				RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
				TravelDisutilityTollAndIncomeHeterogeneity.Builder travelDisutilityFactory = new TravelDisutilityTollAndIncomeHeterogeneity.Builder(ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), scheme, scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(),  this.incomeHeterogeneity);
				travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
				return travelDisutilityFactory;
			}
		}
}
