package playground.jhackney.postprocessing;

import java.util.Iterator;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.population.algorithms.PlanAlgorithm;

public class WriteActivityLocationsByType implements PlanAlgorithm{

	public WriteActivityLocationsByType(Population plans) {
		Iterator<Person> planIt=plans.getPersons().values().iterator();
		while(planIt.hasNext()){
		Plan myPlan=planIt.next().getSelectedPlan();
		run(myPlan);
		}
	}

	public void run(Plan plan) {
		// TODO Auto-generated method stub
		ActIterator aIt=plan.getIteratorAct();
		while(aIt.hasNext()){
			Activity act = (Activity) aIt.next();
			Id id = (Id) act.getFacilityId();
			String type = (String) act.getType();
			System.out.println(type+"\t"+id);
		}

	}
}
