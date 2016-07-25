package playground.pieter.singapore.utils.plans;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;

class PlansSubsampler {

	public void run(Scenario s, String fileName, double samplingProbability) {
		Population plans = (Population) s.getPopulation();
		StreamingUtils.setIsStreaming(plans, true);
		StreamingPopulationWriter pw = new StreamingPopulationWriter(
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
