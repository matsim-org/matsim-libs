/**
 * 
 */
package playground.yu.analysis.forBln;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.basic.v01.population.BasicLeg.Mode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.roadpricing.RoadPricingScheme;

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
	public DailyEnRouteTime4Bln() {
		super();
	}

	public DailyEnRouteTime4Bln(RoadPricingScheme toll) {
		super(toll);
	}

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

	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double bikeDayTime = 0.0;
		double othersDayTime = 0.0;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();

			ActType at = null;
			String tmpActType = plan.getNextActivity(bl).getType();
			for (ActType a : ActType.values()) {
				if (tmpActType.equals(a.getActTypeName())) {
					at = a;
					break;
				}
			}
			if (at == null)
				at = ActType.other;

			double time = bl.getTravelTime() / 60.0;
			if (time < 0)
				time = 0;
			// if (bl.getDepartureTime() < 86400) {
			dayTime += time;
			Mode mode = bl.getMode();
			switch (mode) {
			case car:
				this.carTime += time;
				carDayTime += time;
				switch (at) {
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
					this.carOtherTime += time;
					break;
				}
				this.carLegTimeCounts[Math.min(100, (int) time / 2)]++;
				break;
			case pt:
				this.ptTime += time;
				ptDayTime += time;
				switch (at) {
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
					this.ptOtherTime += time;
					break;
				}
				this.ptLegTimeCounts[Math.min(100, (int) time / 2)]++;
				break;
			case walk:
				this.wlkTime += time;
				wlkDayTime += time;
				switch (at) {
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
					this.wlkOtherTime += time;
					break;
				}
				this.wlkLegTimeCounts[Math.min(100, (int) time / 2)]++;
				break;
			case bike:
				bikeTime += time;
				bikeDayTime += time;
				switch (at) {
				case home:
					this.bikeHomeTime += time;
					break;
				case work:
					this.bikeWorkTime += time;
					break;
				case education:
					this.bikeEducTime += time;
					break;
				case shopping:
					this.bikeShopTime += time;
					break;
				case leisure:
					this.bikeLeisTime += time;
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
					this.bikeOtherTime += time;
					break;
				}
				this.bikeLegTimeCounts[Math.min(100, (int) time / 2)]++;
				break;
			default:
				this.othersTime += time;
				othersDayTime += time;
				switch (at) {
				case home:
					this.othersHomeTime += time;
					break;
				case work:
					this.othersWorkTime += time;
					break;
				case education:
					this.othersEducTime += time;
					break;
				case shopping:
					this.othersShopTime += time;
					break;
				case leisure:
					this.othersLeisTime += time;
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
					this.othersOtherTime += time;
					break;
				}
				this.othersLegTimeCounts[Math.min(100, (int) time / 2)]++;
				break;
			}

		}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			this.totalDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) othersDayTime); i++)
			this.othersDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			this.carDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			this.ptDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) wlkDayTime); i++)
			this.wlkDayEnRouteTimeCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) bikeDayTime); i++)
			this.bikeDayEnRouteTimeCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carTime + this.ptTime + this.othersTime + wlkTime
				+ bikeTime;

		SimpleWriter sw = new SimpleWriter(outputFilename
				+ "dailyEnRouteTime.txt");
		sw.writeln("\tDaily En Route Time\tn_agents\t" + count);
		sw.writeln("\tavg.[min]\t%\tsum.[min]");

		double avgCarTime = carTime / (double) this.count;
		double avgPtTime = ptTime / (double) count;
		double avgWlkTime = wlkTime / (double) count;
		double avgBikeTime = bikeTime / (double) count;
		double avgOtherTime = othersTime / (double) count;

		sw.writeln("car\t" + avgCarTime + "\t" + this.carTime / sum * 100.0
				+ "\t" + carTime);
		sw.writeln("pt\t" + avgPtTime + "\t" + this.ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw.writeln("bike\t" + avgBikeTime + "\t" + bikeTime / sum * 100.0
				+ "\t" + bikeTime);
		sw.writeln("others\t" + avgOtherTime + "\t" + this.othersTime / sum
				* 100.0 + "\t" + othersTime);

		PieChart pieChart = new PieChart(
				"Avg. Daily En Route Time -- Modal Split");
		pieChart.addSeries(
				new String[] { "car", "pt", "wlk", "bike", "others" },
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
				+ carShopTime + "\t" + carEducTime + "\t" + this.carLeisTime
				+ "\t" + this.carOtherTime + "\t" + carNotSpecifiedTime + "\t"
				+ carBusinessTime + "\t" + carEinkaufSonstigesTime + "\t"
				+ carFreizeitSonstSportTime + "\t" + carSeeADoctorTime + "\t"
				+ carHolidayJourneyTime + "\t" + carMultipleTime);
		sw.writeln("pt\t" + ptHomeTime + "\t" + ptWorkTime + "\t" + ptShopTime
				+ "\t" + ptEducTime + "\t" + this.ptLeisTime + "\t"
				+ this.ptOtherTime + "\t" + ptNotSpecifiedTime + "\t"
				+ ptBusinessTime + "\t" + ptEinkaufSonstigesTime + "\t"
				+ ptFreizeitSonstSportTime + "\t" + ptSeeADoctorTime + "\t"
				+ ptHolidayJourneyTime + "\t" + ptMultipleTime);
		sw.writeln("walk\t" + wlkHomeTime + "\t" + wlkWorkTime + "\t"
				+ wlkShopTime + "\t" + wlkEducTime + "\t" + this.wlkLeisTime
				+ "\t" + this.wlkOtherTime + "\t" + wlkNotSpecifiedTime + "\t"
				+ wlkBusinessTime + "\t" + wlkEinkaufSonstigesTime + "\t"
				+ wlkFreizeitSonstSportTime + "\t" + wlkSeeADoctorTime + "\t"
				+ wlkHolidayJourneyTime + "\t" + wlkMultipleTime);
		sw.writeln("bike\t" + bikeHomeTime + "\t" + bikeWorkTime + "\t"
				+ bikeShopTime + "\t" + bikeEducTime + "\t" + this.bikeLeisTime
				+ "\t" + this.bikeOtherTime + "\t" + bikeNotSpecifiedTime
				+ "\t" + bikeBusinessTime + "\t" + bikeEinkaufSonstigesTime
				+ "\t" + bikeFreizeitSonstSportTime + "\t" + bikeSeeADoctorTime
				+ "\t" + bikeHolidayJourneyTime + "\t" + bikeMultipleTime);
		sw.writeln("others\t" + othersHomeTime + "\t" + othersWorkTime + "\t"
				+ othersShopTime + "\t" + othersEducTime + "\t"
				+ this.othersLeisTime + "\t" + this.othersOtherTime + "\t"
				+ othersNotSpecifiedTime + "\t" + othersBusinessTime + "\t"
				+ othersEinkaufSonstigesTime + "\t"
				+ othersFreizeitSonstSportTime + "\t" + othersSeeADoctorTime
				+ "\t" + othersHolidayJourneyTime + "\t" + othersMultipleTime);

		sw
				.writeln("total\t"
						+ (this.carHomeTime + this.ptHomeTime + wlkHomeTime
								+ bikeHomeTime + othersHomeTime)
						+ "\t"
						+ (this.carWorkTime + this.ptWorkTime + wlkWorkTime
								+ bikeWorkTime + othersWorkTime)
						+ "\t"
						+ (this.carShopTime + this.ptShopTime + wlkShopTime
								+ bikeShopTime + othersEducTime)
						+ "\t"
						+ (this.carEducTime + this.ptEducTime + wlkEducTime
								+ bikeEducTime + othersEducTime)
						+ "\t"
						+ (this.carLeisTime + this.ptLeisTime + wlkLeisTime
								+ bikeLeisTime + othersLeisTime)
						+ "\t"
						+ (this.carOtherTime + this.ptOtherTime + wlkOtherTime
								+ bikeOtherTime + othersOtherTime)
						+ "\t"
						+ (this.carNotSpecifiedTime + this.ptNotSpecifiedTime
								+ wlkNotSpecifiedTime + bikeNotSpecifiedTime + othersNotSpecifiedTime)
						+ "\t"
						+ (this.carBusinessTime + this.ptBusinessTime
								+ wlkBusinessTime + bikeBusinessTime + othersBusinessTime)
						+ "\t"
						+ (this.carEinkaufSonstigesTime
								+ this.ptEinkaufSonstigesTime
								+ wlkEinkaufSonstigesTime
								+ bikeEinkaufSonstigesTime + othersEinkaufSonstigesTime)
						+ "\t"
						+ (this.carFreizeitSonstSportTime
								+ this.ptFreizeitSonstSportTime
								+ wlkFreizeitSonstSportTime
								+ bikeFreizeitSonstSportTime + othersFreizeitSonstSportTime)
						+ "\t"
						+ (this.carSeeADoctorTime + this.ptSeeADoctorTime
								+ wlkSeeADoctorTime + bikeSeeADoctorTime + othersSeeADoctorTime)
						+ "\t"
						+ (this.carHolidayJourneyTime
								+ this.ptHolidayJourneyTime
								+ wlkHolidayJourneyTime
								+ bikeHolidayJourneyTime + othersHolidayJourneyTime)
						+ "\t"
						+ (this.carMultipleTime + this.ptMultipleTime
								+ wlkMultipleTime + bikeMultipleTime + othersMultipleTime));

		StackedBarChart stackedBarChart = new StackedBarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destinations", "daily En Route Time [min]",
				PlotOrientation.VERTICAL);
		stackedBarChart.addSeries(new String[] { "car", "pt", "walk", "bike",
				"others" }, new String[] { "home", "work", "shopping",
				"education", "leisure", "other", "not specified", "business",
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
			yTotal[i] = this.totalDayEnRouteTimeCounts[i] / this.count * 100.0;
			yCar[i] = this.carDayEnRouteTimeCounts[i] / this.count * 100.0;
			yPt[i] = this.ptDayEnRouteTimeCounts[i] / this.count * 100.0;
			yWlk[i] = this.wlkDayEnRouteTimeCounts[i] / this.count * 100.0;
			yBike[i] = this.bikeDayEnRouteTimeCounts[i] / this.count * 100.0;
			yOthers[i] = this.othersDayEnRouteTimeCounts[i] / this.count
					* 100.0;
		}
		XYLineChart chart = new XYLineChart("Daily En Route Time Distribution",
				"Daily En Route Time in min",
				"fraction of persons with daily en route time longer than x... in %");
		chart.addSeries("car", x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries("pt", x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries("walk", x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries("bike", x, yBike);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries("others", x, yOthers);
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
			yCarFracs[i] = this.carLegTimeCounts[i] / sumOfLegTimeCounts
					* 100.0;
			yPtFracs[i] = this.ptLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
			yWlkFracs[i] = this.wlkLegTimeCounts[i] / sumOfLegTimeCounts
					* 100.0;
			yBikeFracs[i] = bikeLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
			yOthersFracs[i] = othersLegTimeCounts[i] / sumOfLegTimeCounts
					* 100.0;
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
		chart2.addMatsimLogo();
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../berlin data/osm/bb_osm_wip_cl.xml.gz";
		final String plansFilename = "../runs-svn/run756/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/run756/dailyEnRouteTime/";
		// String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();

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

		DailyEnRouteTime4Bln ert = new DailyEnRouteTime4Bln(null);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		ert.run(population);
		ert.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
