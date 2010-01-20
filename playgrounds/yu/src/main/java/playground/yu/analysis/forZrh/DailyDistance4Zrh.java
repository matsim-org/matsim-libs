/* *********************************************************************** *
 * project: org.matsim.*
 * DailyDistance4Zrh.java
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
package playground.yu.analysis.forZrh;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.DailyDistance;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute daily distance of Zurich and Kanton Zurich respectively with through
 * traffic
 * 
 * @author yu
 * 
 */
public class DailyDistance4Zrh extends DailyDistance implements Analysis4Zrh {
	private static final String CAR = "car", OTHERS = "others",
			THROUGH = "through";
	private double throughWorkDist, throughEducDist, throughShopDist,
			throughLeisDist, throughHomeDist, throughOtherDist, throughDist;
	private double[] throughLegDistanceCounts;
	private double[] throughDayDistanceCounts;

	public DailyDistance4Zrh(final Network network) {
		super(network);
		throughWorkDist = 0.0;
		throughEducDist = 0.0;
		throughShopDist = 0.0;
		throughLeisDist = 0.0;
		throughHomeDist = 0.0;
		throughOtherDist = 0.0;
	}

	public DailyDistance4Zrh(final RoadPricingScheme toll, final Network network) {
		this(network);
		throughLegDistanceCounts = new double[101];
		throughDayDistanceCounts = new double[101];
		this.toll = toll;
	}

