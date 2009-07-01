package playground.yu.visum.filter;

import java.util.List;

import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

public class ActTypeFilter extends PersonFilterA {
	private boolean result = false;
	
	private static String criterion = "s1";

	@SuppressWarnings("unchecked")
	@Override
	public boolean judge(PersonImpl person) {
		for (PlanImpl plan : person.getPlans()) {
			List actsLegs = plan.getPlanElements();
			for (int i = 0; i < actsLegs.size(); i += 2) {
				ActivityImpl act = (ActivityImpl) actsLegs.get(i);
				result=(act.getType().equals(criterion));
				if (result) return result;
			}
		}
		return result;
	}
}
