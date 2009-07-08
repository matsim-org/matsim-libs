/**
 * 
 */
package playground.yu.analysis;

import java.util.Iterator;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * judge, which transport mode was taken. This class can only be used with
 * plansfile, in that an agent only can take one transport mode in a day.
 * 
 * @author yu
 * 
 */
public class PlanModeJudger {
	private static boolean useMode(PlanImpl plan, TransportMode mode) {
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof LegImpl) {
				LegImpl l = (LegImpl) o;
				if (!l.getMode().equals(mode)) {
					return false;
				}
			}
		}
		return true;
	}

	public static TransportMode getMode(PlanImpl plan) {
		TransportMode tmpMode = null;
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof LegImpl) {
				LegImpl l = (LegImpl) o;
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

	public static boolean useCar(PlanImpl plan) {
		return useMode(plan, TransportMode.car);
	}

	public static boolean usePt(PlanImpl plan) {
		return useMode(plan, TransportMode.pt);
	}

	public static boolean useMiv(PlanImpl plan) {
		return useMode(plan, TransportMode.miv);
	}

	public static boolean useRide(PlanImpl plan) {
		return useMode(plan, TransportMode.ride);
	}

	public static boolean useMotorbike(PlanImpl plan) {
		return useMode(plan, TransportMode.motorbike);
	}

	public static boolean useTrain(PlanImpl plan) {
		return useMode(plan, TransportMode.train);
	}

	public static boolean useBus(PlanImpl plan) {
		return useMode(plan, TransportMode.bus);
	}

	public static boolean useTram(PlanImpl plan) {
		return useMode(plan, TransportMode.tram);
	}

	public static boolean useBike(PlanImpl plan) {
		return useMode(plan, TransportMode.bike);
	}

	public static boolean useWalk(PlanImpl plan) {
		return useMode(plan, TransportMode.walk);
	}

	public static boolean useUndefined(PlanImpl plan) {
		return useMode(plan, TransportMode.undefined);
	}
}
