package playground.yu.visum.filter;

import java.util.List;

import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

public class ActTypeFilter extends PersonFilterA {
	private boolean result = false;
	
	private static String criterion = "s1";

	@SuppressWarnings("unchecked")
	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()) {
			List actsLegs = plan.getPlanElements();
			for (int i = 0; i < actsLegs.size(); i += 2) {
				Activity act = (Activity) actsLegs.get(i);
				result=(act.getType().equals(criterion));
				if (result) return result;
			}
		}
		return result;
	}
}
