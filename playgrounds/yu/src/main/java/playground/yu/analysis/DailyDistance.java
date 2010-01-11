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
 * compute modal split of through distance
 * 
 * @author yu
 * 
 */
public class DailyDistance extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private static final String CAR = "car";
	protected double carDist, ptDist, wlkDist, bikeDist// TODO
			, othersDist;
	protected final double totalDayDistanceCounts[], carDayDistanceCounts[],
			ptDayDistanceCounts[], wlkDayDistanceCounts[],
			bikeDayDistanceCounts[], othersDayDistanceCounts[];
	protected final double wlkLegDistanceCounts[], ptLegDistanceCounts[],
			carLegDistanceCounts[], bikeLegDistanceCounts[],
			othersLegDistanceCounts[];
	protected double carWorkDist, carEducDist, carShopDist, carLeisDist,
			carOtherDist;
	protected double ptWorkDist, ptEducDist, ptShopDist, ptLeisDist,
			ptOtherDist;
	protected double wlkWorkDist, wlkEducDist, wlkShopDist, wlkLeisDist,
			wlkOtherDist;
	protected double bikeWorkDist, bikeEducDist, bikeShopDist, bikeLeisDist,
			bikeOtherDist;
	protected double othersWorkDist, othersEducDist, othersShopDist,
			othersLeisDist, othersOtherDist;
	protected double wlkHomeDist, ptHomeDist, carHomeDist, bikeHomeDist,
			othersHomeDist;
	protected int count;
	protected Person person;
	protected RoadPricingScheme toll = null;

	public DailyDistance(final RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public DailyDistance() {
		carDist = 0.0;
		ptDist = 0.0;
		wlkDist = 0.0;
		othersDist = 0.0;
		bikeDist = 0.0;

		count = 0;

		totalDayDistanceCounts = new double[101];
		carDayDistanceCounts = new double[101];
		ptDayDistanceCounts = new double[101];
		wlkDayDistanceCounts = new double[101];
		othersDayDistanceCounts = new double[101];
		bikeDayDistanceCounts = new double[101];

		carLegDistanceCounts = new double[101];
		ptLegDistanceCounts = new double[101];
		wlkLegDistanceCounts = new double[101];
		bikeLegDistanceCounts = new double[101];
		othersLegDistanceCounts = new double[101];

		carWorkDist = 0.0;
		carEducDist = 0.0;
		carShopDist = 0.0;
		carLeisDist = 0.0;
		carHomeDist = 0.0;
		carOtherDist = 0.0;

		ptWorkDist = 0.0;
		ptEducDist = 0.0;
		ptShopDist = 0.0;
		ptLeisDist = 0.0;
		ptHomeDist = 0.0;
		ptOtherDist = 0.0;

		wlkWorkDist = 0.0;
		wlkEducDist = 0.0;
		wlkShopDist = 0.0;
		wlkLeisDist = 0.0;
		wlkHomeDist = 0.0;
		wlkOtherDist = 0.0;

		bikeWorkDist = 0.0;
		bikeEducDist = 0.0;
		bikeShopDist = 0.0;
		bikeLeisDist = 0.0;
		bikeHomeDist = 0.0;
		bikeOtherDist = 0.0;

		othersWorkDist = 0.0;
		othersEducDist = 0.0;
		othersShopDist = 0.0;
		othersLeisDist = 0.0;
		othersHomeDist = 0.0;
		othersOtherDist = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = person;
		Plan plan = person.getSelectedPlan();
		if (toll == null) {
			count++;
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity().getLinkId(), toll)) {
			count++;
			run(plan);
		}
	}

	public void run(final Plan p) {
		PlanImpl plan = (PlanImpl) p;
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double wlkDayDist = 0.0;
		double bikeDayDist = 0.0;
		double othersDayDist = 0.0;
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {
				LegImpl bl = (LegImpl) pe;
				ActType at = null;
				String tmpActTypeStartsWith = plan.getNextActivity(bl)
						.getType().substring(0, 1);
				for (ActType a : ActType.values())
					if (tmpActTypeStartsWith.equals(a.getFirstLetter())) {
						at = a;
						break;
					}
				if (at == null)
					at = ActType.others;

				double dist = bl.getRoute().getDistance() / 1000.0;
				// if (bl.getDepartureTime() < 86400)
				TransportMode mode = bl.getMode();
				switch (mode) {
				case car:
					carDist += dist;
					carDayDist += dist;
					switch (at) {
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
					break;
				case pt:
					ptDist += dist;
					ptDayDist += dist;
					switch (at) {
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
					break;
				case walk:
					dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
							.getLink().getCoord(), plan.getNextActivity(bl)
							.getLink().getCoord()) * 1.5 / 1000.0;
					wlkDist += dist;
					wlkDayDist += dist;
					switch (at) {
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
					break;
				case bike:
					dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
							.getLink().getCoord(), plan.getNextActivity(bl)
							.getLink().getCoord()) / 1000.0;
					bikeDist += dist;
					bikeDayDist += dist;
					switch (at) {
					case home:
						bikeHomeDist += dist;
						break;
					case work:
						bikeWorkDist += dist;
						break;
					case education:
						bikeEducDist += dist;
						break;
					case shopping:
						bikeShopDist += dist;
						break;
					case leisure:
						bikeLeisDist += dist;
						break;
					default:
						bikeOtherDist += dist;
						break;
					}
					bikeLegDistanceCounts[Math.min(100, (int) dist)]++;
					break;
				default:
					dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
							.getLink().getCoord(), plan.getNextActivity(bl)
							.getLink().getCoord()) / 1000.0;
					othersDist += dist;
					othersDayDist += dist;
					switch (at) {
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
					break;
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
		for (int i = 0; i <= Math.min(100, (int) bikeDayDist); i++)
			bikeDayDistanceCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = carDist + ptDist + wlkDist + bikeDist + othersDist;

		double avgCarDist = carDist / count;
		double avgPtDist = ptDist / count;
		double avgWlkDist = wlkDist / count;
		double avgBikeDist = bikeDist / count;
		double avgOthersDist = othersDist / count;

		SimpleWriter sw = new SimpleWriter(outputFilename + "dailyDistance.txt");
		sw.writeln("\tDaily Distance\tn_agents\t" + count);
		sw.writeln("mode\tavg. [km]\tfraction [%]\tsum [km]");

		sw.writeln("car\t" + avgCarDist + "\t" + carDist / sum * 100.0 + "\t"
				+ carDist);
		sw.writeln("pt\t" + avgPtDist + "\t" + ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + wlkDist / sum * 100.0 + "\t"
				+ wlkDist);
		sw.writeln("bike\t" + avgBikeDist + "\t" + bikeDist / sum * 100.0
				+ "\t" + bikeDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart.addSeries(
				new String[] { CAR, "pt", "walk", "bike", "others" },
				new double[] { avgCarDist, avgPtDist, avgWlkDist, avgBikeDist,
						avgOthersDist });
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("----------------------------------------------");
		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("mode\twork\teducation\tshopping\tleisure\thome\tothers");
		sw.writeln("car\t" + carWorkDist + "\t" + carEducDist + "\t"
				+ carShopDist + "\t" + carLeisDist + "\t" + carHomeDist + "\t"
				+ carOtherDist);
		sw.writeln("pt\t" + ptWorkDist + "\t" + ptEducDist + "\t" + ptShopDist
				+ "\t" + ptLeisDist + "\t" + ptHomeDist + "\t" + ptOtherDist);
		sw.writeln("walk\t" + wlkWorkDist + "\t" + wlkEducDist + "\t"
				+ wlkShopDist + "\t" + wlkLeisDist + "\t" + wlkHomeDist + "\t"
				+ wlkOtherDist);
		sw.writeln("bike\t" + bikeWorkDist + "\t" + bikeEducDist + "\t"
				+ bikeShopDist + "\t" + bikeLeisDist + "\t" + bikeHomeDist
				+ "\t" + bikeOtherDist);
		sw.writeln("others\t" + othersWorkDist + "\t" + othersEducDist + "\t"
				+ othersShopDist + "\t" + othersLeisDist + "\t"
				+ othersHomeDist + "\t" + othersOtherDist);
		sw
				.writeln("total\t"
						+ (carWorkDist + ptWorkDist + wlkWorkDist
								+ bikeWorkDist + othersWorkDist)
						+ "\t"
						+ (carEducDist + ptEducDist + wlkEducDist
								+ bikeEducDist + othersEducDist)
						+ "\t"
						+ (carShopDist + ptShopDist + wlkShopDist
								+ bikeShopDist + othersEducDist)
						+ "\t"
						+ (carLeisDist + ptLeisDist + wlkLeisDist
								+ bikeLeisDist + othersLeisDist)
						+ "\t"
						+ (carHomeDist + ptHomeDist + wlkHomeDist
								+ bikeHomeDist + othersHomeDist)
						+ "\t"
						+ (carOtherDist + ptOtherDist + wlkOtherDist
								+ bikeOtherDist + othersOtherDist));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily distance",
				"travel destination", "daily distance [km]", new String[] {
						"work", "education", "shopping", "leisure", "home",
						"others" });
		barChart.addSeries(CAR, new double[] { carWorkDist, carEducDist,
				carShopDist, carLeisDist, carHomeDist, carOtherDist });

		double[] ptDestination = new double[] { ptWorkDist, ptEducDist,
				ptShopDist, ptLeisDist, ptHomeDist, ptOtherDist };
		if (CollectionSum.getSum(ptDestination) > 0)
			barChart.addSeries("pt", ptDestination);

		double[] wlkDestination = new double[] { wlkWorkDist, wlkEducDist,
				wlkShopDist, wlkLeisDist, wlkHomeDist, wlkOtherDist };
		if (CollectionSum.getSum(wlkDestination) > 0)
			barChart.addSeries("walk (sum of 1.5 linear distances)",
					wlkDestination);

		double[] bikeDestination = new double[] { bikeWorkDist, bikeEducDist,
				bikeShopDist, bikeLeisDist, bikeHomeDist, bikeOtherDist };
		if (CollectionSum.getSum(bikeDestination) > 0)
			barChart
					.addSeries("bike (sum of linear distances", bikeDestination);

		double[] othersDestination = new double[] { othersWorkDist,
				othersEducDist, othersShopDist, othersLeisDist, othersHomeDist,
				othersOtherDist };
		if (CollectionSum.getSum(othersDestination) > 0)
			barChart.addSeries("others (sum of linear distances)",
					othersDestination);

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
		double yBike[] = new double[101];
		double yOthers[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayDistanceCounts[i] / count * 100.0;
			yCar[i] = carDayDistanceCounts[i] / count * 100.0;
			yPt[i] = ptDayDistanceCounts[i] / count * 100.0;
			yWlk[i] = wlkDayDistanceCounts[i] / count * 100.0;
			yBike[i] = bikeDayDistanceCounts[i] / count * 100.0;
			yOthers[i] = othersDayDistanceCounts[i] / count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries(CAR, x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries("pt", x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries("walk", x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries("bike", x, yBike);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries("other", x, yOthers);
		chart.addSeries("total", x, yTotal);
		chart.saveAsPng(outputFilename + "dailyDistanceDistribution.png", 800,
				600);

		sw
				.writeln("-------------------------------------------------------------");
		sw.writeln("--Modal split -- leg distance--");
		sw
				.writeln("leg Distance [km]\tcar legs no.\tpt legs no.\twalk legs no.\tbike legs no.\tothers legs no."
						+ "\tcar fraction [%]\tpt fraction [%]\twalk fraction [%]\tbike fraction [%]\tothers fraction [%]");

		double xs[] = new double[101];
		double yCarFracs[] = new double[101];
		double yPtFracs[] = new double[101];
		double yWlkFracs[] = new double[101];
		double yBikeFracs[] = new double[101];
		double yOthersFracs[] = new double[101];
		for (int i = 0; i < 101; i++) {
			double sumLegDistanceCounts = ptLegDistanceCounts[i]
					+ carLegDistanceCounts[i] + wlkLegDistanceCounts[i]
					+ bikeLegDistanceCounts[i] + othersLegDistanceCounts[i];
			xs[i] = i;
			if (sumLegDistanceCounts > 0) {
				yCarFracs[i] = carLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yPtFracs[i] = ptLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yWlkFracs[i] = wlkLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yBikeFracs[i] = bikeLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yOthersFracs[i] = othersLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
			} else {
				yCarFracs[i] = 0;
				yPtFracs[i] = 0;
				yWlkFracs[i] = 0;
				yBikeFracs[i] = 0;
				yOthersFracs[i] = 0;
			}
			sw.writeln(i + "+\t" + carLegDistanceCounts[i] + "\t"
					+ ptLegDistanceCounts[i] + "\t" + wlkLegDistanceCounts[i]
					+ "\t" + bikeLegDistanceCounts[i] + "\t"
					+ othersLegDistanceCounts[i] + "\t" + yCarFracs[i] + "\t"
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t" + yBikeFracs[i]
					+ "\t" + yOthersFracs[i]);
		}
		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Distance",
				"leg Distance [km]", "mode fraction [%]");
		chart2.addSeries(CAR, xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries("pt", xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries("walk", xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries("bike", xs, yBikeFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries("others", xs, yOthersFracs);
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

		DailyDistance dd = new DailyDistance(tollReader.getScheme());
		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
