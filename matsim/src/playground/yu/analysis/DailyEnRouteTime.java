/**
 *
 */
package playground.yu.analysis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.population.BasicLeg.Mode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
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
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.forZrh.Analysis4Zrh.ActType;
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
public class DailyEnRouteTime extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
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

	protected Person person;

	protected RoadPricingScheme toll = null;

	public DailyEnRouteTime(RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public DailyEnRouteTime() {
		this.count = 0;

		this.carTime = 0.0;
		this.ptTime = 0.0;
		this.wlkTime = 0.0;
		bikeTime = 0.0;
		this.othersTime = 0.0;

		this.totalDayEnRouteTimeCounts = new double[101];
		this.carDayEnRouteTimeCounts = new double[101];
		this.ptDayEnRouteTimeCounts = new double[101];
		this.wlkDayEnRouteTimeCounts = new double[101];
		bikeDayEnRouteTimeCounts = new double[101];
		this.othersDayEnRouteTimeCounts = new double[101];

		this.carLegTimeCounts = new double[101];
		this.ptLegTimeCounts = new double[101];
		this.wlkLegTimeCounts = new double[101];
		bikeLegTimeCounts = new double[101];
		othersLegTimeCounts = new double[101];

		this.carWorkTime = 0.0;
		this.carEducTime = 0.0;
		this.carShopTime = 0.0;
		this.carLeisTime = 0.0;
		this.carHomeTime = 0.0;
		this.carOtherTime = 0.0;

		this.ptWorkTime = 0.0;
		this.ptEducTime = 0.0;
		this.ptShopTime = 0.0;
		this.ptLeisTime = 0.0;
		this.ptHomeTime = 0.0;
		this.ptOtherTime = 0.0;

		this.wlkWorkTime = 0.0;
		this.wlkEducTime = 0.0;
		this.wlkShopTime = 0.0;
		this.wlkLeisTime = 0.0;
		this.wlkHomeTime = 0.0;
		this.wlkOtherTime = 0.0;

		this.bikeWorkTime = 0.0;
		this.bikeEducTime = 0.0;
		this.bikeShopTime = 0.0;
		this.bikeLeisTime = 0.0;
		this.bikeHomeTime = 0.0;
		this.bikeOtherTime = 0.0;

		this.othersWorkTime = 0.0;
		this.othersEducTime = 0.0;
		this.othersShopTime = 0.0;
		this.othersLeisTime = 0.0;
		this.othersHomeTime = 0.0;
		this.othersOtherTime = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = person;
		Plan plan = person.getSelectedPlan();
		if (toll == null) {
			this.count++;
			run(plan);
		} else if (TollTools.isInRange(plan.getFirstActivity().getLink(), toll)) {
			this.count++;
			run(plan);
		}
	}

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
			String tmpActTypeStartsWith = plan.getNextActivity(bl).getType()
					.substring(0, 1);
			for (ActType a : ActType.values()) {
				if (tmpActTypeStartsWith.equals(a.getFirstLetter())) {
					at = a;
					break;
				}
			}
			if (at == null)
				at = ActType.others;

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
		pieChart.saveAsPng(
				outputFilename + "dailyEnRouteTimeModalSplitPie.png", 800, 600);

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
		sw.writeln("bike\t" + this.bikeWorkTime + "\t" + this.bikeEducTime
				+ "\t" + this.bikeShopTime + "\t" + this.bikeLeisTime + "\t"
				+ this.bikeHomeTime + "\t" + this.bikeOtherTime);
		sw.writeln("others\t" + this.othersWorkTime + "\t"
				+ this.othersEducTime + "\t" + this.othersShopTime + "\t"
				+ this.othersLeisTime + "\t" + this.othersHomeTime + "\t"
				+ this.othersOtherTime);
		sw.writeln("total\t"
				+ (this.carWorkTime + this.ptWorkTime + wlkWorkTime
						+ bikeWorkTime + othersWorkTime)
				+ "\t"
				+ (this.carEducTime + this.ptEducTime + wlkEducTime
						+ bikeEducTime + othersEducTime)
				+ "\t"
				+ (this.carShopTime + this.ptShopTime + wlkShopTime
						+ bikeShopTime + othersShopTime)
				+ "\t"
				+ (this.carLeisTime + this.ptLeisTime + wlkLeisTime
						+ bikeLeisTime + othersLeisTime)
				+ "\t"
				+ (this.carHomeTime + this.ptHomeTime + wlkHomeTime
						+ bikeHomeTime + othersHomeTime)
				+ "\t"
				+ (this.carOtherTime + this.ptOtherTime + wlkOtherTime
						+ bikeOtherTime + othersOtherTime));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destination", "daily En Route Time [min]",
				new String[] { "work", "education", "shopping", "leisure",
						"home", "others" });
		barChart.addSeries("car", new double[] { this.carWorkTime,
				this.carEducTime, this.carShopTime, this.carLeisTime,
				this.carHomeTime, this.carOtherTime });

		double[] ptDestinationTime = new double[] { this.ptWorkTime,
				this.ptEducTime, this.ptShopTime, this.ptLeisTime,
				this.ptHomeTime, this.ptOtherTime };
		if (CollectionSum.getSum(ptDestinationTime) > 0)
			barChart.addSeries("pt", ptDestinationTime);

		double[] wlkDestinationTime = new double[] { this.wlkWorkTime,
				this.wlkEducTime, this.wlkShopTime, this.wlkLeisTime,
				this.wlkHomeTime, this.wlkOtherTime };
		if (CollectionSum.getSum(wlkDestinationTime) > 0)
			barChart.addSeries("walk", wlkDestinationTime);

		double[] bikeDestinationTime = new double[] { this.bikeWorkTime,
				this.bikeEducTime, this.bikeShopTime, this.bikeLeisTime,
				this.bikeHomeTime, this.bikeOtherTime };
		if (CollectionSum.getSum(bikeDestinationTime) > 0)
			barChart.addSeries("bike", bikeDestinationTime);

		double[] othersDestinationTime = new double[] { this.othersWorkTime,
				this.othersEducTime, this.othersShopTime, this.othersLeisTime,
				this.othersHomeTime, this.othersOtherTime };
		if (CollectionSum.getSum(othersDestinationTime) > 0)
			barChart.addSeries("others", othersDestinationTime);

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
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/analysis/";
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

		DailyEnRouteTime ert = new DailyEnRouteTime(tollReader.getScheme());

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		ert.run(population);
		ert.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
