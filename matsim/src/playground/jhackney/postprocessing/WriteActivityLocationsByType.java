package playground.jhackney.postprocessing;

import java.util.Iterator;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PlanAlgorithm;

public class WriteActivityLocationsByType implements PlanAlgorithm{

	public WriteActivityLocationsByType(Population plans) {
		Iterator<Person> planIt=plans.iterator();
		while(planIt.hasNext()){
		Plan myPlan=planIt.next().getSelectedPlan();
		run(myPlan);
		}
	}

	public void run(Plan plan) {
		// TODO Auto-generated method stub
		ActIterator aIt=plan.getIteratorAct();
		while(aIt.hasNext()){
			Act act = (Act) aIt.next();
			Id id = (Id) act.getFacilityId();
			String type = (String) act.getType();
			System.out.println(type+"\t"+id);
		}

	}
}
