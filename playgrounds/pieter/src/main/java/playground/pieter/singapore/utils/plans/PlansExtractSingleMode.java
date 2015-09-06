package playground.pieter.singapore.utils.plans;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

class PlansExtractSingleMode {

	public void run(Scenario s, String fileName) {
		PopulationImpl plans = (PopulationImpl) s.getPopulation();
		plans.setIsStreaming(true);
		org.matsim.core.population.PopulationWriter pw = new org.matsim.core.population.PopulationWriter(plans, s.getNetwork());
		pw.startStreaming(fileName);
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");
		
		Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
		int countCarPlans = 0;
		while (pid_it.hasNext()) {
			Id<Person> personId = pid_it.next();
			Person person = plans.getPersons().get(personId);

			for (int i = person.getPlans().size() - 1; i >= 0; i--) {
				boolean carDriver = false;
				if(PersonImpl.getCarAvail(person).equals("always")|| PersonImpl.getCarAvail(person).equals("sometimes"))
					carDriver=true;
//				Plan plan = person.getPlans().get(i);
//				boolean transitUser = false;
//				for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
//					LegImpl leg = (LegImpl) plan.getPlanElements().get(j);
//
//					if (leg.getMode().equals("car")) {
//						carDriver = true;
//					}
//				}
				if(carDriver){
						countCarPlans++;
					pw.writePerson(person);
				}
			}

		}
		pw.closeStreaming();
		// okay, now remove in a 2nd step all persons we do no longer need
		System.out.println("Wrote " + countCarPlans
				+ " car only plans.");
		System.out.println("processed " + plans.getPersons().size() + " plans");
		System.out.println("    done.");

	}

}
