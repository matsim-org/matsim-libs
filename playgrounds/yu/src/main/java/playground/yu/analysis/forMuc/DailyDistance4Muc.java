/**
 *
 */
package playground.yu.analysis.forMuc;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.CalcRouteDistance;
import playground.yu.analysis.DailyDistance;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.charts.StackedBarChart;
import playground.yu.utils.container.CollectionSum;
import playground.yu.utils.io.SimpleWriter;

/**
 * daily distance analysis only for Berlin or for Berlin & Brandenburg
 *
 * @author yu
 *
 */
public class DailyDistance4Muc extends DailyDistance implements Analysis4Muc {
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
	private int[] rideLegDistanceCounts, rideDayDistanceCounts;
	private double rideDist;

	public DailyDistance4Muc(final Network network) {
		super(network);
		this.rideDist = 0.0;

		this.rideDayDistanceCounts = new int[101];
		this.rideLegDistanceCounts = new int[101];

		this.carUnknownDist = 0d;
		this.carBusinessDist = 0d;
		this.carPrivateDist = 0d;
		this.carSportsDist = 0d;
		this.carFriendsDist = 0d;
		this.carPickupDist = 0d;
		this.carWithAdultDist = 0d;

		this.ptUnknownDist = 0d;
		this.ptBusinessDist = 0d;
		this.ptPrivateDist = 0d;
		this.ptSportsDist = 0d;
		this.ptFriendsDist = 0d;
		this.ptPickupDist = 0d;
		this.ptWithAdultDist = 0d;

		this.wlkUnknownDist = 0d;
		this.wlkBusinessDist = 0d;
		this.wlkPrivateDist = 0d;
		this.wlkSportsDist = 0d;
		this.wlkFriendsDist = 0d;
		this.wlkPickupDist = 0d;
		this.wlkWithAdultDist = 0d;

		this.bikeUnknownDist = 0d;
		this.bikeBusinessDist = 0d;
		this.bikePrivateDist = 0d;
		this.bikeSportsDist = 0d;
		this.bikeFriendsDist = 0d;
		this.bikePickupDist = 0d;
		this.bikeWithAdultDist = 0d;

		rideWorkDist = 0.0;
		rideEducDist = 0.0;
		rideShopDist = 0.0;
		rideLeisDist = 0.0;
		rideHomeDist = 0.0;
		rideOtherDist = 0.0;

		this.rideUnknownDist = 0d;
		this.rideBusinessDist = 0d;
		this.ridePrivateDist = 0d;
		this.rideSportsDist = 0d;
		this.rideFriendsDist = 0d;
		this.ridePickupDist = 0d;
		this.rideWithAdultDist = 0d;

		this.othersUnknownDist = 0d;
		this.othersBusinessDist = 0d;
		this.othersPrivateDist = 0d;
		this.othersSportsDist = 0d;
		this.othersFriendsDist = 0d;
		this.othersPickupDist = 0d;
		this.othersWithAdultDist = 0d;
	}

	public DailyDistance4Muc(final RoadPricingScheme toll, final Network network) {
		this(network);
		this.toll = toll;
	}

	@Override
	protected ActType getLegIntent(PlanImpl plan, Leg currentLeg) {
		ActType intent = null;
		String tmpActType = plan.getNextActivity(currentLeg).getType();
		for (ActTypeMuc actType : ActTypeMuc.values())
			if (tmpActType.equals(actType.getActTypeName())) {
				intent = actType;
				break;
			}
		if (intent == null)
			intent = ActTypeMuc.other;
		return intent;
	}

