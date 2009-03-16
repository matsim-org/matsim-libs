/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ModeSplit extends AbstractPersonAlgorithm implements PlanAlgorithm {
	private int carUser = 0, ptUser = 0, walker = 0, zrhCarUser = 0,
			zrhPtUser = 0, zrhWalker = 0;
	private RoadPricingScheme toll = null;

	public ModeSplit(RoadPricingScheme toll) {
		this.toll = toll;
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		Link homeLoc = plan.getFirstActivity().getLink();
		if (PlanModeJudger.useCar(plan)) {
			carUser++;
			if (toll != null)
				if (TollTools.isInRange(homeLoc, toll))
					zrhCarUser++;
		} else if (PlanModeJudger.usePt(plan)) {
			ptUser++;
			if (toll != null)
				if (TollTools.isInRange(homeLoc, toll))
					zrhPtUser++;
		} else if (PlanModeJudger.useWalk(plan)) {
			walker++;
			if (toll != null)
				if (TollTools.isInRange(homeLoc, toll))
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

		if (toll != null) {
			sum = zrhCarUser + zrhPtUser + zrhWalker;
			sb.append("(toll area)mode\tnumber\tfraction[%]\n");
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
		if (toll != null) {
			PieChart chart2 = new PieChart(
					"ModalSplit Center(toll area) -- agents");
			chart2.addSeries(new String[] { "car", "pt", "walk" },
					new double[] { zrhCarUser, zrhPtUser, zrhWalker });
			chart2.saveAsPng(outputPath + "modalSplitToll.png", 800, 600);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		final String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";
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

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(network);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ModeSplit ms = new ModeSplit(tollReader.getScheme());
		ms.run(population);
		ms.write(outputPath);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
