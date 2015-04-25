package playground.artemc.heterogeneity;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import playground.artemc.utils.MapWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

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
			log.info("Loading heterogeneity config group...");

			Population population = scenario.getPopulation();

			/*Check if personAttributes has been loaded*/
			if(population.getPersonAttributes().equals(null))
			{
				log.error("Person attributes are empty!");
			}

			Map<String, String> params = scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams();

			IncomeHeterogeneityImpl incomeHeterogeneityImpl = new IncomeHeterogeneityImpl(population);
			incomeHeterogeneityImpl.setName("Income dependent heterogeneity in perception of travel cost");
			incomeHeterogeneityImpl.setType(params.get("incomeOnTravelCostType"));
			incomeHeterogeneityImpl.setLambda_income(Double.valueOf(params.get("incomeOnTravelCostLambda")));

			if (incomeHeterogeneityImpl.getType().equals("homo")) {
				log.info("Simulation with homogeneuos agents...");
			} else {
				log.info("Simulation with "+incomeHeterogeneityImpl.getType()+" heterogeneity: calculating income factors.");

				/*Calculate Income Statistics*/
				Integer incomeSum=0;
				Double incomeMean = 0.0;

				for(Id<Person> personId:population.getPersons().keySet()){
					incomeSum = incomeSum + (int) population.getPersonAttributes().getAttribute(personId.toString(), "income");
				}
				incomeMean = (double) incomeSum / (double) population.getPersons().size();

				/*Create map of personal income factors*/
				Double factorSum=0.0;
				for(Id<Person> personId:population.getPersons().keySet()){
					Integer personIncome = (int) population.getPersonAttributes().getAttribute(personId.toString(), "income");

					double incomeFactor = Math.pow((double) personIncome/incomeMean,(incomeHeterogeneityImpl.getLambda_income()));

					population.getPersonAttributes().putAttribute(personId.toString(),"incomeAlphaFactor",incomeFactor);

					incomeHeterogeneityImpl.getIncomeFactors().put(personId, incomeFactor);
					factorSum = factorSum + incomeFactor;
				}

				/*Write personal income factors to file*/
				MapWriter writer = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/incomeFactors.csv");
				writer.write(incomeHeterogeneityImpl.getIncomeFactors(), "PersonId", "IncomeFactor;");

			}
			return incomeHeterogeneityImpl;
		}
	}
}
