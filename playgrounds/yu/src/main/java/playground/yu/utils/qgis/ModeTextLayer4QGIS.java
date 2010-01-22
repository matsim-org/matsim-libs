/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class ModeTextLayer4QGIS extends TextLayer4QGIS {
	/**
	 *dummy constructor, please don't use it.
	 */
	public ModeTextLayer4QGIS() {

	}

	/**
	 * @param textFilename
	 */
	public ModeTextLayer4QGIS(String textFilename) {
		super(textFilename);
		writer.writeln("mode");
	}

	public ModeTextLayer4QGIS(String textFilename, RoadPricingScheme toll) {
		super(textFilename, toll);
		writer.writeln("mode");
	}

	public void run(Plan plan) {
		Coord homeLoc = ((PlanImpl) plan).getFirstActivity().getCoord();
		String mode = "";
		if (PlanModeJudger.useCar(plan)) {
			mode = TransportMode.car.name();
		} else if (PlanModeJudger.usePt(plan)) {
			mode = TransportMode.pt.name();
		} else if (PlanModeJudger.useWalk(plan)) {
			mode = TransportMode.walk.name();
		}
		writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		final String textFilename = "../runs_SVN/run684/it.1000/1000.analysis/mode_Kanton.txt";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich.xml";
		// final String netFilename = "../matsimTests/scoringTest/network.xml";
		// final String plansFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/100.plans.xml.gz";
		// final String textFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/mode.txt";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1();
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ModeTextLayer4QGIS mtl = new ModeTextLayer4QGIS(textFilename,
				tollReader.getScheme());
		mtl.run(population);
		mtl.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
