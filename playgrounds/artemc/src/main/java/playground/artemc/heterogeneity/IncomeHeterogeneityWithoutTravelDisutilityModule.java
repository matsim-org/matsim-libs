package playground.artemc.heterogeneity;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import playground.artemc.utils.MapWriter;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by artemc on 28/1/15.
 */
public class IncomeHeterogeneityWithoutTravelDisutilityModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(IncomeHeterogeneityWithoutTravelDisutilityModule.class);

	private final IncomeHeterogeneityImpl incomeHeterogeneityImpl;

	public IncomeHeterogeneityWithoutTravelDisutilityModule() {
		this.incomeHeterogeneityImpl = null;
	}

	public IncomeHeterogeneityWithoutTravelDisutilityModule(IncomeHeterogeneityImpl incomeHeterogeneityImpl) {
		this.incomeHeterogeneityImpl = incomeHeterogeneityImpl;
	}

	@Override
	public void install() {
		// Passing parameters directly to the module until alternative solution is found. artemc

		if (this.incomeHeterogeneityImpl != null) {
			bind(IncomeHeterogeneity.class).toInstance(this.incomeHeterogeneityImpl);
		} else {
			bind(IncomeHeterogeneity.class).toProvider(IncomeHeterogeneityProvider.class).in(Singleton.class);
		}
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

			if (incomeFile == null) {
				throw new RuntimeException("No income file path given.");
			} else if (lambdaIncomeTravelCost == null) { throw new RuntimeException("No sensitivity parameter for income dependent travel cost is given.");}

			IncomeHeterogeneityImpl incomeHeterogeneityImpl = new IncomeHeterogeneityImpl(this.scenario.getPopulation());
			incomeHeterogeneityImpl.setName("Income dependent heterogeneity in perception of travel cost");
			incomeHeterogeneityImpl.setType(heterogeneityConfig.getIncomeOnTravelCostType());
			incomeHeterogeneityImpl.setLambda_income(Double.valueOf(heterogeneityConfig.getLambdaIncomeTravelcost()));

			if (heterogeneityConfig.getIncomeOnTravelCostType().equals("homo")) {
				log.info("Simulation with homogeneuos users. No income information added.");
			} else {
				log.info("Reading income file...");
				IncomePopulationReader incomesReader = new IncomePopulationReader(incomeHeterogeneityImpl, this.scenario.getPopulation());
				incomesReader.parse(incomeFile);

				MapWriter writer = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/incomeFactors.csv");
				writer.write(incomeHeterogeneityImpl.getIncomeFactors(), "PersonId", "IncomeFactor;");

				if (incomeHeterogeneityImpl.getType().equals("heteroAlphaProp")) {
					for (Id<Person> personId : this.scenario.getPopulation().getPersons().keySet()) {

						double randomFactor = 0.0;
						do {
							randomFactor = (MatsimRandom.getRandom().nextGaussian() * 0.2) + 1;
						} while (randomFactor < 0 && randomFactor > 2);
						incomeHeterogeneityImpl.getBetaFactors().put(personId, randomFactor);
					}

					MapWriter writerBetaFactors = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/betaNormalFactors.csv");
					writerBetaFactors.write(incomeHeterogeneityImpl.getBetaFactors(), "PersonId", "BetaFactor;");
				}
			}
			return incomeHeterogeneityImpl;
		}
	}
}
