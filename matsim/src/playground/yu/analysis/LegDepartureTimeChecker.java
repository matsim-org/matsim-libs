/**
 *
 */
package playground.yu.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * check, whether the departure time of leg is later than 24:00
 * 
 * @author yu
 * 
 */
public class LegDepartureTimeChecker extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private SimpleWriter sw = null;
	private Id personId = null;

	public LegDepartureTimeChecker(final String outputFilename) {
		this.sw = new SimpleWriter(outputFilename);
		this.sw.writeln("personId\ttime[s]\ttime[hh:mm:ss]\tlegNo.");
	}

	@Override
	public void run(final Person person) {
		this.personId = person.getId();
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				double legDepTime = ((Leg) pe).getDepartureTime();
				if (legDepTime >= 86400.0) {
					this.sw.writeln(this.personId + "\t" + legDepTime + "\t"
							+ Time.writeTime(legDepTime) + "\t" + c);
					this.sw.flush();
				}
				c++;
			}
		}
	}

	public void close() {
		this.sw.close();
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String plansFilename =
		// "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		final String plansFilename = "../runs/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "output/legDepTime_669.1000.txt";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();
		LegDepartureTimeChecker fldtc = new LegDepartureTimeChecker(
				outputFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);
		fldtc.run(population);

		fldtc.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
