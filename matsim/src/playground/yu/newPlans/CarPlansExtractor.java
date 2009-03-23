/**
 * 
 */
package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class CarPlansExtractor extends NewPopulation implements PlanAlgorithm {
	private Person person;
	private List<Plan> tmpPersonPlans = new ArrayList<Plan>();

	/**
	 * @param plans
	 */
	public CarPlansExtractor(Population plans) {
		super(plans);
	}

	/**
	 * @param population
	 * @param filename
	 */
	public CarPlansExtractor(Population population, String filename) {
		super(population, filename);
	}

	@Override
	public void run(Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			this.person = person;
			tmpPersonPlans.addAll(person.getPlans());
			for (Plan pl : tmpPersonPlans)
				run(pl);
			tmpPersonPlans.clear();
		}
		pw.writePerson(person);
	}

	public void run(Plan plan) {
		if (PlanModeJudger.usePt(plan)) {
			person.getPlans().remove(plan);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String outputFilename = args[2];

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl(true);

		CarPlansExtractor cpe = new CarPlansExtractor(population,
				outputFilename);
		population.addAlgorithm(cpe);
		new MatsimPopulationReader(population, network).readFile(plansFilename);
		population.runAlgorithms();

		// cpe.run(population);
		cpe.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
