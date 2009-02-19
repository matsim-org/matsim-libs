package playground.yu.visum.test;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;

import playground.yu.visum.filter.ActTypeFilter;
import playground.yu.visum.filter.DepTimeFilter;
import playground.yu.visum.filter.PersonFilterAlgorithm;
import playground.yu.visum.filter.PersonIDFilter;
import playground.yu.visum.filter.finalFilters.PersonIDsExporter;

/**
 * @author ychen
 */
public class PersonFilterTest {

	public static void testRunIDandActTypeundDepTimeFilter() {

		System.out.println("TEST RUN ---FilterTest---:");
		// reading all available input
		System.out.println("  creating network layer... ");
		NetworkLayer network = new NetworkLayer();
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network()
				.getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Population plans = new Population(Population.USE_STREAMING);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonIDsExporter pid = new PersonIDsExporter();
		DepTimeFilter dtf = new DepTimeFilter();
		ActTypeFilter atf = new ActTypeFilter();
		PersonIDFilter idf = new PersonIDFilter(10);
		PersonFilterAlgorithm pfa = new PersonFilterAlgorithm();
		pfa.setNextFilter(idf);
		idf.setNextFilter(atf);
		atf.setNextFilter(dtf);
		dtf.setNextFilter(pid);
		plans.addAlgorithm(pfa);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans,
				network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		plans.runAlgorithms();
		System.out.println("we have " + pfa.getCount()
				+ "persons at last -- FilterAlgorithm");
		System.out.println("we have " + idf.getCount()
				+ "persons at last -- PersonIDFilter");
		System.out.println("we have " + atf.getCount()
				+ "persons at last -- ActTypeFilter");
		System.out.println("we have " + dtf.getCount()
				+ "persons at last -- DepTimeFilter");
		System.out.println("  done.");
		// writing all available input

		System.out.println("PersonFiterTEST SUCCEEDED.");
		System.out.println();
	}

	/**
	 * @param args
	 *            test/yu/config_hms.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRunIDandActTypeundDepTimeFilter();
		Gbl.printElapsedTime();
	}
}
