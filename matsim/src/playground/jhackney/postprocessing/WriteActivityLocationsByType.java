package playground.jhackney.postprocessing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class WriteActivityLocationsByType implements PlanAlgorithm{

	public WriteActivityLocationsByType(PopulationImpl plans) {
		for (Person person : plans.getPersons().values()) {
		Plan myPlan=person.getSelectedPlan();
		run(myPlan);
		}
	}

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				Id id = act.getFacilityId();
				String type = act.getType();
				System.out.println(type+"\t"+id);
			}
		}
	}
}
