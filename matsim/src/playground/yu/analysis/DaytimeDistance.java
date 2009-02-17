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

import playground.yu.utils.SimpleWriter;

/**
 * compute modal split of daytime distance
 * 
 * @author yu
 * 
 */
public class DaytimeDistance extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private double carDist, ptDist, otherDist;
	private int count;
	private Person person;

	public DaytimeDistance() {
		carDist = 0.0;
		ptDist = 0.0;
		otherDist = 0.0;
		count = 0;
	}

	public void run(Person person) {
		this.person = person;
		count++;
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			double dist = bl.getRoute().getDist();
			if (bl.getDepartureTime() < 86400)
				if (Long.parseLong(person.getId().toString()) > 1000000000)
					otherDist += dist;
				else if (bl.getMode().equals(Mode.car))
					carDist += dist;
				else if (bl.getMode().equals(Mode.pt))
					ptDist += dist;
		}
	}

	public void write(String outputFilename) {
		double sum = carDist + ptDist + otherDist;
		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("\tDaytime Distance\t(exkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + carDist / (double) count / 1000.0 + "\t" + carDist
				/ sum * 100.0);
		sw.writeln("pt\t" + ptDist / count / 1000.0 + "\t" + ptDist / sum
				* 100.0);
		sw.writeln("through\t" + otherDist / count / 1000.0 + "\t" + otherDist
				/ sum * 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tDaytime Distance\t(inkl. through-traffic)");
		sw.writeln("\tkm\t%");
		sw.writeln("car\t" + (carDist + otherDist) / (double) count / 1000.0
				+ "\t" + (carDist + otherDist) / sum * 100.0);
		sw.writeln("pt\t" + ptDist / count / 1000.0 + "\t" + ptDist / sum
				* 100.0);
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "output/669daytimeDistance.txt";

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
