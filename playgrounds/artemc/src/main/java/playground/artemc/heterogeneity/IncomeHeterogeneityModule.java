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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by artemc on 28/1/15.
 */
public class IncomeHeterogeneityModule extends AbstractModule {

	private static final Logger log = Logger.getLogger(IncomeHeterogeneityModule.class);

	private final IncomeHeterogeneityImpl incomeHeterogeneityImpl;

	public IncomeHeterogeneityModule() {
		this.incomeHeterogeneityImpl = null;
	}

	public IncomeHeterogeneityModule(IncomeHeterogeneityImpl incomeHeterogeneityImpl) {
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

				/*Create map of personal income factors and calculate the mean in order to adjust the utility parameters*/
				HashMap<Id<Person>, Double> incomeCostSensitivityFactors = new HashMap<Id<Person>, Double>();
				Double factorSum=0.0;
				Double factorMean;
				Double inverseFactorSum=0.0;
				Double inverseFactorMean;

				for(Id<Person> personId:population.getPersons().keySet()) {
					Integer personIncome = (int) population.getPersonAttributes().getAttribute(personId.toString(), "income");
					double incomeCostSensitivity = Math.pow((double) personIncome / incomeMean, (incomeHeterogeneityImpl.getLambda_income()));
					incomeCostSensitivityFactors.put(personId, incomeCostSensitivity);
					factorSum = factorSum + incomeCostSensitivity;
					inverseFactorSum = inverseFactorSum + (1.0/incomeCostSensitivity);
				}

				factorMean = factorSum / (double) incomeCostSensitivityFactors.size();
				inverseFactorMean = inverseFactorSum / (double) incomeCostSensitivityFactors.size();

				/*Add normalized income dependent value of time (alpha) factors (= 1/incomeCostSensitivityFactor) to the agent population*/
				/*Add schedule delays beta factors*/
				for(Id<Person> personId:population.getPersons().keySet()){

					double incomeAlphaFactor = (1.0/incomeCostSensitivityFactors.get(personId))/inverseFactorMean;
					population.getPersonAttributes().putAttribute(personId.toString(),"incomeAlphaFactor",incomeAlphaFactor);
					population.getPersons().get(personId).getCustomAttributes().put("incomeAlphaFactor", incomeAlphaFactor);

					double sdBetaFactor = (double) population.getPersonAttributes().getAttribute(personId.toString(),"betaFactor");
					population.getPersons().get(personId).getCustomAttributes().put("sdBetaFactor",sdBetaFactor);

					double incomeGammaFactor = incomeCostSensitivityFactors.get(personId)/factorMean;
					population.getPersonAttributes().putAttribute(personId.toString(),"incomeGammaFactor",incomeGammaFactor);
					population.getPersons().get(personId).getCustomAttributes().put("incomeGammaFactor", incomeGammaFactor);

					//TODO remove incomeHeterogeneityImpl
					incomeHeterogeneityImpl.getIncomeFactors().put(personId, incomeAlphaFactor);
				}

				/*Write personal income factors to file*/
				HashMap<Id<Person>, ArrayList<String>> factors = new HashMap<Id<Person>, ArrayList<String>>();
				for(Id<Person> personId:population.getPersons().keySet()){
					factors.put(personId, new ArrayList<String>());
					factors.get(personId).add(incomeCostSensitivityFactors.get(personId).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("incomeAlphaFactor")).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("sdBetaFactor")).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("incomeGammaFactor")).toString());
				}

				MapWriter writer = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/incomeCostSensitivityFactors.csv");
				ArrayList<String> head = new ArrayList<String>();
				head.add("PersonId");
				head.add("incomeCostSensitivity");
				head.add("incomeAlpha");
				head.add("sdBetaFactor");
				head.add("incomeGamma");
				writer.writeArray(factors, head);

			}
			return incomeHeterogeneityImpl;
		}
	}
}
