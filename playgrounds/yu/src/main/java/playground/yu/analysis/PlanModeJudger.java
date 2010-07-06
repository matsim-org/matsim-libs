/**
 *
 */
package playground.yu.analysis;

import java.util.Iterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * judge, which transport mode was taken. This class can only be used with
 * plansfile, in that an agent only can take one transport mode in a day.
 *
 * @author yu
 *
 */
public class PlanModeJudger {
	private static boolean useMode(Plan plan, String mode) {
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof Leg) {
				Leg l = (Leg) o;
				if (!l.getMode().equals(mode)) {
					return false;
				}
			}
		}
		return true;
	}

	public static String getMode(Plan plan) {
		String tmpMode = null;
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof Leg) {
				Leg l = (Leg) o;
				String tmpMode2 = l.getMode();
				if (tmpMode != null) {
					if (!tmpMode.equals(tmpMode2)) {
						return "undefined";
					}
				} else
					tmpMode = tmpMode2;
			}
		}
		return tmpMode;
	}

	public static boolean useCar(Plan plan) {
		return useMode(plan, TransportMode.car);
	}

	public static boolean usePt(Plan plan) {
		return useMode(plan, TransportMode.pt);
	}

	public static boolean useMiv(Plan plan) {
		return useMode(plan, "miv");
	}

	public static boolean useRide(Plan plan) {
		return useMode(plan, TransportMode.ride);
	}

	public static boolean useMotorbike(Plan plan) {
		return useMode(plan, "motorbike");
	}

	public static boolean useTrain(Plan plan) {
		return useMode(plan, "train");
	}

	public static boolean useBus(Plan plan) {
		return useMode(plan, "bus");
	}

	public static boolean useTram(Plan plan) {
		return useMode(plan, "tram");
	}

	public static boolean useBike(Plan plan) {
		return useMode(plan, TransportMode.bike);
	}

	public static boolean useWalk(Plan plan) {
		return useMode(plan, TransportMode.walk);
	}

	public static boolean useUndefined(Plan plan) {
		return useMode(plan, "undefined");
	}
}
