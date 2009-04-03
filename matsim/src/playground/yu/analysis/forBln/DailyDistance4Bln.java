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
public class DailyDistance4Bln extends DailyDistance implements Analysis4Bln {

	public DailyDistance4Bln() {
		super();
	}

	public DailyDistance4Bln(RoadPricingScheme toll) {
		super(toll);
	}

	private double carBusinessDist, carEinkaufSonstigesDist,
			carFreizeitSonstSportDist, carHolidayJourneyDist, carMultipleDist,
			carSeeADoctorDist, carNotSpecifiedDist;
	private double ptBusinessDist, ptEinkaufSonstigesDist,
			ptFreizeitSonstSportDist, ptHolidayJourneyDist, ptMultipleDist,
			ptSeeADoctorDist, ptNotSpecifiedDist;
	private double wlkBusinessDist, wlkEinkaufSonstigesDist,
			wlkFreizeitSonstSportDist, wlkHolidayJourneyDist, wlkMultipleDist,
			wlkSeeADoctorDist, wlkNotSpecifiedDist;
	private double bikeBusinessDist, bikeEinkaufSonstigesDist,
			bikeFreizeitSonstSportDist, bikeHolidayJourneyDist,
			bikeMultipleDist, bikeSeeADoctorDist, bikeNotSpecifiedDist;
	private double othersBusinessDist, othersEinkaufSonstigesDist,
			othersFreizeitSonstSportDist, othersHolidayJourneyDist,
			othersMultipleDist, othersSeeADoctorDist, othersNotSpecifiedDist;

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
			String tmpActType = plan.getNextActivity(bl).getType();
			for (ActType a : ActType.values()) {
				if (tmpActType.equals(a.getActTypeName())) {
					at = a;
					break;
				}
			}
			if (at == null)
				at = ActType.other;

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
				case business:
					carBusinessDist += dist;
					break;
				case Einkauf_sonstiges:
					carEinkaufSonstigesDist += dist;
					break;
				case Freizeit_sonstiges_incl_Sport:
					carFreizeitSonstSportDist += dist;
					break;
				case holiday_journey:
					carHolidayJourneyDist += dist;
					break;
				case multiple:
					carMultipleDist += dist;
					break;
				case not_specified:
					carNotSpecifiedDist += dist;
					break;
				case see_a_doctor:
					carSeeADoctorDist += dist;
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
				case business:
					ptBusinessDist += dist;
					break;
				case Einkauf_sonstiges:
					ptEinkaufSonstigesDist += dist;
					break;
				case Freizeit_sonstiges_incl_Sport:
					ptFreizeitSonstSportDist += dist;
					break;
				case holiday_journey:
					ptHolidayJourneyDist += dist;
					break;
				case multiple:
					ptMultipleDist += dist;
					break;
				case not_specified:
					ptNotSpecifiedDist += dist;
					break;
				case see_a_doctor:
					ptSeeADoctorDist += dist;
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
				case business:
					wlkBusinessDist += dist;
					break;
				case Einkauf_sonstiges:
					wlkEinkaufSonstigesDist += dist;
					break;
				case Freizeit_sonstiges_incl_Sport:
					wlkFreizeitSonstSportDist += dist;
					break;
				case holiday_journey:
					wlkHolidayJourneyDist += dist;
					break;
				case multiple:
					wlkMultipleDist += dist;
					break;
				case not_specified:
					wlkNotSpecifiedDist += dist;
					break;
				case see_a_doctor:
					wlkSeeADoctorDist += dist;
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
				case business:
					bikeBusinessDist += dist;
					break;
				case Einkauf_sonstiges:
					bikeEinkaufSonstigesDist += dist;
					break;
				case Freizeit_sonstiges_incl_Sport:
					bikeFreizeitSonstSportDist += dist;
					break;
				case holiday_journey:
					bikeHolidayJourneyDist += dist;
					break;
				case multiple:
					bikeMultipleDist += dist;
					break;
				case not_specified:
					bikeNotSpecifiedDist += dist;
					break;
				case see_a_doctor:
					bikeSeeADoctorDist += dist;
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
				case business:
					othersBusinessDist += dist;
					break;
				case Einkauf_sonstiges:
					othersEinkaufSonstigesDist += dist;
					break;
				case Freizeit_sonstiges_incl_Sport:
					othersFreizeitSonstSportDist += dist;
					break;
				case holiday_journey:
					othersHolidayJourneyDist += dist;
					break;
				case multiple:
					othersMultipleDist += dist;
					break;
				case not_specified:
					othersNotSpecifiedDist += dist;
					break;
				case see_a_doctor:
					othersSeeADoctorDist += dist;
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
		pieChart.addMatsimLogo();
		pieChart.saveAsPng(outputFilename + "dailyDistanceModalSplitPie.png",
				800, 600);

