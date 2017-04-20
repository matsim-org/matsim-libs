package playground.sergioo.mixedtraffic2016;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class GrowPopulation {

	private static final int NUM = 10;

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);
		PopulationFactory factory = PopulationUtils.getFactory();
		int size = scenario.getPopulation().getPersons().size();
		Set<Person> newPeople = new HashSet<>();
		int k = 0;
		for(int i=0; i<NUM; i++) {
			for(Person person:scenario.getPopulation().getPersons().values()){
				Person person2 = factory.createPerson(Id.createPersonId(size+k+1));
				for(Plan plan:person.getPlans()) {
					Plan plan2 = factory.createPlan();
					for(PlanElement element:plan.getPlanElements()) {
						if(element instanceof Activity){
							Activity activity = (Activity)element;
							Activity activity2 = factory.createActivityFromCoord(activity.getType(), activity.getCoord());
							double endTime = activity.getEndTime();
							if(Double.isInfinite(endTime))
								activity2.setMaximumDuration(activity.getMaximumDuration());
							else
								activity2.setEndTime(endTime+(i+1)*2700);
							plan2.addActivity(activity2);
						}
						else if (element instanceof Leg) {
							Leg leg = factory.createLeg(((Leg)element).getMode());
							plan2.addLeg(leg);
						}
					}
					person2.addPlan(plan2);
				}
				newPeople.add(person2);
				k++;
			}
		}
		for(Person person:newPeople)
			scenario.getPopulation().addPerson(person);
		new PopulationWriter(scenario.getPopulation()).write(args[1]);
	}

}
