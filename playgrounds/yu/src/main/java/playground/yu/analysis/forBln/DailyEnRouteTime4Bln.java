/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.yu.analysis.forBln;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.DailyEnRouteTime;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.charts.StackedBarChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * daily en route time analysis only for Berlin or for Berlin & Brandenburg
 *
 * @author yu
 *
 */
public class DailyEnRouteTime4Bln extends DailyEnRouteTime implements
		Analysis4Bln {
	private static final String CAR = "car", BIKE = "bike", WALK = "walk",
			OTHERS = "others";
	private double carBusinessTime, carEinkaufSonstigesTime,
			carFreizeitSonstSportTime, carHolidayJourneyTime, carMultipleTime,
			carSeeADoctorTime, carNotSpecifiedTime;
	private double ptBusinessTime, ptEinkaufSonstigesTime,
			ptFreizeitSonstSportTime, ptHolidayJourneyTime, ptMultipleTime,
			ptSeeADoctorTime, ptNotSpecifiedTime;
	private double wlkBusinessTime, wlkEinkaufSonstigesTime,
			wlkFreizeitSonstSportTime, wlkHolidayJourneyTime, wlkMultipleTime,
			wlkSeeADoctorTime, wlkNotSpecifiedTime;
	private double bikeBusinessTime, bikeEinkaufSonstigesTime,
			bikeFreizeitSonstSportTime, bikeHolidayJourneyTime,
			bikeMultipleTime, bikeSeeADoctorTime, bikeNotSpecifiedTime;
	private double othersBusinessTime, othersEinkaufSonstigesTime,
			othersFreizeitSonstSportTime, othersHolidayJourneyTime,
			othersMultipleTime, othersSeeADoctorTime, othersNotSpecifiedTime;

	public DailyEnRouteTime4Bln() {
		super();
	}

	public DailyEnRouteTime4Bln(final RoadPricingScheme toll) {
		super(toll);
	}

	@Override
	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double bikeDayTime = 0.0;
		double othersDayTime = 0.0;
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof Leg) {

				Leg bl = (Leg) pe;

				ActTypeBln at = null;
				String tmpActType = ((PlanImpl) plan).getNextActivity(bl).getType();
				for (ActTypeBln a : ActTypeBln.values())
					if (tmpActType.equals(a.getActTypeName())) {
						at = a;
						break;
					}
				if (at == null)
					at = ActTypeBln.other;

				double time = bl.getTravelTime() / 60.0;
				if (time < 0)
					time = 0;
				// if (bl.getDepartureTime() < 86400) {
				dayTime += time;
				TransportMode mode = bl.getMode();
				switch (mode) {
				case car:
					carTime += time;
					carDayTime += time;
					switch (at) {
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
					case business:
						carBusinessTime += time;
						break;
					case Einkauf_sonstiges:
						carEinkaufSonstigesTime += time;
						break;
					case Freizeit_sonstiges_incl_Sport:
						carFreizeitSonstSportTime += time;
						break;
					case holiday_journey:
						carHolidayJourneyTime += time;
						break;
					case multiple:
						carMultipleTime += time;
						break;
					case not_specified:
						carNotSpecifiedTime += time;
						break;
					case see_a_doctor:
						carSeeADoctorTime += time;
						break;
					default:
						carOtherTime += time;
						break;
					}
					carLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				case pt:
					ptTime += time;
					ptDayTime += time;
					switch (at) {
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
					case business:
						ptBusinessTime += time;
						break;
					case Einkauf_sonstiges:
						ptEinkaufSonstigesTime += time;
						break;
					case Freizeit_sonstiges_incl_Sport:
						ptFreizeitSonstSportTime += time;
						break;
					case holiday_journey:
						ptHolidayJourneyTime += time;
						break;
					case multiple:
						ptMultipleTime += time;
						break;
					case not_specified:
						ptNotSpecifiedTime += time;
						break;
					case see_a_doctor:
						ptSeeADoctorTime += time;
						break;
					default:
						ptOtherTime += time;
						break;
					}
					ptLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				case walk:
					wlkTime += time;
					wlkDayTime += time;
					switch (at) {
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
					case business:
						wlkBusinessTime += time;
						break;
					case Einkauf_sonstiges:
						wlkEinkaufSonstigesTime += time;
						break;
					case Freizeit_sonstiges_incl_Sport:
						wlkFreizeitSonstSportTime += time;
						break;
					case holiday_journey:
						wlkHolidayJourneyTime += time;
						break;
					case multiple:
						wlkMultipleTime += time;
						break;
					case not_specified:
						wlkNotSpecifiedTime += time;
						break;
					case see_a_doctor:
						wlkSeeADoctorTime += time;
						break;
					default:
						wlkOtherTime += time;
						break;
					}
					wlkLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				case bike:
					bikeTime += time;
					bikeDayTime += time;
					switch (at) {
					case home:
						bikeHomeTime += time;
						break;
					case work:
						bikeWorkTime += time;
						break;
					case education:
						bikeEducTime += time;
						break;
					case shopping:
						bikeShopTime += time;
						break;
					case leisure:
						bikeLeisTime += time;
						break;
					case business:
						bikeBusinessTime += time;
						break;
					case Einkauf_sonstiges:
						bikeEinkaufSonstigesTime += time;
						break;
					case Freizeit_sonstiges_incl_Sport:
						bikeFreizeitSonstSportTime += time;
						break;
					case holiday_journey:
						bikeHolidayJourneyTime += time;
						break;
					case multiple:
						bikeMultipleTime += time;
						break;
					case not_specified:
						bikeNotSpecifiedTime += time;
						break;
					case see_a_doctor:
						bikeSeeADoctorTime += time;
						break;
					default:
						bikeOtherTime += time;
						break;
					}
					bikeLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				default:
					othersTime += time;
					othersDayTime += time;
					switch (at) {
					case home:
						othersHomeTime += time;
						break;
					case work:
						othersWorkTime += time;
						break;
					case education:
						othersEducTime += time;
						break;
					case shopping:
						othersShopTime += time;
						break;
					case leisure:
						othersLeisTime += time;
						break;
					case business:
						othersBusinessTime += time;
						break;
					case Einkauf_sonstiges:
						othersEinkaufSonstigesTime += time;
						break;
					case Freizeit_sonstiges_incl_Sport:
						othersFreizeitSonstSportTime += time;
						break;
					case holiday_journey:
						othersHolidayJourneyTime += time;
						break;
					case multiple:
						othersMultipleTime += time;
						break;
					case not_specified:
						othersNotSpecifiedTime += time;
						break;
					case see_a_doctor:
						othersSeeADoctorTime += time;
						break;
					default:
						othersOtherTime += time;
						break;
					}
					othersLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				}
			}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			totalDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) othersDayTime); i++)
			othersDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			carDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			ptDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayTime); i++)
			wlkDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) bikeDayTime); i++)
			bikeDayEnRouteTimeCounts[i]++;
	}

	@Override
	public void write(final String outputFilename) {
		double sum = carTime + ptTime + othersTime + wlkTime + bikeTime;

		SimpleWriter sw = new SimpleWriter(outputFilename
				+ "dailyEnRouteTime.txt");
		sw.writeln("\tDaily En Route Time\tn_agents\t" + count);
		sw.writeln("\tavg.[min]\t%\tsum.[min]");

		double avgCarTime = carTime / count;
		double avgPtTime = ptTime / count;
		double avgWlkTime = wlkTime / count;
		double avgBikeTime = bikeTime / count;
		double avgOtherTime = othersTime / count;

		sw.writeln("car\t" + avgCarTime + "\t" + carTime / sum * 100.0 + "\t"
				+ carTime);
		sw.writeln("pt\t" + avgPtTime + "\t" + ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw.writeln("bike\t" + avgBikeTime + "\t" + bikeTime / sum * 100.0
				+ "\t" + bikeTime);
		sw.writeln("others\t" + avgOtherTime + "\t" + othersTime / sum * 100.0
				+ "\t" + othersTime);

		PieChart pieChart = new PieChart(
				"Avg. Daily En Route Time -- Modal Split");
		pieChart.addSeries(new String[] { CAR, "pt", "wlk", BIKE, OTHERS },
				new double[] { avgCarTime, avgPtTime, avgWlkTime, avgBikeTime,
						avgOtherTime });
		pieChart.addMatsimLogo();
		pieChart.saveAsPng(
				outputFilename + "dailyEnRouteTimeModalSplitPie.png", 800, 600);

		sw
				.writeln("--travel destination and modal split--daily on route time--");
		sw
				.writeln("mode\thome\twork\tshopping\teducation\tleisure\tother\tnot specified\tbusiness\tEinkauf sonstiges\tFreizeit(Sport usw.)\tsee a doctor\tholiday/journey\tmultiple");
		sw.writeln("car\t" + carHomeTime + "\t" + carWorkTime + "\t"
				+ carShopTime + "\t" + carEducTime + "\t" + carLeisTime + "\t"
				+ carOtherTime + "\t" + carNotSpecifiedTime + "\t"
				+ carBusinessTime + "\t" + carEinkaufSonstigesTime + "\t"
				+ carFreizeitSonstSportTime + "\t" + carSeeADoctorTime + "\t"
				+ carHolidayJourneyTime + "\t" + carMultipleTime);
		sw.writeln("pt\t" + ptHomeTime + "\t" + ptWorkTime + "\t" + ptShopTime
				+ "\t" + ptEducTime + "\t" + ptLeisTime + "\t" + ptOtherTime
				+ "\t" + ptNotSpecifiedTime + "\t" + ptBusinessTime + "\t"
				+ ptEinkaufSonstigesTime + "\t" + ptFreizeitSonstSportTime
				+ "\t" + ptSeeADoctorTime + "\t" + ptHolidayJourneyTime + "\t"
				+ ptMultipleTime);
		sw.writeln("walk\t" + wlkHomeTime + "\t" + wlkWorkTime + "\t"
				+ wlkShopTime + "\t" + wlkEducTime + "\t" + wlkLeisTime + "\t"
				+ wlkOtherTime + "\t" + wlkNotSpecifiedTime + "\t"
				+ wlkBusinessTime + "\t" + wlkEinkaufSonstigesTime + "\t"
				+ wlkFreizeitSonstSportTime + "\t" + wlkSeeADoctorTime + "\t"
				+ wlkHolidayJourneyTime + "\t" + wlkMultipleTime);
		sw.writeln("bike\t" + bikeHomeTime + "\t" + bikeWorkTime + "\t"
				+ bikeShopTime + "\t" + bikeEducTime + "\t" + bikeLeisTime
				+ "\t" + bikeOtherTime + "\t" + bikeNotSpecifiedTime + "\t"
				+ bikeBusinessTime + "\t" + bikeEinkaufSonstigesTime + "\t"
				+ bikeFreizeitSonstSportTime + "\t" + bikeSeeADoctorTime + "\t"
				+ bikeHolidayJourneyTime + "\t" + bikeMultipleTime);
		sw.writeln("others\t" + othersHomeTime + "\t" + othersWorkTime + "\t"
				+ othersShopTime + "\t" + othersEducTime + "\t"
				+ othersLeisTime + "\t" + othersOtherTime + "\t"
				+ othersNotSpecifiedTime + "\t" + othersBusinessTime + "\t"
				+ othersEinkaufSonstigesTime + "\t"
				+ othersFreizeitSonstSportTime + "\t" + othersSeeADoctorTime
				+ "\t" + othersHolidayJourneyTime + "\t" + othersMultipleTime);

		sw
				.writeln("total\t"
						+ (carHomeTime + ptHomeTime + wlkHomeTime
								+ bikeHomeTime + othersHomeTime)
						+ "\t"
						+ (carWorkTime + ptWorkTime + wlkWorkTime
								+ bikeWorkTime + othersWorkTime)
						+ "\t"
						+ (carShopTime + ptShopTime + wlkShopTime
								+ bikeShopTime + othersEducTime)
						+ "\t"
						+ (carEducTime + ptEducTime + wlkEducTime
								+ bikeEducTime + othersEducTime)
						+ "\t"
						+ (carLeisTime + ptLeisTime + wlkLeisTime
								+ bikeLeisTime + othersLeisTime)
						+ "\t"
						+ (carOtherTime + ptOtherTime + wlkOtherTime
								+ bikeOtherTime + othersOtherTime)
						+ "\t"
						+ (carNotSpecifiedTime + ptNotSpecifiedTime
								+ wlkNotSpecifiedTime + bikeNotSpecifiedTime + othersNotSpecifiedTime)
						+ "\t"
						+ (carBusinessTime + ptBusinessTime + wlkBusinessTime
								+ bikeBusinessTime + othersBusinessTime)
						+ "\t"
						+ (carEinkaufSonstigesTime + ptEinkaufSonstigesTime
								+ wlkEinkaufSonstigesTime
								+ bikeEinkaufSonstigesTime + othersEinkaufSonstigesTime)
						+ "\t"
						+ (carFreizeitSonstSportTime + ptFreizeitSonstSportTime
								+ wlkFreizeitSonstSportTime
								+ bikeFreizeitSonstSportTime + othersFreizeitSonstSportTime)
						+ "\t"
						+ (carSeeADoctorTime + ptSeeADoctorTime
								+ wlkSeeADoctorTime + bikeSeeADoctorTime + othersSeeADoctorTime)
						+ "\t"
						+ (carHolidayJourneyTime + ptHolidayJourneyTime
								+ wlkHolidayJourneyTime
								+ bikeHolidayJourneyTime + othersHolidayJourneyTime)
						+ "\t"
						+ (carMultipleTime + ptMultipleTime + wlkMultipleTime
								+ bikeMultipleTime + othersMultipleTime));

		StackedBarChart stackedBarChart = new StackedBarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destinations", "daily En Route Time [min]",
				PlotOrientation.VERTICAL);
		stackedBarChart.addSeries(
				new String[] { CAR, "pt", WALK, BIKE, OTHERS }, new String[] {
						"home", "work", "shopping", "education", "leisure",
						"other", "not specified", "business",
						"Einkauf sonstiges", "Freizeit (sonstiges incl.Sport)",
						"see a doctor", "holiday / journey", "multiple" },
				new double[][] {
						{ carHomeTime, carWorkTime, carShopTime, carEducTime,
								carLeisTime, carOtherTime, carNotSpecifiedTime,
								carBusinessTime, carEinkaufSonstigesTime,
								carFreizeitSonstSportTime, carSeeADoctorTime,
								carHolidayJourneyTime, carMultipleTime },
						{ ptHomeTime, ptWorkTime, ptShopTime, ptEducTime,
								ptLeisTime, ptOtherTime, ptNotSpecifiedTime,
								ptBusinessTime, ptEinkaufSonstigesTime,
								ptFreizeitSonstSportTime, ptSeeADoctorTime,
								ptHolidayJourneyTime, ptMultipleTime },
						{ wlkHomeTime, wlkWorkTime, wlkShopTime, wlkEducTime,
								wlkLeisTime, wlkOtherTime, wlkNotSpecifiedTime,
								wlkBusinessTime, wlkEinkaufSonstigesTime,
								wlkFreizeitSonstSportTime, wlkSeeADoctorTime,
								wlkHolidayJourneyTime, wlkMultipleTime },
						{ bikeHomeTime, bikeWorkTime, bikeShopTime,
								bikeEducTime, bikeLeisTime, bikeOtherTime,
								bikeNotSpecifiedTime, bikeBusinessTime,
								bikeEinkaufSonstigesTime,
								bikeFreizeitSonstSportTime, bikeSeeADoctorTime,
								bikeHolidayJourneyTime, bikeMultipleTime },
						{ othersHomeTime, othersWorkTime, othersShopTime,
								othersEducTime, othersLeisTime,
								othersOtherTime, othersNotSpecifiedTime,
								othersBusinessTime, othersEinkaufSonstigesTime,
								othersFreizeitSonstSportTime,
								othersSeeADoctorTime, othersHolidayJourneyTime,
								othersMultipleTime } });
		stackedBarChart.addMatsimLogo();
		stackedBarChart.saveAsPng(outputFilename
				+ "dailyEnRouteTimeTravelDistination.png", 1280, 1024);

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101];
		double yCar[] = new double[101];
		double yPt[] = new double[101];
		double yWlk[] = new double[101];
		double yBike[] = new double[101];
		double yOthers[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayEnRouteTimeCounts[i] / count * 100.0;
			yCar[i] = carDayEnRouteTimeCounts[i] / count * 100.0;
			yPt[i] = ptDayEnRouteTimeCounts[i] / count * 100.0;
			yWlk[i] = wlkDayEnRouteTimeCounts[i] / count * 100.0;
			yBike[i] = bikeDayEnRouteTimeCounts[i] / count * 100.0;
			yOthers[i] = othersDayEnRouteTimeCounts[i] / count * 100.0;
		}
		XYLineChart chart = new XYLineChart("Daily En Route Time Distribution",
				"Daily En Route Time in min",
				"fraction of persons with daily en route time longer than x... in %");
		chart.addSeries(CAR, x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries("pt", x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries(WALK, x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries(BIKE, x, yBike);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries(OTHERS, x, yOthers);
		chart.addSeries("total", x, yTotal);
		chart.addMatsimLogo();
		chart.saveAsPng(outputFilename + "dailyEnRouteTimeDistribution.png",
				800, 600);

		sw.writeln("\n--Modal split -- leg duration--");
		sw
				.writeln("leg Duration [min]\tcar legs no.\tpt legs no.\twalk legs no.\tbike legs no.\tothers legs no.\t"
						+ "car fraction [%]\tpt fraction [%]\twalk fraction [%]\tbike fraction [%]\tothers fraction [%]");

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
		chart2.addSeries(CAR, xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries("pt", xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries(WALK, xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries(BIKE, xs, yBikeFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries(OTHERS, xs, yOthersFracs);
		chart2.addMatsimLogo();
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../berlin data/osm/bb_osm_wip_cl.xml.gz";
		final String plansFilename = "../runs-svn/run756/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/run756/dailyEnRouteTime/";
		String tollFilename = "../berlin data/Hundekopf/osm/tollBerlinHundekopf.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		scenario.getConfig().scenario().setUseRoadpricing(true);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(scenario.getRoadPricingScheme());
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyEnRouteTime4Bln ert = new DailyEnRouteTime4Bln(scenario.getRoadPricingScheme());
		ert.run(population);
		ert.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
