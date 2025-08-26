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
        Population drtPlans = PopulationUtils.readPopulation("/Users/luchengqi/Documents/MATSimScenarios/Berlin/accessibility-drt-study/v6.4/drt-plans-0.5pct.xml.gz");
        Random rand = new Random(1);
        for (Person drtPerson : drtPlans.getPersons().values()) {
            if (PersonUtils.getAge(drtPerson) != null && PersonUtils.getAge(drtPerson) > 67) {
				if (rand.nextDouble() < 0.5) {
					drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.SPECIAL_NEED);
				} else {
					drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.NORMAL);
				}
            } else if (rand.nextDouble() < 0.05) {
                drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.SPECIAL_NEED);
            } else if (rand.nextDouble() < 0.05) {
                drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.PREMIUM);
            } else {
                drtPerson.getAttributes().putAttribute(PassengerAttribute.ATTRIBUTE_NAME, PassengerAttribute.NORMAL);
            }
        }
        new PopulationWriter(drtPlans).write("/Users/luchengqi/Documents/MATSimScenarios/Berlin/accessibility-drt-study/v6.4/drt-plans-0.5pct-with-remarks.xml.gz");
    }
}
