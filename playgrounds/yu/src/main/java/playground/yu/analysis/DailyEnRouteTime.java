/**
 *
 */
package playground.yu.analysis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.forZrh.Analysis4Zrh.ActTypeZrh;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.TollTools;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute modal split of en route time
 *
 * @author yu
 *
 */
public class DailyEnRouteTime extends DailyAnalysis {
	protected int count;
	protected double carTime, ptTime, wlkTime, bikeTime, othersTime;
	protected final double totalDayEnRouteTimeCounts[],
			carDayEnRouteTimeCounts[], ptDayEnRouteTimeCounts[],
			wlkDayEnRouteTimeCounts[], bikeDayEnRouteTimeCounts[],
			othersDayEnRouteTimeCounts[];
	protected final double wlkLegTimeCounts[], ptLegTimeCounts[],
			carLegTimeCounts[], bikeLegTimeCounts[], othersLegTimeCounts[];
	protected double carWorkTime, carEducTime, carShopTime, carLeisTime,
			carOtherTime, carHomeTime;
	protected double ptWorkTime, ptEducTime, ptShopTime, ptLeisTime,
			ptOtherTime, ptHomeTime;
	protected double wlkWorkTime, wlkEducTime, wlkShopTime, wlkLeisTime,
			wlkOtherTime, wlkHomeTime;
	protected double bikeWorkTime, bikeEducTime, bikeShopTime, bikeLeisTime,
			bikeOtherTime, bikeHomeTime;
	protected double othersWorkTime, othersEducTime, othersShopTime,
			othersLeisTime, othersOtherTime, othersHomeTime;
	protected PersonImpl person;
	protected RoadPricingScheme toll = null;

	public DailyEnRouteTime(final RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public DailyEnRouteTime() {
		count = 0;

		carTime = 0.0;
		ptTime = 0.0;
		wlkTime = 0.0;
		bikeTime = 0.0;
		othersTime = 0.0;

		totalDayEnRouteTimeCounts = new double[101];
		carDayEnRouteTimeCounts = new double[101];
		ptDayEnRouteTimeCounts = new double[101];
		wlkDayEnRouteTimeCounts = new double[101];
		bikeDayEnRouteTimeCounts = new double[101];
		othersDayEnRouteTimeCounts = new double[101];

		carLegTimeCounts = new double[101];
		ptLegTimeCounts = new double[101];
		wlkLegTimeCounts = new double[101];
		bikeLegTimeCounts = new double[101];
		othersLegTimeCounts = new double[101];

		carWorkTime = 0.0;
		carEducTime = 0.0;
		carShopTime = 0.0;
		carLeisTime = 0.0;
		carHomeTime = 0.0;
		carOtherTime = 0.0;

		ptWorkTime = 0.0;
		ptEducTime = 0.0;
		ptShopTime = 0.0;
		ptLeisTime = 0.0;
		ptHomeTime = 0.0;
		ptOtherTime = 0.0;

		wlkWorkTime = 0.0;
		wlkEducTime = 0.0;
		wlkShopTime = 0.0;
		wlkLeisTime = 0.0;
		wlkHomeTime = 0.0;
		wlkOtherTime = 0.0;

		bikeWorkTime = 0.0;
		bikeEducTime = 0.0;
		bikeShopTime = 0.0;
		bikeLeisTime = 0.0;
		bikeHomeTime = 0.0;
		bikeOtherTime = 0.0;

		othersWorkTime = 0.0;
		othersEducTime = 0.0;
		othersShopTime = 0.0;
		othersLeisTime = 0.0;
		othersHomeTime = 0.0;
		othersOtherTime = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = (PersonImpl) person;
		Plan plan = this.person.getSelectedPlan();
		if (toll == null) {
			count++;
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity()
				.getLinkId(), toll)) {
			count++;
			run(plan);
		}
	}

