package playground.artemc.heterogeneity;

import com.google.inject.Singleton;
import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import playground.artemc.utils.MapWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.math.special.Erf.erf;

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
				Long incomeSum= Long.valueOf(0);
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

					double betaFactor = (double) population.getPersonAttributes().getAttribute(personId.toString(),"betaFactor");
					population.getPersons().get(personId).getCustomAttributes().put("betaFactor",betaFactor);

					double normalizedCSFactor = incomeCostSensitivityFactors.get(personId)/factorMean;
					population.getPersonAttributes().putAttribute(personId.toString(),"normalizedCSFactor",normalizedCSFactor);
					population.getPersons().get(personId).getCustomAttributes().put("normalizedCSFactor", normalizedCSFactor);

					/*Random double*/
					double rnd = MatsimRandom.getRandom().nextDouble();
					double n = incomeHeterogeneityImpl.getLambda_income() / (-0.1697);

					/*VOT deviations -15 to +15*/
					//Double votDeviation = ((sdBetaFactor - 1) / 0.6 ) * n * 3;
					double votDeviation = (rnd- 0.5) *  6 * n;
					population.getPersonAttributes().putAttribute(personId.toString(), "votDeviation", votDeviation);
					population.getPersons().get(personId).getCustomAttributes().put("votDeviation", votDeviation);

					/*0.1 - 0.9, mean=0.5*/
					double uniformSdBetaFactor = (rnd- 0.5) * 2 *  0.08 * n + 0.5;
					population.getPersonAttributes().putAttribute(personId.toString(), "uniformSdBetaFactor", uniformSdBetaFactor);
					population.getPersons().get(personId).getCustomAttributes().put("uniformSdBetaFactor", uniformSdBetaFactor);

					/*1 - 6.8, mean=3.9*/
					double uniformSdGammaFactor = (rnd - 0.5) *  1.16 * n + 3.9;
					population.getPersonAttributes().putAttribute(personId.toString(), "uniformSdGammaFactor", uniformSdGammaFactor);
					population.getPersons().get(personId).getCustomAttributes().put("uniformSdGammaFactor", uniformSdGammaFactor);


					//TODO remove incomeHeterogeneityImpl
					incomeHeterogeneityImpl.getIncomeFactors().put(personId, incomeAlphaFactor);
				}

				/*Write personal income factors to file*/
				HashMap<Id<Person>, ArrayList<String>> factors = new HashMap<Id<Person>, ArrayList<String>>();
				for(Id<Person> personId:population.getPersons().keySet()){
					factors.put(personId, new ArrayList<String>());
					factors.get(personId).add(incomeCostSensitivityFactors.get(personId).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("incomeAlphaFactor")).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("betaFactor")).toString());
					factors.get(personId).add((population.getPersons().get(personId).getCustomAttributes().get("normalizedCSFactor")).toString());
				}

				MapWriter writer = new MapWriter(this.scenario.getConfig().controler().getOutputDirectory() + "/incomeCostSensitivityFactors.csv");
				ArrayList<String> head = new ArrayList<String>();
				head.add("PersonId");
				head.add("incomeCostSensitivity");
				head.add("incomeAlpha");
				head.add("betaFactor");
				head.add("normalizedCSFactor");
				writer.writeArray(factors, head);

			}
			return incomeHeterogeneityImpl;
		}
	}
}
