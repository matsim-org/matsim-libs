package playground.yu.visum.filter;

import java.util.List;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class ActTypeFilter extends PersonFilterA {
	private boolean result = false;
	
	private static String criterion = "s1";

	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()) {
			List actsLegs = plan.getActsLegs();
			for (int i = 0; i < actsLegs.size(); i += 2) {
				Act act = (Act) actsLegs.get(i);
				result=(act.getType().equals(criterion));
				if (result) return result;
			}
		}
		return result;
	}
}
