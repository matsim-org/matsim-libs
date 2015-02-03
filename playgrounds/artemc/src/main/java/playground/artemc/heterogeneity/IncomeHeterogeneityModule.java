package playground.artemc.heterogeneity;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import playground.artemc.heterogeneity.routing.TravelDisutilityIncludingIncome;
import playground.artemc.utils.MapWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;

/**
 * Created by artemc on 28/1/15.
 */
public class IncomeHeterogeneityModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(IncomeHeterogeneityModule.class);

	private final IncomeHeterogeneityImpl incomeHeterogeneityImpl;
	private static String inputPath;

	public IncomeHeterogeneityModule(String inputPath) {
		this.incomeHeterogeneityImpl = null;
		this.inputPath = inputPath;

	}

	public IncomeHeterogeneityModule(IncomeHeterogeneityImpl incomeHeterogeneityImpl, String inputPath) {
		this.incomeHeterogeneityImpl = incomeHeterogeneityImpl;
		this.inputPath = inputPath;
	}

	@Override
	public void install() {
		// Passing parameters directly to the module until alternative solution is found. artemc

		if (this.incomeHeterogeneityImpl != null) {
			bindToInstance(IncomeHeterogeneity.class, this.incomeHeterogeneityImpl);
		} else {
			bindToProviderAsSingleton(IncomeHeterogeneity.class, IncomeHeterogeneityProvider.class);
		}

		// use ControlerDefaults configuration, replacing the TravelDisutility with a income-dependent one
				include(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new AbstractModule() {
					@Override
					public void install() {
				bindToProvider(TravelDisutilityFactory.class, TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider.class);
			}
		}));
	}

	private static class IncomeHeterogeneityProvider implements Provider<IncomeHeterogeneity> {

		private final Config config;
		private final Scenario scenario;

		@Inject
		IncomeHeterogeneityProvider(Config config, Scenario scenario) {
			this.config = config;
			this.scenario = scenario;
		}

		@Override
		public IncomeHeterogeneity get() {
			HeterogeneityConfigGroup heterogeneityConfig = ConfigUtils.addOrGetModule(config, HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class);
			String incomeFile = heterogeneityConfig.getIncomeFile();
			String lambdaIncomeTravelCost = heterogeneityConfig.getLambdaIncomeTravelcost();
			String incomeType = heterogeneityConfig.getIncomeOnTravelCostType();

			log.info("Adding income heterogeneity and parcing income data... Heterogeneity type: " + incomeType);

			if (!(incomeFile != null && lambdaIncomeTravelCost != null)) {
				throw new RuntimeException("Income heterogeneity inserted but no income file path is found." + "Such an execution path is not allowed.  If you want a base case without income heterogeneity, " + "no income file and no lambda income parameter should be given.");
			} else if (incomeType == null) { throw new RuntimeException("Unknown income heterogeneity type");}

			IncomeHeterogeneityImpl incomeHeterogeneityImpl = new IncomeHeterogeneityImpl(this.scenario.getPopulation());
			incomeHeterogeneityImpl.setName("Income dependent heterogeneity in perception of travel cost");
			incomeHeterogeneityImpl.setType(heterogeneityConfig.getIncomeOnTravelCostType());
			incomeHeterogeneityImpl.setLambda_income(Double.valueOf(heterogeneityConfig.getLambdaIncomeTravelcost()));

			IncomePopulationReader incomesReader = new IncomePopulationReader(incomeHeterogeneityImpl, this.scenario.getPopulation());
			incomesReader.parse(inputPath+incomeFile);

			for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()) {
				if (incomeHeterogeneityImpl.getType().equals("heteroAlphaProp")) {
					double randomFactor = 0.0;
					do {
						randomFactor = (MatsimRandom.getRandom().nextGaussian() * 0.2) + 1;
					} while (randomFactor < 0 && randomFactor > 2);
					System.out.println();
					incomeHeterogeneityImpl.getBetaFactors().put(personId, randomFactor);
				}
			}

			MapWriter writer = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/betaNormalFactors.csv");
			writer.writeBetaFactors(incomeHeterogeneityImpl.getBetaFactors(), "PersonId", "BetaFactor;");

			return incomeHeterogeneityImpl;
		}
	}

	private static class TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider implements Provider<TravelDisutilityFactory> {

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
