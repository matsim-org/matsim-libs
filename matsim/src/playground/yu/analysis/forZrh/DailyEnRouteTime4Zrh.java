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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.DailyEnRouteTime;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.charts.PieChart;
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
		this.throughWorkTime = 0.0;
		this.throughEducTime = 0.0;
		this.throughShopTime = 0.0;
		this.throughLeisTime = 0.0;
		this.throughHomeTime = 0.0;
		this.throughOtherTime = 0.0;
	}

	public DailyEnRouteTime4Zrh(RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double otherDayTime = 0.0;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			ActType ats = null;
			String tmpActType = plan.getNextActivity(bl).getType();
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
			double time = bl.getTravelTime() / 60.0;
			if (time < 0)
				time = 0;
			if (bl.getDepartureTime() < 86400) {
				dayTime += time;
				if (Long.parseLong(this.person.getId().toString()) > 1000000000) {
					this.othersTime += time;
					otherDayTime += time;
					switch (ats) {
					case home:
						this.throughHomeTime += time;
						break;
					case work:
						this.throughWorkTime += time;
						break;
					case education:
						this.throughEducTime += time;
						break;
					case shopping:
						this.throughShopTime += time;
						break;
					case leisure:
						this.throughLeisTime += time;
						break;
					default:
						this.throughOtherTime += time;
						break;
					}
				} else {
					if (bl.getMode().equals(TransportMode.car)) {
						this.carTime += time;
						carDayTime += time;
						switch (ats) {
						case home:
							this.carHomeTime += time;
							break;
						case work:
							this.carWorkTime += time;
							break;
						case education:
							this.carEducTime += time;
							break;
						case shopping:
							this.carShopTime += time;
							break;
						case leisure:
							this.carLeisTime += time;
							break;
						default:
							this.carOtherTime += time;
							break;
						}
						this.carLegTimeCounts[Math.min(100, (int) time / 2)]++;
					} else if (bl.getMode().equals(TransportMode.pt)) {
						this.ptTime += time;
						ptDayTime += time;
						switch (ats) {
						case home:
							this.ptHomeTime += time;
							break;
						case work:
							this.ptWorkTime += time;
							break;
						case education:
							this.ptEducTime += time;
							break;
						case shopping:
							this.ptShopTime += time;
							break;
						case leisure:
							this.ptLeisTime += time;
							break;
						default:
							this.ptOtherTime += time;
							break;
						}
						this.ptLegTimeCounts[Math.min(100, (int) time / 2)]++;
					} else if (bl.getMode().equals(TransportMode.walk)) {
						this.wlkTime += time;
						wlkDayTime += time;
						switch (ats) {
						case home:
							this.wlkHomeTime += time;
							break;
						case work:
							this.wlkWorkTime += time;
							break;
						case education:
							this.wlkEducTime += time;
							break;
						case shopping:
							this.wlkShopTime += time;
							break;
						case leisure:
							this.wlkLeisTime += time;
							break;
						default:
							this.wlkOtherTime += time;
							break;
						}
						this.wlkLegTimeCounts[Math.min(100, (int) time / 2)]++;
					}
				}
			}
		}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			this.totalDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayTime); i++)
			this.othersDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			this.carDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			this.ptDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayTime); i++)
			this.wlkDayEnRouteTimeCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carTime + this.ptTime + this.othersTime + wlkTime;

		SimpleWriter sw = new SimpleWriter(outputFilename
				+ "dailyEnRouteTime.txt");
		sw.writeln("\tDaily En Route Time\t(exkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("\tavg.[min]\t%\tsum.[min]");

		double avgCarTime = carTime / (double) this.count;
		double avgPtTime = ptTime / (double) count;
		double avgWlkTime = wlkTime / (double) count;
		double avgOtherTime = othersTime / (double) count;

		sw.writeln("car\t" + avgCarTime + "\t" + this.carTime / sum * 100.0
				+ "\t" + carTime);
		sw.writeln("pt\t" + avgPtTime + "\t" + this.ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw.writeln("through\t" + avgOtherTime + "\t" + this.othersTime / sum
				* 100.0 + "\t" + othersTime);

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
				+ (this.carTime + this.othersTime) / sum * 100.0 + "\t"
				+ (this.carTime + this.othersTime));
		sw.writeln("pt\t" + avgPtTime + "\t" + this.ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw
				.writeln("--travel destination and modal split--daily on route time--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + this.carWorkTime + "\t" + this.carEducTime + "\t"
				+ this.carShopTime + "\t" + this.carLeisTime + "\t"
				+ this.carHomeTime + "\t" + this.carOtherTime);
		sw.writeln("pt\t" + this.ptWorkTime + "\t" + this.ptEducTime + "\t"
				+ this.ptShopTime + "\t" + this.ptLeisTime + "\t"
				+ this.ptHomeTime + "\t" + this.ptOtherTime);
		sw.writeln("walk\t" + this.wlkWorkTime + "\t" + this.wlkEducTime + "\t"
				+ this.wlkShopTime + "\t" + this.wlkLeisTime + "\t"
				+ this.wlkHomeTime + "\t" + this.wlkOtherTime);
		sw.writeln("through\t" + this.throughWorkTime + "\t"
				+ this.throughEducTime + "\t" + this.throughShopTime + "\t"
				+ this.throughLeisTime + "\t" + this.throughHomeTime + "\t"
				+ this.throughOtherTime);
		sw
				.writeln("total\t"
						+ (this.carWorkTime + this.ptWorkTime + wlkWorkTime + this.throughWorkTime)
						+ "\t"
						+ (this.carEducTime + this.ptEducTime + wlkEducTime + this.throughEducTime)
						+ "\t"
						+ (this.carShopTime + this.ptShopTime + wlkShopTime + this.throughShopTime)
						+ "\t"
						+ (this.carLeisTime + this.ptLeisTime + wlkLeisTime + this.throughLeisTime)
						+ "\t"
						+ (this.carHomeTime + this.ptHomeTime + wlkHomeTime + this.throughHomeTime)
						+ "\t"
						+ (this.carOtherTime + this.ptOtherTime + wlkOtherTime + this.throughOtherTime));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destination", "daily En Route Time [min]",
				new String[] { "work", "education", "shopping", "leisure",
						"home", "others" });
		barChart.addSeries("car", new double[] { this.carWorkTime,
				this.carEducTime, this.carShopTime, this.carLeisTime,
				this.carHomeTime, this.carOtherTime });
		barChart.addSeries("pt", new double[] { this.ptWorkTime,
				this.ptEducTime, this.ptShopTime, this.ptLeisTime,
				this.ptHomeTime, this.ptOtherTime });
		barChart.addSeries("walk", new double[] { this.wlkWorkTime,
				this.wlkEducTime, this.wlkShopTime, this.wlkLeisTime,
				this.wlkHomeTime, this.wlkOtherTime });
		barChart.addSeries("through", new double[] { this.throughWorkTime,
				this.throughEducTime, this.throughShopTime,
				this.throughLeisTime, this.throughHomeTime,
				this.throughOtherTime });
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
			yTotal[i] = this.totalDayEnRouteTimeCounts[i] / this.count * 100.0;
			yCar[i] = this.carDayEnRouteTimeCounts[i] / this.count * 100.0;
			yPt[i] = this.ptDayEnRouteTimeCounts[i] / this.count * 100.0;
			yWlk[i] = this.wlkDayEnRouteTimeCounts[i] / this.count * 100.0;
			yOther[i] = this.othersDayEnRouteTimeCounts[i] / this.count * 100.0;
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
				yCarFracs[i] = this.carLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
				yPtFracs[i] = this.ptLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
				yWlkFracs[i] = this.wlkLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
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
