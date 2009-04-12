/**
 * 
 */
package playground.yu.analysis;

import java.util.Iterator;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.core.api.population.Leg;

/**
 * judge, which transport mode was taken. This class can only be used with
 * plansfile, in that an agent only can take one transport mode in a day.
 * 
 * @author yu
 * 
 */
public class PlanModeJudger {
	private static boolean useMode(BasicPlan plan, TransportMode mode) {
		for (Iterator li = plan.getPlanElements().iterator(); li.hasNext();) {
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

	public static TransportMode getMode(BasicPlan plan) {
		TransportMode tmpMode = null;
		for (Iterator li = plan.getPlanElements().iterator(); li.hasNext();) {
			Object o = li.next();
			if (o instanceof Leg) {
				Leg l = (Leg) o;
				TransportMode tmpMode2 = l.getMode();
				if (tmpMode != null) {
					if (!tmpMode.equals(tmpMode2)) {
						return TransportMode.undefined;
					}
				} else
					tmpMode = tmpMode2;
			}
		}
		return tmpMode;
	}

	public static boolean useCar(BasicPlan plan) {
		return useMode(plan, TransportMode.car);
	}

	public static boolean usePt(BasicPlan plan) {
		return useMode(plan, TransportMode.pt);
	}

	public static boolean useMiv(BasicPlan plan) {
		return useMode(plan, TransportMode.miv);
	}

	public static boolean useRide(BasicPlan plan) {
		return useMode(plan, TransportMode.ride);
	}

	public static boolean useMotorbike(BasicPlan plan) {
		return useMode(plan, TransportMode.motorbike);
	}

	public static boolean useTrain(BasicPlan plan) {
		return useMode(plan, TransportMode.train);
	}

	public static boolean useBus(BasicPlan plan) {
		return useMode(plan, TransportMode.bus);
	}

	public static boolean useTram(BasicPlan plan) {
		return useMode(plan, TransportMode.tram);
	}

	public static boolean useBike(BasicPlan plan) {
		return useMode(plan, TransportMode.bike);
	}

	public static boolean useWalk(BasicPlan plan) {
		return useMode(plan, TransportMode.walk);
	}

	public static boolean useUndefined(BasicPlan plan) {
		return useMode(plan, TransportMode.undefined);
	}
}
