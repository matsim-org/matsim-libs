package playground.pieter.singapore.utils.plans;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

public class PlansAddCarAvailability {
	public void run(Population plans) {
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			Id<Person> personId = pid_it.next();
			PersonImpl person = (PersonImpl) plans.getPersons().get(personId);

			for (int i = person.getPlans().size() - 1; i >= 0; i--) {
				Plan plan = person.getPlans().get(i);
				boolean carAvail = false;
				for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
					LegImpl leg = (LegImpl) plan.getPlanElements().get(j);

					if (leg.getMode().equals("car")) {
						carAvail = true;
					}

				}
				if (carAvail)
					person.setCarAvail("always");
				else
					person.setCarAvail("never");
			}

		}

		// okay, now remove in a 2nd step all persons we do no longer need

		System.out.println("    done.");

	}
}