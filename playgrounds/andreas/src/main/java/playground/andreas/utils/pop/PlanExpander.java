package playground.andreas.utils.pop;

import java.io.File;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.andreas.bln.pop.SharedNetScenario;

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

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(plansFile + ".xml.gz");

		DuplicatePlans dp = new DuplicatePlans(net, inPop, "tmp.xml.gz", numberOfAdditionalCopies);
		dp.run(inPop);
		dp.writeEndPlans();

		System.out.println("Dublicating plans finished");
		Gbl.printElapsedTime();

		inPop = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		popReader = new MatsimPopulationReader(new SharedNetScenario(sc, inPop));
		popReader.readFile("tmp.xml.gz");

		ShuffleCoords shuffleCoords = new ShuffleCoords(net, inPop, plansFile + "_" + (numberOfAdditionalCopies + 1) + "x.xml.gz", radiusOfPerimeter, TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.DHDN_GK4));
		shuffleCoords.setChangeHomeActsOnlyOnceTrue("home");
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();

		(new File("tmp.xml.gz")).deleteOnExit();

		Gbl.printElapsedTime();

	}

}
