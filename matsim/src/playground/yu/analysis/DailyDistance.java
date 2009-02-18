/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
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
	private double carDist, ptDist, otherDist, totalCounts[], carCounts[],
			ptCounts[], otherCounts[], carWorkDist, carEducDist, carShopDist,
			carLeisDist, carHomeDist, carOtherDist, ptWorkDist, ptEducDist,
			ptShopDist, ptLeisDist, ptHomeDist, ptOtherDist, throughWorkDist,
			throughEducDist, throughShopDist, throughLeisDist, throughHomeDist,
			throughOtherDist;

	private int count;

	private Person person;

	public enum ActTypeStart {
		h, w, s, e, l, o
	};

	public DailyDistance() {
		carDist = 0.0;
		ptDist = 0.0;
		otherDist = 0.0;
		count = 0;
		totalCounts = new double[101];
		carCounts = new double[101];
		ptCounts = new double[101];
		otherCounts = new double[101];
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
		throughWorkDist = 0.0;
		throughEducDist = 0.0;
		throughShopDist = 0.0;
		throughLeisDist = 0.0;
		throughHomeDist = 0.0;
		throughOtherDist = 0.0;
	}

	public void run(Person person) {
		this.person = person;
		count++;
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
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
				if (Long.parseLong(person.getId().toString()) > 1000000000) {
					otherDist += dist;
					otherDayDist += dist;
					switch (ats) {
					case h:
						throughHomeDist += dist;
						break;
					case w:
						throughWorkDist += dist;
						break;
					case e:
						throughEducDist += dist;
						break;
					case s:
						throughShopDist += dist;
						break;
					case l:
						throughLeisDist += dist;
						break;
					default:
						throughOtherDist += dist;
						break;
					}
				} else if (bl.getMode().equals(Mode.car)) {
					carDist += dist;
					carDayDist += dist;
					switch (ats) {
					case h:
						carHomeDist += dist;
						break;
					case w:
						carWorkDist += dist;
						break;
					case e:
						carEducDist += dist;
						break;
					case s:
						carShopDist += dist;
						break;
					case l:
						carLeisDist += dist;
						break;
					default:
						carOtherDist += dist;
						break;
					}
				} else if (bl.getMode().equals(Mode.pt)) {
					ptDist += dist;
					ptDayDist += dist;
					switch (ats) {
					case h:
						ptHomeDist += dist;
						break;
					case w:
						ptWorkDist += dist;
						break;
					case e:
						ptEducDist += dist;
						break;
					case s:
						ptShopDist += dist;
						break;
					case l:
						ptLeisDist += dist;
						break;
					default:
						ptOtherDist += dist;
						break;
					}
				}
			}
		}
		for (int i = 0; i <= Math.min(100, (int) dayDist); i++)
			totalCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayDist); i++)
			otherCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayDist); i++)
			carCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayDist); i++)
			ptCounts[i]++;
	}

	public void write(String outputFilename) {
		double sum = carDist + ptDist + otherDist;
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("\tDaily Distance\t(exkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + carDist / (double) count + "\t" + carDist / sum
				* 100.0);
		sw.writeln("pt\t" + ptDist / (double) count + "\t" + ptDist / sum
				* 100.0);
		sw.writeln("through\t" + otherDist / (double) count + "\t" + otherDist
				/ sum * 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily Distance\t(inkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + (carDist + otherDist) / (double) count + "\t"
				+ (carDist + otherDist) / sum * 100.0);
		sw.writeln("pt\t" + ptDist / (double) count + "\t" + ptDist / sum
				* 100.0);
		sw.writeln("--travel destination and modal split--daily distance--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + carWorkDist + "\t" + carEducDist + "\t"
				+ carShopDist + "\t" + carLeisDist + "\t" + carHomeDist + "\t"
				+ carOtherDist);
		sw.writeln("pt\t" + ptWorkDist + "\t" + ptEducDist + "\t" + ptShopDist
				+ "\t" + ptLeisDist + "\t" + ptHomeDist + "\t" + ptOtherDist);
		sw.writeln("through\t" + throughWorkDist + "\t" + throughEducDist
				+ "\t" + throughShopDist + "\t" + throughLeisDist + "\t"
				+ throughHomeDist + "\t" + throughOtherDist);
		sw.writeln("total\t" + (carWorkDist + ptWorkDist + throughWorkDist)
				+ "\t" + (carEducDist + ptEducDist + throughEducDist) + "\t"
				+ (carShopDist + ptShopDist + throughShopDist) + "\t"
				+ (carLeisDist + ptLeisDist + throughLeisDist) + "\t"
				+ (carHomeDist + ptHomeDist + throughHomeDist) + "\t"
				+ (carOtherDist + ptOtherDist + throughOtherDist));
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		double x[] = new double[101];
		for (int i = 0; i < 101; i++)
			x[i] = (double) i;
		double yTotal[] = new double[101];
		double yCar[] = new double[101];
		double yPt[] = new double[101];
		double yOther[] = new double[101];
		for (int i = 0; i < 101; i++) {
			yTotal[i] = totalCounts[i] / (double) count * 100.0;
			yCar[i] = carCounts[i] / (double) count * 100.0;
			yPt[i] = ptCounts[i] / (double) count * 100.0;
			yOther[i] = otherCounts[i] / (double) count * 100.0;
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
	public static void main(String[] args) {
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
