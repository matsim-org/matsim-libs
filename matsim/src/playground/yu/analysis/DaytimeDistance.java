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
 * compute modal split of daytime distance
 * 
 * @author yu
 * 
 */
public class DaytimeDistance extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private double carDist, ptDist, otherDist, totalCounts[], carCounts[],
			ptCounts[], otherCounts[];

	private int count;

	private Person person;

	public DaytimeDistance() {
		carDist = 0.0;
		ptDist = 0.0;
		otherDist = 0.0;
		count = 0;
		totalCounts = new double[101];
		carCounts = new double[101];
		ptCounts = new double[101];
		otherCounts = new double[101];
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
			double dist = bl.getRoute().getDist() / 1000.0;
			if (bl.getDepartureTime() < 86400) {
				dayDist += dist;
				if (Long.parseLong(person.getId().toString()) > 1000000000) {
					otherDist += dist;
					otherDayDist += dist;
				} else if (bl.getMode().equals(Mode.car)) {
					carDist += dist;
					carDayDist += dist;
				} else if (bl.getMode().equals(Mode.pt)) {
					ptDist += dist;
					ptDayDist += dist;
				}
			}
		}
		for (int i = 0; i <= (int) dayDist; i++)
			totalCounts[i]++;
		for (int i = 0; i <= (int) otherDist; i++)
			otherCounts[i]++;
		for (int i = 0; i <= (int) carDist; i++)
			carCounts[i]++;
		for (int i = 0; i <= (int) ptDist; i++)
			ptCounts[i]++;
	}

	public void write(String outputFilename) {
		double sum = carDist + ptDist + otherDist;
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("\tDaytime Distance\t(exkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + carDist / (double) count + "\t" + carDist / sum
				* 100.0);
		sw.writeln("pt\t" + ptDist / (double) count + "\t" + ptDist / sum
				* 100.0);
		sw.writeln("through\t" + otherDist / (double) count + "\t" + otherDist
				/ sum * 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaytime Distance\t(inkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + (carDist + otherDist) / (double) count + "\t"
				+ (carDist + otherDist) / sum * 100.0);
		sw.writeln("pt\t" + ptDist / (double) count + "\t" + ptDist / sum
				* 100.0);
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
		XYLineChart chart = new XYLineChart("Daytime Distance distribution",
				"Daytime Distance in km",
				"fraction of persons with daytime distance bigger than x... in %");
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
		final String outputFilename = "output/669daytimeDistance";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		DaytimeDistance dd = new DaytimeDistance();
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
