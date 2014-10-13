package playground.pieter.singapore.utils.plans;

import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;

/**
 * @author fouriep Utility to strip out transit plans, for running e.g.
 *         SelectedPlans2ESRIShape
 */
class PlansStripOutTransitPlans {

	public void run(Population plans) {
		int planCount = 0;
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		TreeSet<Id<Person>> pid_set = new TreeSet<>(); // ids of persons to remove
		Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			Id<Person> personId = pid_it.next();
			Person person = plans.getPersons().get(personId);

			for (int i = person.getPlans().size() - 1; i >= 0; i--) {
				Plan plan = person.getPlans().get(i);
				boolean transitPlan = false;

				for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
					LegImpl leg = (LegImpl) plan.getPlanElements().get(j);
					try {

						if (!leg.getMode().equals("car")) {
							transitPlan = true;
						}
					} catch (NullPointerException e) {
						transitPlan = true;
					}
				}
				if (transitPlan) {
					person.getPlans().remove(i);
					i--; // otherwise, we would skip one plan
					planCount++;
				}

			}
			if (person.getPlans().isEmpty()) {
				// the person has no plans left. remove the person afterwards
				// (so we do not disrupt the Iterator)
				pid_set.add(personId);
			}
		}

		// okay, now remove in a 2nd step all persons we do no longer need
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			Id<Person> pid = pid_it.next();
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
		System.out.println("Number of plans removed:   " + planCount);
		System.out.println("Number of persons removed: " + pid_set.size());
	}

}
