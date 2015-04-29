package playground.artemc.plansTools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.pt.router.TransitActsRemover;

import java.util.Iterator;
import java.util.TreeSet;

public class PlanRouteStripper {
	public void run(Population plans) {
		int planCount = 0;
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		TreeSet<Id<Person>> pid_set = new TreeSet<>(); // ids of persons to remove
		Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
		TransitActsRemover tar = new TransitActsRemover();
		while (pid_it.hasNext()) {
			Id<Person> personId = pid_it.next();
			Person person = plans.getPersons().get(personId);

			for (int i = person.getPlans().size() - 1; i >= 0; i--) {
				Plan plan = person.getPlans().get(i);
				tar.run(plan);
				for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
					LegImpl leg = (LegImpl) plan.getPlanElements().get(j);

//					if(leg.getMode().equals("pt")) {
//						leg.setRoute(null);
//					}
					leg.setRoute(null);
					leg.setTravelTime(Double.NaN);
					
				}
			}
		}
	}
}
