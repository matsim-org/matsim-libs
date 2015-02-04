package playground.artemc.heterogeneity;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import playground.artemc.heterogeneity.routing.TravelDisutilityIncludingIncome;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by artemc on 3/2/15.
 */
public class TravelDisutilityIncomeHeterogeneityProviderWrapper {

	public static class TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider implements Provider<TravelDisutilityFactory> {

		private final Scenario scenario;
		private final IncomeHeterogeneity incomeHeterogeneity;

		@Inject
		TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider(Scenario scenario, IncomeHeterogeneity incomeHeterogeneity) {
			this.scenario = scenario;
			this.incomeHeterogeneity = incomeHeterogeneity;
		}

		@Override
		public TravelDisutilityFactory get() {
			HeterogeneityConfigGroup heterogeneityConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class);
			TravelDisutilityIncludingIncome.Builder travelDisutilityFactory =
					new TravelDisutilityIncludingIncome.Builder( ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), this.incomeHeterogeneity);
			return travelDisutilityFactory;
		}

	}

}
