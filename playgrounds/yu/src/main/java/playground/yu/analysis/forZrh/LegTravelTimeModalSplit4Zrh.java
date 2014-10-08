/* *********************************************************************** *
 * project: org.matsim.*
 * LegTravelTimeModalSplit4Zrh.java
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
package playground.yu.analysis.forZrh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute average leg travel Time of Zurich and Kanton Zurich respectively with
 * through traffic
 * 
 * @author yu
 * 
 */
public class LegTravelTimeModalSplit4Zrh extends LegTravelTimeModalSplit {

	private final double[] throughTravelTimes;
	private final int[] throughArrCount;

	public LegTravelTimeModalSplit4Zrh(int binSize, int nofBins,
			Population plans) {
		super(binSize, nofBins, plans);
		throughTravelTimes = new double[nofBins + 1];
		throughArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit4Zrh(int binSize, Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit4Zrh(Population ppl, RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	public LegTravelTimeModalSplit4Zrh(Population plans) {
		this(300, plans);
	}

	@Override
	protected void internalCompute(String agentId, double arrTime) {
		Double dptTime = tmpDptTimes.remove(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(arrTime);
			double travelTime = arrTime - dptTime;
			travelTimes[binIdx] += travelTime;
			arrCount[binIdx]++;

			Plan selectedplan = plans.getPersons().get(Id.create(agentId, Person.class))
					.getSelectedPlan();
			if (Integer.parseInt(agentId) < 1000000000) {
				if (PlanModeJudger.useCar(selectedplan)) {
					carTravelTimes[binIdx] += travelTime;
					carArrCount[binIdx]++;
				} else if (PlanModeJudger.usePt(selectedplan)) {
					ptTravelTimes[binIdx] += travelTime;
					ptArrCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(selectedplan)) {
					wlkTravelTimes[binIdx] += travelTime;
					wlkArrCount[binIdx]++;
				}
			} else {
				throughTravelTimes[binIdx] += travelTime;
				throughArrCount[binIdx]++;
			}
		}
	}

	@Override
	public void write(final String filename) {
		SimpleWriter sw = new SimpleWriter(filename);
		sw.writeln("time\ttimeBin"
				+ "\tall_traveltimes [s]\tn._arrivals\tavg. traveltimes [s]"
				+ "\tcar_traveltimes [s]\tcar_n._arrivals\tcar_avg. traveltimes [s]"
				+ "\tpt_traveltimes [s]\tpt_n._arrivals\tpt_avg. traveltimes [s]"
				+ "\twalk_traveltimes [s]\twalk_n._arrivals\twalk_avg. traveltimes [s]"
				+ "\tthrough_traveltimes [s]\tthrough_n._arrivals\tthrough_avg. traveltimes [s]");
		for (int i = 0; i < travelTimes.length; i++) {
			sw.writeln(Time.writeTime(i * binSize) + "\t" + i * binSize + "\t"
					+ travelTimes[i] + "\t" + arrCount[i] + "\t"
					+ travelTimes[i] / arrCount[i] + "\t" + carTravelTimes[i]
					+ "\t" + carArrCount[i] + "\t"
					+ carTravelTimes[i] / carArrCount[i] + "\t"
					+ ptTravelTimes[i] + "\t" + ptArrCount[i] + "\t"
					+ ptTravelTimes[i] / ptArrCount[i] + wlkTravelTimes[i]
					+ "\t" + wlkArrCount[i] + "\t" + wlkTravelTimes[i]
					/ wlkArrCount[i] + throughTravelTimes[i] + "\t"
					+ throughArrCount[i] + "\t" + throughTravelTimes[i]
					/ throughArrCount[i]);
		}
		sw.write("----------------------------------------\n");
		double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0, wlkTtSum = 0.0, otherTtSum = 0.0;
		int nTrips = 0, nCarTrips = 0, nPtTrips = 0, nWlkTrips = 0, nOtherTrips = 0;
		for (int i = 0; i < travelTimes.length; i++) {
			ttSum += travelTimes[i];
			carTtSum += carTravelTimes[i];
			ptTtSum += ptTravelTimes[i];
			wlkTtSum += wlkTravelTimes[i];
			otherTtSum += throughTravelTimes[i];

			nTrips += arrCount[i];
			nCarTrips += carArrCount[i];
			nPtTrips += ptArrCount[i];
			nWlkTrips += wlkArrCount[i];
			nOtherTrips += throughArrCount[i];
		}
		sw.writeln("the sum of all the traveltimes [s]: " + ttSum + "\n"
				+ "the number of all the Trips: " + nTrips + "\n"
				+ "the sum of all the drivers traveltimes [s]: " + carTtSum
				+ "\n" + "the number of all the drivers Trips: " + nCarTrips
				+ "\n"
				+ "the sum of all the public transit unsers traveltimes [s]: "
				+ ptTtSum + "\n" + "the number of all the public users Trips: "
				+ nPtTrips + "\n"
				+ "the sum of all the walkers traveltimes [s]: " + wlkTtSum
				+ "\n" + "the number of all the walkers Trips: " + nWlkTrips
				+ "\n" + "the sum of all the through-traffic traveltimes [s]: "
				+ otherTtSum + "\n"
				+ "the number of all the through-traffic Trips: " + nOtherTrips);
		sw.close();
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../matsimTests/changeLegModeTests/500.events.txt.gz";
		final String plansFilename = "../matsimTests/changeLegModeTests/500.plans.xml.gz";
		String outputFilename = "../matsimTests/changeLegModeTests/500.legTravelTime.txt";
		String chartFilename = "../matsimTests/changeLegModeTests/";
		// String tollFilename =
		// "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml";

		Gbl.startMeasurement();
		// Gbl.createConfig(null);

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
//		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		// RoadPricingReaderXMLv1 tollReader = new
		// RoadPricingReaderXMLv1(network);
		// try {
		// tollReader.parse(tollFilename);
		// } catch (SAXException e) {
		// e.printStackTrace();
		// } catch (ParserConfigurationException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		EventsManager events = EventsUtils.createEventsManager();

		LegTravelTimeModalSplit lttms = new LegTravelTimeModalSplit(population
		// ,tollReader.getScheme()
				, null);
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
