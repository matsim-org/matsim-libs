package playground.andreas.bln.pop;

import java.io.File;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * Combines DuplicatePlans and ShuffleCoords to<br>
 *  - first - expand a given plan by a certain number of clones<br>
 *  - second - alternate the coords of the clones, so that new coord is in a perimeter
 *  with radius specified and new coords are equally distributed within that perimeter.
 *
 * @author aneumann
 *
 */
public class PlanExpander {

	public static void main(String[] args) {

		String networkFile = "./bb_cl.xml.gz";
		String plansFile = "./baseplan";
		int numberOfAdditionalCopies = 9;
		double radiusOfPerimeter = 1000.0;

		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		PopulationImpl inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(plansFile + ".xml.gz");

		DuplicatePlans dp = new DuplicatePlans(net, inPop, "tmp.xml.gz", numberOfAdditionalCopies);
		dp.run(inPop);
		dp.writeEndPlans();

		System.out.println("Dublicating plans finished");
		Gbl.printElapsedTime();

		inPop = new ScenarioImpl().getPopulation();
		popReader = new MatsimPopulationReader(new SharedNetScenario(sc, inPop));
		popReader.readFile("tmp.xml.gz");

		ShuffleCoords shuffleCoords = new ShuffleCoords(net, inPop, plansFile + "_" + (numberOfAdditionalCopies + 1) + "x.xml.gz", radiusOfPerimeter);
		shuffleCoords.setChangeHomeActsOnlyOnceTrue("home");
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();

		(new File("tmp.xml.gz")).deleteOnExit();

		Gbl.printElapsedTime();

	}

}
