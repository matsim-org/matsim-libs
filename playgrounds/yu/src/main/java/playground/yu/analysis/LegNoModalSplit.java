/* *********************************************************************** *
 * project: org.matsim.*
 * LegNoModalSplit.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.container.CollectionSum;
import playground.yu.utils.io.SimpleWriter;

/**
 * extract average number of {@code Leg}s in population
 * 
 * @author yu
 * 
 */
public class LegNoModalSplit extends AbstractPersonAlgorithm implements
		PlanAlgorithm, LegAlgorithm {
	/**
	 * distingwishes person in through traffic (transit verkehr) by "tta"
	 * activity
	 * 
	 * @param person
	 * @return
	 */
	public static boolean isThroughTraffic(Person person) {
		List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
		for (int i = 0; i < pes.size(); i += 2) {
			Activity act = (Activity) pes.get(i);
			if (act.getType().equals("tta")) {
				return true;
			}
		}
		return false;

	}

	/**
	 * ascertains if the person lives in toll ambit
	 * 
	 * @param person
	 * @param toll
	 * @return
	 */
	public static boolean homeInTollRange(Person person, RoadPricingScheme toll) {
		List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
		for (int i = 0; i < pes.size(); i += 2) {
			Activity act = (Activity) pes.get(i);
			if ((act.getType().startsWith("h") || act.getType().startsWith("H"))
					&& TollTools.isInRange(act.getLinkId(), toll))/*
																 * home in toll
																 * region
																 */{
				return true;
			}
		}
		return false;

	}

	private RoadPricingScheme toll;

	private int personCnt = 0;
	private Map<String, Integer> modeCnts = new HashMap<String, Integer>();

	public LegNoModalSplit(RoadPricingScheme toll) {
		this.toll = toll;
	}

	@Override
	public void run(Person person) {
		if (!isThroughTraffic(person)) {
			if (this.toll != null) {
				if (homeInTollRange(person, toll)) {
					run(person.getSelectedPlan());
					personCnt++;
				}
			} else/* null toll */{
				run(person.getSelectedPlan());
				personCnt++;
			}
		}
	}

	@Override
	public void run(Plan plan) {
		List<PlanElement> pes = plan.getPlanElements();
		for (int i = 1; i < pes.size(); i += 2) {
			run((LegImpl) pes.get(i));
		}
	}

	@Override
	public void run(LegImpl leg) {
		String mode = leg.getMode();
		Integer cnt = this.modeCnts.get(mode);
		if (cnt == null) {
			this.modeCnts.put(mode, 1);
		} else/* not the first time */{
			this.modeCnts.put(mode, cnt + 1);
		}
	}

	public void output(String outputFilenameBase) {
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "txt");

		writer.writeln("mode\tavg. number of Legs with this mode");
		for (Entry<String, Integer> modeCntEntry : this.modeCnts.entrySet()) {
			writer.writeln(modeCntEntry.getKey()/* mode */+ "\t"
					+ modeCntEntry.getValue().doubleValue() / this.personCnt);
		}
		Collection<Integer> modeCntSet = this.modeCnts.values();
		writer.writeln("No. of person:\t" + this.personCnt + "\tNo. of Legs:\t"
				+ CollectionSum.getSum(modeCntSet));

		writer.close();

		PieChart chart = new PieChart(
				"Modal split (Kanton Zurich) - Number of Legs");
		chart.addSeries(Collection2Array.toArray(this.modeCnts.keySet()),
				Collection2Array.toDoubleArray(modeCntSet));
		chart.saveAsPng(outputFilenameBase + "png", 1024, 768);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run699/ITERS/it.1000/699.1000.plans.xml.gz";
		final String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";
		final String outputFilenameBase = "../runs-svn/run699/ITERS/it.1000/699.1000.LegNoModalSplit.";
		// final String netFilename = "../matsimTests/scoringTest/network.xml";
		// final String plansFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/100.plans.xml.gz";
		// final String textFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/mode.txt";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		scenario.getConfig().scenario().setUseRoadpricing(true);

		RoadPricingScheme tollScheme = scenario.getRoadPricingScheme();
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(
				tollScheme);
		tollReader.parse(tollFilename);

		LegNoModalSplit ms = new LegNoModalSplit(tollScheme);
		ms.run(population);
		ms.output(outputFilenameBase);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
