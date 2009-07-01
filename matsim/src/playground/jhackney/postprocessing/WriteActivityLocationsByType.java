package playground.jhackney.postprocessing;

import java.util.Iterator;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class WriteActivityLocationsByType implements PlanAlgorithm{

	public WriteActivityLocationsByType(Population plans) {
		Iterator<PersonImpl> planIt=plans.getPersons().values().iterator();
		while(planIt.hasNext()){
		PlanImpl myPlan=planIt.next().getSelectedPlan();
		run(myPlan);
		}
	}

	public void run(PlanImpl plan) {
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
