/**
 *
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.charts.XYLineChart;

import playground.yu.utils.SimpleWriter;

/**
 * compute modal split of through distance
 *
 * @author yu
 *
 */
public class DailyDistance extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private double carDist, ptDist, otherDist;

	final double totalCounts[], carCounts[], ptCounts[], otherCounts[];

	private double carWorkDist, carEducDist, carShopDist, carLeisDist, carHomeDist, carOtherDist, ptWorkDist, ptEducDist, ptShopDist, ptLeisDist,
			ptHomeDist, ptOtherDist, throughWorkDist, throughEducDist, throughShopDist, throughLeisDist, throughHomeDist, throughOtherDist;

	private int count;

	private Person person;

	public enum ActTypeStart {
		h, w, s, e, l, o
	}

	public DailyDistance() {
		this.carDist = 0.0;
		this.ptDist = 0.0;
		this.otherDist = 0.0;
		this.count = 0;
		this.totalCounts = new double[101];
		this.carCounts = new double[101];
		this.ptCounts = new double[101];
		this.otherCounts = new double[101];
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
		this.throughWorkDist = 0.0;
		this.throughEducDist = 0.0;
		this.throughShopDist = 0.0;
		this.throughLeisDist = 0.0;
		this.throughHomeDist = 0.0;
		this.throughOtherDist = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = person;
		this.count++;
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		double dayDist = 0.0;
		double carDayDist = 0.0;
		double ptDayDist = 0.0;
		double otherDayDist = 0.0;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			ActTypeStart ats = null;
			String tmpActType = plan.getNextActivity(bl).getType();
			if (tmpActType.startsWith("h"))
				ats = ActTypeStart.h;
			else if (tmpActType.startsWith("w"))
				ats = ActTypeStart.w;
			else if (tmpActType.startsWith("e"))
				ats = ActTypeStart.e;
			else if (tmpActType.startsWith("s"))
				ats = ActTypeStart.s;
			else if (tmpActType.startsWith("l"))
				ats = ActTypeStart.l;
			else
				ats = ActTypeStart.o;
			double dist = bl.getRoute().getDist() / 1000.0;
			if (bl.getDepartureTime() < 86400) {
				dayDist += dist;
				if (Long.parseLong(this.person.getId().toString()) > 1000000000) {
					this.otherDist += dist;
					otherDayDist += dist;
					switch (ats) {
					case h:
						this.throughHomeDist += dist;
						break;
					case w:
						this.throughWorkDist += dist;
						break;
					case e:
						this.throughEducDist += dist;
						break;
					case s:
						this.throughShopDist += dist;
						break;
					case l:
						this.throughLeisDist += dist;
						break;
					default:
						this.throughOtherDist += dist;
						break;
					}
				} else if (bl.getMode().equals(Mode.car)) {
					this.carDist += dist;
					carDayDist += dist;
					switch (ats) {
					case h:
						this.carHomeDist += dist;
						break;
					case w:
						this.carWorkDist += dist;
						break;
					case e:
						this.carEducDist += dist;
						break;
					case s:
						this.carShopDist += dist;
						break;
					case l:
						this.carLeisDist += dist;
						break;
					default:
						this.carOtherDist += dist;
						break;
					}
				} else if (bl.getMode().equals(Mode.pt)) {
					this.ptDist += dist;
					ptDayDist += dist;
					switch (ats) {
					case h:
						this.ptHomeDist += dist;
						break;
					case w:
						this.ptWorkDist += dist;
						break;
					case e:
						this.ptEducDist += dist;
						break;
					case s:
						this.ptShopDist += dist;
						break;
					case l:
						this.ptLeisDist += dist;
						break;
					default:
						this.ptOtherDist += dist;
						break;
					}
				}
			}
		}
		for (int i = 0; i <= Math.min(100, (int) dayDist); i++)
			this.totalCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayDist); i++)
			this.otherCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayDist); i++)
			this.carCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayDist); i++)
			this.ptCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carDist + this.ptDist + this.otherDist;
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("\tDaily Distance\t(exkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + this.carDist / this.count + "\t" + this.carDist / sum
				* 100.0);
		sw.writeln("pt\t" + this.ptDist / this.count + "\t" + this.ptDist / sum
				* 100.0);
		sw.writeln("through\t" + this.otherDist / this.count + "\t" + this.otherDist
				/ sum * 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily Distance\t(inkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + (this.carDist + this.otherDist) / this.count + "\t"
				+ (this.carDist + this.otherDist) / sum * 100.0);
		sw.writeln("pt\t" + this.ptDist / this.count + "\t" + this.ptDist / sum
				* 100.0);
		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + this.carWorkDist + "\t" + this.carEducDist + "\t"
				+ this.carShopDist + "\t" + this.carLeisDist + "\t" + this.carHomeDist + "\t"
				+ this.carOtherDist);
		sw.writeln("pt\t" + this.ptWorkDist + "\t" + this.ptEducDist + "\t" + this.ptShopDist
				+ "\t" + this.ptLeisDist + "\t" + this.ptHomeDist + "\t" + this.ptOtherDist);
		sw.writeln("through\t" + this.throughWorkDist + "\t" + this.throughEducDist
				+ "\t" + this.throughShopDist + "\t" + this.throughLeisDist + "\t"
				+ this.throughHomeDist + "\t" + this.throughOtherDist);
		sw.writeln("total\t" + (this.carWorkDist + this.ptWorkDist + this.throughWorkDist)
				+ "\t" + (this.carEducDist + this.ptEducDist + this.throughEducDist) + "\t"
				+ (this.carShopDist + this.ptShopDist + this.throughShopDist) + "\t"
				+ (this.carLeisDist + this.ptLeisDist + this.throughLeisDist) + "\t"
				+ (this.carHomeDist + this.ptHomeDist + this.throughHomeDist) + "\t"
				+ (this.carOtherDist + this.ptOtherDist + this.throughOtherDist));
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = i;
		double yTotal[] = new double[101];
		double yCar[] = new double[101];
		double yPt[] = new double[101];
		double yOther[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = this.totalCounts[i] / this.count * 100.0;
			yCar[i] = this.carCounts[i] / this.count * 100.0;
			yPt[i] = this.ptCounts[i] / this.count * 100.0;
			yOther[i] = this.otherCounts[i] / this.count * 100.0;
		}
		XYLineChart chart = new XYLineChart("Daily Distance distribution",
				"Daily Distance in km",
				"fraction of persons with daily distance bigger than x... in %");
		chart.addSeries("car", x, yCar);
		chart.addSeries("pt", x, yPt);
		chart.addSeries("other", x, yOther);
		chart.addSeries("total", x, yTotal);
		chart.saveAsPng(outputFilename + ".png", 800, 600);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "output/669dailyDistance";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		DailyDistance dd = new DailyDistance();
		population.addAlgorithm(dd);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		dd.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
