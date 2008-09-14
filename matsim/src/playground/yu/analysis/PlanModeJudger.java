/**
 * 
 */
package playground.yu.analysis;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;

/**
 * @author yu
 * 
 */
public class PlanModeJudger {
	private static boolean useMode(BasicPlan plan, String mode) {
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			if (!li.next().getMode().equals(mode)) {
				return false;
			}
		}
		return true;
	}

	public static String getMode(BasicPlan plan) {
		String tmpMode = null;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			String tmpMode2 = li.next().getMode();
			;
			if (tmpMode != null) {
				if (!tmpMode.equals(tmpMode2)) {
					return BasicLeg.UNDEFINEDMODE;
				}
			}
			tmpMode = tmpMode2;
		}
		return tmpMode;
	}

	public static boolean useCar(BasicPlan plan) {
		return useMode(plan, BasicLeg.CARMODE);
	}

	public static boolean usePt(BasicPlan plan) {
		return useMode(plan, BasicLeg.PTMODE);
	}

	public static boolean useMiv(BasicPlan plan) {
		return useMode(plan, BasicLeg.MIVMODE);
	}

	public static boolean useRide(BasicPlan plan) {
		return useMode(plan, BasicLeg.RIDEMODE);
	}

	public static boolean useMotorbike(BasicPlan plan) {
		return useMode(plan, BasicLeg.MOTORBIKEMODE);
	}

	public static boolean useTrain(BasicPlan plan) {
		return useMode(plan, BasicLeg.TRAINMODE);
	}

	public static boolean useBus(BasicPlan plan) {
		return useMode(plan, BasicLeg.BUSMODE);
	}

	public static boolean useTram(BasicPlan plan) {
		return useMode(plan, BasicLeg.TRAMMODE);
	}

	public static boolean useBike(BasicPlan plan) {
		return useMode(plan, BasicLeg.BIKEMODE);
	}

	public static boolean useWalk(BasicPlan plan) {
		return useMode(plan, BasicLeg.WALKMODE);
	}

	public static boolean useUndefined(BasicPlan plan) {
		return useMode(plan, BasicLeg.UNDEFINEDMODE);
	}
}
