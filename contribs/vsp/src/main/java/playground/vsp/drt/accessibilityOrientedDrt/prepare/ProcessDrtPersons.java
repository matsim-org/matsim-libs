package playground.vsp.drt.accessibilityOrientedDrt.prepare;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.PassengerAttribute;

import java.util.Random;

/**
 * Tag person with has a disability and passengers who are willing to pay extra for the premium service
 */
public class ProcessDrtPersons {
	public static void main(String[] args) {
		// the frequency of premium passengers
		double premiumRatio = 0.01;
		Population drtPlans = PopulationUtils.readPopulation("/Users/luchengqi/Documents/MATSimScenarios/Berlin/accessibility-drt-study/v6.4/drt-plans-0.5pct-processed.xml.gz");
		Random rand = new Random(1);
		for (Person drtPerson : drtPlans.getPersons().values()) {
			// default case: normal
			drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.NORMAL);

			// then we try to change the attribute for some persons
			if (PersonUtils.getAge(drtPerson) != null && PersonUtils.getAge(drtPerson) > 67) {
				// if the person is old, then there is an increased chance of becoming "SPECIAL_NEED"
				if (rand.nextDouble() < 0.5) {
					drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.SPECIAL_NEED);
				}
			} else {
				// otherwise, a lower chance of becoming "SPECIAL_NEED"
				if (rand.nextDouble() < 0.05) {
					drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.SPECIAL_NEED);
				}
			}

			// on top of that, we also mark some "premium passengers."
			if (rand.nextDouble() < premiumRatio) {
				drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.PREMIUM);
			}

		}
		new PopulationWriter(drtPlans).write("/Users/luchengqi/Documents/MATSimScenarios/Berlin/accessibility-drt-study/v6.4/drt-plans-0.5pct-heterogeneous-with-"+ premiumRatio +"-premium.xml.gz");
	}
}
