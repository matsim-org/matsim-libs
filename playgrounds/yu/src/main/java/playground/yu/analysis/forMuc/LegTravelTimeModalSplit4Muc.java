/* *********************************************************************** *
 * project: org.matsim.*
 * LegTravelTimeModalSplit4Muc.java
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
package playground.yu.analysis.forMuc;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute average leg travel Time of Munich network and/or Munich city Region
 * 
 * @author yu
 * 
 */
public class LegTravelTimeModalSplit4Muc extends LegTravelTimeModalSplit
		implements Analysis4Muc {

	private final double[] rideTravelTimes;
	private final int[] rideArrCount;

	public LegTravelTimeModalSplit4Muc(int binSize, int nofBins,
			Population plans) {
		super(binSize, nofBins, plans);
		rideTravelTimes = new double[nofBins + 1];
		rideArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit4Muc(int binSize, Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit4Muc(Population ppl, RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	public LegTravelTimeModalSplit4Muc(Population plans) {
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
			} else if (TransportMode.ride.equals(mode)) {
				rideTravelTimes[binIdx] += travelTime;
				rideArrCount[binIdx]++;
			} else {
				othersTravelTimes[binIdx] += travelTime;
				othersArrCount[binIdx]++;
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
				+ "\tbike_traveltimes [s]\tbike_n._arrivals\tbike_avg. traveltimes [s]"
				+ "\tride_traveltimes [s]\tride_n._arrivals\tride_avg. traveltimes [s]"
				+ "\tthrough_traveltimes [s]\tthrough_n._arrivals\tthrough_avg. traveltimes [s]"
				+ "\tothers_traveltimes [s]\tothers_n._arrivals\tothers_avg. traveltimes [s]");

		for (int i = 0; i < travelTimes.length; i++) {
			sw.writeln(Time.writeTime(i * binSize) + "\t" + i * binSize + "\t"
					+ travelTimes[i] + "\t" + arrCount[i] + "\t"
					+ travelTimes[i] / arrCount[i] + "\t" + carTravelTimes[i]
					+ "\t" + carArrCount[i] + "\t"
					+ carTravelTimes[i] / carArrCount[i] + "\t"
					+ ptTravelTimes[i] + "\t" + ptArrCount[i] + "\t"
					+ ptTravelTimes[i] / ptArrCount[i] + wlkTravelTimes[i]
					+ "\t" + wlkArrCount[i] + "\t" + wlkTravelTimes[i]
					/ wlkArrCount[i] + bikeTravelTimes[i] + "\t"
					+ bikeArrCount[i] + "\t" + bikeTravelTimes[i]
					/ bikeArrCount[i] + rideTravelTimes[i] + "\t"
					+ rideArrCount[i] + "\t" + rideTravelTimes[i]
					/ rideArrCount[i] + othersTravelTimes[i] + "\t"
					+ othersArrCount[i] + "\t" + othersTravelTimes[i]
					/ othersArrCount[i]);
		}
		sw.write("----------------------------------------\n");
		double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0, wlkTtSum = 0.0, bikeTtSum = 0.0, rideTtSum = 0.0, othersTtSum = 0.0;
		int nTrips = 0, nCarTrips = 0, nPtTrips = 0, nWlkTrips = 0, nBikeTrips = 0, nRideTrips = 0, nOthersTrips = 0;
		for (int i = 0; i < travelTimes.length; i++) {
			ttSum += travelTimes[i];
			carTtSum += carTravelTimes[i];
			ptTtSum += ptTravelTimes[i];
			wlkTtSum += wlkTravelTimes[i];
			bikeTtSum += bikeTravelTimes[i];
			rideTtSum += rideTravelTimes[i];
			othersTtSum += othersTravelTimes[i];

			nTrips += arrCount[i];
			nCarTrips += carArrCount[i];
			nPtTrips += ptArrCount[i];
			nWlkTrips += wlkArrCount[i];
			nBikeTrips += bikeArrCount[i];
			nRideTrips += rideArrCount[i];
			nOthersTrips += othersArrCount[i];
		}
		sw.writeln("the sum of all the traveltimes [s]: " + ttSum
				+ "\nthe number of all the Trips: " + nTrips
				+ "\nthe sum of all the drivers traveltimes [s]: " + carTtSum
				+ "\nthe number of all the drivers Trips: " + nCarTrips
				+ "\nhe sum of all the public transit unsers traveltimes [s]: "
				+ ptTtSum + "\nthe number of all the public users Trips: "
				+ nPtTrips + "\nthe sum of all the walkers traveltimes [s]: "
				+ wlkTtSum + "\nthe number of all the walkers Trips: "
				+ nWlkTrips

				+ "\nthe sum of all the cyclists traveltimes [s]: " + bikeTtSum
				+ "\nthe number of all the cyclists traffic Trips: "
				+ nBikeTrips

				+ "\nthe sum of all the ride traveltimes [s]: " + rideTtSum
				+ "\nthe number of all the ride traffic Trips: " + nRideTrips

				+ "\nthe sum of all the other traffic traveltimes [s]: "
				+ othersTtSum + "\nthe number of all the other traffic Trips: "
				+ nOthersTrips);
		sw.close();
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../matsimTests/changeLegModeTests/500.events.txt.gz";
		final String plansFilename = "../matsimTests/changeLegModeTests/500.plans.xml.gz";
		String outputFilename = "../matsimTests/changeLegModeTests/500.legTravelTime.txt";
		String chartFilename = "../matsimTests/changeLegModeTests/";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml";

		Gbl.startMeasurement();
		// Gbl.createConfig(null);

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingSchemeImpl toll = (RoadPricingSchemeImpl) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(toll);
		tollReader.parse(tollFilename);

		EventsManager events = EventsUtils.createEventsManager();

		LegTravelTimeModalSplit4Muc lttms = new LegTravelTimeModalSplit4Muc(
				population, toll);
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
