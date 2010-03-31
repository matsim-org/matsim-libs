/**
 * 
 */
package playground.yu.analysis.forMuc;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.DailyDistance;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.charts.StackedBarChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * daily distance analysis only for Berlin or for Berlin & Brandenburg
 * 
 * @author yu
 * 
 */
public class DailyDistance4Muc extends DailyDistance implements Analysis4Muc {
	private static final String CAR = "car", BIKE = "bike", WALK = "walk";
	private double carBusinessDist, carUnknownDist, carPrivateDist,
			carSportsDist, carFriendsDist, carPickupDist, carWithAdultDist;
	private double ptBusinessDist, ptUnknownDist, ptPrivateDist, ptSportsDist,
			ptFriendsDist, ptPickupDist, ptWithAdultDist;
	private double wlkBusinessDist, wlkUnknownDist, wlkPrivateDist,
			wlkSportsDist, wlkFriendsDist, wlkPickupDist, wlkWithAdultDist;
	private double bikeBusinessDist, bikeUnknownDist, bikePrivateDist,
			bikeSportsDist, bikeFriendsDist, bikePickupDist, bikeWithAdultDist;
	private double rideBusinessDist, rideUnknownDist, ridePrivateDist,
			rideSportsDist, rideFriendsDist, ridePickupDist, rideWithAdultDist,
			rideHomeDist, rideWorkDist, rideShopDist, rideEducDist,
			rideLeisDist, rideOtherDist;
	private double othersBusinessDist, othersUnknownDist, othersPrivateDist,
			othersSportsDist, othersFriendsDist, othersPickupDist,
			othersWithAdultDist;

	public DailyDistance4Muc(final Network network) {
		super(network);
	}

	public DailyDistance4Muc(final RoadPricingScheme toll, final Network network) {
		super(toll, network);
	}

