package playground.pieter.singapore.utils.plans;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationImpl;

class PlansSubsampler {

	public void run(Scenario s, String fileName, double samplingProbability) {
		PopulationImpl plans = (PopulationImpl) s.getPopulation();
		plans.setIsStreaming(true);
		org.matsim.core.population.PopulationWriter pw = new org.matsim.core.population.PopulationWriter(
				plans, s.getNetwork());
		pw.startStreaming(fileName);
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
		int countPlans = 0;
		while (pid_it.hasNext()) {
			Id<Person> personId = pid_it.next();
			if (Math.random() > samplingProbability) {
				continue;
			}
			Person person = plans.getPersons().get(personId);

				countPlans++;
				pw.writePerson(person);


		}
		pw.closeStreaming();
		System.out.println("Wrote " + countPlans + " plans.");
		System.out.println("    done.");

	}


}
