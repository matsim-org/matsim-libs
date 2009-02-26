/**
 *
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.misc.Time;

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

	/**
	 *
	 */
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
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			double legDepTime = li.next().getDepartureTime();
			if (legDepTime >= 86400.0) {
				this.sw.writeln(this.personId + "\t" + legDepTime + "\t"
						+ Time.writeTime(legDepTime) + "\t" + c);
				try {
					this.sw.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			c++;
		}
	}

	public void close() {
		try {
			this.sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String plansFilename =
		// "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		final String plansFilename = "../runs/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "output/legDepTime_669.1000.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		LegDepartureTimeChecker fldtc = new LegDepartureTimeChecker(
				outputFilename);
		population.addAlgorithm(fldtc);
		new MatsimPopulationReader(population, network).readFile(plansFilename);
		population.runAlgorithms();

		fldtc.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