	public void run(final Plan p) {
		PlanImpl plan = (PlanImpl) p;
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double bikeDayTime = 0.0;
		double othersDayTime = 0.0;
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {
				LegImpl bl = (LegImpl) pe;

				ActTypeZrh legIntent = (ActTypeZrh) this.getLegIntent(plan, bl);

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
					switch (legIntent) {
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
					break;
				case pt:
					ptTime += time;
					ptDayTime += time;
					switch (legIntent) {
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
					break;
				case walk:
					wlkTime += time;
					wlkDayTime += time;
					switch (legIntent) {
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
					break;
				case bike:
					bikeTime += time;
					bikeDayTime += time;
					switch (legIntent) {
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
					default:
						bikeOtherTime += time;
						break;
					}
					bikeLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				default:
					othersTime += time;
					othersDayTime += time;
					switch (legIntent) {
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
		pieChart.saveAsPng(
				outputFilename + "dailyEnRouteTimeModalSplitPie.png", 800, 600);

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
		sw.writeln("bike\t" + bikeWorkTime + "\t" + bikeEducTime + "\t"
				+ bikeShopTime + "\t" + bikeLeisTime + "\t" + bikeHomeTime
				+ "\t" + bikeOtherTime);
		sw.writeln("others\t" + othersWorkTime + "\t" + othersEducTime + "\t"
				+ othersShopTime + "\t" + othersLeisTime + "\t"
				+ othersHomeTime + "\t" + othersOtherTime);
		sw
				.writeln("total\t"
						+ (carWorkTime + ptWorkTime + wlkWorkTime
								+ bikeWorkTime + othersWorkTime)
						+ "\t"
						+ (carEducTime + ptEducTime + wlkEducTime
								+ bikeEducTime + othersEducTime)
						+ "\t"
						+ (carShopTime + ptShopTime + wlkShopTime
								+ bikeShopTime + othersShopTime)
						+ "\t"
						+ (carLeisTime + ptLeisTime + wlkLeisTime
								+ bikeLeisTime + othersLeisTime)
						+ "\t"
						+ (carHomeTime + ptHomeTime + wlkHomeTime
								+ bikeHomeTime + othersHomeTime)
						+ "\t"
						+ (carOtherTime + ptOtherTime + wlkOtherTime
								+ bikeOtherTime + othersOtherTime));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destination", "daily En Route Time [min]",
				new String[] { "work", "education", "shopping", "leisure",
						"home", OTHERS });
		barChart.addSeries(CAR, new double[] { carWorkTime, carEducTime,
				carShopTime, carLeisTime, carHomeTime, carOtherTime });

		double[] ptDestinationTime = new double[] { ptWorkTime, ptEducTime,
				ptShopTime, ptLeisTime, ptHomeTime, ptOtherTime };
		if (CollectionSum.getSum(ptDestinationTime) > 0)
			barChart.addSeries("pt", ptDestinationTime);

		double[] wlkDestinationTime = new double[] { wlkWorkTime, wlkEducTime,
				wlkShopTime, wlkLeisTime, wlkHomeTime, wlkOtherTime };
		if (CollectionSum.getSum(wlkDestinationTime) > 0)
			barChart.addSeries("walk", wlkDestinationTime);

		double[] bikeDestinationTime = new double[] { bikeWorkTime,
				bikeEducTime, bikeShopTime, bikeLeisTime, bikeHomeTime,
				bikeOtherTime };
		if (CollectionSum.getSum(bikeDestinationTime) > 0)
			barChart.addSeries(BIKE, bikeDestinationTime);

		double[] othersDestinationTime = new double[] { othersWorkTime,
				othersEducTime, othersShopTime, othersLeisTime, othersHomeTime,
				othersOtherTime };
		if (CollectionSum.getSum(othersDestinationTime) > 0)
			barChart.addSeries(OTHERS, othersDestinationTime);

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
			chart.addSeries("walk", x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries(BIKE, x, yBike);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries(OTHERS, x, yOthers);
		chart.addSeries("total", x, yTotal);
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
			chart2.addSeries("walk", xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries(BIKE, xs, yBikeFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries(OTHERS, xs, yOthersFracs);
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run684/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/run684/DailyEnRouteTime/";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();

		scenario.getConfig().scenario().setUseRoadpricing(true);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(scenario
				.getRoadPricingScheme());
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		DailyEnRouteTime ert = new DailyEnRouteTime(scenario
				.getRoadPricingScheme());

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		ert.run(population);
		ert.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	@Override
	protected ActType getLegIntent(PlanImpl plan, LegImpl currentLeg) {
		ActType legIntent = null;
		String tmpActTypeStartsWith = plan.getNextActivity(currentLeg)
				.getType().substring(0, 1);
		for (ActTypeZrh a : ActTypeZrh.values())
			if (tmpActTypeStartsWith.equals(a.getFirstLetter())) {
				legIntent = a;
				break;
			}
		if (legIntent == null)
			legIntent = ActTypeZrh.others;
		return legIntent;
	}

}
