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

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author ychen
 * 
 */
public class LegTravelTimeModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	// private final NetworkLayer network;

	private final Population plans;
	private RoadPricingScheme toll = null;

	private final int binSize;

	private final double[] travelTimes, carTravelTimes, ptTravelTimes,
			wlkTravelTimes;

	private final int[] arrCount, carArrCount, ptArrCount, wlkArrCount;

	/**
	 * @param arg0
	 *            - String agentId
	 * @param arg1
	 *            - Double departure time
	 */
	private final HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();
	private double[] otherTravelTimes;
	private int[] otherArrCount;

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
		otherTravelTimes = new double[nofBins + 1];

		this.carArrCount = new int[nofBins + 1];
		this.ptArrCount = new int[nofBins + 1];
		this.wlkArrCount = new int[nofBins + 1];
		otherArrCount = new int[nofBins + 1];
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

	public void handleEvent(final AgentDepartureEvent event) {
		if (toll != null) {
			// only inhabitant from Kanton
			if (TollTools.isInRange(plans.getPersons().get(event.getPersonId())
					.getSelectedPlan().getFirstActivity().getLink(), toll))
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
		else if (TollTools.isInRange(plans.getPersons()
				.get(event.getPersonId()).getSelectedPlan().getFirstActivity()
				.getLink(), toll))
			internalCompute(agentId, arrTime);
	}

	private void internalCompute(String agentId, double arrTime) {
		Double dptTime = this.tmpDptTimes.remove(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(arrTime);
			double travelTime = arrTime - dptTime;
			this.travelTimes[binIdx] += travelTime;
			this.arrCount[binIdx]++;

			Plan selectedplan = plans.getPersons().get(new IdImpl(agentId))
					.getSelectedPlan();
			if (Integer.parseInt(agentId) < 1000000000) {
				if (PlanModeJudger.useCar(selectedplan)) {
					this.carTravelTimes[binIdx] += travelTime;
					this.carArrCount[binIdx]++;
				} else if (PlanModeJudger.usePt(selectedplan)) {
					this.ptTravelTimes[binIdx] += travelTime;
					this.ptArrCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(selectedplan)) {
					wlkTravelTimes[binIdx] += travelTime;
					wlkArrCount[binIdx]++;
				}
			} else {
				this.otherTravelTimes[binIdx] += travelTime;
				this.otherArrCount[binIdx]++;
			}
		}
	}

	private int getBinIndex(final double time) {
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
						+ "\tother_traveltimes [s]\tother_n._arrivals\tother_avg. traveltimes [s]");
		for (int i = 0; i < this.travelTimes.length; i++)
			sw
					.writeln(Time.writeTime(i * this.binSize)
							+ "\t"
							+ i * this.binSize
							+ "\t"
							+ this.travelTimes[i]
							+ "\t"
							+ this.arrCount[i]
							+ "\t"
							+ this.travelTimes[i] / (double) this.arrCount[i]
							+ "\t"
							+ this.carTravelTimes[i]
							+ "\t"
							+ this.carArrCount[i]
							+ "\t"
							+ this.carTravelTimes[i]
									/ (double) this.carArrCount[i]
							+ "\t"
							+ this.ptTravelTimes[i]
							+ "\t"
							+ this.ptArrCount[i]
							+ "\t"
							+ this.ptTravelTimes[i]
									/ (double) this.ptArrCount[i]
							+ this.wlkTravelTimes[i] + "\t"
							+ this.wlkArrCount[i] + "\t"
							+ this.wlkTravelTimes[i]
							/ (double) this.wlkArrCount[i]
							+ this.otherTravelTimes[i] + "\t"
							+ this.otherArrCount[i] + "\t"
							+ this.otherTravelTimes[i]
							/ (double) this.otherArrCount[i]);
		sw.write("----------------------------------------\n");
		double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0, wlkTtSum = 0.0, otherTtSum = 0.0;
		int nTrips = 0, nCarTrips = 0, nPtTrips = 0, nWlkTrips = 0, nOtherTrips = 0;
		for (int i = 0; i < this.travelTimes.length; i++) {
			ttSum += this.travelTimes[i];
			carTtSum += this.carTravelTimes[i];
			ptTtSum += this.ptTravelTimes[i];
			wlkTtSum += this.wlkTravelTimes[i];
			otherTtSum += otherTravelTimes[i];

			nTrips += this.arrCount[i];
			nCarTrips += this.carArrCount[i];
			nPtTrips += this.ptArrCount[i];
			nWlkTrips += wlkArrCount[i];
			nOtherTrips += otherArrCount[i];
		}
		sw
				.writeln("the sum of all the traveltimes [s]: "
						+ ttSum
						+ "\n"
						+ "the number of all the Trips: "
						+ nTrips
						+ "\n"
						+ "the sum of all the drivers traveltimes [s]: "
						+ carTtSum
						+ "\n"
						+ "the number of all the drivers Trips: "
						+ nCarTrips
						+ "\n"
						+ "the sum of all the public transit unsers traveltimes [s]: "
						+ ptTtSum
						+ "\n"
						+ "the number of all the public users Trips: "
						+ nPtTrips
						+ "\n"
						+ "the sum of all the walkers traveltimes [s]: "
						+ wlkTtSum
						+ "\n"
						+ "the number of all the walkers Trips: "
						+ nWlkTrips
						+ "\n"
						+ "the sum of all the through-traffic traveltimes [s]: "
						+ otherTtSum + "\n"
						+ "the number of all the through-traffic Trips: "
						+ nOtherTrips);
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
		travelTimeSumChart.addSeries(
				"sum of traveltime of public transit users", xs,
				this.ptTravelTimes);
		travelTimeSumChart.addSeries("sum of traveltime of walkers", xs,
				this.wlkTravelTimes);
		travelTimeSumChart.addSeries("sum of traveltime of others", xs,
				this.otherTravelTimes);
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
			this.otherTravelTimes[j] = this.otherArrCount[j] == 0 ? -1
					: this.otherTravelTimes[j] / this.otherArrCount[j];
		}
		XYLineChart avgTravelTimeChart = new XYLineChart(
				"average LegTravelTime", "time", "average TravelTimes [s]");
		avgTravelTimeChart.addSeries("average traveltime of drivers", xs,
				this.carTravelTimes);
		avgTravelTimeChart.addSeries(
				"average traveltime of public transit Users", xs,
				this.ptTravelTimes);
		avgTravelTimeChart.addSeries("average traveltime of walkers", xs,
				this.wlkTravelTimes);
		avgTravelTimeChart.addSeries("average traveltime of others", xs,
				this.otherTravelTimes);
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
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(network);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Events events = new Events();

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
