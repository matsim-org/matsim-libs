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

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.CollectionSum;
import playground.yu.utils.TollTools;
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
		AgentArrivalEventHandler {

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

	/**
	 *
	 */
	public LegTravelTimeModalSplit(final int binSize, final int nofBins,
			final Population plans) {
		this.plans = plans;
		this.binSize = binSize;

		this.travelTimes = new double[nofBins + 1];
		this.arrCount = new int[nofBins + 1];

		this.carTravelTimes = new double[nofBins + 1];
		this.ptTravelTimes = new double[nofBins + 1];
		this.wlkTravelTimes = new double[nofBins + 1];
		bikeTravelTimes = new double[nofBins + 1];
		othersTravelTimes = new double[nofBins + 1];

		this.carArrCount = new int[nofBins + 1];
		this.ptArrCount = new int[nofBins + 1];
		this.wlkArrCount = new int[nofBins + 1];
		bikeArrCount = new int[nofBins + 1];
		othersArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit(final int binSize, final Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit(final Population plans) {
		this(300, plans);
	}

	public LegTravelTimeModalSplit(PopulationImpl ppl, RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		if (toll != null) {
			// only inhabitant from Kanton
			if (TollTools.isInRange(((PlanImpl) plans.getPersons().get(event.getPersonId())
					.getSelectedPlan()).getFirstActivity().getLinkId(), toll))
				this.tmpDptTimes.put(event.getPersonId().toString(), event
						.getTime());
		} else
			this.tmpDptTimes.put(event.getPersonId().toString(), event
					.getTime());
	}

	public void reset(final int iteration) {
		this.tmpDptTimes.clear();
	}

	public void handleEvent(final AgentArrivalEvent event) {
		double arrTime = event.getTime();
		String agentId = event.getPersonId().toString();
		if (toll == null)
			internalCompute(agentId, arrTime);
		else if (TollTools.isInRange(((PlanImpl) plans.getPersons()
				.get(event.getPersonId()).getSelectedPlan()).getFirstActivity()
				.getLinkId(), toll))
			internalCompute(agentId, arrTime);
	}

	protected void internalCompute(String agentId, double arrTime) {
		Double dptTime = this.tmpDptTimes.remove(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(arrTime);
			double travelTime = arrTime - dptTime;
			this.travelTimes[binIdx] += travelTime;
			this.arrCount[binIdx]++;

			Plan selectedplan = plans.getPersons().get(new IdImpl(agentId)).getSelectedPlan();
			TransportMode mode = PlanModeJudger.getMode(selectedplan);
			switch (mode) {
			case car:
				this.carTravelTimes[binIdx] += travelTime;
				this.carArrCount[binIdx]++;
				break;
			case pt:
				this.ptTravelTimes[binIdx] += travelTime;
				this.ptArrCount[binIdx]++;
				break;
			case walk:
				wlkTravelTimes[binIdx] += travelTime;
				wlkArrCount[binIdx]++;
				break;
			case bike:
				bikeTravelTimes[binIdx] += travelTime;
				bikeArrCount[binIdx]++;
				break;
			default:
				this.othersTravelTimes[binIdx] += travelTime;
				this.othersArrCount[binIdx]++;
				break;
			}
		}
	}

	protected int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.travelTimes.length)
			return this.travelTimes.length - 1;
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
		for (int i = 0; i < this.travelTimes.length; i++)
			sw.writeln(Time.writeTime(i * this.binSize) + "\t"
					+ i * this.binSize + "\t" + this.travelTimes[i] + "\t"
					+ this.arrCount[i] + "\t"
					+ this.travelTimes[i] / (double) this.arrCount[i] + "\t"
					+ this.carTravelTimes[i] + "\t" + this.carArrCount[i]
					+ "\t"
					+ this.carTravelTimes[i] / (double) this.carArrCount[i]
					+ "\t" + this.ptTravelTimes[i] + "\t" + this.ptArrCount[i]
					+ "\t"
					+ this.ptTravelTimes[i] / (double) this.ptArrCount[i]
					+ "\t" + this.wlkTravelTimes[i] + "\t"
					+ this.wlkArrCount[i] + "\t" + this.wlkTravelTimes[i]
					/ (double) this.wlkArrCount[i] + "\t"
					+ this.bikeTravelTimes[i] + "\t" + this.bikeArrCount[i]
					+ "\t" + this.bikeTravelTimes[i]
					/ (double) this.bikeArrCount[i] + "\t"
					+ this.othersTravelTimes[i] + "\t" + this.othersArrCount[i]
					+ "\t" + this.othersTravelTimes[i]
					/ (double) this.othersArrCount[i]);

		sw.write("----------------------------------------\n");
		sw
				.writeln("the sum of all the traveltimes [s]: "
						+ CollectionSum.getSum(travelTimes)
						+ "\n"
						+ "the number of all the Trips: "
						+ CollectionSum.getSum(arrCount)
						+ "\n"
						+ "the sum of all the drivers' traveltimes [s]: "
						+ CollectionSum.getSum(carTravelTimes)
						+ "\n"
						+ "the number of all the drivers' Trips: "
						+ CollectionSum.getSum(carArrCount)
						+ "\n"
						+ "the sum of all the public transit unsers' traveltimes [s]: "
						+ CollectionSum.getSum(ptTravelTimes)
						+ "\n"
						+ "the number of all the public users' Trips: "
						+ CollectionSum.getSum(ptArrCount)
						+ "\n"
						+ "the sum of all the walkers' traveltimes [s]: "
						+ CollectionSum.getSum(wlkTravelTimes)
						+ "\n"
						+ "the number of all the walkers' Trips: "
						+ CollectionSum.getSum(wlkArrCount)
						+ "\n"
						+ "the sum of all the cyclists' traveltimes [s]: "
						+ CollectionSum.getSum(bikeTravelTimes)
						+ "\n"
						+ "the number of all the cyclists' Trips: "
						+ CollectionSum.getSum(bikeArrCount)
						+ "\n"
						+ "the sum of all the persons with other modes traveltimes [s]: "
						+ CollectionSum.getSum(othersTravelTimes)
						+ "\n"
						+ "the number of all the persons with other modes Trips: "
						+ CollectionSum.getSum(othersArrCount));
		sw.close();
	}

	public void writeCharts(final String filename) {
		int xsLength = this.travelTimes.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++)
			xs[i] = (double) i * (double) this.binSize / 3600.0;
		XYLineChart travelTimeSumChart = new XYLineChart("TravelTimes", "time",
				"sum of TravelTimes [s]");
		travelTimeSumChart.addSeries("sum of traveltimes of drivers", xs,
				this.carTravelTimes);
		if (CollectionSum.getSum(ptTravelTimes) > 0)
			travelTimeSumChart.addSeries(
					"sum of traveltime of public transit users", xs,
					this.ptTravelTimes);
		if (CollectionSum.getSum(wlkTravelTimes) > 0)
			travelTimeSumChart.addSeries("sum of traveltime of walkers", xs,
					this.wlkTravelTimes);
		if (CollectionSum.getSum(bikeTravelTimes) > 0)
			travelTimeSumChart.addSeries("sum of traveltime of cyclists", xs,
					this.bikeTravelTimes);
		if (CollectionSum.getSum(othersTravelTimes) > 0)
			travelTimeSumChart.addSeries(
					"sum of traveltime of other modes users", xs,
					this.othersTravelTimes);
		travelTimeSumChart.addSeries("sum of traveltimes of all agents", xs,
				this.travelTimes);
		travelTimeSumChart.saveAsPng(filename + "Sum.png", 1024, 768);

		for (int j = 0; j < xsLength - 1; j++) {
			this.travelTimes[j] = this.arrCount[j] == 0 ? -1
					: this.travelTimes[j] / this.arrCount[j];
			this.carTravelTimes[j] = this.carArrCount[j] == 0 ? -1
					: this.carTravelTimes[j] / this.carArrCount[j];
			this.ptTravelTimes[j] = this.ptArrCount[j] == 0 ? -1
					: this.ptTravelTimes[j] / this.ptArrCount[j];
			this.wlkTravelTimes[j] = this.wlkArrCount[j] == 0 ? -1
					: this.wlkTravelTimes[j] / this.wlkArrCount[j];
			this.bikeTravelTimes[j] = this.bikeArrCount[j] == 0 ? -1
					: this.bikeTravelTimes[j] / this.bikeArrCount[j];
			this.othersTravelTimes[j] = this.othersArrCount[j] == 0 ? -1
					: this.othersTravelTimes[j] / this.othersArrCount[j];
		}
		XYLineChart avgTravelTimeChart = new XYLineChart(
				"average LegTravelTime", "time", "average TravelTimes [s]");
		avgTravelTimeChart.addSeries("average traveltime of drivers", xs,
				this.carTravelTimes);
		if (CollectionSum.getSum(ptArrCount) > 0)
			avgTravelTimeChart.addSeries(
					"average traveltime of public transit Users", xs,
					this.ptTravelTimes);
		if (CollectionSum.getSum(wlkArrCount) > 0)
			avgTravelTimeChart.addSeries("average traveltime of walkers", xs,
					this.wlkTravelTimes);
		if (CollectionSum.getSum(bikeArrCount) > 0)
			avgTravelTimeChart.addSeries("average traveltime of cyclists", xs,
					this.bikeTravelTimes);
		if (CollectionSum.getSum(othersArrCount) > 0)
			avgTravelTimeChart.addSeries("average traveltime of others", xs,
					this.othersTravelTimes);
		avgTravelTimeChart.addSeries("average traveltime of all agents", xs,
				this.travelTimes);
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

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1();
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EventsManagerImpl events = new EventsManagerImpl();

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
