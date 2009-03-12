/**
 * 
 */
package playground.yu.utils.qgis;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.core.v01.Coord;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class ModeTextLayer4QGIS extends TextLayer4QGIS {

	/**
	 * @param textFilename
	 */
	public ModeTextLayer4QGIS(String textFilename) {
		super(textFilename);
		writer.writeln("mode");
	}

	public void run(Plan plan) {
		Coord homeLoc = plan.getFirstActivity().getCoord();
		String mode = "";
		if (PlanModeJudger.useCar(plan)) {
			mode = Mode.car.name();
		} else if (PlanModeJudger.usePt(plan)) {
			mode = Mode.pt.name();
		} else if (PlanModeJudger.useWalk(plan)) {
			mode = Mode.walk.name();
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
		final String textFilename = "../runs_SVN/run684/it.1000/1000.analysis/mode.txt";
		// final String netFilename = "../matsimTests/scoringTest/network.xml";
		// final String plansFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/100.plans.xml.gz";
		// final String textFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/mode.txt";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		ModeTextLayer4QGIS mtl = new ModeTextLayer4QGIS(textFilename);
		mtl.run(population);
		mtl.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
