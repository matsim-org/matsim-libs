/**
 *
 */
package playground.yu.analysis.forMuc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
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
public class DailyEnRouteTime4Muc extends DailyEnRouteTime implements
		Analysis4Muc {
	private double carBusinessTime, carUnknownTime, carPrivateTime,
			carSportsTime, carFriendsTime, carPickupTime, carWithAdultTime;
	private double ptBusinessTime, ptUnknownTime, ptPrivateTime, ptSportsTime,
			ptFriendsTime, ptPickupTime, ptWithAdultTime;
	private double wlkBusinessTime, wlkUnknownTime, wlkPrivateTime,
			wlkSportsTime, wlkFriendsTime, wlkPickupTime, wlkWithAdultTime;
	private double bikeBusinessTime, bikeUnknownTime, bikePrivateTime,
			bikeSportsTime, bikeFriendsTime, bikePickupTime, bikeWithAdultTime;
	private double othersBusinessTime, othersUnknownTime, othersPrivateTime,
			othersSportsTime, othersFriendsTime, othersPickupTime,
			othersWithAdultTime;
	private double rideBusinessTime, rideUnknownTime, ridePrivateTime,
			rideSportsTime, rideFriendsTime, ridePickupTime, rideWithAdultTime,
			rideHomeTime, rideWorkTime, rideShopTime, rideEducTime,
			rideLeisTime, rideOtherTime;
	private int[] rideLegTimeCounts, rideDayEnRouteTimeCounts;
	private double rideTime;

	public DailyEnRouteTime4Muc() {
		super();
		this.rideTime = 0.0;

		this.rideDayEnRouteTimeCounts = new int[101];
		this.rideLegTimeCounts = new int[101];

		this.carUnknownTime = 0d;
		this.carBusinessTime = 0d;
		this.carPrivateTime = 0d;
		this.carSportsTime = 0d;
		this.carFriendsTime = 0d;
		this.carPickupTime = 0d;
		this.carWithAdultTime = 0d;

		this.ptUnknownTime = 0d;
		this.ptBusinessTime = 0d;
		this.ptPrivateTime = 0d;
		this.ptSportsTime = 0d;
		this.ptFriendsTime = 0d;
		this.ptPickupTime = 0d;
		this.ptWithAdultTime = 0d;

		this.wlkUnknownTime = 0d;
		this.wlkBusinessTime = 0d;
		this.wlkPrivateTime = 0d;
		this.wlkSportsTime = 0d;
		this.wlkFriendsTime = 0d;
		this.wlkPickupTime = 0d;
		this.wlkWithAdultTime = 0d;

		this.bikeUnknownTime = 0d;
		this.bikeBusinessTime = 0d;
		this.bikePrivateTime = 0d;
		this.bikeSportsTime = 0d;
		this.bikeFriendsTime = 0d;
		this.bikePickupTime = 0d;
		this.bikeWithAdultTime = 0d;

		rideWorkTime = 0.0;
		rideEducTime = 0.0;
		rideShopTime = 0.0;
		rideLeisTime = 0.0;
		rideHomeTime = 0.0;
		rideOtherTime = 0.0;

		this.rideUnknownTime = 0d;
		this.rideBusinessTime = 0d;
		this.ridePrivateTime = 0d;
		this.rideSportsTime = 0d;
		this.rideFriendsTime = 0d;
		this.ridePickupTime = 0d;
		this.rideWithAdultTime = 0d;

		this.othersUnknownTime = 0d;
		this.othersBusinessTime = 0d;
		this.othersPrivateTime = 0d;
		this.othersSportsTime = 0d;
		this.othersFriendsTime = 0d;
		this.othersPickupTime = 0d;
		this.othersWithAdultTime = 0d;
	}

	public DailyEnRouteTime4Muc(final RoadPricingScheme toll) {
		this();
		this.toll = toll;
	}

	@Override
	protected ActType getLegIntent(PlanImpl plan, LegImpl currentLeg) {
		ActTypeMuc legIntent = null;
		String tmpActType = plan.getNextActivity(currentLeg).getType();
		for (ActTypeMuc a : ActTypeMuc.values())
			if (tmpActType.equals(a.getActTypeName())) {
				legIntent = a;
				break;
			}
		if (legIntent == null)
			legIntent = ActTypeMuc.other;
		return legIntent;
	}

	@Override
	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double wlkDayTime = 0.0;
		double bikeDayTime = 0.0;
		double rideDayTime = 0.0;
		double othersDayTime = 0.0;

		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {

				LegImpl bl = (LegImpl) pe;

				ActTypeMuc legIntent = (ActTypeMuc) this.getLegIntent(
						(PlanImpl) plan, bl);

				double time/* [min] */= bl.getTravelTime() / 60.0;
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
					case business:
						carBusinessTime += time;
						break;

					case unknown:
						carUnknownTime += time;
						break;
					case private_:
						carPrivateTime += time;
						break;
					case sports:
						carSportsTime += time;
						break;
					case friends:
						carFriendsTime += time;
						break;
					case pickup:
						carPickupTime += time;
						break;
					case with_adult:
						carWithAdultTime += time;
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
					case business:
						ptBusinessTime += time;
						break;

					case unknown:
						ptUnknownTime += time;
						break;
					case private_:
						ptPrivateTime += time;
						break;
					case sports:
						ptSportsTime += time;
						break;
					case friends:
						ptFriendsTime += time;
						break;
					case pickup:
						ptPickupTime += time;
						break;
					case with_adult:
						ptWithAdultTime += time;
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
					case business:
						wlkBusinessTime += time;
						break;

					case unknown:
						wlkUnknownTime += time;
						break;
					case private_:
						wlkPrivateTime += time;
						break;
					case sports:
						wlkSportsTime += time;
						break;
					case friends:
						wlkFriendsTime += time;
						break;
					case pickup:
						wlkPickupTime += time;
						break;
					case with_adult:
						wlkWithAdultTime += time;
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
					case business:
						bikeBusinessTime += time;
						break;

					case unknown:
						bikeUnknownTime += time;
						break;
					case private_:
						bikePrivateTime += time;
						break;
					case sports:
						bikeSportsTime += time;
						break;
					case friends:
						bikeFriendsTime += time;
						break;
					case pickup:
						bikePickupTime += time;
						break;
					case with_adult:
						bikeWithAdultTime += time;
						break;

					default:
						bikeOtherTime += time;
						break;
					}
					bikeLegTimeCounts[Math.min(100, (int) time / 2)]++;
					break;
				case ride:
					rideTime += time;
					rideDayTime += time;
					switch (legIntent) {
					case home:
						rideHomeTime += time;
						break;
					case work:
						rideWorkTime += time;
						break;
					case education:
						rideEducTime += time;
						break;
					case shopping:
						rideShopTime += time;
						break;
					case leisure:
						rideLeisTime += time;
						break;
					case business:
						rideBusinessTime += time;
						break;

					case unknown:
						rideUnknownTime += time;
						break;
					case private_:
						ridePrivateTime += time;
						break;
					case sports:
						rideSportsTime += time;
						break;
					case friends:
						rideFriendsTime += time;
						break;
					case pickup:
						ridePickupTime += time;
						break;
					case with_adult:
						rideWithAdultTime += time;
						break;

					default:
						rideOtherTime += time;
						break;
					}
					rideLegTimeCounts[Math.min(100, (int) time / 2)]++;
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
					case business:
						othersBusinessTime += time;
						break;

					case unknown:
						othersUnknownTime += time;
						break;
					case private_:
						othersPrivateTime += time;
						break;
					case sports:
						othersSportsTime += time;
						break;
					case friends:
						othersFriendsTime += time;
						break;
					case pickup:
						othersPickupTime += time;
						break;
					case with_adult:
						othersWithAdultTime += time;
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
		for (int i = 0; i <= Math.min(100, (int) rideDayTime); i++)
			rideDayEnRouteTimeCounts[i]++;
	}

	@Override
	public void write(final String outputFilename) {
		double sum = carTime + ptTime + othersTime + wlkTime + bikeTime
				+ rideTime;

		SimpleWriter sw = new SimpleWriter(outputFilename
				+ "dailyEnRouteTime.txt");
		sw.writeln("\tDaily En Route Time\tn_agents\t" + count);
		sw.writeln("\tavg.[min]\t%\tsum.[min]");

		double avgCarTime = carTime / count, avgPtTime = ptTime / count, avgWlkTime = wlkTime
				/ count, avgBikeTime = bikeTime / count, avgRideTime = rideTime
				/ count, avgOtherTime = othersTime / count;

		sw.writeln("car\t" + avgCarTime + "\t" + carTime / sum * 100.0 + "\t"
				+ carTime);
		sw.writeln("pt\t" + avgPtTime + "\t" + ptTime / sum * 100.0 + "\t"
				+ ptTime);
		sw.writeln("walk\t" + avgWlkTime + "\t" + wlkTime / sum * 100.0 + "\t"
				+ wlkTime);
		sw.writeln("bike\t" + avgBikeTime + "\t" + bikeTime / sum * 100.0
				+ "\t" + bikeTime);
		sw.writeln("ride\t" + avgRideTime + "\t" + rideTime / sum * 100.0
				+ "\t" + rideTime);
		sw.writeln("others\t" + avgOtherTime + "\t" + othersTime / sum * 100.0
				+ "\t" + othersTime);

		PieChart pieChart = new PieChart(
				"Avg. Daily En Route Time -- Modal Split");
		pieChart.addSeries(new String[] { CAR, PT, WALK, BIKE, RIDE, OTHERS },
				new double[] { avgCarTime, avgPtTime, avgWlkTime, avgBikeTime,
						avgRideTime, avgOtherTime });
		pieChart.addMatsimLogo();
		pieChart.saveAsPng(
				outputFilename + "dailyEnRouteTimeModalSplitPie.png", 800, 600);

		sw
				.writeln("--travel destination and modal split--daily on route time--");
		sw
				.writeln("mode\thome\twork\tshopping\teducation\tleisure\tother"
						+ "\tpick_up\tbusiness\tunknown\tprivate\twith_adult\tsports\tfriends");
		sw.writeln("car\t" + carHomeTime + "\t" + carWorkTime + "\t"
				+ carShopTime + "\t" + carEducTime + "\t" + carLeisTime + "\t"
				+ carOtherTime + "\t" + carPickupTime + "\t" + carBusinessTime
				+ "\t" + carUnknownTime + "\t" + carPrivateTime + "\t"
				+ carWithAdultTime + "\t" + carSportsTime + "\t"
				+ carFriendsTime);
		sw.writeln("pt\t" + ptHomeTime + "\t" + ptWorkTime + "\t" + ptShopTime
				+ "\t" + ptEducTime + "\t" + ptLeisTime + "\t" + ptOtherTime
				+ "\t" + ptPickupTime + "\t" + ptBusinessTime + "\t"
				+ ptUnknownTime + "\t" + ptPrivateTime + "\t" + ptWithAdultTime
				+ "\t" + ptSportsTime + "\t" + ptFriendsTime);
		sw.writeln("walk\t" + wlkHomeTime + "\t" + wlkWorkTime + "\t"
				+ wlkShopTime + "\t" + wlkEducTime + "\t" + wlkLeisTime + "\t"
				+ wlkOtherTime + "\t" + wlkPickupTime + "\t" + wlkBusinessTime
				+ "\t" + wlkUnknownTime + "\t" + wlkPrivateTime + "\t"
				+ wlkWithAdultTime + "\t" + wlkSportsTime + "\t"
				+ wlkFriendsTime);
		sw.writeln("bike\t" + bikeHomeTime + "\t" + bikeWorkTime + "\t"
				+ bikeShopTime + "\t" + bikeEducTime + "\t" + bikeLeisTime
				+ "\t" + bikeOtherTime + "\t" + bikePickupTime + "\t"
				+ bikeBusinessTime + "\t" + bikeUnknownTime + "\t"
				+ bikePrivateTime + "\t" + bikeWithAdultTime + "\t"
				+ bikeSportsTime + "\t" + bikeFriendsTime);
		sw.writeln("ride\t" + rideHomeTime + "\t" + rideWorkTime + "\t"
				+ rideShopTime + "\t" + rideEducTime + "\t" + rideLeisTime
				+ "\t" + rideOtherTime + "\t" + ridePickupTime + "\t"
				+ rideBusinessTime + "\t" + rideUnknownTime + "\t"
				+ ridePrivateTime + "\t" + rideWithAdultTime + "\t"
				+ rideSportsTime + "\t" + rideFriendsTime);
		sw.writeln("others\t" + othersHomeTime + "\t" + othersWorkTime + "\t"
				+ othersShopTime + "\t" + othersEducTime + "\t"
				+ othersLeisTime + "\t" + othersOtherTime + "\t"
				+ othersPickupTime + "\t" + othersBusinessTime + "\t"
				+ othersUnknownTime + "\t" + othersPrivateTime + "\t"
				+ othersWithAdultTime + "\t" + othersSportsTime + "\t"
				+ othersFriendsTime);

		sw
				.writeln("total\t"
						+ (carHomeTime + ptHomeTime + wlkHomeTime
								+ bikeHomeTime + rideHomeTime + othersHomeTime)
						+ "\t"
						+ (carWorkTime + ptWorkTime + wlkWorkTime
								+ bikeWorkTime + rideWorkTime + othersWorkTime)
						+ "\t"
						+ (carShopTime + ptShopTime + wlkShopTime
								+ bikeShopTime + rideShopTime + othersEducTime)
						+ "\t"
						+ (carEducTime + ptEducTime + wlkEducTime
								+ bikeEducTime + rideEducTime + othersEducTime)
						+ "\t"
						+ (carLeisTime + ptLeisTime + wlkLeisTime
								+ bikeLeisTime + rideLeisTime + othersLeisTime)
						+ "\t"
						+ (carOtherTime + ptOtherTime + wlkOtherTime
								+ bikeOtherTime + rideOtherTime + othersOtherTime)
						+ "\t"
						+ (carPickupTime + ptPickupTime + wlkPickupTime
								+ bikePickupTime + ridePickupTime + othersPickupTime)
						+ "\t"
						+ (carBusinessTime + ptBusinessTime + wlkBusinessTime
								+ bikeBusinessTime + rideBusinessTime + othersBusinessTime)
						+ "\t"
						+ (carUnknownTime + ptUnknownTime + wlkUnknownTime
								+ bikeUnknownTime + rideUnknownTime + othersUnknownTime)
						+ "\t"
						+ (carPrivateTime + ptPrivateTime + wlkPrivateTime
								+ bikePrivateTime + ridePrivateTime + othersPrivateTime)
						+ "\t"
						+ (carWithAdultTime + ptWithAdultTime
								+ wlkWithAdultTime + bikeWithAdultTime
								+ rideWithAdultTime + othersWithAdultTime)
						+ "\t"
						+ (carSportsTime + ptSportsTime + wlkSportsTime
								+ bikeSportsTime + rideSportsTime + othersSportsTime)
						+ "\t"
						+ (carFriendsTime + ptFriendsTime + wlkFriendsTime
								+ bikeFriendsTime + rideFriendsTime + othersFriendsTime));

		StackedBarChart stackedBarChart = new StackedBarChart(
				"travel destination and modal split--daily En Route Time",
				"travel destinations", "daily En Route Time [min]",
				PlotOrientation.VERTICAL);
		stackedBarChart.addSeries(new String[] { CAR, "pt", WALK, BIKE, RIDE,
				OTHERS }, new String[] { "home", "work", "shopping",
				"education", "leisure", "other", "pick_up", "business",
				"unknown", "private", "with_adult", "sports", "friends" },
				new double[][] {
						{ carHomeTime, carWorkTime, carShopTime, carEducTime,
								carLeisTime, carOtherTime, carPickupTime,
								carBusinessTime, carUnknownTime,
								carPrivateTime, carWithAdultTime,
								carSportsTime, carFriendsTime },
						{ ptHomeTime, ptWorkTime, ptShopTime, ptEducTime,
								ptLeisTime, ptOtherTime, ptPickupTime,
								ptBusinessTime, ptUnknownTime, ptPrivateTime,
								ptWithAdultTime, ptSportsTime, ptFriendsTime },
						{ wlkHomeTime, wlkWorkTime, wlkShopTime, wlkEducTime,
								wlkLeisTime, wlkOtherTime, wlkPickupTime,
								wlkBusinessTime, wlkUnknownTime,
								wlkPrivateTime, wlkWithAdultTime,
								wlkSportsTime, wlkFriendsTime },
						{ bikeHomeTime, bikeWorkTime, bikeShopTime,
								bikeEducTime, bikeLeisTime, bikeOtherTime,
								bikePickupTime, bikeBusinessTime,
								bikeUnknownTime, bikePrivateTime,
								bikeWithAdultTime, bikeSportsTime,
								bikeFriendsTime },
						{ rideHomeTime, rideWorkTime, rideShopTime,
								rideEducTime, rideLeisTime, rideOtherTime,
								ridePickupTime, rideBusinessTime,
								rideUnknownTime, ridePrivateTime,
								rideWithAdultTime, rideSportsTime,
								rideFriendsTime },
						{ othersHomeTime, othersWorkTime, othersShopTime,
								othersEducTime, othersLeisTime,
								othersOtherTime, othersPickupTime,
								othersBusinessTime, othersUnknownTime,
								othersPrivateTime, othersWithAdultTime,
								othersSportsTime, othersFriendsTime } });
		stackedBarChart.addMatsimLogo();
		stackedBarChart.saveAsPng(outputFilename
				+ "dailyEnRouteTimeTravelDistination.png", 1280, 1024);

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101], yCar[] = new double[101], yPt[] = new double[101], yWlk[] = new double[101], yBike[] = new double[101], yRide[] = new double[101], yOthers[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalDayEnRouteTimeCounts[i] / count * 100.0;
			yCar[i] = carDayEnRouteTimeCounts[i] / count * 100.0;
			yPt[i] = ptDayEnRouteTimeCounts[i] / count * 100.0;
			yWlk[i] = wlkDayEnRouteTimeCounts[i] / count * 100.0;
			yBike[i] = bikeDayEnRouteTimeCounts[i] / count * 100.0;
			yRide[i] = rideDayEnRouteTimeCounts[i] / count * 100.0;
			yOthers[i] = othersDayEnRouteTimeCounts[i] / count * 100.0;
		}

		XYLineChart chart = new XYLineChart("Daily En Route Time Distribution",
				"Daily En Route Time in min",
				"fraction of persons with daily en route time longer than x... in %");
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
		chart.addSeries(TOTAL, x, yTotal);
		chart.addMatsimLogo();
		chart.saveAsPng(outputFilename + "dailyEnRouteTimeDistribution.png",
				800, 600);

		sw.writeln("\n--Modal split -- leg duration--");
		sw
				.writeln("leg Duration [min]\tcar legs no.\tpt legs no.\twalk legs no.\tbike legs no.\tride legs no.\tothers legs no.\t"
						+ "car fraction [%]\tpt fraction [%]\twalk fraction [%]\tbike fraction [%]\tride fraction [%]\tothers fraction [%]");

		double xs[] = new double[101], yCarFracs[] = new double[101], yPtFracs[] = new double[101], yWlkFracs[] = new double[101], yBikeFracs[] = new double[101], yRideFracs[] = new double[101], yOthersFracs[] = new double[101];
		for (int i = 0; i < 101; i++) {
			double sumOfLegTimeCounts = carLegTimeCounts[i]
					+ ptLegTimeCounts[i] + wlkLegTimeCounts[i]
					+ bikeLegTimeCounts[i] + rideLegTimeCounts[i]
					+ othersLegTimeCounts[i];
			xs[i] = i * 2;
			if (sumOfLegTimeCounts > 0) {
				yCarFracs[i] = carLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yPtFracs[i] = ptLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yWlkFracs[i] = wlkLegTimeCounts[i] / sumOfLegTimeCounts * 100.0;
				yBikeFracs[i] = bikeLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
				yRideFracs[i] = rideLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
				yOthersFracs[i] = othersLegTimeCounts[i] / sumOfLegTimeCounts
						* 100.0;
			} else {
				yCarFracs[i] = 0;
				yPtFracs[i] = 0;
				yWlkFracs[i] = 0;
				yBikeFracs[i] = 0;
				yRideFracs[i] = 0;
				yOthersFracs[i] = 0;
			}

			sw.writeln(i + "+\t" + carLegTimeCounts[i] + "\t"
					+ ptLegTimeCounts[i] + "\t" + wlkLegTimeCounts[i] + "\t"
					+ bikeLegTimeCounts[i] + "\t" + rideLegTimeCounts[i] + "\t"
					+ othersLegTimeCounts[i] + "\t" + yCarFracs[i] + "\t"
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t" + yBikeFracs[i]
					+ "\t" + yRideFracs[i] + "\t" + yOthersFracs[i]);
		}

		XYLineChart chart2 = new XYLineChart("Modal Split -- leg Duration",
				"leg Duration [min]", "mode fraction [%]");
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
		chart2.saveAsPng(outputFilename + "legTimeModalSplit2.png", 800, 600);
		sw.close();
	}

	public static void main(final String[] args) {
		for (int i = 0; i < 10; i++)
			System.out.println(">>>>>output-Test");
		final String netFilename = "../detailedEval/data/network.xml.gz", //
		plansFilename = "../../run950/output/950.output_plans.xml.gz", //
		outputFilename = "../detailedEval/test/", //
		tollFilename = "../detailedEval/data/boundary.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

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

		PopulationImpl population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyEnRouteTime4Muc ert = new DailyEnRouteTime4Muc(scenario
				.getRoadPricingScheme());
		ert.run(population);
		ert.write(outputFilename);

		System.out.println("--> Done!");
		System.exit(0);
	}

}
