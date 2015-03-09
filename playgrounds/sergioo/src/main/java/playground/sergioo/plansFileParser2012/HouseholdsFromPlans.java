package playground.sergioo.plansFileParser2012;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;

public class HouseholdsFromPlans {

	/**
	 * @param args
	 * 0 - Population file
	 * 1 - Home activity text
	 * 2 = Households file
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Population plans = scenario.getPopulation();
		final PopulationReader matsimPlansReader = new MatsimPopulationReader(scenario);
		matsimPlansReader.readFile(args[0]);
		Map<Id<ActivityFacility>, Household> facilityIdsHouseholds = new HashMap<Id<ActivityFacility>, Household>();
		Households households = new HouseholdsImpl();
		for(Person person:plans.getPersons().values()) {
			Activity homeActivity = null;
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Activity && ((Activity)planElement).getType().equals(args[1]))
					homeActivity = (Activity)planElement;
			if(homeActivity != null) {
				Household household = facilityIdsHouseholds.get(homeActivity.getFacilityId());
				if(household == null) {
					household = new HouseholdImpl(Id.create(households.getHouseholds().size(), Household.class));
					((HouseholdImpl)household).setMemberIds(new ArrayList<Id<Person>>());
					households.getHouseholds().put(household.getId(), household);
					facilityIdsHouseholds.put(homeActivity.getFacilityId(), household);
				}
				household.getMemberIds().add(person.getId());
			}
		}
		new HouseholdsWriterV10(households).writeFile(args[2]);
	}

}
