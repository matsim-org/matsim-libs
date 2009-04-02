/**
 * 
 */
package playground.yu.analysis.forZrh;

import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.AgentEvent;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.EnRouteModalSplit;
import playground.yu.analysis.PlanModeJudger;

/**
 * compute daily En Route/ departures/ arrivals of Zurich and Kanton Zurich
 * respectively with through traffic
 * 
 * @author yu
 * 
 */
public class EnRouteModalSplit4Zrh extends EnRouteModalSplit {
	private double[] throughDep = null, throughArr = null, throughStuck = null,
			throughEnRoute = null;

	/**
	 * @param scenario
	 * @param binSize
	 * @param nofBins
	 * @param plans
	 */
	public EnRouteModalSplit4Zrh(String scenario, int binSize, int nofBins,
			Population plans) {
		super(scenario, binSize, nofBins, plans);
		if (scenario.equals("Zurich") || scenario.equals("Kanton_Zurich")) {
			// through traffic
			this.throughDep = new double[nofBins + 1];
			this.throughArr = new double[nofBins + 1];
			this.throughEnRoute = new double[nofBins + 1];
			this.throughStuck = new double[nofBins + 1];
		}
	}

	/**
	 * @param scenario
	 * @param binSize
	 * @param plans
	 */
	public EnRouteModalSplit4Zrh(String scenario, int binSize, Population plans) {
		super(scenario, binSize, plans);
	}

	/**
	 * @param scenario
	 * @param plans
	 */
	public EnRouteModalSplit4Zrh(String scenario, Population plans) {
		super(scenario, plans);
	}

	/**
	 * @param scenario
	 * @param ppl
	 * @param toll
	 */
	public EnRouteModalSplit4Zrh(String scenario, Population ppl,
			RoadPricingScheme toll) {
		super(scenario, ppl, toll);
	}

	protected void internalCompute(int binIdx, AgentEvent ae, Plan plan,
			double[] allCount, double[] carCount, double[] ptCount,
			double[] wlkCount, double[] otherCount) {
		allCount[binIdx]++;
		if (otherCount != null)
			if (Integer.parseInt(ae.getPersonId().toString()) > 1000000000)
				otherCount[binIdx]++;
			else {
				if (PlanModeJudger.useCar(plan))
					carCount[binIdx]++;
				else if (PlanModeJudger.usePt(plan)) {
					if (ptCount != null)
						ptCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(plan))
					if (wlkCount != null)
						wlkCount[binIdx]++;
			}
		else {
			if (PlanModeJudger.useCar(plan))
				carCount[binIdx]++;
			else if (PlanModeJudger.usePt(plan)) {
				if (ptCount != null)
					ptCount[binIdx]++;
			} else if (PlanModeJudger.useWalk(plan))
				if (wlkCount != null)
					wlkCount[binIdx]++;
		}
	}

}
