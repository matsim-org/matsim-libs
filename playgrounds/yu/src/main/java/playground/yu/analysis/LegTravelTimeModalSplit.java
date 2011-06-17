/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.yu.analysis;

import java.util.HashMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;
import playground.yu.utils.container.CollectionMath;
import playground.yu.utils.io.SimpleWriter;

/**
 * make time variation curve of average {@code Leg} travel time and their sum.
 * This class can only be used with plansfile, in that all <code>Leg</code>s in
 * a <code>Plan</code> muss be equiped with the same {@code Mode}
 * {@link org.matsim.api.core.v01.TransportMode} in a day.
 * 
 * @author ychen
 * 
 */
public class LegTravelTimeModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, Analysis {

	protected final Population plans;

	protected RoadPricingScheme toll = null;

	protected final int binSize;

	protected final double[] travelTimes, carTravelTimes, ptTravelTimes,
			wlkTravelTimes, bikeTravelTimes, othersTravelTimes;

	protected final int[] arrCount, carArrCount, ptArrCount, wlkArrCount,
			bikeArrCount, othersArrCount;

	/**
	 * @param arg0
	 *            - String agentId
	 * @param arg1
	 *            - Double departure time
	 */
	protected final HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();

	public LegTravelTimeModalSplit(final int binSize, final int nofBins,
			final Population plans) {
		this.plans = plans;
		this.binSize = binSize;

		travelTimes = new double[nofBins + 1];
		arrCount = new int[nofBins + 1];

		carTravelTimes = new double[nofBins + 1];
		ptTravelTimes = new double[nofBins + 1];
		wlkTravelTimes = new double[nofBins + 1];
		bikeTravelTimes = new double[nofBins + 1];
		othersTravelTimes = new double[nofBins + 1];

		carArrCount = new int[nofBins + 1];
		ptArrCount = new int[nofBins + 1];
		wlkArrCount = new int[nofBins + 1];
		bikeArrCount = new int[nofBins + 1];
		othersArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit(final int binSize, final Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit(final Population plans) {
		this(300, plans);
	}

	public LegTravelTimeModalSplit(Population ppl, RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		if (toll != null) {
			// only inhabitant from Kanton
			if (TollTools.isInRange(((PlanImpl) plans.getPersons().get(
					event.getPersonId()).getSelectedPlan()).getFirstActivity()
					.getLinkId(), toll)) {
				tmpDptTimes
						.put(event.getPersonId().toString(), event.getTime());
			}
		} else {
			tmpDptTimes.put(event.getPersonId().toString(), event.getTime());
		}
	}

	@Override
	public void reset(final int iteration) {
		tmpDptTimes.clear();
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		double arrTime = event.getTime();
		String agentId = event.getPersonId().toString();
		if (toll == null) {
			internalCompute(agentId, arrTime);
		} else if (TollTools.isInRange(((PlanImpl) plans.getPersons().get(
				event.getPersonId()).getSelectedPlan()).getFirstActivity()
				.getLinkId(), toll)) {
			internalCompute(agentId, arrTime);
		}
	}

	protected void internalCompute(String agentId, double arrTime) {
		Double dptTime = tmpDptTimes.remove(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(arrTime);
			double travelTime = arrTime - dptTime;
			travelTimes[binIdx] += travelTime;
			arrCount[binIdx]++;

			Plan selectedplan = plans.getPersons().get(new IdImpl(agentId))
					.getSelectedPlan();
			String mode = PlanModeJudger.getMode(selectedplan);
			if (TransportMode.car.equals(mode)) {
				carTravelTimes[binIdx] += travelTime;
				carArrCount[binIdx]++;
			} else if (TransportMode.pt.equals(mode)) {
				ptTravelTimes[binIdx] += travelTime;
				ptArrCount[binIdx]++;
			} else if (TransportMode.walk.equals(mode)) {
				wlkTravelTimes[binIdx] += travelTime;
				wlkArrCount[binIdx]++;
			} else if (TransportMode.bike.equals(mode)) {
				bikeTravelTimes[binIdx] += travelTime;
				bikeArrCount[binIdx]++;
			} else {
				othersTravelTimes[binIdx] += travelTime;
				othersArrCount[binIdx]++;
			}
		}
	}

	protected int getBinIndex(final double time) {
		int bin = (int) (time / binSize);
		if (bin >= travelTimes.length) {
			return travelTimes.length - 1;
		}
		return bin;
	}

	public void write(final String filename) {
		SimpleWriter sw = new SimpleWriter(filename);
		sw
				.writeln("time\ttimeBin"
						+ "\tall_traveltimes [s]\tn._arrivals\tavg. traveltimes [s]"
						+ "\tcar_traveltimes [s]\tcar_n._arrivals\tcar_avg. traveltimes [s]"
						+ "\tpt_traveltimes [s]\tpt_n._arrivals\tpt_avg. traveltimes [s]"
						+ "\twalk_traveltimes [s]\twalk_n._arrivals\twalk_avg. traveltimes [s]"
						+ "\tbike_traveltimes [s]\tbike_n._arrivals\tbike_avg. traveltimes [s]"
						+ "\tothers_traveltimes [s]\tothers_n._arrivals\tothers_avg. traveltimes [s]");
		for (int i = 0; i < travelTimes.length; i++) {
			sw.writeln(Time.writeTime(i * binSize) + "\t" + i * binSize + "\t"
					+ travelTimes[i] + "\t" + arrCount[i] + "\t"
					+ travelTimes[i] / arrCount[i] + "\t" + carTravelTimes[i]
					+ "\t" + carArrCount[i] + "\t"
					+ carTravelTimes[i] / carArrCount[i] + "\t"
					+ ptTravelTimes[i] + "\t" + ptArrCount[i] + "\t"
					+ ptTravelTimes[i] / ptArrCount[i] + "\t"
					+ wlkTravelTimes[i] + "\t" + wlkArrCount[i] + "\t"
					+ wlkTravelTimes[i] / wlkArrCount[i] + "\t"
					+ bikeTravelTimes[i] + "\t" + bikeArrCount[i] + "\t"
					+ bikeTravelTimes[i] / bikeArrCount[i] + "\t"
					+ othersTravelTimes[i] + "\t" + othersArrCount[i] + "\t"
					+ othersTravelTimes[i] / othersArrCount[i]);
		}

		sw.write("----------------------------------------\n");
		sw
				.writeln("the sum of all the traveltimes [s]: "
						+ CollectionMath.getSum(travelTimes)
						+ "\n"
						+ "the number of all the Trips: "
						+ CollectionMath.getSum(arrCount)
						+ "\n"
						+ "the sum of all the drivers' traveltimes [s]: "
						+ CollectionMath.getSum(carTravelTimes)
						+ "\n"
						+ "the number of all the drivers' Trips: "
						+ CollectionMath.getSum(carArrCount)
						+ "\n"
						+ "the sum of all the public transit unsers' traveltimes [s]: "
						+ CollectionMath.getSum(ptTravelTimes)
						+ "\n"
						+ "the number of all the public users' Trips: "
						+ CollectionMath.getSum(ptArrCount)
						+ "\n"
						+ "the sum of all the walkers' traveltimes [s]: "
						+ CollectionMath.getSum(wlkTravelTimes)
						+ "\n"
						+ "the number of all the walkers' Trips: "
						+ CollectionMath.getSum(wlkArrCount)
						+ "\n"
						+ "the sum of all the cyclists' traveltimes [s]: "
						+ CollectionMath.getSum(bikeTravelTimes)
						+ "\n"
						+ "the number of all the cyclists' Trips: "
						+ CollectionMath.getSum(bikeArrCount)
						+ "\n"
						+ "the sum of all the persons with other modes traveltimes [s]: "
						+ CollectionMath.getSum(othersTravelTimes)
						+ "\n"
						+ "the number of all the persons with other modes Trips: "
						+ CollectionMath.getSum(othersArrCount));
		sw.close();
	}

	public void writeCharts(final String filename) {
		int xsLength = travelTimes.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			xs[i] = (double) i * (double) binSize / 3600.0;
		}
		XYLineChart travelTimeSumChart = new XYLineChart("TravelTimes", "time",
				"sum of TravelTimes [s]");
		travelTimeSumChart.addSeries("sum of traveltimes of drivers", xs,
				carTravelTimes);
		if (CollectionMath.getSum(ptTravelTimes) > 0) {
			travelTimeSumChart.addSeries(
					"sum of traveltime of public transit users", xs,
					ptTravelTimes);
		}
		if (CollectionMath.getSum(wlkTravelTimes) > 0) {
			travelTimeSumChart.addSeries("sum of traveltime of walkers", xs,
					wlkTravelTimes);
		}
		if (CollectionMath.getSum(bikeTravelTimes) > 0) {
			travelTimeSumChart.addSeries("sum of traveltime of cyclists", xs,
					bikeTravelTimes);
		}
		if (CollectionMath.getSum(othersTravelTimes) > 0) {
			travelTimeSumChart.addSeries(
					"sum of traveltime of other modes users", xs,
					othersTravelTimes);
		}
		travelTimeSumChart.addSeries("sum of traveltimes of all agents", xs,
				travelTimes);
		travelTimeSumChart.saveAsPng(filename + "Sum.png", 1024, 768);

		for (int j = 0; j < xsLength - 1; j++) {
			travelTimes[j] = arrCount[j] == 0 ? -1 : travelTimes[j]
					/ arrCount[j];
			carTravelTimes[j] = carArrCount[j] == 0 ? -1 : carTravelTimes[j]
					/ carArrCount[j];
			ptTravelTimes[j] = ptArrCount[j] == 0 ? -1 : ptTravelTimes[j]
					/ ptArrCount[j];
			wlkTravelTimes[j] = wlkArrCount[j] == 0 ? -1 : wlkTravelTimes[j]
					/ wlkArrCount[j];
			bikeTravelTimes[j] = bikeArrCount[j] == 0 ? -1 : bikeTravelTimes[j]
					/ bikeArrCount[j];
			othersTravelTimes[j] = othersArrCount[j] == 0 ? -1
					: othersTravelTimes[j] / othersArrCount[j];
		}
		XYLineChart avgTravelTimeChart = new XYLineChart(
				"average LegTravelTime", "time", "average TravelTimes [s]");
		avgTravelTimeChart.addSeries("average traveltime of drivers", xs,
				carTravelTimes);
		if (CollectionMath.getSum(ptArrCount) > 0) {
			avgTravelTimeChart.addSeries(
					"average traveltime of public transit Users", xs,
					ptTravelTimes);
		}
		if (CollectionMath.getSum(wlkArrCount) > 0) {
			avgTravelTimeChart.addSeries("average traveltime of walkers", xs,
					wlkTravelTimes);
		}
		if (CollectionMath.getSum(bikeArrCount) > 0) {
			avgTravelTimeChart.addSeries("average traveltime of cyclists", xs,
					bikeTravelTimes);
		}
		if (CollectionMath.getSum(othersArrCount) > 0) {
			avgTravelTimeChart.addSeries("average traveltime of others", xs,
					othersTravelTimes);
		}
		avgTravelTimeChart.addSeries("average traveltime of all agents", xs,
				travelTimes);
		avgTravelTimeChart.saveAsPng(filename + "Avg.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../runs_SVN/run684/it.1000/1000.events.txt.gz";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/analysis/legTravelTime.txt";
		String chartFilename = "../matsimTests/analysis/";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		Gbl.startMeasurement();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		scenario.getConfig().scenario().setUseRoadpricing(true);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(scenario.getRoadPricingScheme());
		tollReader.parse(tollFilename);

		EventsManager events = EventsUtils.createEventsManager();

		LegTravelTimeModalSplit lttms = new LegTravelTimeModalSplit(population
		// ,tollReader.getScheme()
		);
		events.addHandler(lttms);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		lttms.write(outputFilename);
		lttms.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
