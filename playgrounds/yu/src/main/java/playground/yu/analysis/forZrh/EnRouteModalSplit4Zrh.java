/**
 *
 */
package playground.yu.analysis.forZrh;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.EnRouteModalSplit;
import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.TollTools;
import playground.yu.utils.container.CollectionMath;

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
			double[] wlkCount, double[] throughCount) {
		allCount[binIdx]++;
		if (throughCount != null)
			if (Integer.parseInt(ae.getPersonId().toString()) > 1000000000)
				throughCount[binIdx]++;
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

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		internalHandleEvent(event, this.arr, this.carArr, this.ptArr, wlkArr,
				throughArr);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id id = event.getPersonId();
		Integer itg = legCounts.get(id);
		if (itg == null)
			itg = Integer.valueOf(-1);
		legCounts.put(id, itg.intValue() + 1);
		internalHandleEvent(event, this.dep, this.carDep, this.ptDep, wlkDep,
				throughDep);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		internalHandleEvent(event, this.stuck, this.carStuck, null, null,
				this.throughStuck);
	}

	protected void internalHandleEvent(AgentEvent ae, double[] allCount,
			double[] carCount, double[] ptCount, double[] wlkCount,
			double[] throughCount) {
		int binIdx = getBinIndex(ae.getTime());
		Plan selectedPlan = plans.getPersons().get(ae.getPersonId())
				.getSelectedPlan();
		if (toll != null) {
			if (TollTools.isInRange(((PlanImpl) selectedPlan).getFirstActivity().getLinkId(),
					toll)) {
				this.internalCompute(binIdx, ae, selectedPlan, allCount,
						carCount, ptCount, wlkCount, throughCount);
			}
		} else {
			internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
					ptCount, wlkCount, throughCount);
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param bw
	 *            The data stream where to write the gathered data.
	 */
	@Override
	public void write(final BufferedWriter bw) {
		calcOnRoute();
		try {
			bw
					.write("time\ttimeBin\t"
							+ "departures\tarrivals\tstuck\ton_route\t"
							+ "carDepartures\tcarArrivals\tcarStuck\tcarOnRoute\t"
							+ "ptDepartures\tptArrivals\tptStuck\tptOnRoute\t"
							+ "walkDepartures\twalkArrivals\twalkStuck\twalkOnRoute\t"
							+ "throughDepartures\tthroughArrivals\tthroughStuck\tthroughOnRoute\n");
			for (int i = 0; i < this.dep.length; i++) {
				bw
						.write(Time.writeTime(i * this.binSize)
								+ "\t"
								+ i * this.binSize
								+ "\t"
								+ this.dep[i]
								+ "\t"
								+ this.arr[i]
								+ "\t"
								+ this.stuck[i]
								+ "\t"
								+ this.enRoute[i]
								+ "\t"
								+ this.carDep[i]
								+ "\t"
								+ this.carArr[i]
								+ "\t"
								+ this.carStuck[i]
								+ "\t"
								+ this.carEnRoute[i]
								+ "\t"
								+ this.ptDep[i]
								+ "\t"
								+ this.ptArr[i]
								+ "\t"
								+ 0
								+ "\t"
								+ this.ptEnRoute[i]
								+ "\t"
								+ this.wlkDep[i]
								+ "\t"
								+ this.wlkArr[i]
								+ "\t"
								+ 0
								+ "\t"
								+ this.wlkEnRoute[i]
								+ "\t"
								+ ((throughEnRoute != null) ? (this.throughDep[i]
										+ "\t"
										+ this.throughArr[i]
										+ "\t"
										+ this.throughStuck[i] + "\t" + this.throughEnRoute[i])
										: (0 + "\t" + 0 + "\t" + 0 + "\t" + 0))
								+ "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* output methods */

	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename
	 *            The name of a file where to write the gathered data.
	 */
	@Override
	public void write(final String filename) {
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(filename);
			write(bw);
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeCharts(final String filename) {
		int length = enRoute.length;
		double[] xs = new double[length];
		for (int j = 0; j < xs.length; j++) {
			xs[j] = ((double) j) * (double) this.binSize / 3600.0;
		}
		// enRoute chart
		XYLineChart enRouteChart = new XYLineChart("Leg Histogramm - En Route",
				"time", "agents en route from " + scenario);
		enRouteChart.addSeries("drivers", xs, carEnRoute);
		if (CollectionMath.getSum(ptEnRoute) > 0)
			enRouteChart.addSeries("public transit users", xs, ptEnRoute);
		if (CollectionMath.getSum(wlkEnRoute) > 0)
			enRouteChart.addSeries("walkers", xs, wlkEnRoute);
		if (CollectionMath.getSum(throughEnRoute) > 0)
			enRouteChart.addSeries("through", xs, throughEnRoute);
		enRouteChart.addSeries("all agents", xs, enRoute);
		enRouteChart.saveAsPng(filename + "enRoute.png", 1024, 768);
		// departures chart
		XYLineChart departChart = new XYLineChart(
				"Leg Histogramm - Departures", "time", "departing agents from "
						+ scenario);
		departChart.addSeries("drivers", xs, carDep);
		if (CollectionMath.getSum(ptDep) > 0)
			departChart.addSeries("public transit users", xs, ptDep);
		if (CollectionMath.getSum(wlkDep) > 0)
			departChart.addSeries("walkers", xs, wlkDep);
		if (CollectionMath.getSum(throughDep) > 0)
			departChart.addSeries("through", xs, throughDep);
		departChart.addSeries("all agents", xs, dep);
		departChart.saveAsPng(filename + "departures.png", 1024, 768);
		// arrivals chart
		XYLineChart arrChart = new XYLineChart("Leg Histogramm - Arrivals",
				"time", "arriving agents from " + scenario);
		arrChart.addSeries("drivers", xs, carArr);
		if (CollectionMath.getSum(ptArr) > 0)
			arrChart.addSeries("public transit users", xs, ptArr);
		if (CollectionMath.getSum(wlkArr) > 0)
			arrChart.addSeries("walkers", xs, wlkArr);
		if (CollectionMath.getSum(throughArr) > 0)
			arrChart.addSeries("through", xs, throughArr);
		arrChart.addSeries("all agents", xs, arr);
		arrChart.saveAsPng(filename + "arrivals.png", 1024, 768);
	}

}