	@Override
	public void run(final Plan plan) {
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
				String tmpActType = ((PlanImpl) plan).getNextActivity(bl)
						.getType();
				for (ActType a : ActType.values())
					if (tmpActType.equals(a.getActTypeName())) {
						at = a;
						break;
					}
				if (at == null)
					at = ActType.other;

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
					case business:
						carBusinessDist += dist;
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
					case business:
						ptBusinessDist += dist;
						break;
					default:
						ptOtherDist += dist;
						break;
					}
					ptLegDistanceCounts[Math.min(100, (int) dist)]++;
					break;
				case walk:
					dist = CoordUtils.calcDistance(this.network.getLinks().get(
							((PlanImpl) plan).getPreviousActivity(bl)
									.getLinkId()).getCoord(), this.network
							.getLinks().get(
									((PlanImpl) plan).getNextActivity(bl)
											.getLinkId()).getCoord()) * 1.5 / 1000.0;
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
					case business:
						wlkBusinessDist += dist;
						break;
					default:
						wlkOtherDist += dist;
						break;
					}
					wlkLegDistanceCounts[Math.min(100, (int) dist)]++;
					break;
				case bike:
					dist = CoordUtils.calcDistance(this.network.getLinks().get(
							((PlanImpl) plan).getPreviousActivity(bl)
									.getLinkId()).getCoord(), this.network
							.getLinks().get(
									((PlanImpl) plan).getNextActivity(bl)
											.getLinkId()).getCoord()) / 1000.0;
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
					case business:
						bikeBusinessDist += dist;
						break;
					default:
						bikeOtherDist += dist;
						break;
					}
					bikeLegDistanceCounts[Math.min(100, (int) dist)]++;
					break;
				default:
					dist = CoordUtils.calcDistance(this.network.getLinks().get(
							((PlanImpl) plan).getPreviousActivity(bl)
									.getLinkId()).getCoord(), this.network
							.getLinks().get(
									((PlanImpl) plan).getNextActivity(bl)
											.getLinkId()).getCoord()) / 1000.0;
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
					case business:
						othersBusinessDist += dist;
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

	@Override
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
		pieChart.addSeries(new String[] { CAR, "pt", WALK, BIKE, "others" },
				new double[] { avgCarDist, avgPtDist, avgWlkDist, avgBikeDist,
						avgOthersDist });
		pieChart.addMatsimLogo();
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("----------------------------------------------");
		sw.writeln("--travel destination and modal split--daily distance--");
		sw
				.writeln("mode\thome\twork\tshopping\teducation\tleisure\tother\t"
						+ "unknown\tbusiness\tprivate\tsports\tfriends\tpickup\twith_adult");

		sw.writeln("car\t" + carHomeDist + "\t" + carWorkDist + "\t"
				+ carShopDist + "\t" + carEducDist + "\t" + carLeisDist + "\t"
				+ carOtherDist + "\t" + carUnknownDist + "\t" + carBusinessDist
				+ "\t" + carPrivateDist + "\t" + carSportsDist + "\t"
				+ carFriendsDist + "\t" + carPickupDist + "\t"
				+ carWithAdultDist);
		sw.writeln("pt\t" + ptHomeDist + "\t" + ptWorkDist + "\t" + ptShopDist
				+ "\t" + ptEducDist + "\t" + ptLeisDist + "\t" + ptOtherDist
				+ "\t" + ptUnknownDist + "\t" + ptBusinessDist + "\t"
				+ ptPrivateDist + "\t" + ptSportsDist + "\t" + ptFriendsDist
				+ "\t" + ptPickupDist + "\t" + ptWithAdultDist);
		sw.writeln("wlk\t" + wlkHomeDist + "\t" + wlkWorkDist + "\t"
				+ wlkShopDist + "\t" + wlkEducDist + "\t" + wlkLeisDist + "\t"
				+ wlkOtherDist + "\t" + wlkUnknownDist + "\t" + wlkBusinessDist
				+ "\t" + wlkPrivateDist + "\t" + wlkSportsDist + "\t"
				+ wlkFriendsDist + "\t" + wlkPickupDist + "\t"
				+ wlkWithAdultDist);
		sw.writeln("bike\t" + bikeHomeDist + "\t" + bikeWorkDist + "\t"
				+ bikeShopDist + "\t" + bikeEducDist + "\t" + bikeLeisDist
				+ "\t" + bikeOtherDist + "\t" + bikeUnknownDist + "\t"
				+ bikeBusinessDist + "\t" + bikePrivateDist + "\t"
				+ bikeSportsDist + "\t" + bikeFriendsDist + "\t"
				+ bikePickupDist + "\t" + bikeWithAdultDist);
		sw.writeln("ride\t" + rideHomeDist + "\t" + rideWorkDist + "\t"
				+ rideShopDist + "\t" + rideEducDist + "\t" + rideLeisDist
				+ "\t" + rideOtherDist + "\t" + rideUnknownDist + "\t"
				+ rideBusinessDist + "\t" + ridePrivateDist + "\t"
				+ rideSportsDist + "\t" + rideFriendsDist + "\t"
				+ ridePickupDist + "\t" + rideWithAdultDist);
		sw.writeln("others\t" + othersHomeDist + "\t" + othersWorkDist + "\t"
				+ othersShopDist + "\t" + othersEducDist + "\t"
				+ othersLeisDist + "\t" + othersOtherDist + "\t"
				+ othersUnknownDist + "\t" + othersBusinessDist + "\t"
				+ othersPrivateDist + "\t" + othersSportsDist + "\t"
				+ othersFriendsDist + "\t" + othersPickupDist + "\t"
				+ othersWithAdultDist);

		sw
				.writeln("total\t"
						+ (carHomeDist + ptHomeDist + wlkHomeDist
								+ bikeHomeDist + rideHomeDist + othersHomeDist)
						+ "\t"
						+ (carWorkDist + ptWorkDist + wlkWorkDist
								+ bikeWorkDist + rideWorkDist + othersWorkDist)
						+ "\t"
						+ (carShopDist + ptShopDist + wlkShopDist
								+ bikeShopDist + rideShopDist + othersEducDist)
						+ "\t"
						+ (carEducDist + ptEducDist + wlkEducDist
								+ bikeEducDist + rideEducDist + othersEducDist)
						+ "\t"
						+ (carLeisDist + ptLeisDist + wlkLeisDist
								+ bikeLeisDist + rideLeisDist + othersLeisDist)
						+ "\t"
						+ (carOtherDist + ptOtherDist + wlkOtherDist
								+ bikeOtherDist + rideOtherDist + othersOtherDist)
						+ "\t"

						+ (carUnknownDist + ptUnknownDist + wlkUnknownDist
								+ bikeUnknownDist + rideUnknownDist + othersUnknownDist)
						+ "\t"
						+ (carBusinessDist + ptBusinessDist + wlkBusinessDist
								+ bikeBusinessDist + rideBusinessDist + othersBusinessDist)
						+ "\t"
						+ (carPrivateDist + ptPrivateDist + wlkPrivateDist
								+ bikePrivateDist + ridePrivateDist + othersPrivateDist)
						+ "\t"
						+ (carSportsDist + ptSportsDist + wlkSportsDist
								+ bikeSportsDist + rideSportsDist + othersSportsDist)
						+ "\t"
						+ (carFriendsDist + ptFriendsDist + wlkFriendsDist
								+ bikeFriendsDist + rideFriendsDist + othersFriendsDist)
						+ "\t"
						+ (carPickupDist + ptPickupDist + wlkPickupDist
								+ bikePickupDist + ridePickupDist + othersPickupDist)
						+ "\t"
						+ (carWithAdultDist + ptWithAdultDist
								+ wlkWithAdultDist + bikeWithAdultDist
								+ rideWithAdultDist + othersWithAdultDist));

		StackedBarChart stackedBarChart = new StackedBarChart(
				"travel destination and modal split--daily distance",
				"travel destinations", "daily distances [km]",
				PlotOrientation.VERTICAL);
		stackedBarChart.addSeries(new String[] { CAR, "pt", WALK, BIKE,
				"others" }, new String[] { "home", "work", "shopping",
				"education", "leisure", "other", "not specified", "business",
				"Einkauf sonstiges", "Freizeit (sonstiges incl.Sport)",
				"see a doctor", "holiday / journey", "multiple" },
				new double[][] {

				// { carHomeDist, carWorkDist, carShopDist, carEducDist,
				// carLeisDist, carOtherDist, carNotSpecifiedDist,
				// carBusinessDist, carPrivateDist,
				// carSportsDist, carFriendsDist,
				// carPickupDist, carWithAdultDist },
				// { ptHomeDist, ptWorkDist, ptShopDist, ptEducDist,
				// ptLeisDist, ptOtherDist, ptNotSpecifiedDist,
				// ptBusinessDist, ptPrivateDist,
				// ptSportsDist, ptFriendsDist,
				// ptPickupDist, ptWithAdultDist },
				// { wlkHomeDist, wlkWorkDist, wlkShopDist, wlkEducDist,
				// wlkLeisDist, wlkOtherDist, wlkNotSpecifiedDist,
				// wlkBusinessDist, wlkPrivateDist,
				// wlkSportsDist, wlkFriendsDist,
				// wlkPickupDist, wlkWithAdultDist },
				// { bikeHomeDist, bikeWorkDist, bikeShopDist,
				// bikeEducDist, bikeLeisDist, bikeOtherDist,
				// bikeNotSpecifiedDist, bikeBusinessDist,
				// bikePrivateDist,
				// bikeSportsDist, bikeFriendsDist,
				// bikePickupDist, bikeWithAdultDist },
				// { othersHomeDist, othersWorkDist, othersShopDist,
				// othersEducDist, othersLeisDist,
				// othersOtherDist, othersNotSpecifiedDist,
				// othersBusinessDist, othersPrivateDist,
				// othersSportsDist,
				// othersFriendsDist, othersPickupDist,
				// othersWithAdultDist }
				});

		stackedBarChart.addMatsimLogo();
		stackedBarChart.saveAsPng(outputFilename
				+ "dailyDistanceTravelDistination.png", 1280, 1024);

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
			chart.addSeries(WALK, x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries(BIKE, x, yBike);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries("other", x, yOthers);
		chart.addSeries("total", x, yTotal);
		chart.addMatsimLogo();
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
			chart2.addSeries(WALK, xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries(BIKE, xs, yBikeFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries("others", xs, yOthersFracs);
		chart2.addMatsimLogo();
		chart2.saveAsPng(outputFilename + "legDistanceModalSplit2.png", 800,
				600);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "..";
		final String plansFilename = "..";
		String outputFilename = "..";
		// String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		// Gbl.createConfig(null);

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

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

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyDistance4Muc dd = new DailyDistance4Muc(null);
		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
