/* *********************************************************************** *
 * project: org.matsim.*
 * DailyEnRouteTime4Zrh.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.DailyEnRouteTime;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.container.CollectionSum;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute daily En Route Time of Zurich and Kanton Zurich respectively with
 * through traffic
 *
 * @author yu
 *
 */
public class DailyEnRouteTime4Zrh extends DailyEnRouteTime implements
		Analysis4Zrh {
	private double throughWorkTime, throughEducTime, throughShopTime,
			throughLeisTime, throughHomeTime, throughOtherTime;

	public DailyEnRouteTime4Zrh() {
		throughWorkTime = 0.0;
		throughEducTime = 0.0;
		throughShopTime = 0.0;
		throughLeisTime = 0.0;
		throughHomeTime = 0.0;
		throughOtherTime = 0.0;
	}

	public DailyEnRouteTime4Zrh(final RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	@Override
	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double otherDayTime = 0.0;
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof Leg) {
				Leg bl = (Leg) pe;
				ActTypeZrh ats = null;
				String tmpActType = ((PlanImpl) plan).getNextActivity(bl).getType();
				if (tmpActType.startsWith("h"))
					ats = ActTypeZrh.home;
				else if (tmpActType.startsWith("w"))
					ats = ActTypeZrh.work;
				else if (tmpActType.startsWith("e"))
					ats = ActTypeZrh.education;
				else if (tmpActType.startsWith("s"))
					ats = ActTypeZrh.shopping;
				else if (tmpActType.startsWith("l"))
					ats = ActTypeZrh.leisure;
				else
					ats = ActTypeZrh.others;
				double time = bl.getTravelTime() / 60.0;
				if (time < 0)
					time = 0;
				if (bl.getDepartureTime() < 86400) {
					dayTime += time;
					if (Long.parseLong(person.getId().toString()) > 1000000000) {
						othersTime += time;
						otherDayTime += time;
						switch (ats) {
						case home:
							throughHomeTime += time;
							break;
						case work:
							throughWorkTime += time;
							break;
						case education:
							throughEducTime += time;
							break;
						case shopping:
							throughShopTime += time;
							break;
						case leisure:
							throughLeisTime += time;
							break;
						default:
							throughOtherTime += time;
							break;
						}
					} else if (bl.getMode().equals(TransportMode.car)) {
						carTime += time;
						carDayTime += time;
						switch (ats) {
						case home:
							carHomeTime += time;
							break;
						case work:
							carWorkTime += time;
							break;
						case education:
							carEducTime += time;
							break;
						case shopping:
							carShopTime += time;
							break;
						case leisure:
							carLeisTime += time;
							break;
						default:
							carOtherTime += time;
							break;
						}
						carLegTimeCounts[Math.min(100, (int) time / 2)]++;
					} else if (bl.getMode().equals(TransportMode.pt)) {
						ptTime += time;
						ptDayTime += time;
						switch (ats) {
						case home:
							ptHomeTime += time;
							break;
						case work:
							ptWorkTime += time;
							break;
						case education:
							ptEducTime += time;
							break;
						case shopping:
							ptShopTime += time;
							break;
						case leisure:
							ptLeisTime += time;
							break;
						default:
							ptOtherTime += time;
							break;
						}
						ptLegTimeCounts[Math.min(100, (int) time / 2)]++;
					} else if (bl.getMode().equals(TransportMode.walk)) {
						wlkTime += time;
						wlkDayTime += time;
						switch (ats) {
						case home:
							wlkHomeTime += time;
							break;
						case work:
							wlkWorkTime += time;
							break;
						case education:
							wlkEducTime += time;
							break;
						case shopping:
							wlkShopTime += time;
							break;
						case leisure:
							wlkLeisTime += time;
							break;
						default:
							wlkOtherTime += time;
							break;
						}
						wlkLegTimeCounts[Math.min(100, (int) time / 2)]++;
					}
				}
			}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			totalDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayTime); i++)
			othersDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			carDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			ptDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayTime); i++)
			wlkDayEnRouteTimeCounts[i]++;
	}

	@Override
	public void write(final String outputFilename) {
		double sum = carTime + ptTime + othersTime + wlkTime;

		SimpleWriter sw = new SimpleWriter(outputFilename
				+ "dailyEnRouteTime.txt");
		sw.writeln("\tDaily En Route Time\t(exkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("\tavg.[min]\t%\tsum.[min]");

		double avgCarTime = carTime / count;
		double avgPtTime = ptTime / count;
		double avgWlkTime = wlkTime / count;
		double avgOtherTime = othersTime / count;

		sw.writeln("car\t" + avgCarTime + "\t" + carTime / sum * 100.0 + "\t"
				+ carTime);
		sw.writeln("pt\t" + avgPtTime + "\t" + ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw.writeln("through\t" + avgOtherTime + "\t" + othersTime / sum * 100.0
				+ "\t" + othersTime);

		PieChart pieChart = new PieChart(
				"Avg. Daily En Route Time -- Modal Split");
		pieChart
				.addSeries(new String[] { "car", "pt", "wlk", "through" },
						new double[] { avgCarTime, avgPtTime, avgWlkTime,
								avgOtherTime });
		pieChart.saveAsPng(
				outputFilename + "dailyEnRouteTimeModalSplitPie.png", 800, 600);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily En Route Time\t(inkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + (avgCarTime + avgOtherTime) + "\t"
				+ (carTime + othersTime) / sum * 100.0 + "\t"
				+ (carTime + othersTime));
		sw.writeln("pt\t" + avgPtTime + "\t" + ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw
				.writeln("--travel destination and modal split--daily on route time--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + carWorkTime + "\t" + carEducTime + "\t"
				+ carShopTime + "\t" + carLeisTime + "\t" + carHomeTime + "\t"
				+ carOtherTime);
		sw.writeln("pt\t" + ptWorkTime + "\t" + ptEducTime + "\t" + ptShopTime
				+ "\t" + ptLeisTime + "\t" + ptHomeTime + "\t" + ptOtherTime);
		sw.writeln("walk\t" + wlkWorkTime + "\t" + wlkEducTime + "\t"
				+ wlkShopTime + "\t" + wlkLeisTime + "\t" + wlkHomeTime + "\t"
				+ wlkOtherTime);
		sw.writeln("through\t" + throughWorkTime + "\t" + throughEducTime
				+ "\t" + throughShopTime + "\t" + throughLeisTime + "\t"
				+ throughHomeTime + "\t" + throughOtherTime);
		sw
				.writeln("total\t"
						+ (carWorkTime + ptWorkTime + wlkWorkTime + throughWorkTime)
						+ "\t"
						+ (carEducTime + ptEducTime + wlkEducTime + throughEducTime)
						+ "\t"
						+ (carShopTime + ptShopTime + wlkShopTime + throughShopTime)
						+ "\t"
						+ (carLeisTime + ptLeisTime + wlkLeisTime + throughLeisTime)
						+ "\t"
						+ (carHomeTime + ptHomeTime + wlkHomeTime + throughHomeTime)
						+ "\t"
						+ (carOtherTime + ptOtherTime + wlkOtherTime + throughOtherTime));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destination", "daily En Route Time [min]",
				new String[] { "work", "education", "shopping", "leisure",
						"home", "others" });
		barChart.addSeries("car", new double[] { carWorkTime, carEducTime,
				carShopTime, carLeisTime, carHomeTime, carOtherTime });
		barChart.addSeries("pt", new double[] { ptWorkTime, ptEducTime,
				ptShopTime, ptLeisTime, ptHomeTime, ptOtherTime });
		barChart.addSeries("walk", new double[] { wlkWorkTime, wlkEducTime,
				wlkShopTime, wlkLeisTime, wlkHomeTime, wlkOtherTime });
		barChart.addSeries("through", new double[] { throughWorkTime,
				throughEducTime, throughShopTime, throughLeisTime,
				throughHomeTime, throughOtherTime });
		barChart.addMatsimLogo();
		barChart.saveAsPng(outputFilename
				+ "dailyEnRouteTimeTravelDistination.png", 1200, 900);

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101];
		double yCar[] = new double[101];
		double yPt[] = new double[101];
		double yWlk[] = new double[101];
		double yOther[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayEnRouteTimeCounts[i] / count * 100.0;
			yCar[i] = carDayEnRouteTimeCounts[i] / count * 100.0;
			yPt[i] = ptDayEnRouteTimeCounts[i] / count * 100.0;
			yWlk[i] = wlkDayEnRouteTimeCounts[i] / count * 100.0;
			yOther[i] = othersDayEnRouteTimeCounts[i] / count * 100.0;
		}
		XYLineChart chart = new XYLineChart("Daily En Route Time Distribution",
				"Daily En Route Time in min",
				"fraction of persons with daily en route time longer than x... in %");
		chart.addSeries("car", x, yCar);
		chart.addSeries("pt", x, yPt);
		chart.addSeries("walk", x, yWlk);
		chart.addSeries("other", x, yOther);
		chart.addSeries("total", x, yTotal);
		chart.saveAsPng(outputFilename + "dailyEnRouteTime.png", 800, 600);

		sw.writeln("");
		sw.writeln("--Modal split -- leg duration--");
		sw
				.writeln("leg Duration [min]\tcar legs no.\tpt legs no.\twalk legs no.\tcar fraction [%]\tpt fraction [%]\twalk fraction [%]");

		double xs[] = new double[101];
		double yCarFracs[] = new double[101];
		double yPtFracs[] = new double[101];
		double yWlkFracs[] = new double[101];
		double yBikeFracs[] = new double[101];
		double yOthersFracs[] = new double[101];
		for (int i = 0; i < 101; i++) {
			double sumOfLegTimeCounts = carLegTimeCounts[i]
					+ ptLegTimeCounts[i] + wlkLegTimeCounts[i]
					+ bikeLegTimeCounts[i] + othersLegTimeCounts[i];
			xs[i] = i * 2;
			if (sumOfLegTimeCounts > 0) {
				yCarFracs[i] = carLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yPtFracs[i] = ptLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yWlkFracs[i] = wlkLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yBikeFracs[i] = bikeLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
				yOthersFracs[i] = othersLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
			} else {
				yCarFracs[i] = 0;
				yPtFracs[i] = 0;
				yWlkFracs[i] = 0;
				yBikeFracs[i] = 0;
				yOthersFracs[i] = 0;
			}

			sw.writeln(i + "+\t" + carLegTimeCounts[i] + "\t"
					+ ptLegTimeCounts[i] + "\t" + wlkLegTimeCounts[i] + "\t"
					+ bikeLegTimeCounts[i] + "\t" + othersLegTimeCounts[i]
					+ "\t" + yCarFracs[i] + "\t" + yPtFracs[i] + "\t"
					+ yWlkFracs[i] + "\t" + yBikeFracs[i] + "\t"
					+ yOthersFracs[i]);
		}
		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Duration",
				"leg Duration [min]", "mode fraction [%]");
		chart2.addSeries("car", xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries("pt", xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries("walk", xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries("bike", xs, yBikeFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries("others", xs, yOthersFracs);
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

}
