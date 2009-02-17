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
 * @author yu
 * 
 */
public class EnRouteTime extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private int count;
	private double carTime, ptTime, otherTime;
	private Person person;

	public EnRouteTime() {
		count = 0;
		carTime = 0.0;
		ptTime = 0.0;
		otherTime = 0.0;
	}

	@Override
	public void run(Person person) {
		this.person = person;
		count++;
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg bl = (Leg) li.next();
			double time = bl.getTravelTime() / 60.0;
			if (bl.getDepartureTime() < 86400)
				if (Long.parseLong(person.getId().toString()) > 1000000000)
					otherTime += time;
				else if (bl.getMode().equals(Mode.car))
					carTime += time;
				else if (bl.getMode().equals(Mode.pt))
					ptTime += time;
		}
	}

	public void write(String outputFilename) {
		double sum = carTime + ptTime + otherTime;
		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("\tEn Route Time\t(exkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + carTime / (double) count + "\t" + carTime / sum
				* 100.0);
		sw.writeln("pt\t" + ptTime / count + "\t" + ptTime / sum * 100.0);
		sw.writeln("through\t" + otherTime / count + "\t" + otherTime / sum
				* 100.0);
		sw.writeln("--------------------------------------------");
		sw.writeln("\tEn Route Time\t(inkl. through-traffic)");
		sw.writeln("\tmin\t%");
		sw.writeln("car\t" + (carTime + otherTime) / (double) count + "\t"
				+ (carTime + otherTime) / sum * 100.0);
		sw.writeln("pt\t" + ptTime / count + "\t" + ptTime / sum * 100.0);
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
		final String outputFilename = "output/669enRouteTime.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		EnRouteTime ert = new EnRouteTime();
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