	@Override
	public void run(final Plan plan) {
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double wlkDayDist = 0.0;
		double othersDayDist = 0.0;
		double throughDayDist = 0.0;

		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {
				LegImpl bl = (LegImpl) pe;
				ActType ats = null;
				String tmpActType = ((PlanImpl) plan).getNextActivity(bl).getType();
				if (tmpActType.startsWith("h"))
					ats = ActType.home;
				else if (tmpActType.startsWith("w"))
					ats = ActType.work;
				else if (tmpActType.startsWith("e"))
					ats = ActType.education;
				else if (tmpActType.startsWith("s"))
					ats = ActType.shopping;
				else if (tmpActType.startsWith("l"))
					ats = ActType.leisure;
				else
					ats = ActType.others;
				double dist = bl.getRoute().getDistance() / 1000.0;
				// if (bl.getDepartureTime() < 86400)

				if (Long.parseLong(person.getId().toString()) > 1000000000) {
					throughDist += dist;
					throughDayDist += dist;
					switch (ats) {
					case home:
						throughHomeDist += dist;
						break;
					case work:
						throughWorkDist += dist;
						break;
					case education:
						throughEducDist += dist;
						break;
					case shopping:
						throughShopDist += dist;
						break;
					case leisure:
						throughLeisDist += dist;
						break;
					default:
						throughOtherDist += dist;
						break;
					}
					throughLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (bl.getMode().equals(TransportMode.car)) {
					carDist += dist;
					carDayDist += dist;
					switch (ats) {
					case home:
						carHomeDist += dist;
						break;
					case work:
						carWorkDist += dist;
						break;
					case education:
						carEducDist += dist;
						break;
					case shopping:
						carShopDist += dist;
						break;
					case leisure:
						carLeisDist += dist;
						break;
					default:
						carOtherDist += dist;
						break;
					}
					carLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (bl.getMode().equals(TransportMode.pt)) {
					ptDist += dist;
					ptDayDist += dist;
					switch (ats) {
					case home:
						ptHomeDist += dist;
						break;
					case work:
						ptWorkDist += dist;
						break;
					case education:
						ptEducDist += dist;
						break;
					case shopping:
						ptShopDist += dist;
						break;
					case leisure:
						ptLeisDist += dist;
						break;
					default:
						ptOtherDist += dist;
						break;
					}
					ptLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (bl.getMode().equals(TransportMode.walk)) {
					dist = CoordUtils.calcDistance(this.network.getLinks().get(((PlanImpl) plan).getPreviousActivity(bl)
							.getLinkId()).getCoord(), this.network.getLinks().get(((PlanImpl) plan).getNextActivity(bl)
							.getLinkId()).getCoord()) * 1.5 / 1000.0;
					wlkDist += dist;
					wlkDayDist += dist;
					switch (ats) {
					case home:
						wlkHomeDist += dist;
						break;
					case work:
						wlkWorkDist += dist;
						break;
					case education:
						wlkEducDist += dist;
						break;
					case shopping:
						wlkShopDist += dist;
						break;
					case leisure:
						wlkLeisDist += dist;
						break;
					default:
						wlkOtherDist += dist;
						break;
					}
					wlkLegDistanceCounts[Math.min(100, (int) dist)]++;

				} else {
					othersDist += dist;
					othersDayDist += dist;
					switch (ats) {
					case home:
						othersHomeDist += dist;
						break;
					case work:
						othersWorkDist += dist;
						break;
					case education:
						othersEducDist += dist;
						break;
					case shopping:
						othersShopDist += dist;
						break;
					case leisure:
						othersLeisDist += dist;
						break;
					default:
						othersOtherDist += dist;
						break;
					}
					othersLegDistanceCounts[Math.min(100, (int) dist)]++;
				}
				dayDist += dist;
			}
		for (int i = 0; i <= Math.min(100, (int) dayDist); i++)
			totalDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) othersDayDist); i++)
			othersDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayDist); i++)
			carDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayDist); i++)
			ptDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayDist); i++)
			wlkDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) throughDayDist); i++)
			throughDayDistanceCounts[i]++;
	}

	@Override
	public void write(final String outputFilename) {
		double sum = carDist + ptDist + wlkDist + othersDist + throughDist;

		SimpleWriter sw = new SimpleWriter(outputFilename + "dailyDistance.txt");
		sw.writeln("\tDaily Distance\t(exkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tavg.[km]\t%\tsum.[km]");

		double avgCarDist = carDist / count;
		double avgPtDist = ptDist / count;
		double avgWlkDist = wlkDist / count;
		double avgOthersDist = othersDist / count;
		double avgThroughDist = throughDist / count;

		sw.writeln("car\t" + avgCarDist + "\t" + carDist / sum * 100.0 + "\t"
				+ carDist);
		sw.writeln("pt\t" + avgPtDist + "\t" + ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + wlkDist / sum * 100.0 + "\t"
				+ wlkDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);
		sw.writeln("through\t" + avgThroughDist + "\t" + throughDist / sum
				* 100.0 + "\t" + throughDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart.addSeries(new String[] { CAR, "pt", "wlk", OTHERS, THROUGH },
				new double[] { avgCarDist, avgPtDist, avgWlkDist,
						avgOthersDist, avgThroughDist });
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily Distance\t(inkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tkm\t%\tsum.[km]");
		sw.writeln("car\t" + (avgCarDist + avgOthersDist) + "\t"
				+ (carDist + throughDist) / sum * 100.0 + "\t"
				+ (carDist + throughDist));
		sw.writeln("pt\t" + avgPtDist + "\t" + ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + wlkDist / sum * 100.0 + "\t"
				+ wlkDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);
		sw.writeln("----------------------------------------------");

		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother");
		sw.writeln("car\t" + carWorkDist + "\t" + carEducDist + "\t"
				+ carShopDist + "\t" + carLeisDist + "\t" + carHomeDist + "\t"
				+ carOtherDist);
		sw.writeln("pt\t" + ptWorkDist + "\t" + ptEducDist + "\t" + ptShopDist
				+ "\t" + ptLeisDist + "\t" + ptHomeDist + "\t" + ptOtherDist);
		sw.writeln("walk\t" + wlkWorkDist + "\t" + wlkEducDist + "\t"
				+ wlkShopDist + "\t" + wlkLeisDist + "\t" + wlkHomeDist + "\t"
				+ wlkOtherDist);
		sw.writeln("others\t" + othersWorkDist + "\t" + othersEducDist + "\t"
				+ othersShopDist + "\t" + othersLeisDist + "\t"
				+ othersHomeDist + "\t" + othersOtherDist);
		sw.writeln("through\t" + throughWorkDist + "\t" + throughEducDist
				+ "\t" + throughShopDist + "\t" + throughLeisDist + "\t"
				+ throughHomeDist + "\t" + throughOtherDist);
		sw
				.writeln("total\t"
						+ (carWorkDist + ptWorkDist + wlkWorkDist
								+ othersWorkDist + throughWorkDist)
						+ "\t"
						+ (carEducDist + ptEducDist + wlkEducDist
								+ othersEducDist + throughEducDist)
						+ "\t"
						+ (carShopDist + ptShopDist + wlkShopDist
								+ othersShopDist + throughShopDist)
						+ "\t"
						+ (carLeisDist + ptLeisDist + wlkLeisDist
								+ othersLeisDist + throughLeisDist)
						+ "\t"
						+ (carHomeDist + ptHomeDist + wlkHomeDist
								+ othersHomeDist + throughHomeDist)
						+ "\t"
						+ (carOtherDist + ptOtherDist + wlkOtherDist
								+ othersOtherDist + throughOtherDist));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily distance",
				"travel destination", "daily distance [km]", new String[] {
						"work", "education", "shopping", "leisure", "home",
						OTHERS });
		barChart.addSeries(CAR, new double[] { carWorkDist, carEducDist,
				carShopDist, carLeisDist, carHomeDist, carOtherDist });
		barChart.addSeries("pt", new double[] { ptWorkDist, ptEducDist,
				ptShopDist, ptLeisDist, ptHomeDist, ptOtherDist });
		barChart.addSeries("walk", new double[] { wlkWorkDist, wlkEducDist,
				wlkShopDist, wlkLeisDist, wlkHomeDist, wlkOtherDist });
		barChart.addSeries(OTHERS, new double[] { othersWorkDist,
				othersEducDist, othersShopDist, othersLeisDist, othersHomeDist,
				othersOtherDist });
		barChart.addSeries(THROUGH, new double[] { throughWorkDist,
				throughEducDist, throughShopDist, throughLeisDist,
				throughHomeDist, throughOtherDist });
		barChart.addMatsimLogo();
		barChart.saveAsPng(outputFilename
				+ "dailyDistanceTravelDistination.png", 1200, 900);

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101];
		double yCar[] = new double[101];
		double yPt[] = new double[101];
		double yWlk[] = new double[101];
		double yOthers[] = new double[101];
		double yThrough[] = new double[101];

		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayDistanceCounts[i] / count * 100.0;
			yCar[i] = carDayDistanceCounts[i] / count * 100.0;
			yPt[i] = ptDayDistanceCounts[i] / count * 100.0;
			yWlk[i] = wlkDayDistanceCounts[i] / count * 100.0;
			yOthers[i] = othersDayDistanceCounts[i] / count * 100.0;
			yThrough[i] = throughDayDistanceCounts[i] / count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries(CAR, x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries("pt", x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries("walk", x, yWlk);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries(OTHERS, x, yOthers);
		if (CollectionSum.getSum(yThrough) > 0)
			chart.addSeries(THROUGH, x, yThrough);
		chart.addSeries("total", x, yTotal);
		chart.saveAsPng(outputFilename + "dailyDistance.png", 800, 600);

		sw
				.writeln("-------------------------------------------------------------");
		sw.writeln("--Modal split -- leg distance--");
		sw
				.writeln("leg Distance [km]\tcar legs no.\tpt legs no.\twalk legs no.\tothers legs no.\tthrough legs no."
						+ "car fraction [%]\tpt fraction [%]\twalk fraction [%]\tothers fraction [%]\tthrough fraction [%]");

		double xs[] = new double[101];
		double yCarFracs[] = new double[101];
		double yPtFracs[] = new double[101];
		double yWlkFracs[] = new double[101];
		double yOthersFracs[] = new double[101];
		double yThroughFracs[] = new double[101];
		for (int i = 0; i < 101; i++) {
			xs[i] = i;
			double sumLegDistanceCounts = ptLegDistanceCounts[i]
					+ carLegDistanceCounts[i] + wlkLegDistanceCounts[i]
					+ othersLegDistanceCounts[i] + throughLegDistanceCounts[i];
			if (sumLegDistanceCounts > 0) {
				yCarFracs[i] = carLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yPtFracs[i] = ptLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yWlkFracs[i] = wlkLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yOthersFracs[i] = othersLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
				yThroughFracs[i] = throughLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
			} else {
				yCarFracs[i] = 0;
				yPtFracs[i] = 0;
				yWlkFracs[i] = 0;
				yOthersFracs[i] = 0;
				yThroughFracs[i] = 0;
			}
			sw.writeln(i + "+\t" + carLegDistanceCounts[i] + "\t"
					+ ptLegDistanceCounts[i] + "\t" + wlkLegDistanceCounts[i]
					+ "\t" + othersLegDistanceCounts[i] + "\t"
					+ throughLegDistanceCounts[i] + "\t" + yCarFracs[i] + "\t"
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t"
					+ yOthersFracs[i] + "\t" + yThroughFracs[i]);
		}

		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Distance",
				"leg Distance [km]", "mode fraction [%]");
		chart2.addSeries(CAR, xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries("pt", xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries("walk", xs, yWlkFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries(OTHERS, xs, yOthersFracs);
		if (CollectionSum.getSum(yThroughFracs) > 0)
			chart2.addSeries(THROUGH, xs, yThroughFracs);
		chart2.saveAsPng(outputFilename + "legDistanceModalSplit2.png", 800,
				600);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run684/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/run684/dailyDistance/";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

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

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyDistance4Zrh dd = new DailyDistance4Zrh(tollReader.getScheme(), network);
		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
