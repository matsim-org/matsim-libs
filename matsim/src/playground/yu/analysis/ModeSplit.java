/**
 * 
 */
package playground.yu.analysis;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Coord;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ModeSplit extends AbstractPersonAlgorithm implements PlanAlgorithm {
	private int carUser = 0, ptUser = 0, walker = 0, zrhCarUser = 0,
			zrhPtUser = 0, zrhWalker = 0;
	private Coord center = null;

	public static boolean isInRange(Coord coord, Coord center, double radius) {
		return coord.calcDistance(center) < radius;
	}

	public ModeSplit(NetworkLayer network) {
		Node centerNode = network.getNode("2531");
		if (centerNode != null)
			center = centerNode.getCoord();
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		Coord homeLoc = plan.getFirstActivity().getCoord();
		if (PlanModeJudger.useCar(plan)) {
			carUser++;
			if (center != null)
				if (isInRange(homeLoc, center, 30000.0))
					zrhCarUser++;
		} else if (PlanModeJudger.usePt(plan)) {
			ptUser++;
			if (center != null)
				if (isInRange(homeLoc, center, 30000.0))
					zrhPtUser++;
		} else if (PlanModeJudger.useWalk(plan)) {
			walker++;
			if (center != null)
				if (isInRange(homeLoc, center, 30000.0))
					zrhWalker++;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(total)mode\tnumber\tfraction[%]\n");
		double sum = carUser + ptUser + walker;
		sb.append("car\t" + carUser + "\t" + ((double) carUser) / sum * 100.0
				+ "\n");
		sb.append("pt\t" + ptUser + "\t" + ((double) ptUser) / sum * 100.0
				+ "\n");
		sb.append("walk\t" + walker + "\t" + ((double) walker / sum * 100.0)
				+ "\n");

		if (center != null) {
			sum = zrhCarUser + zrhPtUser + zrhWalker;
			sb.append("(center30km)mode\tnumber\tfraction[%]\n");
			sb.append("car\t" + zrhCarUser + "\t" + (double) zrhCarUser / sum
					* 100.0 + "\n");
			sb.append("pt\t" + zrhPtUser + "\t" + (double) zrhPtUser / sum
					* 100.0 + "\n");
			sb.append("walk\t" + zrhWalker + "\t" + (double) zrhWalker / sum
					* 100.0 + "\n");
		}

		return sb.toString();
	}

	public void write(String outputPath) {
		SimpleWriter sw = new SimpleWriter(outputPath + "modalSplit.txt");
		sw.write(toString());
		sw.close();
		PieChart chart = new PieChart("ModalSplit -- agents");
		chart.addSeries(new String[] { "car", "pt", "walk" }, new double[] {
				carUser, ptUser, walker });
		chart.saveAsPng(outputPath + "modalSplit.png", 800, 600);
		if (center != null) {
			PieChart chart2 = new PieChart("ModalSplit Center(30km) -- agents");
			chart2.addSeries(new String[] { "car", "pt", "walk" },
					new double[] { zrhCarUser, zrhPtUser, zrhWalker });
			chart2.saveAsPng(outputPath + "modalSplit30km.png", 800, 600);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		final String outputPath = "../runs_SVN/run684/it.1000/1000.analysis/";
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

		ModeSplit ms = new ModeSplit(network);
		ms.run(population);
		ms.write(outputPath);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
