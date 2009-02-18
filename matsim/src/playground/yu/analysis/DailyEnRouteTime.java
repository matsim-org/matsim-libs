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
 * compute modal split of en route time
 * @author yu
 * 
 */
public class DailyEnRouteTime extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private int count;
	private double carTime, ptTime, otherTime, totalCounts[], carCounts[],
	ptCounts[], otherCounts[],carWorkTime, carEducTime, carShopTime,
	carLeisTime, carHomeTime, carOtherTime, ptWorkTime, ptEducTime,
	ptShopTime, ptLeisTime, ptHomeTime, ptOtherTime, throughWorkTime,
	throughEducTime, throughShopTime, throughLeisTime, throughHomeTime,
	throughOtherTime;
	private Person person;

	public DailyEnRouteTime() {
		count = 0;
		carTime = 0.0;
		ptTime = 0.0;
		otherTime = 0.0;
		totalCounts = new double[101];
		carCounts = new double[101];
		ptCounts = new double[101];
		otherCounts = new double[101];
		carWorkTime = 0.0;
		carEducTime = 0.0;
		carShopTime = 0.0;
		carLeisTime = 0.0;
		carHomeTime = 0.0;
		carOtherTime = 0.0;
		ptWorkTime = 0.0;
		ptEducTime = 0.0;
		ptShopTime = 0.0;
		ptLeisTime = 0.0;
		ptHomeTime = 0.0;
		ptOtherTime = 0.0;
		throughWorkTime = 0.0;
		throughEducTime = 0.0;
		throughShopTime = 0.0;
		throughLeisTime = 0.0;
		throughHomeTime = 0.0;
		throughOtherTime = 0.0;
	}

	@Override
	public void run(Person person) {
		this.person = person;
		count++;
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		double dayTime = 0.0;
		double carDayTime = 0.0;
		double ptDayTime = 0.0;
		double otherDayTime = 0.0;
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			double time = bl.getTravelTime() / 60.0;
			if (bl.getDepartureTime() < 86400) {
				dayTime += time;
				if (Long.parseLong(person.getId().toString()) > 1000000000) {
					otherTime += time;
					otherDayTime += time;
				} else if (bl.getMode().equals(Mode.car)) {
					carTime += time;
					carDayTime += time;
				} else if (bl.getMode().equals(Mode.pt)) {
					ptTime += time;
					ptDayTime += time;
				}
			}
		}
		for (int i = 0; i <= Math.min(100, (int) dayTime); i++)
			totalCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) otherDayTime); i++)
			otherCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) carDayTime); i++)
			carCounts[i]++;
		for (int i = 0; i <= Math.min(100, (int) ptDayTime); i++)
			ptCounts[i]++;
	}

	public void write(String outputFilename) {
		double sum = carTime + ptTime + otherTime;
		SimpleWriter sw = new SimpleWriter(outputFilename+".txt");
		sw.writeln("\tDaily En Route Time\t(exkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + carTime / (double) count + "\t" + carTime / sum
				* 100.0);
		sw.writeln("pt\t" + ptTime / count + "\t" + ptTime / sum * 100.0);
		sw.writeln("through\t" + otherTime / count + "\t" + otherTime / sum
				* 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaily En Route Time\t(inkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + (carTime + otherTime) / (double) count + "\t"
				+ (carTime + otherTime) / sum * 100.0);
		sw.writeln("pt\t" + ptTime / count + "\t" + ptTime / sum * 100.0);
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
	public static void main(String[] args) {
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
