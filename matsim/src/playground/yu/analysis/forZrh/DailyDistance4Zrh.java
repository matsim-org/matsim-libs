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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
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
	private double throughWorkDist, throughEducDist, throughShopDist,
			throughLeisDist, throughHomeDist, throughOtherDist, throughDist;
	private double[] throughLegDistanceCounts;
	private double[] throughDayDistanceCounts;

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
		throughLegDistanceCounts = new double[101];
		throughDayDistanceCounts = new double[101];
		this.toll = toll;
	}

	public void run(final Plan plan) {
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double wlkDayDist = 0.0;
		double othersDayDist = 0.0;
		double throughDayDist = 0.0;

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
			double dist = bl.getRoute().getDistance() / 1000.0;
			// if (bl.getDepartureTime() < 86400)

			if (Long.parseLong(this.person.getId().toString()) > 1000000000) {
				this.throughDist += dist;
				throughDayDist += dist;
				switch (ats) {
				case home:
					this.throughHomeDist += dist;
					break;
				case work:
					this.throughWorkDist += dist;
					break;
				case education:
					this.throughEducDist += dist;
					break;
				case shopping:
					this.throughShopDist += dist;
					break;
				case leisure:
					this.throughLeisDist += dist;
					break;
				default:
					this.throughOtherDist += dist;
					break;
				}
				throughLegDistanceCounts[Math.min(100, (int) dist)]++;
			} else if (bl.getMode().equals(TransportMode.car)) {
				this.carDist += dist;
				carDayDist += dist;
				switch (ats) {
				case home:
					this.carHomeDist += dist;
					break;
				case work:
					this.carWorkDist += dist;
					break;
				case education:
					this.carEducDist += dist;
					break;
				case shopping:
					this.carShopDist += dist;
					break;
				case leisure:
					this.carLeisDist += dist;
					break;
				default:
					this.carOtherDist += dist;
					break;
				}
				this.carLegDistanceCounts[Math.min(100, (int) dist)]++;
			} else if (bl.getMode().equals(TransportMode.pt)) {
				this.ptDist += dist;
				ptDayDist += dist;
				switch (ats) {
				case home:
					this.ptHomeDist += dist;
					break;
				case work:
					this.ptWorkDist += dist;
					break;
				case education:
					this.ptEducDist += dist;
					break;
				case shopping:
					this.ptShopDist += dist;
					break;
				case leisure:
					this.ptLeisDist += dist;
					break;
				default:
					this.ptOtherDist += dist;
					break;
				}
				this.ptLegDistanceCounts[Math.min(100, (int) dist)]++;
			} else if (bl.getMode().equals(TransportMode.walk)) {
				dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
						.getLink().getCoord(), plan.getNextActivity(bl)
						.getLink().getCoord()) * 1.5 / 1000.0;
				this.wlkDist += dist;
				wlkDayDist += dist;
				switch (ats) {
				case home:
					this.wlkHomeDist += dist;
					break;
				case work:
					this.wlkWorkDist += dist;
					break;
				case education:
					this.wlkEducDist += dist;
					break;
				case shopping:
					this.wlkShopDist += dist;
					break;
				case leisure:
					this.wlkLeisDist += dist;
					break;
				default:
					this.wlkOtherDist += dist;
					break;
				}
				this.wlkLegDistanceCounts[Math.min(100, (int) dist)]++;

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
				this.othersLegDistanceCounts[Math.min(100, (int) dist)]++;
			}
			dayDist += dist;

		}
		for (int i = 0; i <= Math.min(100, (int) dayDist); i++)
			this.totalDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) othersDayDist); i++)
			this.othersDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayDist); i++)
			this.carDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayDist); i++)
			this.ptDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayDist); i++)
			this.wlkDayDistanceCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) throughDayDist); i++)
			this.throughDayDistanceCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carDist + this.ptDist + wlkDist + this.othersDist
				+ throughDist;

		SimpleWriter sw = new SimpleWriter(outputFilename + "dailyDistance.txt");
		sw.writeln("\tDaily Distance\t(exkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tavg.[km]\t%\tsum.[km]");

		double avgCarDist = this.carDist / (double) this.count;
		double avgPtDist = this.ptDist / (double) this.count;
		double avgWlkDist = this.wlkDist / (double) this.count;
		double avgOthersDist = this.othersDist / (double) this.count;
		double avgThroughDist = throughDist / (double) count;

		sw.writeln("car\t" + avgCarDist + "\t" + this.carDist / sum * 100.0
				+ "\t" + carDist);
		sw.writeln("pt\t" + avgPtDist + "\t" + this.ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + this.wlkDist / sum * 100.0
				+ "\t" + wlkDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);
		sw.writeln("through\t" + avgThroughDist + "\t" + throughDist / sum
				* 100.0 + "\t" + throughDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart.addSeries(new String[] { "car", "pt", "wlk", "others",
				"through" }, new double[] { avgCarDist, avgPtDist, avgWlkDist,
				avgOthersDist, avgThroughDist });
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily Distance\t(inkl. through-traffic)\tn_agents\t"
				+ count);
		sw.writeln("mode\tkm\t%\tsum.[km]");
		sw.writeln("car\t" + (avgCarDist + avgOthersDist) + "\t"
				+ (this.carDist + throughDist) / sum * 100.0 + "\t"
				+ (carDist + throughDist));
		sw.writeln("pt\t" + avgPtDist + "\t" + this.ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + this.wlkDist / sum * 100.0
				+ "\t" + wlkDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);
		sw.writeln("----------------------------------------------");

		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother");
		sw.writeln("car\t" + this.carWorkDist + "\t" + this.carEducDist + "\t"
				+ this.carShopDist + "\t" + this.carLeisDist + "\t"
				+ this.carHomeDist + "\t" + this.carOtherDist);
		sw.writeln("pt\t" + this.ptWorkDist + "\t" + this.ptEducDist + "\t"
				+ this.ptShopDist + "\t" + this.ptLeisDist + "\t"
				+ this.ptHomeDist + "\t" + this.ptOtherDist);
		sw.writeln("walk\t" + this.wlkWorkDist + "\t" + this.wlkEducDist + "\t"
				+ this.wlkShopDist + "\t" + this.wlkLeisDist + "\t"
				+ this.wlkHomeDist + "\t" + this.wlkOtherDist);
		sw.writeln("others\t" + this.othersWorkDist + "\t"
				+ this.othersEducDist + "\t" + this.othersShopDist + "\t"
				+ this.othersLeisDist + "\t" + this.othersHomeDist + "\t"
				+ this.othersOtherDist);
		sw.writeln("through\t" + this.throughWorkDist + "\t"
				+ this.throughEducDist + "\t" + this.throughShopDist + "\t"
				+ this.throughLeisDist + "\t" + this.throughHomeDist + "\t"
				+ this.throughOtherDist);
		sw.writeln("total\t"
				+ (this.carWorkDist + this.ptWorkDist + wlkWorkDist
						+ othersWorkDist + this.throughWorkDist)
				+ "\t"
				+ (this.carEducDist + this.ptEducDist + wlkEducDist
						+ othersEducDist + this.throughEducDist)
				+ "\t"
				+ (this.carShopDist + this.ptShopDist + wlkShopDist
						+ othersShopDist + this.throughShopDist)
				+ "\t"
				+ (this.carLeisDist + this.ptLeisDist + wlkLeisDist
						+ othersLeisDist + this.throughLeisDist)
				+ "\t"
				+ (this.carHomeDist + this.ptHomeDist + wlkHomeDist
						+ othersHomeDist + this.throughHomeDist)
				+ "\t"
				+ (this.carOtherDist + this.ptOtherDist + wlkOtherDist
						+ othersOtherDist + this.throughOtherDist));

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
		barChart.addSeries("others", new double[] { this.othersWorkDist,
				this.othersEducDist, this.othersShopDist, this.othersLeisDist,
				this.othersHomeDist, this.othersOtherDist });
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
		double yOthers[] = new double[101];
		double yThrough[] = new double[101];

		for (int i = 0; i < 101; i++) {
			yTotal[i] = this.totalDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yCar[i] = this.carDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yPt[i] = this.ptDayDistanceCounts[i] / (double) this.count * 100.0;
			yWlk[i] = this.wlkDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yOthers[i] = this.othersDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yThrough[i] = throughDayDistanceCounts[i] / (double) count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries("car", x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries("pt", x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries("walk", x, yWlk);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries("others", x, yOthers);
		if (CollectionSum.getSum(yThrough) > 0)
			chart.addSeries("through", x, yThrough);
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
			double sumLegDistanceCounts = this.ptLegDistanceCounts[i]
					+ this.carLegDistanceCounts[i] + wlkLegDistanceCounts[i]
					+ othersLegDistanceCounts[i] + throughLegDistanceCounts[i];
			if (sumLegDistanceCounts > 0) {
				yCarFracs[i] = this.carLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
				yPtFracs[i] = this.ptLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
				yWlkFracs[i] = this.wlkLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
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
		chart2.addSeries("car", xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries("pt", xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries("walk", xs, yWlkFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries("others", xs, yOthersFracs);
		if (CollectionSum.getSum(yThroughFracs) > 0)
			chart2.addSeries("through", xs, yThroughFracs);
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

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();

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
		DailyDistance4Zrh dd = new DailyDistance4Zrh(tollReader.getScheme());

		new MatsimPopulationReader(population, network).readFile(plansFilename);

		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
