/**
 * 
 */
package playground.yu.newPlans;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author yu
 * 
 */
public class InitialHomeEndTime extends NewPlan implements PlanAlgorithm {

	/**
	 * @param population
	 * @param filename
	 */
	public InitialHomeEndTime(Population population, String filename) {
		super(population, filename);
	}

	@Override
	public void run(Person person) {
		for (Plan pl : person.getPlans())
			run(pl);
		pw.writePerson(person);
	}

	public void run(Plan plan) {
		plan.getFirstActivity().setEndTime(21600.0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		final String outputPlansFilename = "output/plans_all_zrh30km_transitincl_10pct_home_end_6h.xml.gz";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();
		InitialHomeEndTime ihet = new InitialHomeEndTime(population,
				outputPlansFilename);

		population.addAlgorithm(ihet);

		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		population.runAlgorithms();

		ihet.writeEndPlans();
	}

}
