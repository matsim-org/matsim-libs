/**
 * 
 */
package playground.yu.analysis;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicPlan;

/**
 * @author yu
 * 
 */
public class PlanModeJudger {
	private static boolean useMode(BasicPlan plan, BasicLeg.Mode mode) {
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			if (!li.next().getMode().equals(mode)) {
				return false;
			}
		}
		return true;
	}

	public static BasicLeg.Mode getMode(BasicPlan plan) {
		BasicLeg.Mode tmpMode = null;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			BasicLeg.Mode tmpMode2 = li.next().getMode();
			;
			if (tmpMode != null) {
				if (!tmpMode.equals(tmpMode2)) {
					return BasicLeg.Mode.undefined;
				}
			}
			tmpMode = tmpMode2;
		}
		return tmpMode;
	}

	public static boolean useCar(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.car);
	}

	public static boolean usePt(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.pt);
	}

	public static boolean useMiv(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.miv);
	}

	public static boolean useRide(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.ride);
	}

	public static boolean useMotorbike(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.motorbike);
	}

	public static boolean useTrain(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.train);
	}

	public static boolean useBus(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.bus);
	}

	public static boolean useTram(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.tram);
	}

	public static boolean useBike(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.bike);
	}

	public static boolean useWalk(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.walk);
	}

	public static boolean useUndefined(BasicPlan plan) {
		return useMode(plan, BasicLeg.Mode.undefined);
	}
}