		sw.writeln("----------------------------------------------");
		sw.writeln("--travel destination and modal split--daily distance--");
		sw
				.writeln("mode\thome\twork\tshopping\teducation\tleisure\tother\tnot specified\tbusiness\tEinkauf sonstiges\tFreizeit(Sport usw.)\tsee a doctor\tholiday/journey\tmultiple");

		sw.writeln("car\t" + carHomeDist + "\t" + carWorkDist + "\t"
				+ carShopDist + "\t" + carEducDist + "\t" + this.carLeisDist
				+ "\t" + this.carOtherDist + "\t" + carNotSpecifiedDist + "\t"
				+ carBusinessDist + "\t" + carEinkaufSonstigesDist + "\t"
				+ carFreizeitSonstSportDist + "\t" + carSeeADoctorDist + "\t"
				+ carHolidayJourneyDist + "\t" + carMultipleDist);
		sw.writeln("pt\t" + ptHomeDist + "\t" + ptWorkDist + "\t" + ptShopDist
				+ "\t" + ptEducDist + "\t" + this.ptLeisDist + "\t"
				+ this.ptOtherDist + "\t" + ptNotSpecifiedDist + "\t"
				+ ptBusinessDist + "\t" + ptEinkaufSonstigesDist + "\t"
				+ ptFreizeitSonstSportDist + "\t" + ptSeeADoctorDist + "\t"
				+ ptHolidayJourneyDist + "\t" + ptMultipleDist);
		sw.writeln("walk\t" + wlkHomeDist + "\t" + wlkWorkDist + "\t"
				+ wlkShopDist + "\t" + wlkEducDist + "\t" + this.wlkLeisDist
				+ "\t" + this.wlkOtherDist + "\t" + wlkNotSpecifiedDist + "\t"
				+ wlkBusinessDist + "\t" + wlkEinkaufSonstigesDist + "\t"
				+ wlkFreizeitSonstSportDist + "\t" + wlkSeeADoctorDist + "\t"
				+ wlkHolidayJourneyDist + "\t" + wlkMultipleDist);
		sw.writeln("bike\t" + bikeHomeDist + "\t" + bikeWorkDist + "\t"
				+ bikeShopDist + "\t" + bikeEducDist + "\t" + this.bikeLeisDist
				+ "\t" + this.bikeOtherDist + "\t" + bikeNotSpecifiedDist
				+ "\t" + bikeBusinessDist + "\t" + bikeEinkaufSonstigesDist
				+ "\t" + bikeFreizeitSonstSportDist + "\t" + bikeSeeADoctorDist
				+ "\t" + bikeHolidayJourneyDist + "\t" + bikeMultipleDist);
		sw.writeln("others\t" + othersHomeDist + "\t" + othersWorkDist + "\t"
				+ othersShopDist + "\t" + othersEducDist + "\t"
				+ this.othersLeisDist + "\t" + this.othersOtherDist + "\t"
				+ othersNotSpecifiedDist + "\t" + othersBusinessDist + "\t"
				+ othersEinkaufSonstigesDist + "\t"
				+ othersFreizeitSonstSportDist + "\t" + othersSeeADoctorDist
				+ "\t" + othersHolidayJourneyDist + "\t" + othersMultipleDist);

