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

import playground.yu.analysis.DailyDistance.ActTypeStart;
import playground.yu.utils.SimpleWriter;

/**
 * compute modal split of en route time
 * @author yu
 *
 */
public class DailyEnRouteTime extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private int count;
	private double carTime, ptTime, otherTime;
	final double totalCounts[], carCounts[], ptCounts[], otherCounts[];
	private double carWorkTime, carEducTime, carShopTime, carLeisTime, carHomeTime, carOtherTime, ptWorkTime, ptEducTime, ptShopTime, ptLeisTime,
			ptHomeTime, ptOtherTime, throughWorkTime, throughEducTime, throughShopTime, throughLeisTime, throughHomeTime, throughOtherTime;
	private Person person;

	public DailyEnRouteTime() {
		this.count = 0;
		this.carTime = 0.0;
		this.ptTime = 0.0;
		this.otherTime = 0.0;
		this.totalCounts = new double[101];
		this.carCounts = new double[101];
		this.ptCounts = new double[101];
		this.otherCounts = new double[101];
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
		this.throughWorkTime = 0.0;
		this.throughEducTime = 0.0;
		this.throughShopTime = 0.0;
		this.throughLeisTime = 0.0;
		this.throughHomeTime = 0.0;
		this.throughOtherTime = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = person;
		this.count++;
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double otherDayTime = 0.0;
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
			double time = bl.getTravelTime() / 60.0;
			if (bl.getDepartureTime() < 86400) {
				dayTime += time;
				if (Long.parseLong(this.person.getId().toString()) > 1000000000) {
					this.otherTime += time;
					otherDayTime += time;
					switch (ats) {
					case h:
						this.throughHomeTime += time;
						break;
					case w:
						this.throughWorkTime += time;
						break;
					case e:
						this.throughEducTime += time;
						break;
					case s:
						this.throughShopTime += time;
						break;
					case l:
						this.throughLeisTime += time;
						break;
					default:
						this.throughOtherTime += time;
						break;
					}
				} else if (bl.getMode().equals(Mode.car)) {
					this.carTime += time;
					carDayTime += time;
					switch (ats) {
					case h:
						this.carHomeTime += time;
						break;
					case w:
						this.carWorkTime += time;
						break;
					case e:
						this.carEducTime += time;
						break;
					case s:
						this.carShopTime += time;
						break;
					case l:
						this.carLeisTime += time;
						break;
					default:
						this.carOtherTime += time;
						break;
					}
				} else if (bl.getMode().equals(Mode.pt)) {
					this.ptTime += time;
					ptDayTime += time;
					switch (ats) {
					case h:
						this.ptHomeTime += time;
						break;
					case w:
						this.ptWorkTime += time;
						break;
					case e:
						this.ptEducTime += time;
						break;
					case s:
						this.ptShopTime += time;
						break;
					case l:
						this.ptLeisTime += time;
						break;
					default:
						this.ptOtherTime += time;
						break;
					}
				}
			}
		}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			this.totalCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayTime); i++)
			this.otherCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			this.carCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			this.ptCounts[i]++;
	}

	public void write(final String outputFilename) {
		double sum = this.carTime + this.ptTime + this.otherTime;
		SimpleWriter sw = new SimpleWriter(outputFilename+".txt");
		sw.writeln("\tDaily En Route Time\t(exkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + this.carTime / this.count + "\t" + this.carTime / sum
				* 100.0);
		sw.writeln("pt\t" + this.ptTime / this.count + "\t" + this.ptTime / sum * 100.0);
		sw.writeln("through\t" + this.otherTime / this.count + "\t" + this.otherTime / sum
				* 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily En Route Time\t(inkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + (this.carTime + this.otherTime) / this.count + "\t"
				+ (this.carTime + this.otherTime) / sum * 100.0);
		sw.writeln("pt\t" + this.ptTime / this.count + "\t" + this.ptTime / sum * 100.0);
		sw.writeln("--travel destination and modal split--daily on route time--");
		sw.writeln("\twork\teducation\tshopping\tleisure\thome\tother...");
		sw.writeln("car\t" + this.carWorkTime + "\t" + this.carEducTime + "\t"
				+ this.carShopTime + "\t" + this.carLeisTime + "\t" + this.carHomeTime + "\t"
				+ this.carOtherTime);
		sw.writeln("pt\t" + this.ptWorkTime + "\t" + this.ptEducTime + "\t" + this.ptShopTime
				+ "\t" + this.ptLeisTime + "\t" + this.ptHomeTime + "\t" + this.ptOtherTime);
		sw.writeln("through\t" + this.throughWorkTime + "\t" + this.throughEducTime
				+ "\t" + this.throughShopTime + "\t" + this.throughLeisTime + "\t"
				+ this.throughHomeTime + "\t" + this.throughOtherTime);
		sw.writeln("total\t" + (this.carWorkTime + this.ptWorkTime + this.throughWorkTime)
				+ "\t" + (this.carEducTime + this.ptEducTime + this.throughEducTime) + "\t"
				+ (this.carShopTime + this.ptShopTime + this.throughShopTime) + "\t"
				+ (this.carLeisTime + this.ptLeisTime + this.throughLeisTime) + "\t"
				+ (this.carHomeTime + this.ptHomeTime + this.throughHomeTime) + "\t"
				+ (this.carOtherTime + this.ptOtherTime + this.throughOtherTime));
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
		XYLineChart chart = new XYLineChart("Daily En Route Time Distribution",
				"Daily En Route Time in min",
				"fraction of persons with daily en route time longer than x... in %");
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
		final String outputFilename = "output/669dailyEnRouteTime";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		DailyEnRouteTime ert = new DailyEnRouteTime();
		population.addAlgorithm(ert);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		ert.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
