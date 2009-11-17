package playground.jhackney.postprocessing;

import java.util.Iterator;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class WriteActivityLocationsByType implements PlanAlgorithm{

	public WriteActivityLocationsByType(PopulationImpl plans) {
		Iterator<PersonImpl> planIt=plans.getPersons().values().iterator();
		while(planIt.hasNext()){
		Plan myPlan=planIt.next().getSelectedPlan();
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
