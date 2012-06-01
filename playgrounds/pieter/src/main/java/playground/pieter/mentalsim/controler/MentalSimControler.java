package playground.pieter.mentalsim.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author fouriep
 * 
 */
public class MentalSimControler extends Controler {
	private PopulationImpl originalPopulation;
	private PopulationImpl subsetPopulation;

	public Population getOriginalPopulation() {
		return originalPopulation;
	}

	public void setOriginalPopulation(PopulationImpl originalPopulation) {
		this.originalPopulation = originalPopulation;
	}

	/**
	 * @param samplingProbability
	 *            Samples persons for cloning. Only the selected plan of the
	 *            agent is cloned. The original population is stored for later
	 *            retrieval.
	 */
	public void createSubSetAndStoreOriginalPopulation(
			double samplingProbability) {
		originalPopulation = (PopulationImpl) this.getPopulation();
		PopulationFactoryImpl popfac = (PopulationFactoryImpl) this.population
				.getFactory();
		subsetPopulation = new PopulationImpl(
				(ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils
						.createConfig()));
		for (Person p : originalPopulation.getPersons().values()) {
			PersonImpl pax = (PersonImpl) p;
			// sample persons for cloning
			if (Math.random() > samplingProbability) {
				continue;
			}
			PersonImpl fake = (PersonImpl) popfac.createPerson(new IdImpl(
					"fake" + p.getId().toString()));
			fake.setAge(pax.getAge());
			fake.setCarAvail(pax.getCarAvail());
			// fake.setCustomAttributes(pax.getCustomAttributes());
			// fake.setDesires(pax.getDesires());
			// fake.setEmployed(pax.getEmployed);
			fake.setLicence(pax.getLicense());
			Plan plan = pax.copySelectedPlan();
			fake.addPlan(plan);
			fake.setSelectedPlan(plan);
			fake.setSex(pax.getSex());
			// fake.setTravelCards(pax.getTravelcards());
			subsetPopulation.addPerson(fake);
		}
		this.getScenario().setPopulation(subsetPopulation);
		Logger.getLogger("MentalSimControler").error(
				"Replaced the population with "
						+ subsetPopulation.getPersons().size()
						+ " FAKE persons.");
	}

	/**
	 * @param selector
	 *            selects a plan from the subset population according to the
	 *            selector scheme, then copies that plan to the original
	 *            population, and sets it as the selected plan
	 */
	public void restoreOriginalPopulationAndReturnSubSetPlan(
			PlanSelector selector) {
		for (Person p : subsetPopulation.getPersons().values()) {
			PersonImpl pax = (PersonImpl) p;
			PersonImpl original = (PersonImpl) originalPopulation.getPersons()
					.get(new IdImpl(pax.getId().toString().substring(4)));
			Plan plan = selector.selectPlan(pax);
			original.addPlan(plan);
			original.setSelectedPlan(plan);
		}
		this.getScenario().setPopulation(originalPopulation);
		Logger.getLogger("MentalSimControler").error(
				"Replaced the original population with "
						+ originalPopulation.getPersons().size() + " persons.");
	}

	public MentalSimControler(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub

	}

}