		sw
				.writeln("total\t"
						+ (this.carHomeDist + this.ptHomeDist + wlkHomeDist
								+ bikeHomeDist + othersHomeDist)
						+ "\t"
						+ (this.carWorkDist + this.ptWorkDist + wlkWorkDist
								+ bikeWorkDist + othersWorkDist)
						+ "\t"
						+ (this.carShopDist + this.ptShopDist + wlkShopDist
								+ bikeShopDist + othersEducDist)
						+ "\t"
						+ (this.carEducDist + this.ptEducDist + wlkEducDist
								+ bikeEducDist + othersEducDist)
						+ "\t"
						+ (this.carLeisDist + this.ptLeisDist + wlkLeisDist
								+ bikeLeisDist + othersLeisDist)
						+ "\t"
						+ (this.carOtherDist + this.ptOtherDist + wlkOtherDist
								+ bikeOtherDist + othersOtherDist)
						+ "\t"
						+ (this.carNotSpecifiedDist + this.ptNotSpecifiedDist
								+ wlkNotSpecifiedDist + bikeNotSpecifiedDist + othersNotSpecifiedDist)
						+ "\t"
						+ (this.carBusinessDist + this.ptBusinessDist
								+ wlkBusinessDist + bikeBusinessDist + othersBusinessDist)
						+ "\t"
						+ (this.carEinkaufSonstigesDist
								+ this.ptEinkaufSonstigesDist
								+ wlkEinkaufSonstigesDist
								+ bikeEinkaufSonstigesDist + othersEinkaufSonstigesDist)
						+ "\t"
						+ (this.carFreizeitSonstSportDist
								+ this.ptFreizeitSonstSportDist
								+ wlkFreizeitSonstSportDist
								+ bikeFreizeitSonstSportDist + othersFreizeitSonstSportDist)
						+ "\t"
						+ (this.carSeeADoctorDist + this.ptSeeADoctorDist
								+ wlkSeeADoctorDist + bikeSeeADoctorDist + othersSeeADoctorDist)
						+ "\t"
						+ (this.carHolidayJourneyDist
								+ this.ptHolidayJourneyDist
								+ wlkHolidayJourneyDist
								+ bikeHolidayJourneyDist + othersHolidayJourneyDist)
						+ "\t"
						+ (this.carMultipleDist + this.ptMultipleDist
								+ wlkMultipleDist + bikeMultipleDist + othersMultipleDist));

		StackedBarChart stackedBarChart = new StackedBarChart(
				"travel destination and modal split--daily distance",
				"travel destinations", "daily distances [km]",
				PlotOrientation.VERTICAL);
		stackedBarChart.addSeries(new String[] { "car", "pt", "walk", "bike",
				"others" }, new String[] { "home", "work", "shopping",
				"education", "leisure", "other", "not specified", "business",
				"Einkauf sonstiges", "Freizeit (sonstiges incl.Sport)",
				"see a doctor", "holiday / journey", "multiple" },
				new double[][] {
						{ carHomeDist, carWorkDist, carShopDist, carEducDist,
								carLeisDist, carOtherDist, carNotSpecifiedDist,
								carBusinessDist, carEinkaufSonstigesDist,
								carFreizeitSonstSportDist, carSeeADoctorDist,
								carHolidayJourneyDist, carMultipleDist },
						{ ptHomeDist, ptWorkDist, ptShopDist, ptEducDist,
								ptLeisDist, ptOtherDist, ptNotSpecifiedDist,
								ptBusinessDist, ptEinkaufSonstigesDist,
								ptFreizeitSonstSportDist, ptSeeADoctorDist,
								ptHolidayJourneyDist, ptMultipleDist },
						{ wlkHomeDist, wlkWorkDist, wlkShopDist, wlkEducDist,
								wlkLeisDist, wlkOtherDist, wlkNotSpecifiedDist,
								wlkBusinessDist, wlkEinkaufSonstigesDist,
								wlkFreizeitSonstSportDist, wlkSeeADoctorDist,
								wlkHolidayJourneyDist, wlkMultipleDist },
						{ bikeHomeDist, bikeWorkDist, bikeShopDist,
								bikeEducDist, bikeLeisDist, bikeOtherDist,
								bikeNotSpecifiedDist, bikeBusinessDist,
								bikeEinkaufSonstigesDist,
								bikeFreizeitSonstSportDist, bikeSeeADoctorDist,
								bikeHolidayJourneyDist, bikeMultipleDist },
						{ othersHomeDist, othersWorkDist, othersShopDist,
								othersEducDist, othersLeisDist,
								othersOtherDist, othersNotSpecifiedDist,
								othersBusinessDist, othersEinkaufSonstigesDist,
								othersFreizeitSonstSportDist,
								othersSeeADoctorDist, othersHolidayJourneyDist,
								othersMultipleDist } });
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
					+ yPtFracs[i] + "\t" + yWlkFracs[i] + "\t" + yBikeFracs[i]
					+ "\t" + yOthersFracs[i]);
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

		final String netFilename = "../berlin data/osm/bb_osm_wip_cl.xml.gz";
		final String plansFilename = "../runs-svn/run756/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/run756/dailyDistance/";
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
		DailyDistance4Bln dd = new DailyDistance4Bln(null);

		new MatsimPopulationReader(population, network).readFile(plansFilename);

		dd.run(population);
		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