	@Override
	public void run(final Plan plan) {
		double dayDist = 0.0, carDayDist = 0.0, ptDayDist = 0.0, wlkDayDist = 0.0, bikeDayDist = 0.0, rideDayDist = 0.0, othersDayDist = 0.0;
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof Leg) {

				Leg bl = (Leg) pe;
				ActTypeMuc legIntent = (ActTypeMuc) this.getLegIntent(
						(PlanImpl) plan, bl);
				Route route = bl.getRoute();

				double dist;
				if (route != null)
					dist/* [km] */= CalcRouteDistance.getRouteDistance(route,
							network) / 1000.0;
				else {
					dist/* [km] */= CoordUtils.calcDistance(this.network
							.getLinks().get(
									((PlanImpl) plan).getPreviousActivity(bl)
											.getLinkId()).getCoord(),
							this.network.getLinks().get(
									((PlanImpl) plan).getNextActivity(bl)
											.getLinkId()).getCoord()) * 1.5 / 1000.0;
				}

				String mode = bl.getMode();
				if (TransportMode.car.equals(mode)) {
					carDist += dist;
					carDayDist += dist;
					switch (legIntent) {
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
					case unknown:
						carUnknownDist += dist;
						break;
					case private_:
						carPrivateDist += dist;
						break;
					case sports:
						carSportsDist += dist;
						break;
					case friends:
						carFriendsDist += dist;
						break;
					case pickup:
						carPickupDist += dist;
						break;
					case with_adult:
						carWithAdultDist += dist;
						break;
					default:
						carOtherDist += dist;
						break;
					}
					carLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (TransportMode.pt.equals(mode)) {
					ptDist += dist;
					ptDayDist += dist;
					switch (legIntent) {
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
					case unknown:
						ptUnknownDist += dist;
						break;
					case private_:
						ptPrivateDist += dist;
						break;
					case sports:
						ptSportsDist += dist;
						break;
					case friends:
						ptFriendsDist += dist;
						break;
					case pickup:
						ptPickupDist += dist;
						break;
					case with_adult:
						ptWithAdultDist += dist;
						break;
					default:
						ptOtherDist += dist;
						break;
					}
					ptLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (TransportMode.walk.equals(mode)) {
					wlkDist += dist;
					wlkDayDist += dist;
					switch (legIntent) {
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
					case unknown:
						wlkUnknownDist += dist;
						break;
					case private_:
						wlkPrivateDist += dist;
						break;
					case sports:
						wlkSportsDist += dist;
						break;
					case friends:
						wlkFriendsDist += dist;
						break;
					case pickup:
						wlkPickupDist += dist;
						break;
					case with_adult:
						wlkWithAdultDist += dist;
						break;
					default:
						wlkOtherDist += dist;
						break;
					}
					wlkLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (TransportMode.bike.equals(mode)) {
					bikeDist += dist;
					bikeDayDist += dist;
					switch (legIntent) {
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
					case unknown:
						bikeUnknownDist += dist;
						break;
					case private_:
						bikePrivateDist += dist;
						break;
					case sports:
						bikeSportsDist += dist;
						break;
					case friends:
						bikeFriendsDist += dist;
						break;
					case pickup:
						bikePickupDist += dist;
						break;
					case with_adult:
						bikeWithAdultDist += dist;
						break;
					default:
						bikeOtherDist += dist;
						break;
					}
					bikeLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else if (TransportMode.ride.equals(mode)) {
					rideDist += dist;
					rideDayDist += dist;
					switch (legIntent) {
					case home:
						rideHomeDist += dist;
						break;
					case work:
						rideWorkDist += dist;
						break;
					case education:
						rideEducDist += dist;
						break;
					case shopping:
						rideShopDist += dist;
						break;
					case leisure:
						rideLeisDist += dist;
						break;
					case business:
						rideBusinessDist += dist;
						break;
					case unknown:
						rideUnknownDist += dist;
						break;
					case private_:
						ridePrivateDist += dist;
						break;
					case sports:
						rideSportsDist += dist;
						break;
					case friends:
						rideFriendsDist += dist;
						break;
					case pickup:
						ridePickupDist += dist;
						break;
					case with_adult:
						rideWithAdultDist += dist;
						break;
					default:
						rideOtherDist += dist;
						break;
					}
					rideLegDistanceCounts[Math.min(100, (int) dist)]++;
				} else {
					othersDist += dist;
					othersDayDist += dist;
					switch (legIntent) {
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
					case unknown:
						othersUnknownDist += dist;
						break;
					case private_:
						othersPrivateDist += dist;
						break;
					case sports:
						othersSportsDist += dist;
						break;
					case friends:
						othersFriendsDist += dist;
						break;
					case pickup:
						othersPickupDist += dist;
						break;
					case with_adult:
						othersWithAdultDist += dist;
						break;
					default:
						othersOtherDist += dist;
						break;
					}
					othersLegDistanceCounts[Math.min(100, (int) dist)]++;
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
		for (int i = 0; i <= Math.min(100, (int) rideDayDist); i++)
			rideDayDistanceCounts[i]++;
	}

	@Override
	public void write(final String outputFilename) {
		double sum = carDist + ptDist + wlkDist + bikeDist + rideDist
				+ othersDist;

		double avgCarDist = carDist / count, //
		avgPtDist = ptDist / count, //
		avgWlkDist = wlkDist / count, //
		avgBikeDist = bikeDist / count, //
		avgRideDist = rideDist / count, //
		avgOthersDist = othersDist / count;

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
		sw.writeln("ride\t" + avgRideDist + "\t" + rideDist / sum * 100.0
				+ "\t" + rideDist);
		sw.writeln("others\t" + avgOthersDist + "\t" + othersDist / sum * 100.0
				+ "\t" + othersDist);

		PieChart pieChart = new PieChart("Avg. Daily Distance -- Modal Split");
		pieChart.addSeries(new String[] { CAR, PT, WALK, BIKE, RIDE, OTHERS },
				new double[] { avgCarDist, avgPtDist, avgWlkDist, avgBikeDist,
						avgRideDist, avgOthersDist });
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
		stackedBarChart
				.addSeries(new String[] { CAR, PT, WALK, BIKE, RIDE, OTHERS },
						new String[] { "home", "work", "shopping", "education",
								"leisure", "other", "unknown", "business",
								"private", "sports", "friends", "pickup",
								"with adult" }, new double[][] {

								{ carHomeDist, carWorkDist, carShopDist,
										carEducDist, carLeisDist, carOtherDist,
										carUnknownDist, carBusinessDist,
										carPrivateDist, carSportsDist,
										carFriendsDist, carPickupDist,
										carWithAdultDist },

								{ ptHomeDist, ptWorkDist, ptShopDist,
										ptEducDist, ptLeisDist, ptOtherDist,
										ptUnknownDist, ptBusinessDist,
										ptPrivateDist, ptSportsDist,
										ptFriendsDist, ptPickupDist,
										ptWithAdultDist },

								{ wlkHomeDist, wlkWorkDist, wlkShopDist,
										wlkEducDist, wlkLeisDist, wlkOtherDist,
										wlkUnknownDist, wlkBusinessDist,
										wlkPrivateDist, wlkSportsDist,
										wlkFriendsDist, wlkPickupDist,
										wlkWithAdultDist },

								{ bikeHomeDist, bikeWorkDist, bikeShopDist,
										bikeEducDist, bikeLeisDist,
										bikeOtherDist, bikeUnknownDist,
										bikeBusinessDist, bikePrivateDist,
										bikeSportsDist, bikeFriendsDist,
										bikePickupDist, bikeWithAdultDist },

								{ rideHomeDist, rideWorkDist, rideShopDist,
										rideEducDist, rideLeisDist,
										rideOtherDist, rideUnknownDist,
										rideBusinessDist, ridePrivateDist,
										rideSportsDist, rideFriendsDist,
										ridePickupDist, rideWithAdultDist },

								{ othersHomeDist, othersWorkDist,
										othersShopDist, othersEducDist,
										othersLeisDist, othersOtherDist,
										othersUnknownDist, othersBusinessDist,
										othersPrivateDist, othersSportsDist,
										othersFriendsDist, othersPickupDist,
										othersWithAdultDist }

						});
		stackedBarChart.addMatsimLogo();
		stackedBarChart.saveAsPng(outputFilename
				+ "dailyDistanceTravelDistination.png", 1280, 1024);

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101], //
		yCar[] = new double[101], //
		yPt[] = new double[101], //
		yWlk[] = new double[101], //
		yBike[] = new double[101], //
		yRide[] = new double[101], //
		yOthers[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayDistanceCounts[i] / count * 100.0;
			yCar[i] = carDayDistanceCounts[i] / count * 100.0;
			yPt[i] = ptDayDistanceCounts[i] / count * 100.0;
			yWlk[i] = wlkDayDistanceCounts[i] / count * 100.0;
			yBike[i] = bikeDayDistanceCounts[i] / count * 100.0;
			yRide[i] = rideDayDistanceCounts[i] / count * 100.0;
			yOthers[i] = othersDayDistanceCounts[i] / count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily Distance Distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries(CAR, x, yCar);
		if (CollectionSum.getSum(yPt) > 0)
			chart.addSeries(PT, x, yPt);
		if (CollectionSum.getSum(yWlk) > 0)
			chart.addSeries(WALK, x, yWlk);
		if (CollectionSum.getSum(yBike) > 0)
			chart.addSeries(BIKE, x, yBike);
		if (CollectionSum.getSum(yRide) > 0)
			chart.addSeries(RIDE, x, yRide);
		if (CollectionSum.getSum(yOthers) > 0)
			chart.addSeries(OTHERS, x, yOthers);
		chart.addSeries("total", x, yTotal);
		chart.addMatsimLogo();
		chart.saveAsPng(outputFilename + "dailyDistanceDistribution.png", 800,
				600);

		sw
				.writeln("-------------------------------------------------------------");
		sw.writeln("--Modal split -- leg distance--");
		sw
				.writeln("leg Distance [km]\tcar legs no.\tpt legs no.\twalk legs no.\tbike legs no.\tride legs no.\tothers legs no."
						+ "\tcar fraction [%]\tpt fraction [%]\twalk fraction [%]\tbike fraction [%]\tride fraction [%]\tothers fraction [%]");

		double xs[] = new double[101];
		double yCarFracs[] = new double[101];
		double yPtFracs[] = new double[101];
		double yWlkFracs[] = new double[101];
		double yBikeFracs[] = new double[101];
		double yRideFracs[] = new double[101];
		double yOthersFracs[] = new double[101];

		for (int i = 0; i < 101; i++) {
			double sumLegDistanceCounts = ptLegDistanceCounts[i]
					+ carLegDistanceCounts[i] + wlkLegDistanceCounts[i]
					+ bikeLegDistanceCounts[i] + rideLegDistanceCounts[i]
					+ othersLegDistanceCounts[i];
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
				yRideFracs[i] = rideLegDistanceCounts[i] / sumLegDistanceCounts
						* 100.0;
				yOthersFracs[i] = othersLegDistanceCounts[i]
						/ sumLegDistanceCounts * 100.0;
			} else {
				yCarFracs[i] = 0;
				yPtFracs[i] = 0;
				yWlkFracs[i] = 0;
				yBikeFracs[i] = 0;
				yRideFracs[i] = 0;
				yOthersFracs[i] = 0;
			}
			sw.writeln(i + "+\t" + carLegDistanceCounts[i] + "\t"
					+ ptLegDistanceCounts[i] + "\t" + wlkLegDistanceCounts[i]
					+ "\t" + bikeLegDistanceCounts[i] + "\t"
					+ rideLegDistanceCounts[i] + "\t"
					+ othersLegDistanceCounts[i] + "\t" + yCarFracs[i] + "\t"
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t" + yBikeFracs[i]
					+ "\t" + yRideFracs[i] + "\t" + yOthersFracs[i]);
		}

		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Distance",
				"leg Distance [km]", "mode fraction [%]");
		chart2.addSeries(CAR, xs, yCarFracs);
		if (CollectionSum.getSum(yPtFracs) > 0)
			chart2.addSeries(PT, xs, yPtFracs);
		if (CollectionSum.getSum(yWlkFracs) > 0)
			chart2.addSeries(WALK, xs, yWlkFracs);
		if (CollectionSum.getSum(yBikeFracs) > 0)
			chart2.addSeries(BIKE, xs, yBikeFracs);
		if (CollectionSum.getSum(yRideFracs) > 0)
			chart2.addSeries(RIDE, xs, yRideFracs);
		if (CollectionSum.getSum(yOthersFracs) > 0)
			chart2.addSeries(OTHERS, xs, yOthersFracs);
		chart2.addMatsimLogo();
		chart2.saveAsPng(outputFilename + "legDistanceModalSplit2.png", 800,
				600);
		sw.close();
	}

	public static void main(final String[] args) {
		for (int i = 0; i < 10; i++)
			System.out.println("----->output-Test");
		final String netFilename = "../detailedEval/data/network.xml.gz", //
		plansFilename = "../../run950/output/950.output_plans.xml.gz", //
		outputFilename = "../detailedEval/test/", //
		tollFilename = "../detailedEval/data/boundary.xml";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseRoadpricing(true);

		new MatsimNetworkReader(scenario).readFile(netFilename);
		RoadPricingScheme toll = scenario.getRoadPricingScheme();
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(toll);
		tollReader.parse(tollFilename);

		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyDistance4Muc dd = new DailyDistance4Muc(toll, scenario.getNetwork());
		dd.run(scenario.getPopulation());
		dd.write(outputFilename);

		System.out.println("--> Done!");
		System.exit(0);
	}
}
