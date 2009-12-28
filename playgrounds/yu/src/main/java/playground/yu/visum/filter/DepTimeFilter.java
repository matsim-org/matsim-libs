package playground.yu.visum.filter;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

public class DepTimeFilter extends PersonFilterA {
	private boolean result = false;

	private static double criterionMAX = Time.parseTime("09:00");

	private static double criterionMIN = Time.parseTime("06:40");

	@SuppressWarnings("unchecked")
	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()) {
			List actsLegs = plan.getPlanElements();
			for (int i = 1; i < actsLegs.size(); i += 2) {
				LegImpl leg = (LegImpl) actsLegs.get(i);
				result = ((criterionMIN < leg.getDepartureTime()) && (leg
						.getDepartureTime() < criterionMAX));
				if (result)
					return result;
			}
		}
		return result;
	}
}
