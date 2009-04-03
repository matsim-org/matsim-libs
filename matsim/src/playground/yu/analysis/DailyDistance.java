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
	protected double carDist, ptDist, wlkDist, bikeDist// TODO
			, othersDist;

	protected final double totalDayDistanceCounts[], carDayDistanceCounts[],
			ptDayDistanceCounts[], wlkDayDistanceCounts[],
			bikeDayDistanceCounts[]// TODO
			, othersDayDistanceCounts[];

	protected final double wlkLegDistanceCounts[], ptLegDistanceCounts[],
			carLegDistanceCounts[], bikeLegDistanceCounts[],
			othersLegDistanceCounts[];// TODO

	protected double carWorkDist, carEducDist, carShopDist, carLeisDist,
			carOtherDist;

	protected double ptWorkDist, ptEducDist, ptShopDist, ptLeisDist,
			ptOtherDist;

	protected double wlkWorkDist, wlkEducDist, wlkShopDist, wlkLeisDist,
			wlkOtherDist;

	protected double bikeWorkDist, bikeEducDist, bikeShopDist, bikeLeisDist,
			bikeOtherDist;// TODO

	protected double othersWorkDist, othersEducDist, othersShopDist,
			othersLeisDist, othersOtherDist;// TODO

	protected double wlkHomeDist, ptHomeDist, carHomeDist, bikeHomeDist,
			othersHomeDist;// TODO

	protected int count;

	protected Person person;

	protected RoadPricingScheme toll = null;

	public DailyDistance(RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	public DailyDistance() {
		this.carDist = 0.0;
		this.ptDist = 0.0;
		wlkDist = 0.0;
		this.othersDist = 0.0;
		bikeDist = 0.0;

		this.count = 0;

		this.totalDayDistanceCounts = new double[101];
		this.carDayDistanceCounts = new double[101];
		this.ptDayDistanceCounts = new double[101];
		wlkDayDistanceCounts = new double[101];
		this.othersDayDistanceCounts = new double[101];
		bikeDayDistanceCounts = new double[101];

		this.carLegDistanceCounts = new double[101];
		this.ptLegDistanceCounts = new double[101];
		wlkLegDistanceCounts = new double[101];
		bikeLegDistanceCounts = new double[101];
		othersLegDistanceCounts = new double[101];

		this.carWorkDist = 0.0;
		this.carEducDist = 0.0;
		this.carShopDist = 0.0;
		this.carLeisDist = 0.0;
		this.carHomeDist = 0.0;
		this.carOtherDist = 0.0;

		this.ptWorkDist = 0.0;
		this.ptEducDist = 0.0;
		this.ptShopDist = 0.0;
		this.ptLeisDist = 0.0;
		this.ptHomeDist = 0.0;
		this.ptOtherDist = 0.0;

		this.wlkWorkDist = 0.0;
		this.wlkEducDist = 0.0;
		this.wlkShopDist = 0.0;
		this.wlkLeisDist = 0.0;
		this.wlkHomeDist = 0.0;
		this.wlkOtherDist = 0.0;

		this.bikeWorkDist = 0.0;
		this.bikeEducDist = 0.0;
		this.bikeShopDist = 0.0;
		this.bikeLeisDist = 0.0;
		this.bikeHomeDist = 0.0;
		this.bikeOtherDist = 0.0;

		this.othersWorkDist = 0.0;
		this.othersEducDist = 0.0;
		this.othersShopDist = 0.0;
		this.othersLeisDist = 0.0;
		this.othersHomeDist = 0.0;
		this.othersOtherDist = 0.0;
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
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double wlkDayDist = 0.0;
		double bikeDayDist = 0.0;
		double othersDayDist = 0.0;
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

			double dist = bl.getRoute().getDistance() / 1000.0;
			// if (bl.getDepartureTime() < 86400)
			Mode mode = bl.getMode();
			switch (mode) {
			case car:
				this.carDist += dist;
				carDayDist += dist;
				switch (at) {
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
				break;
			case pt:
				this.ptDist += dist;
				ptDayDist += dist;
				switch (at) {
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
				break;
			case walk:
				dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
						.getLink().getCoord(), plan.getNextActivity(bl)
						.getLink().getCoord()) * 1.5 / 1000.0;
				this.wlkDist += dist;
				wlkDayDist += dist;
				switch (at) {
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
				break;
			case bike:
				dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
						.getLink().getCoord(), plan.getNextActivity(bl)
						.getLink().getCoord()) / 1000.0;
				this.bikeDist += dist;
				bikeDayDist += dist;
				switch (at) {
				case home:
					this.bikeHomeDist += dist;
					break;
				case work:
					this.bikeWorkDist += dist;
					break;
				case education:
					this.bikeEducDist += dist;
					break;
				case shopping:
					this.bikeShopDist += dist;
					break;
				case leisure:
					this.bikeLeisDist += dist;
					break;
				default:
					this.bikeOtherDist += dist;
					break;
				}
				this.bikeLegDistanceCounts[Math.min(100, (int) dist)]++;
				break;
			default:
				dist = CoordUtils.calcDistance(plan.getPreviousActivity(bl)
						.getLink().getCoord(), plan.getNextActivity(bl)
						.getLink().getCoord()) / 1000.0;
				this.othersDist += dist;
				othersDayDist += dist;
				switch (at) {
				case home:
					this.othersHomeDist += dist;
					break;
				case work:
					this.othersWorkDist += dist;
					break;
				case education:
					this.othersEducDist += dist;
					break;
				case shopping:
					this.othersShopDist += dist;
					break;
				case leisure:
					this.othersLeisDist += dist;
					break;
				default:
					this.othersOtherDist += dist;
					break;
				}
				this.othersLegDistanceCounts[Math.min(100, (int) dist)]++;
				break;
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
		for (int i = 0; i <= Math.min(100, (int) bikeDayDist); i++)
			this.bikeDayDistanceCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carDist + this.ptDist + wlkDist + bikeDist
				+ this.othersDist;

		double avgCarDist = this.carDist / (double) this.count;
		double avgPtDist = this.ptDist / (double) this.count;
		double avgWlkDist = this.wlkDist / (double) this.count;
		double avgBikeDist = this.bikeDist / (double) this.count;
		double avgOthersDist = this.othersDist / (double) this.count;

		SimpleWriter sw = new SimpleWriter(outputFilename + "dailyDistance.txt");
		sw.writeln("\tDaily Distance\tn_agents\t" + count);
		sw.writeln("mode\tavg. [km]\tfraction [%]\tsum [km]");

		sw.writeln("car\t" + avgCarDist + "\t" + this.carDist / sum * 100.0
				+ "\t" + carDist);
		sw.writeln("pt\t" + avgPtDist + "\t" + this.ptDist / sum * 100.0 + "\t"
				+ ptDist);
		sw.writeln("walk\t" + avgWlkDist + "\t" + this.wlkDist / sum * 100.0
				+ "\t" + wlkDist);
		sw.writeln("bike\t" + avgBikeDist + "\t" + this.bikeDist / sum * 100.0
				+ "\t" + bikeDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + this.othersDist / sum
				* 100.0 + "\t" + othersDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart.addSeries(
				new String[] { "car", "pt", "walk", "bike", "others" },
				new double[] { avgCarDist, avgPtDist, avgWlkDist, avgBikeDist,
						avgOthersDist });
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("----------------------------------------------");
		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("mode\twork\teducation\tshopping\tleisure\thome\tothers");
		sw.writeln("car\t" + this.carWorkDist + "\t" + this.carEducDist + "\t"
				+ this.carShopDist + "\t" + this.carLeisDist + "\t"
				+ this.carHomeDist + "\t" + this.carOtherDist);
		sw.writeln("pt\t" + this.ptWorkDist + "\t" + this.ptEducDist + "\t"
				+ this.ptShopDist + "\t" + this.ptLeisDist + "\t"
				+ this.ptHomeDist + "\t" + this.ptOtherDist);
		sw.writeln("walk\t" + this.wlkWorkDist + "\t" + this.wlkEducDist + "\t"
				+ this.wlkShopDist + "\t" + this.wlkLeisDist + "\t"
				+ this.wlkHomeDist + "\t" + this.wlkOtherDist);
		sw.writeln("bike\t" + this.bikeWorkDist + "\t" + this.bikeEducDist
				+ "\t" + this.bikeShopDist + "\t" + this.bikeLeisDist + "\t"
				+ this.bikeHomeDist + "\t" + this.bikeOtherDist);
		sw.writeln("others\t" + this.othersWorkDist + "\t" + othersEducDist
				+ "\t" + this.othersShopDist + "\t" + this.othersLeisDist
				+ "\t" + this.othersHomeDist + "\t" + this.othersOtherDist);
		sw.writeln("total\t"
				+ (this.carWorkDist + this.ptWorkDist + wlkWorkDist
						+ bikeWorkDist + othersWorkDist)
				+ "\t"
				+ (this.carEducDist + this.ptEducDist + wlkEducDist
						+ bikeEducDist + othersEducDist)
				+ "\t"
				+ (this.carShopDist + this.ptShopDist + wlkShopDist
						+ bikeShopDist + othersEducDist)
				+ "\t"
				+ (this.carLeisDist + this.ptLeisDist + wlkLeisDist
						+ bikeLeisDist + othersLeisDist)
				+ "\t"
				+ (this.carHomeDist + this.ptHomeDist + wlkHomeDist
						+ bikeHomeDist + othersHomeDist)
				+ "\t"
				+ (this.carOtherDist + this.ptOtherDist + wlkOtherDist
						+ bikeOtherDist + othersOtherDist));

		BarChart barChart = new BarChart(
				"travel destination and modal split--daily distance",
				"travel destination", "daily distance [km]", new String[] {
						"work", "education", "shopping", "leisure", "home",
						"others" });
		barChart.addSeries("car", new double[] { this.carWorkDist,
				this.carEducDist, this.carShopDist, this.carLeisDist,
				this.carHomeDist, this.carOtherDist });

		double[] ptDestination = new double[] { this.ptWorkDist,
				this.ptEducDist, this.ptShopDist, this.ptLeisDist,
				this.ptHomeDist, this.ptOtherDist };
		if (CollectionSum.getSum(ptDestination) > 0)
			barChart.addSeries("pt", ptDestination);

		double[] wlkDestination = new double[] { this.wlkWorkDist,
				this.wlkEducDist, this.wlkShopDist, this.wlkLeisDist,
				this.wlkHomeDist, this.wlkOtherDist };
		if (CollectionSum.getSum(wlkDestination) > 0)
			barChart.addSeries("walk (sum of 1.5 linear distances)",
					wlkDestination);

		double[] bikeDestination = new double[] { this.bikeWorkDist,
				this.bikeEducDist, this.bikeShopDist, this.bikeLeisDist,
				this.bikeHomeDist, this.bikeOtherDist };
		if (CollectionSum.getSum(bikeDestination) > 0)
			barChart
					.addSeries("bike (sum of linear distances", bikeDestination);

		double[] othersDestination = new double[] { this.othersWorkDist,
				this.othersEducDist, this.othersShopDist, this.othersLeisDist,
				this.othersHomeDist, this.othersOtherDist };
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
			yTotal[i] = this.totalDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yCar[i] = this.carDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yPt[i] = this.ptDayDistanceCounts[i] / (double) this.count * 100.0;
			yWlk[i] = this.wlkDayDistanceCounts[i] / (double) this.count
					* 100.0;
			yBike[i] = bikeDayDistanceCounts[i] / (double) count * 100.0;
			yOthers[i] = this.othersDayDistanceCounts[i] / (double) this.count
					* 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries("car", x, yCar);
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
			double sumLegDistanceCounts = this.ptLegDistanceCounts[i]
					+ this.carLegDistanceCounts[i] + wlkLegDistanceCounts[i]
					+ bikeLegDistanceCounts[i] + othersLegDistanceCounts[i];
			xs[i] = i;
			yCarFracs[i] = this.carLegDistanceCounts[i] / sumLegDistanceCounts
					* 100.0;
			yPtFracs[i] = this.ptLegDistanceCounts[i] / sumLegDistanceCounts
					* 100.0;
			yWlkFracs[i] = this.wlkLegDistanceCounts[i] / sumLegDistanceCounts
					* 100.0;
			yBikeFracs[i] = bikeLegDistanceCounts[i] / sumLegDistanceCounts
					* 100.0;
			yOthersFracs[i] = othersLegDistanceCounts[i] / sumLegDistanceCounts
					* 100.0;
			sw.writeln(i + "+\t" + carLegDistanceCounts[i] + "\t"
					+ ptLegDistanceCounts[i] + "\t" + wlkLegDistanceCounts[i]
					+ "\t" + bikeLegDistanceCounts[i] + "\t"
					+ othersLegDistanceCounts[i] + "\t" + yCarFracs[i] + "\t"
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t" + yBikeFracs[i] + "\t"
					+ yOthersFracs[i]);
		}
		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Distance",
				"leg Distance [km]", "mode fraction [%]");
		chart2.addSeries("car", xs, yCarFracs);
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
		DailyDistance dd = new DailyDistance(tollReader.getScheme());

		new MatsimPopulationReader(population, network).readFile(plansFilename);

		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
