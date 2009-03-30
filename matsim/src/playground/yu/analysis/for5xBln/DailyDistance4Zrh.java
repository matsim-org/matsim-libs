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

import org.matsim.api.basic.v01.population.BasicLeg.Mode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.DailyDistance;
import playground.yu.utils.charts.BubbleChart;
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
	private double throughWorkDist, throughEducDist, throughShopDist,
			throughLeisDist, throughHomeDist, throughOtherDist;

	public DailyDistance4Zrh() {
		this.throughWorkDist = 0.0;
		this.throughEducDist = 0.0;
		this.throughShopDist = 0.0;
		this.throughLeisDist = 0.0;
		this.throughHomeDist = 0.0;
		this.throughOtherDist = 0.0;
	}

	public DailyDistance4Zrh(RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public void run(final Plan plan) {
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double wlkDayDist = 0.0;
		double otherDayDist = 0.0;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			ActTypeStartWith ats = null;
			String tmpActType = plan.getNextActivity(bl).getType();
			if (tmpActType.startsWith("h"))
				ats = ActTypeStartWith.h;
			else if (tmpActType.startsWith("w"))
				ats = ActTypeStartWith.w;
			else if (tmpActType.startsWith("e"))
				ats = ActTypeStartWith.e;
			else if (tmpActType.startsWith("s"))
				ats = ActTypeStartWith.s;
			else if (tmpActType.startsWith("l"))
				ats = ActTypeStartWith.l;
			else
				ats = ActTypeStartWith.o;
			double dist = bl.getRoute().getDistance() / 1000.0;
			// if (bl.getDepartureTime() < 86400)

			if (Long.parseLong(this.person.getId().toString()) > 1000000000) {
				this.otherDist += dist;
				otherDayDist += dist;
				switch (ats) {
				case h:
					this.throughHomeDist += dist;
					break;
				case w:
					this.throughWorkDist += dist;
					break;
				case e:
					this.throughEducDist += dist;
					break;
				case s:
					this.throughShopDist += dist;
					break;
				case l:
					this.throughLeisDist += dist;
					break;
				default:
					this.throughOtherDist += dist;
					break;
				}
			} else if (bl.getMode().equals(Leg.Mode.car)) {
				this.carDist += dist;
				carDayDist += dist;
				switch (ats) {
				case h:
					this.carHomeDist += dist;
					break;
				case w:
					this.carWorkDist += dist;
					break;
				case e:
					this.carEducDist += dist;
					break;
				case s:
					this.carShopDist += dist;
					break;
				case l:
					this.carLeisDist += dist;
					break;
				default:
					this.carOtherDist += dist;
					break;
				}
				this.carCounts5[Math.min(20, (int) dist / 5)]++;
				this.carCounts1[Math.min(100, (int) dist)]++;
			} else if (bl.getMode().equals(Mode.pt)) {
				this.ptDist += dist;
				ptDayDist += dist;
				switch (ats) {
				case h:
					this.ptHomeDist += dist;
					break;
				case w:
					this.ptWorkDist += dist;
					break;
				case e:
					this.ptEducDist += dist;
					break;
				case s:
					this.ptShopDist += dist;
					break;
				case l:
					this.ptLeisDist += dist;
					break;
				default:
					this.ptOtherDist += dist;
					break;
				}
				this.ptCounts5[Math.min(20, (int) dist / 5)]++;
				this.ptCounts1[Math.min(100, (int) dist)]++;
			} else if (bl.getMode().equals(Mode.walk)) {
				dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
						.getLink().getCoord(), plan.getNextActivity(bl)
						.getLink().getCoord()) * 1.5 / 1000.0;
				this.wlkDist += dist;
				wlkDayDist += dist;
				switch (ats) {
				case h:
					this.wlkHomeDist += dist;
					break;
				case w:
					this.wlkWorkDist += dist;
					break;
				case e:
					this.wlkEducDist += dist;
					break;
				case s:
					this.wlkShopDist += dist;
					break;
				case l:
					this.wlkLeisDist += dist;
					break;
				default:
					this.wlkOtherDist += dist;
					break;
				}
				this.wlkCounts5[Math.min(20, (int) dist / 5)]++;
				this.wlkCounts1[Math.min(100, (int) dist)]++;

			}
			dayDist += dist;

		}
		for (int i = 0; i <= Math.min(100, (int) dayDist); i++)
			this.totalCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayDist); i++)
			this.otherCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayDist); i++)
			this.carCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayDist); i++)
			this.ptCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayDist); i++)
			this.wlkCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carDist + this.ptDist + wlkDist + this.otherDist;

		SimpleWriter sw = new SimpleWriter(outputFilename + "dailyDistance.txt");
		sw.writeln("\tDaily Distance\t(exkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tavg.[km]\t%\tsum.[km]");

		double avgCarDist = this.carDist / (double) this.count;
		double avgPtDist = this.ptDist / (double) this.count;
		double avgWlkDist = this.wlkDist / (double) this.count;
		double avgOtherDist = this.otherDist / (double) this.count;

		sw.writeln("car\t" + avgCarDist + "\t" + this.carDist / sum * 100.0
				+ "\t" + carDist);
		sw.writeln("pt\t" + avgPtDist + "\t" + this.ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + this.wlkDist / sum * 100.0
				+ "\t" + wlkDist);
		sw.writeln("through\t" + avgOtherDist + "\t" + this.otherDist / sum
				* 100.0 + "\t" + otherDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart
				.addSeries(new String[] { "car", "pt", "wlk", "through" },
						new double[] { avgCarDist, avgPtDist, avgWlkDist,
								avgOtherDist });
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily Distance\t(inkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tkm\t%\tsum.[km]");
		sw.writeln("car\t" + (avgCarDist + avgOtherDist) + "\t"
				+ (this.carDist + this.otherDist) / sum * 100.0 + "\t"
				+ (carDist + otherDist));
		sw.writeln("pt\t" + avgPtDist + "\t" + this.ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + this.wlkDist / sum * 100.0
				+ "\t" + wlkDist);
		sw.writeln("----------------------------------------------");

		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + this.carWorkDist + "\t" + this.carEducDist + "\t"
				+ this.carShopDist + "\t" + this.carLeisDist + "\t"
				+ this.carHomeDist + "\t" + this.carOtherDist);
		sw.writeln("pt\t" + this.ptWorkDist + "\t" + this.ptEducDist + "\t"
				+ this.ptShopDist + "\t" + this.ptLeisDist + "\t"
				+ this.ptHomeDist + "\t" + this.ptOtherDist);
		sw.writeln("walk\t" + this.wlkWorkDist + "\t" + this.wlkEducDist + "\t"
				+ this.wlkShopDist + "\t" + this.wlkLeisDist + "\t"
				+ this.wlkHomeDist + "\t" + this.wlkOtherDist);
		sw.writeln("through\t" + this.throughWorkDist + "\t"
				+ this.throughEducDist + "\t" + this.throughShopDist + "\t"
				+ this.throughLeisDist + "\t" + this.throughHomeDist + "\t"
				+ this.throughOtherDist);
		sw
				.writeln("total\t"
						+ (this.carWorkDist + this.ptWorkDist + wlkWorkDist + this.throughWorkDist)
						+ "\t"
						+ (this.carEducDist + this.ptEducDist + wlkEducDist + this.throughEducDist)
						+ "\t"
						+ (this.carShopDist + this.ptShopDist + wlkShopDist + this.throughShopDist)
						+ "\t"
						+ (this.carLeisDist + this.ptLeisDist + wlkLeisDist + this.throughLeisDist)
						+ "\t"
						+ (this.carHomeDist + this.ptHomeDist + wlkHomeDist + this.throughHomeDist)
						+ "\t"
						+ (this.carOtherDist + this.ptOtherDist + wlkOtherDist + this.throughOtherDist));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily distance",
				"travel destination", "daily distance [km]", new String[] {
						"work", "education", "shopping", "leisure", "home",
						"others" });
		barChart.addSeries("car", new double[] { this.carWorkDist,
				this.carEducDist, this.carShopDist, this.carLeisDist,
				this.carHomeDist, this.carOtherDist });
		barChart.addSeries("pt", new double[] { this.ptWorkDist,
				this.ptEducDist, this.ptShopDist, this.ptLeisDist,
				this.ptHomeDist, this.ptOtherDist });
		barChart.addSeries("walk", new double[] { this.wlkWorkDist,
				this.wlkEducDist, this.wlkShopDist, this.wlkLeisDist,
				this.wlkHomeDist, this.wlkOtherDist });
		barChart.addSeries("through", new double[] { this.throughWorkDist,
				this.throughEducDist, this.throughShopDist,
				this.throughLeisDist, this.throughHomeDist,
				this.throughOtherDist });
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
		double yOther[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = this.totalCounts[i] / (double) this.count * 100.0;
			yCar[i] = this.carCounts[i] / (double) this.count * 100.0;
			yPt[i] = this.ptCounts[i] / (double) this.count * 100.0;
			yWlk[i] = this.wlkCounts[i] / (double) this.count * 100.0;
			yOther[i] = this.otherCounts[i] / (double) this.count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries("car", x, yCar);
		chart.addSeries("pt", x, yPt);
		chart.addSeries("walk", x, yWlk);
		chart.addSeries("other", x, yOther);
		chart.addSeries("total", x, yTotal);
		chart.saveAsPng(outputFilename + "dailyDistance.png", 800, 600);

		sw
				.writeln("-------------------------------------------------------------");
		sw.writeln("--Modal split -- leg distance--");
		sw
				.writeln("leg Distance [km]\tcar legs no.\tpt legs no.\twalk legs no.\tcar fraction [%]\tpt fraction [%]\twalk fraction [%]");

		BubbleChart bubbleChart = new BubbleChart(
				"Modal split -- leg distance", "pt fraction [%]",
				"car fraction [%]");
		for (int i = 0; i < 20; i++) {
			double sumCounts5 = this.ptCounts5[i] + this.carCounts5[i]
					+ wlkCounts5[i];
			double ptFraction = this.ptCounts5[i] / sumCounts5 * 100.0;
			double wlkFraction = this.wlkCounts5[i] / sumCounts5 * 100.0;
			double carFraction = this.carCounts5[i] / sumCounts5 * 100.0;
			if (sumCounts5 > 0)
				bubbleChart.addSeries(i * 5 + "-" + (i + 1) * 5 + " km",
						new double[][] { new double[] { ptFraction },
								new double[] { carFraction },
								new double[] { (i + 0.5) / 5.0 } });
			sw.writeln((i * 5) + "+\t" + this.carCounts5[i] + "\t"
					+ this.ptCounts5[i] + "\t" + this.wlkCounts5[i] + "\t"
					+ carFraction + "\t" + ptFraction + "\t" + wlkFraction);
		}
		double sumCounts5 = this.ptCounts5[20] + this.carCounts5[20]
				+ wlkCounts5[20];
		double ptFraction = this.ptCounts5[20] / sumCounts5 * 100.0;
		double wlkFraction = this.wlkCounts5[20] / sumCounts5 * 100.0;
		double carFraction = this.carCounts5[20] / sumCounts5 * 100.0;
		if (sumCounts5 > 0)
			bubbleChart.addSeries("100+ km", new double[][] {
					new double[] { ptFraction }, new double[] { carFraction },
					new double[] { 4.1 } });
		sw.writeln(100 + "+\t" + this.carCounts5[20] + "\t"
				+ this.ptCounts5[20] + "\t" + this.wlkCounts5[20] + "\t"
				+ carFraction + "\t" + ptFraction + "\t" + wlkFraction);
		bubbleChart.saveAsPng(outputFilename + "legDistanceModalSplit.png",
				900, 900);

		double xs[] = new double[101];
		double yCarFracs[] = new double[101];
		double yPtFracs[] = new double[101];
		double yWlkFracs[] = new double[101];
		for (int i = 0; i < 101; i++) {
			xs[i] = i;
			yCarFracs[i] = this.carCounts1[i]
					/ (this.ptCounts1[i] + this.carCounts1[i] + wlkCounts1[i])
					* 100.0;
			yPtFracs[i] = this.ptCounts1[i]
					/ (this.ptCounts1[i] + this.carCounts1[i] + wlkCounts1[i])
					* 100.0;
			yWlkFracs[i] = this.wlkCounts1[i]
					/ (this.ptCounts1[i] + this.carCounts1[i] + wlkCounts1[i])
					* 100.0;
		}
		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Distance",
				"leg Distance [km]", "mode fraction [%]");
		chart2.addSeries("car", xs, yCarFracs);
		chart2.addSeries("pt", xs, yPtFracs);
		chart2.addSeries("walk", xs, yWlkFracs);
		chart2.saveAsPng(outputFilename + "legDistanceModalSplit2.png", 800,
				600);
		sw.close();
	}
}
